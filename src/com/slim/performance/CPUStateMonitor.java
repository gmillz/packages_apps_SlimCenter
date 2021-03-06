package com.slim.performance;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.SparseLongArray;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CPUStateMonitor {

    private Map<Integer, ArrayList<CpuState>> mStates;
    private Map<Integer, SparseLongArray> mOffsets;
    private boolean mOverallStats;
    private int mCpuNum;

    public CPUStateMonitor() {
        mCpuNum = Utils.getNumOfCpus();
        mStates = new HashMap<>();
        mOffsets = new HashMap<>();
        for (int i = 0; i < mCpuNum; i++) {
            mStates.put(i, new ArrayList<CpuState>());
            mOffsets.put(i, new SparseLongArray());
        }
        mOverallStats = Utils.hasOverallStats();
    }

    public boolean hasOverallStats() {
        return mOverallStats;
    }

    @SuppressWarnings("serial")
    public class CPUStateMonitorException extends Exception {
        public CPUStateMonitorException(String s) {
            super(s);
        }
    }

    public class CpuState implements Comparable<CpuState> {
        public CpuState(int cpu, int a, long b) {
            mCpu = cpu;
            freq = a;
            duration = b;
        }

        public int freq = 0;
        public long duration = 0;
        public int mCpu = 0;

        @Override
        public String toString() {
            return mCpu + ":" + freq + ":" + duration;
        }

        public int compareTo(@NonNull CpuState state) {
            Integer a = freq;
            Integer b = state.freq;
            return a.compareTo(b);
        }

        public long getDuration() {
            SparseLongArray offsets = getOffsets(mCpu);
            Long offset = offsets.get(freq);
            return duration - offset;
        }
    }

    public List<CpuState> getStates(int cpu) {
        return mStates.get(cpu);
    }

    public CpuState getFreqState(int cpu, int freq) {
        List<CpuState> cpuStates = mStates.get(cpu);
        for (CpuState state : cpuStates) {
            if (state.freq == freq) {
                return state;
            }
        }
        return null;
    }

    public CpuState getDeepSleepState() {
        List<CpuState> cpuStates = mStates.get(0);
        for (CpuState state : cpuStates) {
            if (state.freq == 0) {
                return state;
            }
        }
        return null;
    }

    public long getTotalStateTime(int cpu, boolean withOffset) {
        long sum = 0;

        List<CpuState> cpuStates = mStates.get(cpu);
        for (CpuState state : cpuStates) {
            if (withOffset) {
                sum += state.getDuration();
            } else {
                sum += state.duration;
            }
        }
        return sum;
    }

    public SparseLongArray getOffsets(int cpu) {
        return mOffsets.get(cpu);
    }

    public void setOffsets(int cpu, SparseLongArray offsets) {
        mOffsets.put(cpu, offsets);
    }

    public void setOffsets() throws CPUStateMonitorException {
        updateStates();
        for (int i = 0; i < mCpuNum; i++) {
            setOffsets(i);
        }
    }

    private void setOffsets(int cpu) throws CPUStateMonitorException {
        SparseLongArray cpuOffsets = mOffsets.get(cpu);
        cpuOffsets.clear();

        List<CpuState> cpuStates = mStates.get(cpu);
        for (CpuState state : cpuStates) {
            cpuOffsets.put(state.freq, state.duration);
        }
    }

    public void removeOffsets() {
        for (int i = 0; i < mCpuNum; i++) {
            removeOffsets(i);
        }
    }

    private void removeOffsets(int cpu) {
        SparseLongArray cpuOffsets = mOffsets.get(cpu);
        cpuOffsets.clear();
    }

    public void clear() {
        int cpuNum = Utils.getNumOfCpus();
        for (int i = 0; i < cpuNum; i++) {
            List<CpuState> cpuStates = mStates.get(i);
            cpuStates.clear();
        }
    }

    public void updateStates() throws CPUStateMonitorException {

        if (mOverallStats) {
            try {
                InputStream is = new FileInputStream(Paths.TIME_IN_STATE_OVERALL_PATH);
                InputStreamReader ir = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(ir);
                clear();
                readInOverallStates(br);
                is.close();
            } catch (IOException e) {
                throw new CPUStateMonitorException(
                        "Problem opening time-in-states file");
            }
        } else {
            List<CpuState> cpuStates = mStates.get(0);
            try {
                InputStream is = new FileInputStream(Paths.TIME_IN_STATE_PATH);
                InputStreamReader ir = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(ir);
                clear();
                readInStates(br, 0, cpuStates);
                is.close();
            } catch (IOException e) {
                throw new CPUStateMonitorException(
                        "Problem opening time-in-states file");
            }
        }

        List<CpuState> cpuStates = mStates.get(0);
        long sleepTime = Math.max((SystemClock.elapsedRealtime() - SystemClock
                .uptimeMillis()) / 10, 0);
        cpuStates.add(new CpuState(0, 0, sleepTime));
    }

    private void readInStates(BufferedReader br, int cpu,
                              List<CpuState> cpuStates) throws CPUStateMonitorException {
        try {
            String line;
            while ((line = br.readLine()) != null) {
                String[] nums = line.split(" ");
                cpuStates.add(new CpuState(cpu, Integer.parseInt(nums[0]), Long
                        .parseLong(nums[1])));
            }
            Collections.sort(cpuStates, Collections.reverseOrder());
        } catch (IOException e) {
            throw new CPUStateMonitorException(
                    "Problem processing time-in-states file");
        }
    }

    private void readInOverallStates(BufferedReader br)
            throws CPUStateMonitorException {
        int cpu = 0;
        List<CpuState> cpuStates = null;
        int firstFreq = 0;
        try {
            String line;
            while ((line = br.readLine()) != null) {
                String[] nums = line.split(" ");
                int freq = Integer.parseInt(nums[0]);
                if (firstFreq == 0) {
                    firstFreq = freq;
                } else if (freq == firstFreq) {
                    cpu++;
                    Collections.sort(cpuStates, Collections.reverseOrder());
                }
                cpuStates = mStates.get(cpu);
                cpuStates.add(new CpuState(cpu, freq, Long.parseLong(nums[1])));
            }
        } catch (IOException e) {
            throw new CPUStateMonitorException(
                    "Problem processing time-in-states file");
        }
    }
}
