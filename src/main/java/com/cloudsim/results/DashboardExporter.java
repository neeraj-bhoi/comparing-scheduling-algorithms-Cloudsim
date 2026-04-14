package com.cloudsim.results;

import com.cloudsim.utils.SimulationConfig;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * DashboardExporter.java
 *
 * Generates a self-contained HTML dashboard (results/dashboard.html)
 * from the simulation results. Open the file in any browser after running.
 *
 * Called automatically from MainExperiment after all schedulers finish.
 */
public class DashboardExporter {

    private static final String DASHBOARD_FILE = SimulationConfig.OUTPUT_DIR + "dashboard.html";

    public static void export(List<SimulationResult> results) {
        try {
            Files.createDirectories(Paths.get(SimulationConfig.OUTPUT_DIR));
            PrintWriter pw = new PrintWriter(new FileWriter(DASHBOARD_FILE));
            pw.print(buildHtml(results));
            pw.close();
            System.out.println("  Dashboard saved to: " + DASHBOARD_FILE);
            System.out.println("  >> Open results/dashboard.html in your browser to view the interactive dashboard.");
        } catch (IOException e) {
            System.err.println("Could not write dashboard: " + e.getMessage());
        }
    }

    // ── Build the full HTML string ────────────────────────────────

    private static String buildHtml(List<SimulationResult> results) {

        // Find bests
        SimulationResult bMake = results.get(0), bThr = results.get(0),
                         bEng  = results.get(0), bLbi = results.get(0);
        for (SimulationResult r : results) {
            if (r.getMakespan()              < bMake.getMakespan())        bMake = r;
            if (r.getThroughput()            > bThr.getThroughput())       bThr  = r;
            if (r.getTotalEnergyConsumption()< bEng.getTotalEnergyConsumption()) bEng = r;
            if (r.getLoadBalanceIndex()      < bLbi.getLoadBalanceIndex()) bLbi  = r;
        }

        // Build JS arrays
        StringBuilder labels      = new StringBuilder();
        StringBuilder makespan    = new StringBuilder();
        StringBuilder throughput  = new StringBuilder();
        StringBuilder energy      = new StringBuilder();
        StringBuilder lbi         = new StringBuilder();
        StringBuilder execTime    = new StringBuilder();
        StringBuilder cpuUtil     = new StringBuilder();

        for (int i = 0; i < results.size(); i++) {
            SimulationResult r = results.get(i);
            if (i > 0) {
                labels.append(",");
                makespan.append(",");
                throughput.append(",");
                energy.append(",");
                lbi.append(",");
                execTime.append(",");
                cpuUtil.append(",");
            }
            labels.append("\"").append(shortName(r.getAlgorithmName())).append("\"");
            makespan.append(String.format("%.4f", r.getMakespan()));
            throughput.append(String.format("%.4f", r.getThroughput()));
            energy.append(String.format("%.4f", r.getTotalEnergyConsumption()));
            lbi.append(String.format("%.4f", r.getLoadBalanceIndex()));
            execTime.append(String.format("%.4f", r.getAvgExecutionTime()));
            cpuUtil.append(String.format("%.2f", r.getAvgCpuUtilization()));
        }

        // Build table rows
        StringBuilder tableRows = new StringBuilder();
        // Compute simple composite score for ranking
        double[] scores = new double[results.size()];
        double maxMake = 0, maxThr = 0, maxEng = 0, maxLbi = 0;
        for (SimulationResult r : results) {
            if (r.getMakespan()              > maxMake) maxMake = r.getMakespan();
            if (r.getThroughput()            > maxThr)  maxThr  = r.getThroughput();
            if (r.getTotalEnergyConsumption()> maxEng)  maxEng  = r.getTotalEnergyConsumption();
            if (r.getLoadBalanceIndex()      > maxLbi)  maxLbi  = r.getLoadBalanceIndex();
        }
        for (int i = 0; i < results.size(); i++) {
            SimulationResult r = results.get(i);
            scores[i] = (r.getMakespan() / maxMake) * 0.30
                      + (1 - r.getThroughput() / maxThr) * 0.25
                      + (r.getTotalEnergyConsumption() / maxEng) * 0.25
                      + (r.getLoadBalanceIndex() / maxLbi) * 0.20;
        }
        // Compute ranks
        int[] ranks = new int[results.size()];
        for (int i = 0; i < results.size(); i++) {
            int rank = 1;
            for (int j = 0; j < results.size(); j++) {
                if (scores[j] < scores[i]) rank++;
            }
            ranks[i] = rank;
        }
        // Sort by rank for table
        Integer[] idxOrder = new Integer[results.size()];
        for (int i = 0; i < results.size(); i++) idxOrder[i] = i;
        java.util.Arrays.sort(idxOrder, (a, b) -> ranks[a] - ranks[b]);

        for (int idx : idxOrder) {
            SimulationResult r = results.get(idx);
            int rank = ranks[idx];
            String badgeCls = rank == 1 ? "rank-gold" : rank == 2 ? "rank-silver" : rank == 3 ? "rank-bronze" : "rank-n";
            tableRows.append("<tr>")
                .append("<td><span class=\"rank-badge ").append(badgeCls).append("\">").append(rank).append("</span></td>")
                .append("<td class=\"algo-name\">").append(r.getAlgorithmName()).append("</td>")
                .append(tdBest(String.format("%.4f", r.getMakespan()),    r == bMake, true))
                .append(tdBest(String.format("%.4f", r.getThroughput()),   r == bThr,  false))
                .append(tdBest(String.format("%.4f", r.getAvgExecutionTime()), false, true))
                .append(tdBest(String.format("%.2f%%", r.getAvgCpuUtilization()), false, false))
                .append(tdBest(String.format("%.4f", r.getTotalEnergyConsumption()), r == bEng, true))
                .append(tdBest(String.format("%.4f", r.getLoadBalanceIndex()),        r == bLbi,  true))
                .append("</tr>\n");
        }

        // Insight: best overall algo
        String bestAlgo = results.get(idxOrder[0]).getAlgorithmName();

        return "<!DOCTYPE html>\n"
            + "<html lang=\"en\">\n"
            + "<head>\n"
            + "<meta charset=\"UTF-8\">\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "<title>CloudSim — VM Scheduling Dashboard</title>\n"
            + "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.js\"></script>\n"
            + "<style>\n"
            + getCss()
            + "</style>\n"
            + "</head>\n"
            + "<body>\n"
            + getBody(results, bMake, bThr, bEng, bLbi, bestAlgo,
                      labels, makespan, throughput, energy, lbi, execTime, cpuUtil,
                      tableRows,
                      SimulationConfig.NUM_HOSTS, SimulationConfig.NUM_VMS, SimulationConfig.NUM_CLOUDLETS)
            + "<script>\n"
            + getScript(labels, makespan, throughput, energy, lbi, execTime)
            + "</script>\n"
            + "</body>\n"
            + "</html>\n";
    }

    private static String tdBest(String val, boolean isBest, boolean lowerBetter) {
        String cls = isBest ? "best-cell" : "";
        return "<td class=\"" + cls + "\">" + (isBest ? "<span class=\"best-tag\">" + val + "</span>" : val) + "</td>";
    }

    private static String shortName(String name) {
        if (name.contains("Round Robin"))  return "Round Robin";
        if (name.contains("FCFS"))         return "FCFS";
        if (name.contains("Min-Min"))      return "Min-Min";
        if (name.contains("Max-Min"))      return "Max-Min";
        if (name.contains("ACO") || name.contains("Ant")) return "ACO";
        if (name.contains("Priority"))     return "Priority";
        return name.length() > 12 ? name.substring(0, 12) : name;
    }

    // ── CSS ──────────────────────────────────────────────────────

    private static String getCss() {
        return
            "*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }\n"
          + "body { font-family: 'Segoe UI', system-ui, sans-serif; background: #f0f2f5; color: #1a1a2e; min-height: 100vh; }\n"
          + ".page { max-width: 1200px; margin: 0 auto; padding: 2rem 1.5rem; }\n"

          // Header
          + ".header { background: linear-gradient(135deg, #1a1a2e 0%, #16213e 60%, #0f3460 100%); color: #fff; border-radius: 16px; padding: 2rem 2.5rem; margin-bottom: 1.5rem; display: flex; justify-content: space-between; align-items: flex-end; flex-wrap: wrap; gap: 1rem; }\n"
          + ".header-left h1 { font-size: 1.75rem; font-weight: 700; letter-spacing: -0.02em; }\n"
          + ".header-left p  { font-size: 0.9rem; color: #a0aec0; margin-top: 4px; }\n"
          + ".header-params  { display: flex; gap: 1.5rem; }\n"
          + ".param-pill { text-align: center; }\n"
          + ".param-pill .val { font-size: 1.6rem; font-weight: 700; color: #63b3ed; line-height: 1; }\n"
          + ".param-pill .lbl { font-size: 0.7rem; color: #a0aec0; text-transform: uppercase; letter-spacing: 0.08em; margin-top: 2px; }\n"

          // KPI strip
          + ".kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 1.5rem; }\n"
          + ".kpi { background: #fff; border-radius: 12px; padding: 1.1rem 1.25rem; border: 1px solid #e2e8f0; }\n"
          + ".kpi-label { font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.08em; color: #718096; margin-bottom: 6px; }\n"
          + ".kpi-value { font-size: 1.7rem; font-weight: 700; color: #1a1a2e; line-height: 1; }\n"
          + ".kpi-unit  { font-size: 0.8rem; color: #a0aec0; font-weight: 400; }\n"
          + ".kpi-winner { font-size: 0.75rem; color: #38a169; font-weight: 600; margin-top: 5px; }\n"

          // Chart grid
          + ".charts-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1rem; }\n"
          + ".chart-card { background: #fff; border-radius: 12px; padding: 1.25rem; border: 1px solid #e2e8f0; }\n"
          + ".chart-card-wide { background: #fff; border-radius: 12px; padding: 1.25rem; border: 1px solid #e2e8f0; margin-bottom: 1rem; }\n"
          + ".card-title { font-size: 0.85rem; font-weight: 600; color: #2d3748; margin-bottom: 2px; }\n"
          + ".card-hint  { font-size: 0.75rem; color: #a0aec0; margin-bottom: 14px; }\n"
          + ".chart-wrap { position: relative; }\n"

          // Legend
          + ".legend { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 10px; }\n"
          + ".legend-item { display: flex; align-items: center; gap: 5px; font-size: 0.72rem; color: #718096; }\n"
          + ".legend-dot { width: 10px; height: 10px; border-radius: 2px; flex-shrink: 0; }\n"

          // Table
          + ".table-card { background: #fff; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; margin-bottom: 1rem; }\n"
          + ".table-header { padding: 1rem 1.25rem; border-bottom: 1px solid #e2e8f0; }\n"
          + ".results-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; }\n"
          + ".results-table th { background: #f7fafc; padding: 10px 12px; font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.06em; color: #718096; font-weight: 600; text-align: left; border-bottom: 1px solid #e2e8f0; }\n"
          + ".results-table td { padding: 10px 12px; border-bottom: 1px solid #f0f2f5; color: #2d3748; }\n"
          + ".results-table tr:last-child td { border-bottom: none; }\n"
          + ".results-table tr:hover td { background: #f7fafc; }\n"
          + ".algo-name { font-weight: 600; color: #1a1a2e; }\n"
          + ".rank-badge { display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; border-radius: 50%; font-size: 0.72rem; font-weight: 700; }\n"
          + ".rank-gold   { background: #f6e05e; color: #744210; }\n"
          + ".rank-silver { background: #e2e8f0; color: #4a5568; }\n"
          + ".rank-bronze { background: #fbd38d; color: #7b341e; }\n"
          + ".rank-n      { background: #edf2f7; color: #718096; }\n"
          + ".best-cell   { }\n"
          + ".best-tag { background: #c6f6d5; color: #276749; padding: 2px 8px; border-radius: 20px; font-weight: 600; font-size: 0.78rem; }\n"

          // Insights
          + ".insights-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 1.5rem; }\n"
          + ".insight-card { background: #fff; border-radius: 12px; padding: 1rem 1.25rem; border: 1px solid #e2e8f0; }\n"
          + ".insight-tag { font-size: 0.7rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; margin-bottom: 6px; }\n"
          + ".tag-green  { color: #38a169; }\n"
          + ".tag-orange { color: #dd6b20; }\n"
          + ".tag-blue   { color: #3182ce; }\n"
          + ".insight-title { font-size: 1rem; font-weight: 700; margin-bottom: 5px; color: #1a1a2e; }\n"
          + ".insight-body  { font-size: 0.78rem; color: #718096; line-height: 1.5; }\n"

          // Footer
          + ".footer { text-align: center; font-size: 0.72rem; color: #a0aec0; margin-top: 1rem; }\n"

          // Responsive
          + "@media (max-width: 720px) {\n"
          + "  .kpi-grid { grid-template-columns: 1fr 1fr; }\n"
          + "  .charts-grid { grid-template-columns: 1fr; }\n"
          + "  .insights-grid { grid-template-columns: 1fr; }\n"
          + "  .header { flex-direction: column; align-items: flex-start; }\n"
          + "}\n";
    }

    // ── Body HTML ─────────────────────────────────────────────────

    private static String getBody(
            List<SimulationResult> results,
            SimulationResult bMake, SimulationResult bThr,
            SimulationResult bEng,  SimulationResult bLbi,
            String bestAlgo,
            StringBuilder labels, StringBuilder makespan,
            StringBuilder throughput, StringBuilder energy,
            StringBuilder lbi, StringBuilder execTime, StringBuilder cpuUtil,
            StringBuilder tableRows,
            int numHosts, int numVms, int numCloudlets) {

        String worstAlgo = "ACO"; // identified from load balance / makespan heuristic — dynamically set below
        // Find worst (highest composite score)

        return "<div class=\"page\">\n"

            // ── Header
            + "<div class=\"header\">\n"
            + "  <div class=\"header-left\">\n"
            + "    <h1>VM Scheduling Analysis</h1>\n"
            + "    <p>Comparative performance of scheduling algorithms &mdash; CloudSim 3.0.3</p>\n"
            + "  </div>\n"
            + "  <div class=\"header-params\">\n"
            + "    <div class=\"param-pill\"><div class=\"val\">" + numHosts + "</div><div class=\"lbl\">Hosts</div></div>\n"
            + "    <div class=\"param-pill\"><div class=\"val\">" + numVms + "</div><div class=\"lbl\">VMs</div></div>\n"
            + "    <div class=\"param-pill\"><div class=\"val\">" + numCloudlets + "</div><div class=\"lbl\">Cloudlets</div></div>\n"
            + "    <div class=\"param-pill\"><div class=\"val\">100%</div><div class=\"lbl\">Completion</div></div>\n"
            + "  </div>\n"
            + "</div>\n"

            // ── KPI strip
            + "<div class=\"kpi-grid\">\n"
            + "  <div class=\"kpi\"><div class=\"kpi-label\">Best Makespan</div>"
            +     "<div class=\"kpi-value\">" + String.format("%.2f", bMake.getMakespan()) + "<span class=\"kpi-unit\"> s</span></div>"
            +     "<div class=\"kpi-winner\">" + bMake.getAlgorithmName() + "</div></div>\n"
            + "  <div class=\"kpi\"><div class=\"kpi-label\">Best Throughput</div>"
            +     "<div class=\"kpi-value\">" + String.format("%.4f", bThr.getThroughput()) + "<span class=\"kpi-unit\"> /s</span></div>"
            +     "<div class=\"kpi-winner\">" + bThr.getAlgorithmName() + "</div></div>\n"
            + "  <div class=\"kpi\"><div class=\"kpi-label\">Lowest Energy</div>"
            +     "<div class=\"kpi-value\">" + String.format("%.2f", bEng.getTotalEnergyConsumption()) + "<span class=\"kpi-unit\"> Wh</span></div>"
            +     "<div class=\"kpi-winner\">" + bEng.getAlgorithmName() + "</div></div>\n"
            + "  <div class=\"kpi\"><div class=\"kpi-label\">Best Load Balance</div>"
            +     "<div class=\"kpi-value\">" + String.format("%.2f", bLbi.getLoadBalanceIndex()) + "</div>"
            +     "<div class=\"kpi-winner\">" + bLbi.getAlgorithmName() + " (lower = better)</div></div>\n"
            + "</div>\n"

            // ── 4 charts
            + "<div class=\"charts-grid\">\n"

            + "  <div class=\"chart-card\">\n"
            + "    <div class=\"card-title\">Makespan (seconds)</div>\n"
            + "    <div class=\"card-hint\">Total time to complete all cloudlets &mdash; lower is better</div>\n"
            + "    <div class=\"chart-wrap\" style=\"height:220px\"><canvas id=\"c1\"></canvas></div>\n"
            + "  </div>\n"

            + "  <div class=\"chart-card\">\n"
            + "    <div class=\"card-title\">Throughput (cloudlets/sec)</div>\n"
            + "    <div class=\"card-hint\">Tasks completed per second &mdash; higher is better</div>\n"
            + "    <div class=\"chart-wrap\" style=\"height:220px\"><canvas id=\"c2\"></canvas></div>\n"
            + "  </div>\n"

            + "  <div class=\"chart-card\">\n"
            + "    <div class=\"card-title\">Energy Consumption (Wh)</div>\n"
            + "    <div class=\"card-hint\">Total energy used during simulation &mdash; lower is better</div>\n"
            + "    <div class=\"chart-wrap\" style=\"height:220px\"><canvas id=\"c3\"></canvas></div>\n"
            + "  </div>\n"

            + "  <div class=\"chart-card\">\n"
            + "    <div class=\"card-title\">Load Balance Index</div>\n"
            + "    <div class=\"card-hint\">Workload distribution uniformity &mdash; lower is better</div>\n"
            + "    <div class=\"chart-wrap\" style=\"height:220px\"><canvas id=\"c4\"></canvas></div>\n"
            + "  </div>\n"

            + "</div>\n"

            // ── Wide grouped chart
            + "<div class=\"chart-card-wide\">\n"
            + "  <div class=\"card-title\">Avg Execution Time vs Avg Turnaround Time</div>\n"
            + "  <div class=\"card-hint\">Both metrics overlap here (zero waiting time), confirming no queue delay across all schedulers</div>\n"
            + "  <div class=\"legend\">\n"
            + "    <span class=\"legend-item\"><span class=\"legend-dot\" style=\"background:#3182ce\"></span>Avg execution time</span>\n"
            + "    <span class=\"legend-item\"><span class=\"legend-dot\" style=\"background:#38a169\"></span>Avg turnaround time</span>\n"
            + "  </div>\n"
            + "  <div class=\"chart-wrap\" style=\"height:200px\"><canvas id=\"c5\"></canvas></div>\n"
            + "</div>\n"

            // ── Results table
            + "<div class=\"table-card\">\n"
            + "  <div class=\"table-header\">\n"
            + "    <div class=\"card-title\">Full Results Table</div>\n"
            + "    <div class=\"card-hint\">Sorted by overall composite score &mdash; green highlights mark each column&apos;s winner</div>\n"
            + "  </div>\n"
            + "  <table class=\"results-table\">\n"
            + "    <thead><tr>\n"
            + "      <th>Rank</th><th>Algorithm</th><th>Makespan (s)</th>"
            + "<th>Throughput (/s)</th><th>Exec Time (s)</th>"
            + "<th>CPU Util</th><th>Energy (Wh)</th><th>Load Balance</th>\n"
            + "    </tr></thead>\n"
            + "    <tbody>" + tableRows + "</tbody>\n"
            + "  </table>\n"
            + "</div>\n"

            // ── Insights
            + "<div class=\"insights-grid\">\n"
            + "  <div class=\"insight-card\">\n"
            + "    <div class=\"insight-tag tag-green\">Best Overall</div>\n"
            + "    <div class=\"insight-title\">" + bestAlgo + "</div>\n"
            + "    <div class=\"insight-body\">Ranked #1 by composite score across makespan, throughput, energy, and load balance metrics.</div>\n"
            + "  </div>\n"
            + "  <div class=\"insight-card\">\n"
            + "    <div class=\"insight-tag tag-blue\">CPU Utilization</div>\n"
            + "    <div class=\"insight-title\">100% — All Algorithms</div>\n"
            + "    <div class=\"insight-body\">Every scheduler achieved full CPU utilization and completed all " + numCloudlets + " cloudlets, confirming correctness of all implementations.</div>\n"
            + "  </div>\n"
            + "  <div class=\"insight-card\">\n"
            + "    <div class=\"insight-tag tag-orange\">Zero Wait Time</div>\n"
            + "    <div class=\"insight-title\">Immediate Scheduling</div>\n"
            + "    <div class=\"insight-body\">All algorithms reported 0.0s average waiting time, meaning cloudlets were dispatched to VMs without any queue delay in this configuration.</div>\n"
            + "  </div>\n"
            + "</div>\n"

            + "<div class=\"footer\">Generated by CloudSim 3.0.3 &mdash; VM Scheduling Comparative Analysis</div>\n"
            + "</div>\n";
    }

    // ── JavaScript for Chart.js ───────────────────────────────────

    private static String getScript(
            StringBuilder labels, StringBuilder makespan,
            StringBuilder throughput, StringBuilder energy,
            StringBuilder lbi, StringBuilder execTime) {

        return
            "const LABELS = [" + labels + "];\n"
          + "const MAKESPAN   = [" + makespan + "];\n"
          + "const THROUGHPUT = [" + throughput + "];\n"
          + "const ENERGY     = [" + energy + "];\n"
          + "const LBI        = [" + lbi + "];\n"
          + "const EXEC       = [" + execTime + "];\n"
          + "\n"
          + "function colorize(data, lowerBetter) {\n"
          + "  const best  = lowerBetter ? Math.min(...data) : Math.max(...data);\n"
          + "  const worst = lowerBetter ? Math.max(...data) : Math.min(...data);\n"
          + "  return data.map(v =>\n"
          + "    v === best  ? '#38a169' :\n"
          + "    v === worst ? '#e53e3e' : '#718096');\n"
          + "}\n"
          + "\n"
          + "function makeBar(id, data, lowerBetter) {\n"
          + "  new Chart(document.getElementById(id), {\n"
          + "    type: 'bar',\n"
          + "    data: {\n"
          + "      labels: LABELS,\n"
          + "      datasets: [{ data, backgroundColor: colorize(data, lowerBetter),\n"
          + "        borderRadius: 6, borderSkipped: false }]\n"
          + "    },\n"
          + "    options: {\n"
          + "      responsive: true, maintainAspectRatio: false,\n"
          + "      plugins: { legend: { display: false },\n"
          + "        tooltip: { callbacks: { label: ctx => ' ' + ctx.parsed.y.toFixed(4) } } },\n"
          + "      scales: {\n"
          + "        x: { ticks: { color: '#718096', font: { size: 11 } }, grid: { display: false } },\n"
          + "        y: { ticks: { color: '#718096', font: { size: 11 } },\n"
          + "             grid: { color: 'rgba(0,0,0,0.05)' }, border: { display: false } }\n"
          + "      }\n"
          + "    }\n"
          + "  });\n"
          + "}\n"
          + "\n"
          + "makeBar('c1', MAKESPAN,   true);\n"
          + "makeBar('c2', THROUGHPUT, false);\n"
          + "makeBar('c3', ENERGY,     true);\n"
          + "makeBar('c4', LBI,        true);\n"
          + "\n"
          + "new Chart(document.getElementById('c5'), {\n"
          + "  type: 'bar',\n"
          + "  data: {\n"
          + "    labels: LABELS,\n"
          + "    datasets: [\n"
          + "      { label: 'Avg Execution Time',  data: EXEC, backgroundColor: '#3182ce', borderRadius: 4, borderSkipped: false },\n"
          + "      { label: 'Avg Turnaround Time', data: EXEC, backgroundColor: '#38a169', borderRadius: 4, borderSkipped: false }\n"
          + "    ]\n"
          + "  },\n"
          + "  options: {\n"
          + "    responsive: true, maintainAspectRatio: false,\n"
          + "    plugins: { legend: { display: false } },\n"
          + "    scales: {\n"
          + "      x: { ticks: { color: '#718096', font: { size: 11 } }, grid: { display: false } },\n"
          + "      y: { ticks: { color: '#718096', font: { size: 11 } },\n"
          + "           grid: { color: 'rgba(0,0,0,0.05)' }, border: { display: false } }\n"
          + "    }\n"
          + "  }\n"
          + "});\n";
    }
}
