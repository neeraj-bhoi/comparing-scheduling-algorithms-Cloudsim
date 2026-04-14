package com.cloudsim.results;

import com.cloudsim.utils.SimulationConfig;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ResultsExporter {

    public static void printComparisonTable(List<SimulationResult> results) {
        System.out.println("\n");
        System.out.println("╔═════════════════════════════════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║              COMPARATIVE ANALYSIS OF VM SCHEDULING ALGORITHMS — RESULTS SUMMARY                       ║");
        System.out.println("╠══════════════════╦══════════╦═════════════╦═════════════╦════════════╦═══════════╦═══════════════════╣");
        System.out.printf("║ %-16s ║ %-8s ║ %-11s ║ %-11s ║ %-10s ║ %-9s ║ %-17s ║%n",
            "Algorithm", "Makespan", "AvgExec(s)", "AvgWait(s)", "Throughput", "CPU Util%", "LoadBalanceIdx");
        System.out.println("╠══════════════════╬══════════╬═════════════╬═════════════╬════════════╬═══════════╬═══════════════════╣");

        for (SimulationResult r : results) {
            System.out.printf("║ %-16s ║ %8.4f ║ %11.4f ║ %11.4f ║ %10.4f ║ %9.2f ║ %17.4f ║%n",
                abbr(r.getAlgorithmName(), 16),
                r.getMakespan(), r.getAvgExecutionTime(), r.getAvgWaitingTime(),
                r.getThroughput(), r.getAvgCpuUtilization(), r.getLoadBalanceIndex());
        }
        System.out.println("╚══════════════════╩══════════╩═════════════╩═════════════╩════════════╩═══════════╩═══════════════════╝");
    }

    public static void printBestAlgorithmSummary(List<SimulationResult> results) {
        if (results.isEmpty()) return;
        SimulationResult bMs  = results.get(0), bTh = results.get(0),
                         bWt  = results.get(0), bLb = results.get(0);
        for (SimulationResult r : results) {
            if (r.getMakespan()        < bMs.getMakespan())        bMs = r;
            if (r.getThroughput()      > bTh.getThroughput())      bTh = r;
            if (r.getAvgWaitingTime()  < bWt.getAvgWaitingTime())  bWt = r;
            if (r.getLoadBalanceIndex()< bLb.getLoadBalanceIndex()) bLb = r;
        }
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println(  "║              BEST ALGORITHM PER METRIC                      ║");
        System.out.println(  "╠══════════════════════════════════════════════════════════════╣");
        System.out.printf(   "║  Min Makespan       : %-38s║%n", bMs.getAlgorithmName() + " (" + String.format("%.4f", bMs.getMakespan()) + "s)");
        System.out.printf(   "║  Max Throughput     : %-38s║%n", bTh.getAlgorithmName() + " (" + String.format("%.4f", bTh.getThroughput()) + ")");
        System.out.printf(   "║  Min Waiting Time   : %-38s║%n", bWt.getAlgorithmName() + " (" + String.format("%.4f", bWt.getAvgWaitingTime()) + "s)");
        System.out.printf(   "║  Best Load Balance  : %-38s║%n", bLb.getAlgorithmName() + " (s=" + String.format("%.4f", bLb.getLoadBalanceIndex()) + ")");
        System.out.println(  "╚══════════════════════════════════════════════════════════════╝");
    }

    public static void exportToCsv(List<SimulationResult> results) {
        try {
            Files.createDirectories(Paths.get(SimulationConfig.OUTPUT_DIR));
            PrintWriter pw = new PrintWriter(new FileWriter(SimulationConfig.CSV_FILE));
            pw.println(SimulationResult.csvHeader());
            for (SimulationResult r : results) pw.println(r.toCsvRow());
            pw.close();
            System.out.println("\n  CSV saved to: " + SimulationConfig.CSV_FILE);
        } catch (IOException e) {
            System.err.println("Could not write CSV: " + e.getMessage());
        }
    }

    public static void exportTextReport(List<SimulationResult> results) {
        try {
            Files.createDirectories(Paths.get(SimulationConfig.OUTPUT_DIR));
            PrintWriter pw = new PrintWriter(new FileWriter(SimulationConfig.OUTPUT_DIR + "report.txt"));
            pw.println("=".repeat(65));
            pw.println("  COMPARATIVE ANALYSIS OF VM SCHEDULING ALGORITHMS");
            pw.println("  Using CloudSim 3.0.3");
            pw.println("=".repeat(65));
            pw.println();
            pw.println("Hosts: "    + com.cloudsim.utils.SimulationConfig.NUM_HOSTS);
            pw.println("VMs: "      + com.cloudsim.utils.SimulationConfig.NUM_VMS);
            pw.println("Cloudlets: "+ com.cloudsim.utils.SimulationConfig.NUM_CLOUDLETS);
            pw.println();
            for (SimulationResult r : results) { pw.println(r); pw.println(); }
            pw.close();
            System.out.println("  Report saved to: " + SimulationConfig.OUTPUT_DIR + "report.txt");
        } catch (IOException e) {
            System.err.println("Could not write report: " + e.getMessage());
        }
    }

    private static String abbr(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}
