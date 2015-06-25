package com.slim.performance;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentStatePagerAdapter;

import com.slim.ota.R;

import java.util.ArrayList;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    private Context mContext;

    private static ArrayList<E> ENTRIES = new ArrayList<>();

    static {
        ENTRIES.add(new E(new TimeInState(), R.string.time_in_state));
        ENTRIES.add(new E(new Performance(), R.string.performance));
        ENTRIES.add(new E(new CPUInfo(), R.string.cpu_info_title));
        ENTRIES.add(new E(new GovernorControl(), R.string.governor_control));
    }

    public ViewPagerAdapter(Context context, FragmentManager manager) {
        super(manager);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        return ENTRIES.get(position).fragment;
    }

    @Override
    public int getCount() {
        return ENTRIES.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getString(ENTRIES.get(position).title);
    }

    public static class E {

        public Fragment fragment;
        public int title;

        public E(Fragment f, int i) {
            fragment = f;
            title = i;
        }
    }
}
