package com.cloudsim.schedulers;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

import java.util.List;

/**
 * First Come First Served (FCFS) Scheduler
 * Fills VMs sequentially in arrival order.
 */
public class FCFSScheduler extends BaseScheduler {

    public FCFSScheduler() {
        super("First Come First Served (FCFS)");
    }

    @Override
    protected void scheduleCloudletsToVms(List<Cloudlet> cloudlets, List<Vm> vms) {
        int cloudletsPerVm = (int) Math.ceil((double) cloudlets.size() / vms.size());
        int vmIndex = 0;
        int countForCurrentVm = 0;

        for (int i = 0; i < cloudlets.size(); i++) {
            if (countForCurrentVm >= cloudletsPerVm && vmIndex < vms.size() - 1) {
                vmIndex++;
                countForCurrentVm = 0;
            }
            cloudlets.get(i).setVmId(vms.get(vmIndex).getId());
            countForCurrentVm++;
        }
    }
}
