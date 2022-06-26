package com.lmwis.appusage;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import com.lmwis.appusage.service.DataService;
import com.lmwis.appusage.service.GyroService;
import com.lmwis.appusage.service.LocationService;
import com.lmwis.appusage.store.DataStore;

public class MainActivity extends AppCompatActivity {

    public static final AppUsageStatisticsFragment appUsageStatisticsFragment = AppUsageStatisticsFragment.newInstance();
    public static final LocationFragment locationFragment = new LocationFragment();
    public static final GyroFragment gyroFragment = new GyroFragment();

    public static final DataStore dataStore = new DataStore();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hasPermissionToReadNetworkStats();
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, appUsageStatisticsFragment)
                    .commit();
        }

        startForegroundService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService();
    }
    public void startForegroundService() {
        startForegroundService(new Intent(getBaseContext(), DataService.class));
        startForegroundService(new Intent(getBaseContext(), LocationService.class));
        startForegroundService(new Intent(getBaseContext(), GyroService.class));
    }

    public void stopService() {
        stopService(new Intent(getBaseContext(), DataService.class));
        stopService(new Intent(getBaseContext(), LocationService.class));
        stopService(new Intent(getBaseContext(), GyroService.class));
    }

    private boolean hasPermissionToReadNetworkStats() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        final AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        if (mode == AppOpsManager.MODE_ALLOWED) {
            return true;
        }

        requestReadNetworkStats();
        return false;
    }

    // 打开“有权查看使用情况的应用”页面
    private void requestReadNetworkStats() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
    }
}