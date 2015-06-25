package com.slim.performance;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.slim.ota.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CPUInfo extends Fragment implements Paths {

    private TextView mKernelInfo;
    private TextView mCPUInfo;
    private TextView mMemoryInfo;
    private TextView mBatteryInfo;
    private ProgressBar mMemoryProgress;
    private ProgressBar mBatteryTempBar;

    private long mTotalMemory;
    private long mTotalUsedMemory;
    private long mActiveMemory;

    private long mBatteryTemp;
    private String mBatteryHealth;

    private class CPUThread extends Thread {
        private boolean mInterrupt = false;

        public void interrupt() {
            mInterrupt = true;
        }

        @Override
        public void run() {
            try {
                while (!mInterrupt) {
                    sleep(500);
                    mCPUHandler.sendMessage(mCPUHandler.obtainMessage(0));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    private CPUThread mCPUThread;

    private Handler mCPUHandler = new Handler() {
        public void handleMessage(Message msg) {
            updateData();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cpu_info, root, false);
        mKernelInfo = (TextView) view.findViewById(R.id.kernel_info);
        mCPUInfo = (TextView) view.findViewById(R.id.cpu_info);
        mMemoryInfo = (TextView) view.findViewById(R.id.memory_info);
        mMemoryProgress = (ProgressBar) view.findViewById(R.id.memory_progress);
        mBatteryInfo = (TextView) view.findViewById(R.id.battery_info);
        mBatteryTempBar = (ProgressBar) view.findViewById(R.id.battery_temp);
        updateData();
        return view;
    }

    public void updateData() {
        mKernelInfo.setText("");
        mCPUInfo.setText("");
        mMemoryInfo.setText("");
        readFile(mKernelInfo, KERNEL_INFO_PATH);
        readFile(mCPUInfo, CPU_INFO_PATH);
        //readFile(mMemoryInfo, MEM_INFO_PATH);

        updateMemoryInfo();
        updateBatteryInfo();

        Log.d("TEST", "mem: " + mTotalUsedMemory + "/" + mTotalMemory);
        mMemoryInfo.setText(mActiveMemory + "MB / " + mTotalMemory + "MB");
        mMemoryProgress.setMax((int) mTotalMemory);
        mMemoryProgress.setProgress((int) mActiveMemory);

        mBatteryInfo.setText("Battery temp: " + parseTemp(mBatteryTemp) + "- Health: " + mBatteryHealth);
        mBatteryTempBar.setProgress(parseTempInt(mBatteryTemp));
    }

    private String parseTemp(long temp) {
        String s = Long.toString(temp);
        return s.substring(0, s.length() - 1) + " C";
    }

    private int parseTempInt(long temp) {
        String s = parseTemp(temp);
        String si[] = s.split(" ");
        return Integer.parseInt(si[0]);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCPUThread == null) {
            mCPUThread = new CPUThread();
            mCPUThread.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCPUThread != null) {
            if (mCPUThread.isAlive()) {
                mCPUThread.interrupt();
                try {
                    mCPUThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mCPUThread = null;
        }
    }

    public void readFile(TextView tView, String fName) {
        FileReader fr = null;
        try {
            fr = new FileReader(fName);
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();
            while (null != line) {
                tView.append(line);
                tView.append("\n");
                line = br.readLine();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (null != fr) {
                try {
                    fr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateMemoryInfo() {
        long result = 0;
        try {
            String firstLine = readLine("/proc/meminfo", 1);
            if (firstLine != null) {
                String parts[] = firstLine.split("\\s+");
                if (parts.length == 3) {
                    result = Long.parseLong(parts[1])/1024;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mTotalMemory = result;

        try {
            String firstLine = readLine("/proc/meminfo", 2);
            if (firstLine != null) {
                String parts[] = firstLine.split("\\s+");
                if (parts.length == 3) {
                    result = Long.parseLong(parts[1])/1024;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mTotalUsedMemory = result;

        try {
            String firstLine = readLine("/proc/meminfo", 6);
            if (firstLine != null) {
                String parts[] = firstLine.split("\\s+");
                if (parts.length == 3) {
                    result = Long.parseLong(parts[1])/1024;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mActiveMemory = result;
    }

    private void updateBatteryInfo() {
        long l = 0;
        try {
            String firstLine = readLine("/sys/class/power_supply/battery/uevent", 21);
            if (firstLine != null) {
                String parts[] = firstLine.split("=");
                if (parts.length == 2) {
                    l = Long.parseLong(parts[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mBatteryTemp = l;

        String s = "";
        try {
            String firstLine = readLine("/sys/class/power_supply/battery/uevent", 5);
            if (firstLine != null) {
                String parts[] = firstLine.split("=");
                if (parts.length == 2) {
                    s = parts[1];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mBatteryHealth = s;
    }

    private static String readLine(String filename, int line) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        for(int i = 1; i < line; i++) {
            reader.readLine();
        }
        String s = reader.readLine();
        reader.close();
        return s;
    }
}
