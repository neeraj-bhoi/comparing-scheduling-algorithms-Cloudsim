package com.cloudsim.utils;

/**
 * SimulationConfig.java
 * Central configuration — change parameters here to adjust the experiment.
 */
public class SimulationConfig {

    // ── Datacenter ────────────────────────────────────────────────
    public static final int    NUM_HOSTS     = 10;
    public static final int    HOST_PES      = 8;       // CPU cores per host
    public static final int    HOST_MIPS     = 10000;   // MIPS per PE
    public static final int    HOST_RAM      = 16384;   // MB
    public static final long   HOST_BW       = 10000;   // Mbps
    public static final long   HOST_STORAGE  = 1000000; // MB

    // ── Virtual Machines ─────────────────────────────────────────
    public static final int    NUM_VMS       = 20;
    public static final int    VM_PES        = 2;
    public static final int    VM_MIPS       = 1000;
    public static final int    VM_RAM        = 2048;    // MB
    public static final long   VM_BW         = 1000;
    public static final long   VM_SIZE       = 10000;   // MB storage image

    // ── Cloudlets (Tasks) ────────────────────────────────────────
    public static final int    NUM_CLOUDLETS = 50;
    public static final long   CLOUDLET_LENGTH      = 10000; // MI base
    public static final int    CLOUDLET_PES         = 1;
    public static final long   CLOUDLET_FILE_SIZE   = 300;
    public static final long   CLOUDLET_OUTPUT_SIZE = 300;

    // ── Output ───────────────────────────────────────────────────
    public static final String OUTPUT_DIR  = "results/";
    public static final String CSV_FILE    = OUTPUT_DIR + "simulation_results.csv";
    public static final String CHARTS_DIR  = OUTPUT_DIR + "charts/";

    private SimulationConfig() {}
}
