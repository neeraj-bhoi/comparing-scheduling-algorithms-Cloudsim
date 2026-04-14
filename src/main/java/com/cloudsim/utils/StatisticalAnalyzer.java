package com.cloudsim.utils;

import com.cloudsim.results.SimulationResult;

import java.util.Arrays;
import java.util.List;

public class StatisticalAnalyzer {

    public static void printRankings(List<SimulationResult> results) {
        double[] scores = compositeScores(results);
        Integer[] idx = new Integer[results.size()];
        for (int i = 0; i < idx.length; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(scores[a], scores[b]));

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println(  "║           COMPOSITE PERFORMANCE RANKINGS                 ║");
        System.out.println(  "╠══════════════════╦═══════════════════╦══════════════════╣");
        System.out.printf(   "║ %-16s ║ %-17s ║ %-16s ║%n", "Algorithm", "Composite Score", "Rank");
        System.out.println(  "╠══════════════════╬═══════════════════╬══════════════════╣");

        String[] medals = {"1st (Best)", "2nd", "3rd", "4th", "5th", "6th"};
        for (int r = 0; r < idx.length; r++) {
            int i = idx[r];
            System.out.printf("║ %-16s ║ %17.4f ║ %-16s ║%n",
                abbr(results.get(i).getAlgorithmName(), 16),
                scores[i],
                r < medals.length ? medals[r] : (r+1)+"th");
        }
        System.out.println("╚══════════════════╩═══════════════════╩══════════════════╝");
        System.out.println("  (Lower composite score = better overall)");
    }

    private static double[] compositeScores(List<SimulationResult> results) {
        int n = results.size();
        double[] mk  = arr(results, r -> r.getMakespan());
        double[] wt  = arr(results, r -> r.getAvgWaitingTime());
        double[] th  = arr(results, r -> r.getThroughput());
        double[] lb  = arr(results, r -> r.getLoadBalanceIndex());
        double[] en  = arr(results, r -> r.getTotalEnergyConsumption());
        double[] s   = new double[n];
        for (int i = 0; i < n; i++) {
            s[i] = 0.25 * normMin(mk[i], mk)
                 + 0.20 * normMin(wt[i], wt)
                 + 0.20 * normMax(th[i], th)
                 + 0.20 * normMin(lb[i], lb)
                 + 0.15 * normMin(en[i], en);
        }
        return s;
    }

    @FunctionalInterface interface Getter { double get(SimulationResult r); }

    private static double[] arr(List<SimulationResult> list, Getter g) {
        double[] a = new double[list.size()];
        for (int i = 0; i < a.length; i++) a[i] = g.get(list.get(i));
        return a;
    }

    private static double normMin(double v, double[] all) {
        double min = all[0], max = all[0];
        for (double x : all) { if (x < min) min = x; if (x > max) max = x; }
        return max == min ? 0.5 : (v - min) / (max - min);
    }

    private static double normMax(double v, double[] all) {
        return 1.0 - normMin(v, all);
    }

    private static String abbr(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}
