package com.cloudsim.schedulers;

import com.cloudsim.utils.SimulationConfig;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

import java.util.Arrays;
import java.util.List;

/**
 * Priority-Based Scheduler
 *
 * Scores each cloudlet:
 *   priority = 0.7 × (1 - normalizedLength) + 0.3 × (1 - normalizedOrder)
 * Higher score → processed first.
 * Each cloudlet is assigned to the least-loaded VM at the time of assignment.
 */
public class PriorityScheduler extends BaseScheduler {

    public PriorityScheduler() {
        super("Priority-Based");
    }

    @Override
    protected void scheduleCloudletsToVms(List<Cloudlet> cloudlets, List<Vm> vms) {
        int n = cloudlets.size();
        int m = vms.size();

        double maxLen = 0;
        for (Cloudlet c : cloudlets)
            if (c.getCloudletLength() > maxLen) maxLen = c.getCloudletLength();

        double[] priority = new double[n];
        for (int i = 0; i < n; i++) {
            double normLen   = 1.0 - cloudlets.get(i).getCloudletLength() / maxLen;
            double normOrder = 1.0 - (double) i / n;
            priority[i]      = 0.7 * normLen + 0.3 * normOrder;
        }

        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(priority[b], priority[a]));

        double[] vmLoad = new double[m];
        for (int id : idx) {
            int best = 0;
            for (int j = 1; j < m; j++)
                if (vmLoad[j] < vmLoad[best]) best = j;

            cloudlets.get(id).setVmId(vms.get(best).getId());
            vmLoad[best] += cloudlets.get(id).getCloudletLength()
                            / (double) SimulationConfig.VM_MIPS;
        }
    }
}
