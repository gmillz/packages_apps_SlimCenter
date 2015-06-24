/*
 * Copyright (C) 2014 The SlimRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.slim.performance;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import com.slim.ota.R;
import com.slim.center.settings.SettingsProvider;

public class Performance extends Fragment implements SeekBar.OnSeekBarChangeListener, Paths {
    private SeekBar mMaxSlider;
    private TextView mMaxSpeed;
    private SeekBar mMinSlider;
    private TextView mMinSpeed;
    private TextView mCurrentSpeed;

    private Spinner mGovernor;
    private Spinner mIOScheduler;

    private String[] mAvailableFrequencies;
    private String mMaxFreq;
    private String mMinFreq;

    private Context mContext;

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
            updateValues();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.performance, root, false);

        mAvailableFrequencies = Utils.readOneLine(FREQ_LIST_FILE).split(" ");
        int frequenciesNum = mAvailableFrequencies.length - 1;
        Arrays.sort(mAvailableFrequencies, new Comparator<String>() {
            @Override
            public int compare(String object1, String object2) {
                return Integer.valueOf(object1).compareTo(Integer.valueOf(object2));
            }
        });

        if (Utils.fileExists(DYN_MAX_FREQ)) {
            mMaxFreq = Utils.readOneLine(DYN_MAX_FREQ);
        } else {
            mMaxFreq = Utils.readOneLine(MAX_FREQ_FILE);
        }
        if (Utils.fileExists(DYN_MIN_FREQ)) {
            mMinFreq = Utils.readOneLine(DYN_MIN_FREQ);
        } else {
            mMinFreq = Utils.readOneLine(MIN_FREQ_FILE);
        }

        mCurrentSpeed = (TextView) view.findViewById(R.id.current_speed);
        mCurrentSpeed.setText(Utils.toMHz(Utils.readOneLine(CUR_FREQ_FILE)));

        mMaxSlider = (SeekBar) view.findViewById(R.id.max_slider);
        mMaxSlider.setMax(frequenciesNum);
        mMaxSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(mMaxFreq));
        mMaxSlider.setOnSeekBarChangeListener(this);
        mMaxSpeed = (TextView) view.findViewById(R.id.max_speed_text);
        mMaxSpeed.setText(Utils.toMHz(mMaxFreq));

        mMinSlider = (SeekBar) view.findViewById(R.id.min_slider);
        mMinSlider.setMax(frequenciesNum);
        mMinSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(mMinFreq));
        mMinSlider.setOnSeekBarChangeListener(this);
        mMinSpeed = (TextView) view.findViewById(R.id.min_speed_text);
        mMinSpeed.setText(Utils.toMHz(mMinFreq));

        String currentGov = Utils.readOneLine(GOV_FILE);
        String[] availableGovernors = Utils.readOneLine(GOV_LIST_FILE).split(" ");

        mGovernor = (Spinner) view.findViewById(R.id.governor);
        ArrayAdapter<CharSequence> governorAdapter = new ArrayAdapter<>(
                mContext, android.R.layout.simple_spinner_item);
        governorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (String governor : availableGovernors) {
            governorAdapter.add(governor);
        }
        mGovernor.setAdapter(governorAdapter);
        mGovernor.setSelection(Arrays.asList(availableGovernors).indexOf(currentGov));
        mGovernor.post(new Runnable() {
            public void run() {
                mGovernor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> adapterView, View view, int pos, long l) {
                        String selected = adapterView.getItemAtPosition(pos).toString();
                        for (int i = 0;  i < Utils.getNumOfCpus(); i++) {
                            Utils.writeValue(GOV_FILE.replace("cpu0", "cpu" + i), selected);
                        }
                        SettingsProvider.putString(mContext, PREF_GOVERNOR, selected);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });
            }
        });

        String currentIOSched = Utils.getCurrentIOScheduler();
        String[] availableIOScheds = Utils.getAvailableIOSchedulers();

        mIOScheduler = (Spinner) view.findViewById(R.id.io_scheduler);
        ArrayAdapter<CharSequence> ioAdapter = new ArrayAdapter<>(
                mContext, android.R.layout.simple_spinner_item);
        ioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (String sched : availableIOScheds) {
            ioAdapter.add(sched);
        }
        mIOScheduler.setAdapter(ioAdapter);
        mIOScheduler.setSelection(Arrays.asList(availableIOScheds).indexOf(currentIOSched));
        mIOScheduler.post(new Runnable() {
            @Override
            public void run() {
                mIOScheduler.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView,
                                               View view, int position, long l) {
                        String selected = adapterView.getItemAtPosition(position).toString();
                        for (String path : IO_SCHEDULER_PATH) {
                            if (new File(path).exists()) {
                                Utils.writeValue(path, selected);
                            }
                        }
                        SettingsProvider.putString(mContext, PREF_IOSCHED, selected);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });
            }
        });

        Switch setOnBoot = (Switch) view.findViewById(R.id.cpu_set_on_boot);
        setOnBoot.setChecked(SettingsProvider.getBoolean(mContext, PREF_CPU_SOB, false));
        setOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SettingsProvider.putBoolean(mContext, PREF_CPU_SOB, b);
            }
        });

        /*FileObserver maxObserver = new FileObserver(maxFile) {
            @Override
            public void onEvent(int i, String s) {
                mMaxSpeed.setText(Utils.toMHz(Utils.readOneLine(s)));
            }
        };
        maxObserver.startWatching();

        FileObserver minObserver = new FileObserver(minFile) {
            @Override
            public void onEvent(int i, String s) {
                mMinSpeed.setText(Utils.toMHz(Utils.readOneLine(s)));
            }
        };
        minObserver.startWatching();

        FileObserver currentObserver = new FileObserver(CUR_FREQ_FILE) {
            @Override
            public void onEvent(int i, String s) {
                mCurrentSpeed.setText(Utils.toMHz(Utils.readOneLine(s)));
            }
        };
        currentObserver.startWatching();*/

        return view;
    }

    private void updateValues() {
        final String curFreq = Utils.readOneLine(CUR_FREQ_FILE);
        if (curFreq != null)
            mCurrentSpeed.setText(Utils.toMHz(curFreq));
        final String maxFreq = Utils.readOneLine(MAX_FREQ_FILE);
        if (maxFreq != null) {
            mMaxFreq = maxFreq;
            mMaxSpeed.setText(Utils.toMHz(maxFreq));
            mMaxSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(maxFreq));
        }
        final String minFreq = Utils.readOneLine(MIN_FREQ_FILE);
        if (minFreq != null) {
            mMinFreq = minFreq;
            mMinSpeed.setText(Utils.toMHz(minFreq));
            mMinSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(minFreq));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        for (int i = 0; i < Utils.getNumOfCpus(); i++) {
            Utils.writeValue(MAX_FREQ_FILE.replace("cpu0", "cpu" + i), mMaxFreq);
            Utils.writeValue(MIN_FREQ_FILE.replace("cpu0", "cpu" + i), mMinFreq);
        }
        //if (mIsDynFreq) {
        //    Utils.writeOneLine(DYN_MAX_FREQ_PATH, mMaxFreqSetting);
        //    Utils.writeOneLine(DYN_MIN_FREQ_PATH, mMinFreqSetting);
        //}
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (seekBar.getId() == R.id.max_slider) {
                setMaxSpeed(progress);
            } else if (seekBar.getId() == R.id.min_slider) {
                setMinSpeed(progress);
            }
        }
    }

    public void setMaxSpeed(int progress) {
        String current;
        current = mAvailableFrequencies[progress];
        int minSliderProgress = mMinSlider.getProgress();
        if (progress <= minSliderProgress) {
            mMinSlider.setProgress(progress);
            mMinSpeed.setText(Utils.toMHz(current));
            mMinFreq = current;
        }
        mMaxSpeed.setText(Utils.toMHz(current));
        mMaxFreq = current;
        SettingsProvider.putString(mContext, PREF_MAX_FREQ, current);
    }

    public void setMinSpeed(int progress) {
        String current;
        current = mAvailableFrequencies[progress];
        int maxSliderProgress = mMaxSlider.getProgress();
        if (progress >= maxSliderProgress) {
            mMaxSlider.setProgress(progress);
            mMaxSpeed.setText(Utils.toMHz(current));
            mMaxFreq = current;
        }
        mMinSpeed.setText(Utils.toMHz(current));
        mMinFreq = current;
        SettingsProvider.putString(mContext, PREF_MIN_FREQ, current);
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

    public static void restore(Context context) {
        Log.d("CPUSettings", "restoring cpu settings");
        if (SettingsProvider.getBoolean(context, PREF_CPU_SOB, false)) {
            String maxFreq = SettingsProvider.getString(context,  PREF_MAX_FREQ, null);
            String minFreq = SettingsProvider.getString(context, PREF_MIN_FREQ, null);
            String governor = SettingsProvider.getString(context, PREF_GOVERNOR, null);
            String iosched = SettingsProvider.getString(context, PREF_IOSCHED, null);

            if (maxFreq != null) {
                Utils.writeValue(MAX_FREQ_FILE, maxFreq);
            }

            if (minFreq != null) {
                Utils.writeValue(MIN_FREQ_FILE, minFreq);
            }

            if (governor != null) {
                Utils.writeValue(GOV_FILE, governor);
            }

            if (iosched != null) {
                for (String path : IO_SCHEDULER_PATH) {
                    if (new File(path).exists()) {
                        Utils.writeValue(path, iosched);
                    }
                }
            }
        }
    }
}
