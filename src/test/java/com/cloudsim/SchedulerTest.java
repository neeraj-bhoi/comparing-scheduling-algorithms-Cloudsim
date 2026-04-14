package com.cloudsim;

import com.cloudsim.results.SimulationResult;
import com.cloudsim.schedulers.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SchedulerTest {

    @Test public void testRoundRobin()  { assertValid(new RoundRobinScheduler().run()); }
    @Test public void testFCFS()        { assertValid(new FCFSScheduler().run()); }
    @Test public void testMinMin()      { assertValid(new MinMinScheduler().run()); }
    @Test public void testMaxMin()      { assertValid(new MaxMinScheduler().run()); }
    @Test public void testACO()         { assertValid(new ACOScheduler().run()); }
    @Test public void testPriority()    { assertValid(new PriorityScheduler().run()); }

    private void assertValid(SimulationResult r) {
        assertNotNull(r);
        assertTrue("makespan > 0",    r.getMakespan()    > 0);
        assertTrue("throughput > 0",  r.getThroughput()  > 0);
        assertTrue("completed > 0",   r.getCompletedCloudlets() > 0);
    }
}
