package com.slim.performance;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentStatePagerAdapter;

import com.slim.center.R;

import java.util.ArrayList;

class ViewPagerAdapter extends FragmentStatePagerAdapter {

    private Context mContext;

    private static ArrayList<Fragment> mFragments = new ArrayList<>();
    private static ArrayList<Integer> mTitles = new ArrayList<>();

    static {
        mFragments.add(new Performance());
        mTitles.add(R.string.performance);
        mFragments.add(new GovernorControl());
        mTitles.add(R.string.governor_control);
    }

    public ViewPagerAdapter(Context context, FragmentManager manager) {
        super(manager);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getString(mTitles.get(position));
    }
}
