package com.slim.performance;

public interface Paths {

    public static final String PREF_MAX_FREQ = "pref_max_freq";
    public static final String PREF_MIN_FREQ = "pref_min_freq";
    public static final String PREF_GOVERNOR = "pref_governor";
    public static final String PREF_IOSCHED = "pref_iosched";
    public static final String PREF_CPU_SOB = "pref_cpu_sob";

    public static final String FREQ_LIST_FILE =
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies";

    public static final String MAX_FREQ_FILE =
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
    public static final String MIN_FREQ_FILE =
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq";

    public static final String CUR_FREQ_FILE =
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";

    public static final String DYN_MAX_FREQ = "/sys/power/cpufreq_max_limit";
    public static final String DYN_MIN_FREQ = "/sys/power/cpufreq_min_limit";

    public static final String CPU_NUM_FILE = "/sys/devices/system/cpu/present";

    public static final String GOV_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
    public static final String GOV_LIST_FILE =
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors";
    String GOVERNOR_CONTROL = "/sys/devices/system/cpu/cpufreq";

    public static final String[] IO_SCHEDULER_PATH = {
            "/sys/block/mmcblk0/queue/scheduler",
            "/sys/block/mmcblk1/queue/scheduler"
    };

    // Time in state
    String TIME_IN_STATE_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state";
    String PREF_OFFSETS = "pref_offsets";
    String TIME_IN_STATE_OVERALL_PATH =
            "/sys/devices/system/cpu/cpufreq/overall_stats/overall_time_in_state";

    // CPU info
    String KERNEL_INFO_PATH = "/proc/version";
    String CPU_INFO_PATH = "/proc/cpuinfo";
    String MEM_INFO_PATH = "/proc/meminfo";

}