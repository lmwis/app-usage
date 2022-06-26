package com.lmwis.appusage;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lmwis.appusage.rpc.DataCenterClient;
import com.lmwis.appusage.rpc.pojo.Constant;
import com.lmwis.appusage.service.FileService;
import com.lmwis.appusage.service.UsageEventService;
import com.lmwis.appusage.store.DataStore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

public class AppUsageStatisticsFragment extends Fragment {
    private static final String TAG = AppUsageStatisticsFragment.class.getSimpleName();
    private static final SimpleDateFormat dateFormat  = new SimpleDateFormat("M-d-yyyy HH:mm:ss");

    //VisibleForTesting for variables below
    UsageStatsManager mUsageStatsManager;
    UsageListAdapter mUsageListAdapter;
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    Button mOpenUsageSettingButton;

    Button mSwitchLocationButton;
    Spinner mSpinner;
    SwipeRefreshLayout swipeRefreshLayout;
    SharedPreferences sp;
    DataStore dataStore;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment {@link AppUsageStatisticsFragment}.
     */
    public static AppUsageStatisticsFragment newInstance() {
        AppUsageStatisticsFragment fragment = new AppUsageStatisticsFragment();
        return fragment;
    }

    public AppUsageStatisticsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.dataStore = MainActivity.dataStore;
        mUsageStatsManager = (UsageStatsManager) getActivity()
                .getSystemService(Context.USAGE_STATS_SERVICE); //Context.USAGE_STATS_SERVICE
        this.sp = getContext().getSharedPreferences(Constant.USER_STORE_KEY,Context.MODE_PRIVATE);;
        // init
        FileService.res = getResources();
        DataCenterClient.pm = getActivity().getPackageManager();
        DataCenterClient.sp = getContext().getSharedPreferences(Constant.USER_STORE_KEY,Context.MODE_PRIVATE);

        UsageEventService.pm = getActivity().getPackageManager();
        UsageEventService.sp = getContext().getSharedPreferences(Constant.USER_STORE_KEY,Context.MODE_PRIVATE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_app_usage_statistics, container, false);
    }

    @Override
    public void onViewCreated(View rootView, Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        swipeRefreshLayout = rootView.findViewById(R.id.swipeLayout);
        mUsageListAdapter = new UsageListAdapter();
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview_app_usage);
        mLayoutManager = mRecyclerView.getLayoutManager();
        mRecyclerView.scrollToPosition(0);
        mRecyclerView.setAdapter(mUsageListAdapter);
        mOpenUsageSettingButton = (Button) rootView.findViewById(R.id.button_open_usage_setting);

        mSwitchLocationButton = (Button) getActivity().findViewById(R.id.switch_location);

        mSwitchLocationButton.setOnClickListener(v -> {
            FragmentManager manager = getActivity().getSupportFragmentManager();
            FragmentTransaction transaction=manager.beginTransaction();
            transaction.replace(R.id.container,MainActivity.locationFragment);
            transaction.commit();
        });

        mSpinner = (Spinner) rootView.findViewById(R.id.spinner_time_span);
        SpinnerAdapter spinnerAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.action_list, android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(spinnerAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            String[] strings = getResources().getStringArray(R.array.action_list);

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                StatsUsageInterval statsUsageInterval = StatsUsageInterval
                        .getValue(strings[position]);
                if (statsUsageInterval != null) {
                    List<UsageStats> usageStatsList =
                            getUsageStatistics(statsUsageInterval.mInterval);
                    Collections.sort(usageStatsList, new LastTimeLaunchedComparatorDesc());
                    updateAppsList(usageStatsList);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "[swipeRefreshLayout] 重新加载app记录");
                        List<UsageStats> usageStatsList =
                                getUsageStatistics(UsageStatsManager.INTERVAL_DAILY);


                        // 异步
                        new Thread(UsageEventService::getAllApplications).start();

                        Collections.sort(usageStatsList, new LastTimeLaunchedComparatorDesc());
                        updateAppsList(usageStatsList);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }, 2000);
            }
        });
    }

    /**
     * Returns the {@link #mRecyclerView} including the time span specified by the
     * intervalType argument.
     *
     * @param intervalType The time interval by which the stats are aggregated.
     *                     Corresponding to the value of {@link UsageStatsManager}.
     *                     E.g. {@link UsageStatsManager#INTERVAL_DAILY}, {@link
     *                     UsageStatsManager#INTERVAL_WEEKLY},
     * @return A list of {@link android.app.usage.UsageStats}.
     */
    public List<UsageStats> getUsageStatistics(int intervalType) {
        // Get the app statistics since one year ago from the current time.
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
//        long startTime = cal.getTimeInMillis();;
        long endTime = System.currentTimeMillis();

        long startTime = endTime-(endTime+ TimeZone.getDefault().getRawOffset())%(1000*3600*24);
        List<UsageStats> queryUsageStats = mUsageStatsManager
                .queryUsageStats(intervalType, startTime,
                        endTime);
        Log.d(TAG, "[getUsageStatistics] startTime:" + startTime + ", endTime:" + endTime);
        try {
            DataCenterClient.batchUploadAppUsage(queryUsageStats, startTime, endTime);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        if (queryUsageStats.size() == 0) {
            Log.i(TAG, "The user may not allow the access to apps usage. ");
            Toast.makeText(getActivity(),
                    getString(R.string.explanation_access_to_appusage_is_not_enabled),
                    Toast.LENGTH_LONG).show();
            mOpenUsageSettingButton.setVisibility(View.VISIBLE);
            mOpenUsageSettingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                }
            });
        }
        Log.d(TAG, "[getUsageStatistics] 获取到的数量:" + queryUsageStats.size());

        return queryUsageStats;
    }

    public ArrayList<UsageEvents.Event> getEventList( long startTime, long endTime){
        ArrayList<UsageEvents.Event> mEventList = new ArrayList<>();

        UsageEvents events = mUsageStatsManager.queryEvents(startTime, endTime);

        while (events.hasNextEvent()) {
            UsageEvents.Event e = new UsageEvents.Event();
            events.getNextEvent(e);
            if (e.getEventType() == 1 || e.getEventType() == 2) {
                mEventList.add(e);
            }
        }

        return mEventList;
    }
    public String getApplicationNameByPackageName(String packageName) {
        return UsageEventService.getApplicationNameByPackageName(getActivity().getPackageManager(),packageName);

    }
    public void getAllApplications(){
        PackageManager packageManager = getActivity().getPackageManager();

    }


    /**
     * Updates the {@link #mRecyclerView} with the list of {@link UsageStats} passed as an argument.
     *
     * @param usageStatsList A list of {@link UsageStats} from which update the
     *                       {@link #mRecyclerView}.
     */
    //VisibleForTesting
    void updateAppsList(List<UsageStats> usageStatsList) {
        List<CustomUsageStats> customUsageStatsList = new ArrayList<>();
        for (int i = 0; i < usageStatsList.size(); i++) {
            CustomUsageStats customUsageStats = new CustomUsageStats();
            customUsageStats.usageStats = usageStatsList.get(i);
            try {

//                Drawable appIcon = getActivity().getPackageManager()
//                        .getApplicationIcon(customUsageStats.usageStats.getPackageName());
                PackageManager pm = getActivity().getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(customUsageStats.usageStats.getPackageName(), 0);

                Drawable appIcon = info.loadIcon(pm);
                ;
                customUsageStats.appIcon = appIcon;
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, String.format("App Icon is not found for %s",
                        customUsageStats.usageStats.getPackageName()));
                customUsageStats.appIcon = getActivity()
                        .getDrawable(R.drawable.ic_launcher_background);
            }
            customUsageStatsList.add(customUsageStats);
        }
        mUsageListAdapter.setCustomUsageStatsList(customUsageStatsList);
        mUsageListAdapter.notifyDataSetChanged();
        mRecyclerView.scrollToPosition(0);
    }

    /**
     * The {@link Comparator} to sort a collection of {@link UsageStats} sorted by the timestamp
     * last time the app was used in the descendant order.
     */
    private static class LastTimeLaunchedComparatorDesc implements Comparator<UsageStats> {

        @Override
        public int compare(UsageStats left, UsageStats right) {
            return Long.compare(right.getLastTimeUsed(), left.getLastTimeUsed());
        }
    }


    /**
     * Enum represents the intervals for {@link android.app.usage.UsageStatsManager} so that
     * values for intervals can be found by a String representation.
     */
    //VisibleForTesting
    static enum StatsUsageInterval {
        DAILY("Daily", UsageStatsManager.INTERVAL_DAILY),
        WEEKLY("Weekly", UsageStatsManager.INTERVAL_WEEKLY),
        MONTHLY("Monthly", UsageStatsManager.INTERVAL_MONTHLY),
        YEARLY("Yearly", UsageStatsManager.INTERVAL_YEARLY);

        private int mInterval;
        private String mStringRepresentation;

        StatsUsageInterval(String stringRepresentation, int interval) {
            mStringRepresentation = stringRepresentation;
            mInterval = interval;
        }

        static StatsUsageInterval getValue(String stringRepresentation) {
            for (StatsUsageInterval statsUsageInterval : values()) {
                if (statsUsageInterval.mStringRepresentation.equals(stringRepresentation)) {
                    return statsUsageInterval;
                }
            }
            return null;
        }
    }


}
