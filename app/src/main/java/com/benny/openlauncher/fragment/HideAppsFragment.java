package com.benny.openlauncher.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.benny.openlauncher.R;
import com.benny.openlauncher.model.AppInfo;
import com.benny.openlauncher.util.App;
import com.benny.openlauncher.util.AppManager;
import com.benny.openlauncher.util.AppSettings;
import com.benny.openlauncher.util.Definitions;

import java.util.ArrayList;
import java.util.List;

public class HideAppsFragment extends Fragment {
    private static final String TAG = "RequestActivity";
    private static final boolean DEBUG = true;

    private ArrayList<String> _listActivitiesHidden = new ArrayList();
    private ArrayList<AppInfo> _listActivitiesAll = new ArrayList();
    private AsyncWorkerList _taskList = new AsyncWorkerList();
    private AppAdapter _appInfoAdapter;
    private ViewSwitcher _switcherLoad;
    private ListView _grid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.request, container, false);
        _switcherLoad = rootView.findViewById(R.id.viewSwitcherLoadingMain);

        FloatingActionButton fab = rootView.findViewById(R.id.fab_rq);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmSelection();
            }
        });

        if (_taskList.getStatus() == AsyncTask.Status.PENDING) {
            // task has not started yet
            _taskList.execute();
        }

        if (_taskList.getStatus() == AsyncTask.Status.FINISHED) {
            // task is done and onPostExecute has been called
            new AsyncWorkerList().execute();
        }

        return rootView;
    }

    public class AsyncWorkerList extends AsyncTask<String, Integer, String> {

        private AsyncWorkerList() {
        }

        @Override
        protected void onPreExecute() {
            List<String> hiddenList = AppSettings.get().getHiddenAppsList();
            _listActivitiesHidden.addAll(hiddenList);

            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... arg0) {
            try {
                // compare to installed apps
                prepareData();
                return null;
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            populateView();
            // switch from loading screen to the main view
            _switcherLoad.showNext();

            super.onPostExecute(result);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(savedInstanceState);
    }

    private void confirmSelection() {
        Thread actionSend_Thread = new Thread() {

            @Override
            public void run() {
                // update hidden apps
                AppSettings.get().setHiddenAppsList(_listActivitiesHidden);
                getActivity().finish();
            }
        };

        if (!actionSend_Thread.isAlive()) {
            // prevents thread from being executed more than once
            actionSend_Thread.start();
        }
    }

    private void prepareData() {
        List<App> apps = AppManager.getInstance(getContext()).getNonFilteredApps();

        for (App app : apps) {
            AppInfo tempAppInfo = new AppInfo(
                    app.getPackageName() + "/" + app.getClassName(),
                    app.getLabel(),
                    app.getIconProvider().getDrawableSynchronously(Definitions.NO_SCALE)
            );
            _listActivitiesAll.add(tempAppInfo);
        }
    }

    private void populateView() {
        _grid = getActivity().findViewById(R.id.app_grid);

        assert _grid != null;
        _grid.setFastScrollEnabled(true);
        _grid.setFastScrollAlwaysVisible(false);

        _appInfoAdapter = new AppAdapter(getActivity(), _listActivitiesAll);

        _grid.setAdapter(_appInfoAdapter);
        _grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> AdapterView, View view, int position, long row) {
                AppInfo appInfo = (AppInfo) AdapterView.getItemAtPosition(position);
                CheckBox checker = view.findViewById(R.id.CBappSelect);
                ViewSwitcher icon = view.findViewById(R.id.viewSwitcherChecked);

                checker.toggle();
                if (checker.isChecked()) {
                    _listActivitiesHidden.add(appInfo.getCode());
                    if (DEBUG) Log.v(TAG, "Selected App: " + appInfo.getName());
                    if (icon.getDisplayedChild() == 0) {
                        icon.showNext();
                    }
                } else {
                    _listActivitiesHidden.remove(appInfo.getCode());
                    if (DEBUG) Log.v(TAG, "Deselected App: " + appInfo.getName());
                    if (icon.getDisplayedChild() == 1) {
                        icon.showPrevious();
                    }
                }
            }
        });
    }

    private class AppAdapter extends ArrayAdapter<AppInfo> {
        private AppAdapter(Context context, ArrayList<AppInfo> adapterArrayList) {
            super(context, R.layout.request_item_list, adapterArrayList);
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.request_item_list, parent, false);
                holder = new ViewHolder();
                holder._apkIcon = convertView.findViewById(R.id.IVappIcon);
                holder._apkName = convertView.findViewById(R.id.TVappName);
                holder._apkPackage = convertView.findViewById(R.id.TVappPackage);
                holder._checker = convertView.findViewById(R.id.CBappSelect);
                holder._switcherChecked = convertView.findViewById(R.id.viewSwitcherChecked);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            AppInfo appInfo = getItem(position);

            holder._apkPackage.setText(appInfo.getCode());
            holder._apkName.setText(appInfo.getName());
            holder._apkIcon.setImageDrawable(appInfo.getImage());

            holder._switcherChecked.setInAnimation(null);
            holder._switcherChecked.setOutAnimation(null);
            holder._checker.setChecked(_listActivitiesHidden.contains(appInfo.getCode()));
            if (_listActivitiesHidden.contains(appInfo.getCode())) {
                if (holder._switcherChecked.getDisplayedChild() == 0) {
                    holder._switcherChecked.showNext();
                }
            } else {
                if (holder._switcherChecked.getDisplayedChild() == 1) {
                    holder._switcherChecked.showPrevious();
                }
            }
            return convertView;
        }
    }

    private class ViewHolder {
        TextView _apkName;
        TextView _apkPackage;
        ImageView _apkIcon;
        CheckBox _checker;
        ViewSwitcher _switcherChecked;
    }
}
