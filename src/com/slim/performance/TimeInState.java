package com.slim.performance;

import static com.slim.performance.Paths.PREF_OFFSETS;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseLongArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ShareActionProvider;
import android.widget.Spinner;
import android.widget.TextView;

import com.slim.ota.R;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class TimeInState extends Fragment {

    private static final String TAG = TimeInState.class.getName();

    private LinearLayout mStatesView;
    private TextView mTotalStateTime;
    private TextView mStatesWarning;
    private TextView mCPUInfo;
    private boolean mUpdatingData = false;
    private CPUStateMonitor monitor = new CPUStateMonitor();
    private Context context;
    private SharedPreferences mPreferences;
    private boolean mOverallStats;
    private int mCpuNum;
    private ShareActionProvider mProvider;
    private Spinner mPeriodTypeSelect;
    private LinearLayout mProgress;
    private int mPeriodType = 1;
    private boolean sHasRefData;

    private static final int MENU_REFRESH = Menu.FIRST;
    private static final int MENU_SHARE = MENU_REFRESH + 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        mOverallStats = monitor.hasOverallStats();
        mCpuNum = Utils.getNumOfCpus();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mPeriodType = mPreferences.getInt("which", 1);
        if (savedInstanceState != null) {
            mUpdatingData = savedInstanceState.getBoolean("updatingData");
            mPeriodType = savedInstanceState.getInt("which");
        }

        loadOffsets();

        setHasOptionsMenu(true);

        mProvider = new ShareActionProvider(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, root, savedInstanceState);

        View view = inflater.inflate(R.layout.time_in_state, root, false);

        mStatesView = (LinearLayout) view.findViewById(R.id.states_view);
        mStatesWarning = (TextView) view.findViewById(R.id.states_warning);
        mTotalStateTime = (TextView) view
                .findViewById(R.id.total_state_time);
        mTotalStateTime.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (mPeriodType == 0 && !sHasRefData) {
                    createResetPoint();
                }
            }
        });

        mCPUInfo = (TextView) view.findViewById(R.id.cpu_info);
        updateData();

        mPeriodTypeSelect = (Spinner) view
                .findViewById(R.id.period_type_select);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, R.array.period_type_entries, R.layout.period_type_item);
        mPeriodTypeSelect.setAdapter(adapter);

        mPeriodTypeSelect
                .setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view, int position, long id) {
                        mPeriodType = position;
                        if (position == 0) {
                            loadOffsets();
                        } else if (position == 1) {
                            monitor.removeOffsets();
                        }
                        refreshData();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });
        mPeriodTypeSelect.setSelection(mPeriodType);
        mProgress = (LinearLayout) view.findViewById(R.id.progress);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("updatingData", mUpdatingData);
        outState.putInt("which", mPeriodType);
    }

    @Override
    public void onResume() {
        refreshData();
        super.onResume();
    }

    @Override
    public void onPause() {
        mPreferences.edit().putInt("which", mPeriodType).apply();
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.cpu_menu, menu);

        menu.add(0, MENU_REFRESH, 0, R.string.refresh)
                .setAlphabeticShortcut('r')
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM
                                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(1, MENU_SHARE, 0, R.string.share)
                .setAlphabeticShortcut('s')
                .setActionProvider(mProvider)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM
                                | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_REFRESH:
                refreshData();
                break;
            case R.id.reset:
                createResetPoint();
                break;
        }

        return true;
    }

    private void updateData() {
        mCPUInfo.setText("");
        readFile(mCPUInfo, Paths.CPU_INFO_PATH);
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

    private void createResetPoint() {
        try {
            monitor.setOffsets();
        } catch (Exception e) {
            // not good
        }
        saveOffsets();
        if (mPeriodType == 1) {
            monitor.removeOffsets();
        }
        refreshData();
    }

    public void updateView() {
        Log.d(TAG, "updateView " + mUpdatingData);
        if (mUpdatingData) {
            return;
        }

        StringBuilder data = new StringBuilder();
        mStatesView.removeAllViews();

        if (monitor.getStates(0).size() == 0) {
            mStatesWarning.setVisibility(View.VISIBLE);
            mTotalStateTime.setVisibility(View.GONE);
            mStatesView.setVisibility(View.GONE);
        } else {
            if (mPeriodType == 0 && !sHasRefData) {
                mTotalStateTime.setText(getResources().getString(R.string.no_stat_because_reset));
            } else {
                long totTime = getStateTime();
                data.append(totTime).append("\n");
                totTime = totTime / 100;
                if (mOverallStats) {
                    int cpu;
                    for (CPUStateMonitor.CpuState state : monitor.getStates(0)) {
                        if (state.freq == 0) {
                            continue;
                        }
                        data.append(state.mCpu).append(" ").append(state.freq).append(" "
                               ).append(state.getDuration()).append("\n");
                        generateStateRowHeader(state, mStatesView);
                        generateStateRow(state, mStatesView);
                        for (cpu = 1; cpu < mCpuNum; cpu++) {
                            state = monitor.getFreqState(cpu, state.freq);
                            generateStateRow(state, mStatesView);
                            data.append(state.mCpu).append(" ").append(state.freq).append(" "
                                   ).append(state.getDuration()).append("\n");
                        }
                    }
                } else {
                    for (CPUStateMonitor.CpuState state : monitor.getStates(0)) {
                        if (state.freq == 0) {
                            continue;
                        }
                        generateStateRowHeader(state, mStatesView);
                        generateStateRow(state, mStatesView);
                        data.append(state.freq).append(" ").append(state.getDuration()).append("\n");
                    }
                }

                CPUStateMonitor.CpuState deepSleepState = monitor.getDeepSleepState();
                if (deepSleepState != null) {
                    generateStateRowHeader(deepSleepState, mStatesView);
                    generateStateRow(deepSleepState, mStatesView);
                    data.append(deepSleepState.freq).append(" "
                           ).append(deepSleepState.getDuration()).append("\n");
                }
                mTotalStateTime.setText(getResources().getString(R.string.total_time)
                       + " " + toString(totTime));
            }
        }
        updateShareIntent(data.toString());
    }

    public void refreshData() {
        if (!mUpdatingData) {
            new RefreshStateDataTask().execute((Void) null);
        }
    }

    private static String toString(long tSec) {
        long h = (long) Math.floor(tSec / (60 * 60));
        long m = (long) Math.floor((tSec - h * 60 * 60) / 60);
        long s = tSec % 60;
        String sDur;
        sDur = h + ":";
        if (m < 10)
            sDur += "0";
        sDur += m + ":";
        if (s < 10)
            sDur += "0";
        sDur += s;

        return sDur;
    }

    private View generateStateRow(CPUStateMonitor.CpuState state, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        LinearLayout view = (LinearLayout) inflater.inflate(
                R.layout.state_row_line, parent, false);

        float per = 0f;
        String sPer = "";
        String sDur = "";
        String sCpu = " ";
        long tSec = 0;

        if (state != null) {
            long duration = state.getDuration();
            if (duration != 0) {
                per = (float) duration * 100 / getStateTime();
                if (per > 100f) {
                    per = 0f;
                }
                tSec = duration / 100;
            }
            sPer = String.format("%3d", (int) per) + "%";
            sDur = toString(tSec);
            if (state.freq != 0 && mOverallStats) {
                sCpu = String.valueOf(state.mCpu);
            }
        }

        TextView cpuText = (TextView) view.findViewById(R.id.ui_cpu_text);
        TextView durText = (TextView) view.findViewById(R.id.ui_duration_text);
        TextView perText = (TextView) view
                .findViewById(R.id.ui_percentage_text);
        ProgressBar bar = (ProgressBar) view.findViewById(R.id.ui_bar);

        cpuText.setText(sCpu);
        perText.setText(sPer);
        durText.setText(sDur);
        bar.setProgress((int) per);

        parent.addView(view);
        return view;
    }

    private View generateStateRowHeader(CPUStateMonitor.CpuState state, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        LinearLayout view = (LinearLayout) inflater.inflate(
                R.layout.state_row_header, parent, false);

        String sFreq;
        if (state.freq == 0) {
            sFreq = getString(R.string.deep_sleep);
        } else {
            sFreq = state.freq / 1000 + " MHz";
        }

        TextView freqText = (TextView) view.findViewById(R.id.ui_freq_text);
        freqText.setText(sFreq);

        parent.addView(view);
        return view;
    }

    protected class RefreshStateDataTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... v) {
            try {
                monitor.updateStates();
            } catch (CPUStateMonitor.CPUStateMonitorException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            mProgress.setVisibility(View.VISIBLE);
            mStatesView.setVisibility(View.GONE);
            mUpdatingData = true;
        }

        @Override
        protected void onPostExecute(Void v) {
            try {
                mProgress.setVisibility(View.GONE);
                mStatesView.setVisibility(View.VISIBLE);
                mUpdatingData = false;
                updateView();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void loadOffsets() {
        String prefs = mPreferences.getString(PREF_OFFSETS, "");
        if (prefs == null || prefs.length() < 1) {
            return;
        }
        String[] cpus = prefs.split(":");
        if (cpus.length != mCpuNum) {
            return;
        }
        for (int cpu = 0; cpu < mCpuNum; cpu++) {
            String cpuData = cpus[cpu];
            SparseLongArray offsets = new SparseLongArray();
            String[] sOffsets = cpuData.split(",");
            for (String offset : sOffsets) {
                String[] parts = offset.split(" ");
                offsets.put(Integer.parseInt(parts[0]),
                        Long.parseLong(parts[1]));
            }
            monitor.setOffsets(cpu, offsets);
        }
        sHasRefData = true;
    }

    public void saveOffsets() {
        SharedPreferences.Editor editor = mPreferences.edit();
        String str = "";
        for (int cpu = 0; cpu < mCpuNum; cpu++) {
            SparseLongArray a = monitor.getOffsets(cpu);
            for (int i = 0; i < monitor.getOffsets(cpu).size(); i++) {
                str += a.keyAt(i) + " " + a.get(a.keyAt(i)) + ",";
            }
            str += ":";
        }
        editor.putString(PREF_OFFSETS, str).apply();
        sHasRefData = true;
    }

    private long getStateTime() {
        return monitor.getTotalStateTime(0, true);
    }

    private void updateShareIntent(String data) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, data);
        mProvider.setShareIntent(shareIntent);
    }
}
