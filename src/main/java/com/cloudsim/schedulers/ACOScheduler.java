package com.cloudsim.schedulers;

import com.cloudsim.utils.SimulationConfig;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

import java.util.List;
import java.util.Random;

/**
 * Ant Colony Optimization (ACO) Scheduler
 *
 * Ants build solutions by probabilistically choosing VMs for each cloudlet
 * using pheromone (τ) and heuristic (η = 1/execTime) information.
 *
 * P(i,j) = [τ(i,j)^α × η(i,j)^β] / Σ_k [τ(i,k)^α × η(i,k)^β]
 *
 * After all ants finish an iteration, pheromones evaporate and are
 * re-deposited proportional to solution quality (1/makespan).
 */
public class ACOScheduler extends BaseScheduler {

    private static final double ALPHA      = 1.0;
    private static final double BETA       = 2.0;
    private static final double RHO        = 0.5;
    private static final double Q          = 100.0;
    private static final int    NUM_ANTS   = 10;
    private static final int    ITERATIONS = 50;

    private final Random random = new Random(42L);

    public ACOScheduler() {
        super("Ant Colony Optimization (ACO)");
    }

    @Override
    protected void scheduleCloudletsToVms(List<Cloudlet> cloudlets, List<Vm> vms) {
        int n = cloudlets.size();
        int m = vms.size();

        double[][] execTime  = buildExecTimeMatrix(cloudlets, m);
        double[][] pheromone = new double[n][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                pheromone[i][j] = 1.0;

        int[]  bestAssignment = new int[n];
        double bestMakespan   = Double.MAX_VALUE;

        for (int iter = 0; iter < ITERATIONS; iter++) {
            int[][]  solutions = new int[NUM_ANTS][n];
            double[] makespans = new double[NUM_ANTS];

            for (int ant = 0; ant < NUM_ANTS; ant++) {
                solutions[ant] = constructSolution(n, m, pheromone, execTime);
                makespans[ant]  = computeMakespan(solutions[ant], n, m, execTime);
            }

            // Evaporate
            for (int i = 0; i < n; i++)
                for (int j = 0; j < m; j++)
                    pheromone[i][j] *= (1 - RHO);

            // Deposit
            for (int ant = 0; ant < NUM_ANTS; ant++) {
                double deposit = Q / makespans[ant];
                for (int i = 0; i < n; i++)
                    pheromone[i][solutions[ant][i]] += deposit;

                if (makespans[ant] < bestMakespan) {
                    bestMakespan = makespans[ant];
                    System.arraycopy(solutions[ant], 0, bestAssignment, 0, n);
                }
            }
        }

        for (int i = 0; i < n; i++)
            cloudlets.get(i).setVmId(vms.get(bestAssignment[i]).getId());
    }

    private int[] constructSolution(int n, int m, double[][] ph, double[][] et) {
        int[] sol = new int[n];
        for (int i = 0; i < n; i++) {
            double[] prob  = new double[m];
            double   total = 0;
            for (int j = 0; j < m; j++) {
                double eta = 1.0 / (et[i][j] + 1e-10);
                prob[j] = Math.pow(ph[i][j], ALPHA) * Math.pow(eta, BETA);
                total  += prob[j];
            }
            double r   = random.nextDouble() * total;
            double cum = 0;
            sol[i] = m - 1;
            for (int j = 0; j < m; j++) {
                cum += prob[j];
                if (r <= cum) { sol[i] = j; break; }
            }
        }
        return sol;
    }

    private double computeMakespan(int[] assignment, int n, int m, double[][] et) {
        double[] load = new double[m];
        for (int i = 0; i < n; i++) load[assignment[i]] += et[i][assignment[i]];
        double max = 0;
        for (double l : load) if (l > max) max = l;
        return max;
    }

    private double[][] buildExecTimeMatrix(List<Cloudlet> cloudlets, int m) {
        int n = cloudlets.size();
        double[][] mat = new double[n][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                mat[i][j] = cloudlets.get(i).getCloudletLength()
                             / (double) SimulationConfig.VM_MIPS;
        return mat;
    }
}
