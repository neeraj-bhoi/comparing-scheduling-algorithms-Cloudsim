package com.cloudsim.experiments;

import com.cloudsim.results.ChartGenerator;
import com.cloudsim.results.DashboardExporter;
import com.cloudsim.results.ResultsExporter;
import com.cloudsim.results.SimulationResult;
import com.cloudsim.schedulers.*;
import com.cloudsim.utils.SimulationConfig;
import com.cloudsim.utils.StatisticalAnalyzer;

import java.util.ArrayList;
import java.util.List;

/**
 * MainExperiment.java — Entry point
 *
 * Runs all 6 scheduling algorithms, prints results to console,
 * exports CSV + text report, and generates bar charts.
 *
 * Run with:
 *   mvn exec:java -Dexec.mainClass="com.cloudsim.experiments.MainExperiment"
 */
public class MainExperiment {

    public static void main(String[] args) {
        printBanner();

        System.out.println("Simulation Configuration:");
        System.out.println("  Hosts       : " + SimulationConfig.NUM_HOSTS);
        System.out.println("  VMs         : " + SimulationConfig.NUM_VMS);
        System.out.println("  Cloudlets   : " + SimulationConfig.NUM_CLOUDLETS);
        System.out.println("  VM MIPS     : " + SimulationConfig.VM_MIPS);
        System.out.println("  Host PEs    : " + SimulationConfig.HOST_PES);
        System.out.println();

        List<BaseScheduler> schedulers = new ArrayList<BaseScheduler>();
        schedulers.add(new RoundRobinScheduler());
        schedulers.add(new FCFSScheduler());
        schedulers.add(new MinMinScheduler());
        schedulers.add(new MaxMinScheduler());
        schedulers.add(new ACOScheduler());
        schedulers.add(new PriorityScheduler());

        List<SimulationResult> results = new ArrayList<SimulationResult>();

        for (BaseScheduler scheduler : schedulers) {
            System.out.println("Running: " + scheduler.getAlgorithmName() + " ...");
            SimulationResult result = scheduler.run();
            results.add(result);
            System.out.printf("  Done. Makespan=%.4fs  Throughput=%.4f tasks/s%n",
                result.getMakespan(), result.getThroughput());
        }

        System.out.println("\n" + "-".repeat(60));
        System.out.println("RESULTS");
        System.out.println("-".repeat(60));

        for (SimulationResult r : results) System.out.println(r);

        ResultsExporter.printComparisonTable(results);
        ResultsExporter.printBestAlgorithmSummary(results);
        StatisticalAnalyzer.printRankings(results);

        System.out.println("\nExporting files...");
        ResultsExporter.exportToCsv(results);
        ResultsExporter.exportTextReport(results);
        DashboardExporter.export(results);

        try {
            ChartGenerator.generateAllCharts(results);
        } catch (Exception e) {
            System.err.println("Chart generation failed: " + e.getMessage());
        }

        System.out.println("\nDone! Check the '" + SimulationConfig.OUTPUT_DIR + "' folder for results.");
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                  ║");
        System.out.println("║    COMPARATIVE ANALYSIS OF VM SCHEDULING ALGORITHMS             ║");
        System.out.println("║    IN CLOUD COMPUTING USING CLOUDSIM                            ║");
        System.out.println("║                                                                  ║");
        System.out.println("║    Algorithms: Round Robin | FCFS | Min-Min | Max-Min           ║");
        System.out.println("║               ACO | Priority-Based                             ║");
        System.out.println("║                                                                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
