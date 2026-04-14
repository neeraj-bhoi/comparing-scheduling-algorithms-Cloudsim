package com.cloudsim.schedulers;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

import java.util.List;

/**
 * Round Robin Scheduler
 * Assigns cloudlet i → VM (i % numVMs) in cyclic order.
 */
public class RoundRobinScheduler extends BaseScheduler {

    public RoundRobinScheduler() {
        super("Round Robin");
    }

    @Override
    protected void scheduleCloudletsToVms(List<Cloudlet> cloudlets, List<Vm> vms) {
        int vmCount = vms.size();
        for (int i = 0; i < cloudlets.size(); i++) {
            cloudlets.get(i).setVmId(vms.get(i % vmCount).getId());
        }
    }
}
