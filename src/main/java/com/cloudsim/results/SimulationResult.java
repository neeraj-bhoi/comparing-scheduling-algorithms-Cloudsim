package com.cloudsim.results;

/**
 * SimulationResult.java
 * Holds all performance metrics for one scheduling algorithm run.
 */
public class SimulationResult {

    private String algorithmName;
    private double makespan;
    private double avgExecutionTime;
    private double avgWaitingTime;
    private double avgTurnaroundTime;
    private double avgResponseTime;
    private int    completedCloudlets;
    private int    totalCloudlets;
    private double throughput;
    private double avgCpuUtilization;
    private double loadBalanceIndex;
    private double totalEnergyConsumption;

    public SimulationResult(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    // ── Getters / Setters ────────────────────────────────────────

    public String getAlgorithmName()                        { return algorithmName; }
    public void   setAlgorithmName(String v)                { this.algorithmName = v; }

    public double getMakespan()                             { return makespan; }
    public void   setMakespan(double v)                     { this.makespan = v; }

    public double getAvgExecutionTime()                     { return avgExecutionTime; }
    public void   setAvgExecutionTime(double v)             { this.avgExecutionTime = v; }

    public double getAvgWaitingTime()                       { return avgWaitingTime; }
    public void   setAvgWaitingTime(double v)               { this.avgWaitingTime = v; }

    public double getAvgTurnaroundTime()                    { return avgTurnaroundTime; }
    public void   setAvgTurnaroundTime(double v)            { this.avgTurnaroundTime = v; }

    public double getAvgResponseTime()                      { return avgResponseTime; }
    public void   setAvgResponseTime(double v)              { this.avgResponseTime = v; }

    public int    getCompletedCloudlets()                   { return completedCloudlets; }
    public void   setCompletedCloudlets(int v)              { this.completedCloudlets = v; }

    public int    getTotalCloudlets()                       { return totalCloudlets; }
    public void   setTotalCloudlets(int v)                  { this.totalCloudlets = v; }

    public double getThroughput()                           { return throughput; }
    public void   setThroughput(double v)                   { this.throughput = v; }

    public double getAvgCpuUtilization()                    { return avgCpuUtilization; }
    public void   setAvgCpuUtilization(double v)            { this.avgCpuUtilization = v; }

    public double getLoadBalanceIndex()                     { return loadBalanceIndex; }
    public void   setLoadBalanceIndex(double v)             { this.loadBalanceIndex = v; }

    public double getTotalEnergyConsumption()               { return totalEnergyConsumption; }
    public void   setTotalEnergyConsumption(double v)       { this.totalEnergyConsumption = v; }

    // ── Display ──────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "\n╔══════════════════════════════════════════════════════════╗\n" +
            "║  Algorithm : %-44s║\n" +
            "╠══════════════════════════════════════════════════════════╣\n" +
            "║  Makespan              : %-10.4f seconds             ║\n" +
            "║  Avg Execution Time    : %-10.4f seconds             ║\n" +
            "║  Avg Waiting Time      : %-10.4f seconds             ║\n" +
            "║  Avg Turnaround Time   : %-10.4f seconds             ║\n" +
            "║  Avg Response Time     : %-10.4f seconds             ║\n" +
            "║  Throughput            : %-10.4f cloudlets/sec       ║\n" +
            "║  Completed Cloudlets   : %-5d / %-5d                ║\n" +
            "║  Avg CPU Utilization   : %-10.2f %%                  ║\n" +
            "║  Load Balance Index    : %-10.4f (lower=better)     ║\n" +
            "║  Energy Consumption    : %-10.4f Wh                 ║\n" +
            "╚══════════════════════════════════════════════════════════╝",
            algorithmName, makespan, avgExecutionTime, avgWaitingTime,
            avgTurnaroundTime, avgResponseTime, throughput,
            completedCloudlets, totalCloudlets,
            avgCpuUtilization, loadBalanceIndex, totalEnergyConsumption
        );
    }

    public String toCsvRow() {
        return String.format("%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%d,%d,%.2f,%.4f,%.4f",
            algorithmName, makespan, avgExecutionTime, avgWaitingTime,
            avgTurnaroundTime, avgResponseTime, throughput,
            completedCloudlets, totalCloudlets,
            avgCpuUtilization, loadBalanceIndex, totalEnergyConsumption);
    }

    public static String csvHeader() {
        return "Algorithm,Makespan,AvgExecutionTime,AvgWaitingTime,AvgTurnaroundTime," +
               "AvgResponseTime,Throughput,CompletedCloudlets,TotalCloudlets," +
               "AvgCpuUtilization,LoadBalanceIndex,EnergyConsumption";
    }
}
