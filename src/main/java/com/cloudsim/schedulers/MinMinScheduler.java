package com.cloudsim.schedulers;

import com.cloudsim.utils.SimulationConfig;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

import java.util.List;

/**
 * Min-Min Scheduler
 *
 * Iteratively selects the cloudlet with the globally minimum
 * completion time and assigns it to its best VM.
 *
 * Steps per iteration:
 *  1. Build CT[i][j] = vmLoad[j] + execTime(i,j)
 *  2. Pick (cloudlet, VM) pair with minimum CT
 *  3. Assign, update vmLoad, repeat
 */
public class MinMinScheduler extends BaseScheduler {

    public MinMinScheduler() {
        super("Min-Min");
    }

    @Override
    protected void scheduleCloudletsToVms(List<Cloudlet> cloudlets, List<Vm> vms) {
        int n = cloudlets.size();
        int m = vms.size();

        double[] vmLoad  = new double[m];
        boolean[] done   = new boolean[n];
        int assigned     = 0;

        while (assigned < n) {
            int    bestC  = -1;
            int    bestV  = -1;
            double bestCT = Double.MAX_VALUE;

            for (int i = 0; i < n; i++) {
                if (done[i]) continue;
                double mi = cloudlets.get(i).getCloudletLength();
                for (int j = 0; j < m; j++) {
                    double ct = vmLoad[j] + mi / SimulationConfig.VM_MIPS;
                    if (ct < bestCT) { bestCT = ct; bestC = i; bestV = j; }
                }
            }

            if (bestC >= 0) {
                cloudlets.get(bestC).setVmId(vms.get(bestV).getId());
                vmLoad[bestV] += cloudlets.get(bestC).getCloudletLength()
                                 / (double) SimulationConfig.VM_MIPS;
                done[bestC] = true;
                assigned++;
            }
        }
    }
}
