package com.cloudsim.schedulers;

import com.cloudsim.results.SimulationResult;
import com.cloudsim.utils.SimulationConfig;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * BaseScheduler.java
 *
 * Abstract base class using the original CloudSim 3.0.3 API
 * (package org.cloudbus.cloudsim).
 *
 * Handles datacenter, host, VM, and cloudlet creation.
 * Each subclass implements scheduleCloudletsToVms() with its own algorithm.
 */
public abstract class BaseScheduler {

    protected String algorithmName;

    public BaseScheduler(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    /**
     * Runs the full simulation and returns collected metrics.
     */
    public SimulationResult run() {
        try {
            // Suppress CloudSim internal logs for clean output
            Log.disable();

            // 1. Initialise CloudSim
            int numUsers = 1;
            Calendar calendar = Calendar.getInstance();
            CloudSim.init(numUsers, calendar, false);

            // 2. Create Datacenter
            Datacenter datacenter = createDatacenter("Datacenter_0");

            // 3. Create Broker
            DatacenterBroker broker = new DatacenterBroker("Broker_0");

            // 4. Create VMs
            List<Vm> vmList = createVms(broker.getId());

            // 5. Create Cloudlets
            List<Cloudlet> cloudletList = createCloudlets(broker.getId());

            // 6. Apply scheduling algorithm (subclass-specific)
            scheduleCloudletsToVms(cloudletList, vmList);

            // 7. Submit to broker
            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            // 8. Start simulation
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // 9. Collect results
            List<Cloudlet> finished = broker.getCloudletReceivedList();
            return collectResults(finished, vmList);

        } catch (Exception e) {
            System.err.println("Simulation error in " + algorithmName + ": " + e.getMessage());
            e.printStackTrace();
            return new SimulationResult(algorithmName);
        }
    }

    /**
     * Subclasses implement their scheduling logic here.
     * Must call cloudlet.setVmId(vm.getId()) for each cloudlet.
     */
    protected abstract void scheduleCloudletsToVms(List<Cloudlet> cloudlets, List<Vm> vms);

    // ── Infrastructure Creation ───────────────────────────────────────────────

    protected Datacenter createDatacenter(String name) throws Exception {
        List<Host> hostList = new ArrayList<Host>();

        for (int i = 0; i < SimulationConfig.NUM_HOSTS; i++) {
            List<Pe> peList = new ArrayList<Pe>();
            for (int j = 0; j < SimulationConfig.HOST_PES; j++) {
                peList.add(new Pe(j, new PeProvisionerSimple(SimulationConfig.HOST_MIPS)));
            }

            hostList.add(new Host(
                i,
                new RamProvisionerSimple(SimulationConfig.HOST_RAM),
                new BwProvisionerSimple(SimulationConfig.HOST_BW),
                SimulationConfig.HOST_STORAGE,
                peList,
                new VmSchedulerTimeShared(peList)
            ));
        }

        String arch      = "x86";
        String os        = "Linux";
        String vmm       = "Xen";
        double timeZone  = 10.0;
        double costPerSec    = 3.0;
        double costPerMem    = 0.05;
        double costPerStorage = 0.001;
        double costPerBw     = 0.0;

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
            arch, os, vmm, hostList, timeZone,
            costPerSec, costPerMem, costPerStorage, costPerBw
        );

        return new Datacenter(name, characteristics,
            new VmAllocationPolicySimple(hostList),
            new LinkedList<Storage>(), 0);
    }

    protected List<Vm> createVms(int brokerId) {
        List<Vm> list = new ArrayList<Vm>();
        for (int i = 0; i < SimulationConfig.NUM_VMS; i++) {
            list.add(new Vm(
                i,
                brokerId,
                SimulationConfig.VM_MIPS,
                SimulationConfig.VM_PES,
                SimulationConfig.VM_RAM,
                SimulationConfig.VM_BW,
                SimulationConfig.VM_SIZE,
                "Xen",
                new CloudletSchedulerTimeShared()
            ));
        }
        return list;
    }

    protected List<Cloudlet> createCloudlets(int brokerId) {
        List<Cloudlet> list = new ArrayList<Cloudlet>();
        UtilizationModelFull um = new UtilizationModelFull();

        for (int i = 0; i < SimulationConfig.NUM_CLOUDLETS; i++) {
            // Vary lengths to simulate heterogeneous workloads
            long length = SimulationConfig.CLOUDLET_LENGTH + (i % 5) * 2000L;

            Cloudlet cloudlet = new Cloudlet(
                i,
                length,
                SimulationConfig.CLOUDLET_PES,
                SimulationConfig.CLOUDLET_FILE_SIZE,
                SimulationConfig.CLOUDLET_OUTPUT_SIZE,
                um, um, um
            );
            cloudlet.setUserId(brokerId);
            list.add(cloudlet);
        }
        return list;
    }

    // ── Metrics Collection ────────────────────────────────────────────────────

    protected SimulationResult collectResults(List<Cloudlet> finished, List<Vm> vmList) {
        SimulationResult result = new SimulationResult(algorithmName);
        result.setTotalCloudlets(SimulationConfig.NUM_CLOUDLETS);
        result.setCompletedCloudlets(finished.size());

        if (finished.isEmpty()) return result;

        double totalExec      = 0;
        double totalWait      = 0;
        double totalTurnaround = 0;
        double totalResponse  = 0;
        double maxFinish      = 0;

        // Per-VM accumulated load (for load balance index)
        double[] vmLoad = new double[SimulationConfig.NUM_VMS];

        for (Cloudlet c : finished) {
            double exec     = c.getActualCPUTime();
            double start    = c.getExecStartTime();
            double finish   = c.getFinishTime();
            double submit   = c.getSubmissionTime();
            double wait     = start - submit;
            if (wait < 0) wait = 0;

            totalExec       += exec;
            totalWait       += wait;
            totalTurnaround += (finish - submit);
            totalResponse   += wait;
            if (finish > maxFinish) maxFinish = finish;

            int vmId = c.getVmId();
            if (vmId >= 0 && vmId < vmLoad.length) {
                vmLoad[vmId] += exec;
            }
        }

        int n = finished.size();
        result.setMakespan(maxFinish);
        result.setAvgExecutionTime(totalExec / n);
        result.setAvgWaitingTime(totalWait / n);
        result.setAvgTurnaroundTime(totalTurnaround / n);
        result.setAvgResponseTime(totalResponse / n);
        result.setThroughput(maxFinish > 0 ? (double) n / maxFinish : 0);

        // Simplified CPU utilization estimate
        double totalLoad = 0;
        for (double l : vmLoad) totalLoad += l;
        double totalCapacity = SimulationConfig.NUM_VMS * maxFinish;
        double cpuUtil = totalCapacity > 0 ? (totalLoad / totalCapacity) * 100.0 : 0;
        result.setAvgCpuUtilization(Math.min(cpuUtil, 100.0));

        // Load balance index = std deviation of VM loads
        result.setLoadBalanceIndex(stdDev(vmLoad));

        // Energy: simplified linear model
        double energy = SimulationConfig.NUM_HOSTS
            * (200 + 300 * (cpuUtil / 100.0))
            * (maxFinish / 3600.0);
        result.setTotalEnergyConsumption(energy);

        return result;
    }

    protected double stdDev(double[] values) {
        double sum = 0, sumSq = 0;
        int n = values.length;
        for (double v : values) { sum += v; sumSq += v * v; }
        double mean = sum / n;
        double variance = (sumSq / n) - (mean * mean);
        return Math.sqrt(Math.max(variance, 0));
    }

    public String getAlgorithmName() { return algorithmName; }
}
