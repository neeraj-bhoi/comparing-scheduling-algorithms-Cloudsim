package com.cloudsim.schedulers;

import com.cloudsim.utils.SimulationConfig;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

import java.util.List;

/**
 * Max-Min Scheduler
 *
 * For each iteration:
 *  1. For each unassigned cloudlet find its minimum CT (best VM)
 *  2. Select the cloudlet with the MAXIMUM of those minimums
 *  3. Assign it to the corresponding VM, update load
 */
public class MaxMinScheduler extends BaseScheduler {

    public MaxMinScheduler() {
        super("Max-Min");
    }

    @Override
    protected void scheduleCloudletsToVms(List<Cloudlet> cloudlets, List<Vm> vms) {
        int n = cloudlets.size();
        int m = vms.size();

        double[] vmLoad  = new double[m];
        boolean[] done   = new boolean[n];
        int assigned     = 0;

        while (assigned < n) {
            double[] minCT     = new double[n];
            int[]    bestVmFor = new int[n];

            for (int i = 0; i < n; i++) {
                if (done[i]) { minCT[i] = -1; continue; }
                double mi  = cloudlets.get(i).getCloudletLength();
                double min = Double.MAX_VALUE;
                int bv     = 0;
                for (int j = 0; j < m; j++) {
                    double ct = vmLoad[j] + mi / SimulationConfig.VM_MIPS;
                    if (ct < min) { min = ct; bv = j; }
                }
                minCT[i]     = min;
                bestVmFor[i] = bv;
            }

            // Pick cloudlet with MAXIMUM of its minimum CTs
            int    selC  = -1;
            double maxOf = -1;
            for (int i = 0; i < n; i++) {
                if (!done[i] && minCT[i] > maxOf) { maxOf = minCT[i]; selC = i; }
            }

            if (selC >= 0) {
                int selV = bestVmFor[selC];
                cloudlets.get(selC).setVmId(vms.get(selV).getId());
                vmLoad[selV] += cloudlets.get(selC).getCloudletLength()
                                / (double) SimulationConfig.VM_MIPS;
                done[selC] = true;
                assigned++;
            }
        }
    }
}
