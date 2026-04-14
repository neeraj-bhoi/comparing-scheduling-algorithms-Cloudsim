package com.cloudsim.results;

import com.cloudsim.utils.SimulationConfig;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ChartGenerator {

    private static final int W = 800, H = 500;

    @FunctionalInterface
    interface Extractor { double get(SimulationResult r); }

    public static void generateAllCharts(List<SimulationResult> results) {
        try { Files.createDirectories(Paths.get(SimulationConfig.CHARTS_DIR)); }
        catch (IOException e) { System.err.println("Cannot create charts dir: " + e.getMessage()); return; }

        save(results, "Makespan (s)",           "Makespan Comparison",          r -> r.getMakespan(),              "makespan.png");
        save(results, "Avg Execution Time (s)", "Average Execution Time",       r -> r.getAvgExecutionTime(),      "avg_exec_time.png");
        save(results, "Avg Waiting Time (s)",   "Average Waiting Time",         r -> r.getAvgWaitingTime(),        "avg_wait_time.png");
        save(results, "Throughput (tasks/s)",   "Throughput Comparison",        r -> r.getThroughput(),            "throughput.png");
        save(results, "CPU Utilization (%)",    "CPU Utilization",              r -> r.getAvgCpuUtilization(),     "cpu_util.png");
        save(results, "Load Balance Index",     "Load Balance Index",           r -> r.getLoadBalanceIndex(),      "load_balance.png");
        save(results, "Energy (Wh)",            "Energy Consumption",           r -> r.getTotalEnergyConsumption(),"energy.png");

        System.out.println("  Charts saved to: " + SimulationConfig.CHARTS_DIR);
    }

    private static void save(List<SimulationResult> results, String yLabel, String title,
                              Extractor ex, String filename) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (SimulationResult r : results)
            ds.addValue(ex.get(r), yLabel, r.getAlgorithmName());

        JFreeChart chart = ChartFactory.createBarChart(
            title, "Algorithm", yLabel, ds,
            PlotOrientation.VERTICAL, true, true, false);

        chart.setBackgroundPaint(Color.WHITE);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(245, 245, 245));
        plot.setRangeGridlinePaint(Color.WHITE);
        plot.getDomainAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 9));

        Color[] palette = {
            new Color(52,152,219), new Color(46,204,113), new Color(231,76,60),
            new Color(155,89,182), new Color(230,126,34), new Color(241,196,15)
        };
        BarRenderer br = (BarRenderer) plot.getRenderer();
        br.setMaximumBarWidth(0.1);
        for (int i = 0; i < results.size(); i++)
            br.setSeriesPaint(i, palette[i % palette.length]);

        try {
            ChartUtils.saveChartAsPNG(new File(SimulationConfig.CHARTS_DIR + filename), chart, W, H);
        } catch (IOException e) {
            System.err.println("Could not save chart " + filename + ": " + e.getMessage());
        }
    }
}
