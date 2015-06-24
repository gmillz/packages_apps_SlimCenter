package com.slim.performance;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.slim.ota.R;
import com.slim.center.settings.SettingsProvider;

import java.util.List;

public class GovernorControl extends Fragment implements Paths {

    private static final int DIALOG_EDIT = 0;
    private List<String> mValues;
    private List<String> mFiles;
    private ListAdapter mAdapter;
    private String mCurrent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFiles = Utils.getGovernorControl();
        mValues = Utils.getGovernorControlValues();
        mAdapter = new ListAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.governor_control, root, false);

        ListView listView = (ListView) view.findViewById(R.id.ListView);

        mAdapter.setListItems(mFiles);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mCurrent = mFiles.get(i);
                showDialogInner(DIALOG_EDIT, mCurrent, mValues.get(i));
            }
        });
        return view;
    }

    public class ListAdapter extends BaseAdapter {
        private List<String> mResults;

        public ListAdapter() {
        }

        @Override
        public int getCount() {
            return mResults.size();
        }

        @Override
        public Object getItem(int position) {
            return mResults.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            final String title = mResults.get(position);
            final String value =
                    Utils.readOneLine(Utils.getGovernorControlPath() + "/" + title);
            if (convertView == null) {
                convertView = View.inflate(getActivity(), R.layout.governor_item, null);
                holder = new ViewHolder();
                holder.mTitle = (TextView) convertView.findViewById(R.id.title);
                holder.mTitle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showDialogInner(DIALOG_EDIT, title, value);
                    }
                });
                holder.mValue = (TextView) convertView.findViewById(R.id.value);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.setTitle(title);
            holder.setValue(value);
            return convertView;
        }

        public void setListItems(List<String> voltages) {
            mResults = voltages;
        }

        public class ViewHolder {
            private TextView mTitle;
            private TextView mValue;

            public void setTitle(String title) {
                mTitle.setText(title);
            }

            public void setValue(String value) {
                mValue.setText(value);
            }
        }
    }

    private void showDialogInner(int id, String title, String value) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id, title, value);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id, String title, String value) {
            MyAlertDialogFragment fragment = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putString("value", value);
            args.putString("title", title);
            fragment.setArguments(args);
            return fragment;
        }

        GovernorControl getOwner() {
            return (GovernorControl) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            final String value = getArguments().getString("value");
            final String title = getArguments().getString("title");

            switch (id) {
                case DIALOG_EDIT:
                    View editDialog = View.inflate(
                            getOwner().getActivity(), R.layout.dialog_edit, null);

                    final EditText editText = (EditText) editDialog.findViewById(R.id.editText);

                    editText.setText(value);

                    return new AlertDialog.Builder(getActivity())
                            .setTitle(title)
                            .setView(editDialog)
                            .setPositiveButton(getResources().getString(R.string.apply),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            String val = editText.getText().toString();
                                            Log.d("TEST", "name=" + title + " : value=" + val);
                                            Utils.writeValue(
                                                    Utils.getGovernorControlPath() + "/" + title, val);
                                            SettingsProvider.putString(getActivity(), value, val);
                                        }
                                    })
                            .setNegativeButton(getString(R.string.cancel),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {

                                        }
                                    })
                            .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }
    }

    public static void restore(Context ctx) {
        List<String> files = Utils.getGovernorControl();
        SharedPreferences prefs = SettingsProvider.get(ctx);
        if (files.size() != 0) {
            for (String file : files) {
                String value = prefs.getString(file, "0");
                Utils.writeValue(Utils.getGovernorControlPath() + "/" + file, value);
            }
        }
    }
}
