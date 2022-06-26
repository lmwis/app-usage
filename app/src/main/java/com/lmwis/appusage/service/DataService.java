package com.lmwis.appusage.service;

import static com.amap.api.maps.model.BitmapDescriptorFactory.getContext;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import com.lmwis.appusage.MainActivity;
import com.lmwis.appusage.ScreenObserver;
import com.lmwis.appusage.rpc.ActionEnum;
import com.lmwis.appusage.rpc.pojo.Constant;
import com.lmwis.appusage.rpc.pojo.IphoneActionDTO;
import com.lmwis.appusage.store.DataStore;

import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;

public class DataService extends Service {

    // 最低唤醒频率为5s
    int TIME_INTERVAL = 1000 * 5;

    int i = 0;

    public static final int NOTICE_ID = 100;

    public static final String CHANNEL_ID_STRING = "lmwis001";

    public static String app_name="app-usage";

    private static final String TAG = DataService.class.getSimpleName();

    private int Time = 1000*2;
    private Timer timer = new Timer();

    private ScreenObserver mScreenObserver;

    PendingIntent pendingIntent;
    AlarmManager alarmManager;

    UsageStatsManager mUsageStatsManager;
    SharedPreferences sp;

    public static final String TEST_ACTION = "LMWIS" + "_TEST_ACTION";

    PowerManager.WakeLock wakeLock = null;

    DataStore dataStore;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        dataStore = MainActivity.dataStore;
        mUsageStatsManager = (UsageStatsManager)getSystemService(Context.USAGE_STATS_SERVICE); //Context.USAGE_STATS_SERVICE
        sp = getSharedPreferences(Constant.USER_STORE_KEY,Context.MODE_PRIVATE);;

        // 屏幕关闭持续运行
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, DataService.class.getName());
        wakeLock.acquire();

        IntentFilter intentFilter = new IntentFilter(TEST_ACTION);
        registerReceiver(receiver, intentFilter);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intentA = new Intent();
        intentA.setAction(TEST_ACTION);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intentA, 0);

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), pendingIntent);

        mScreenObserver = new ScreenObserver(this);
        mScreenObserver.requestScreenStateUpdate(new ScreenObserver.ScreenStateListener() {
            @Override
            public void onScreenOn() {
                Log.d(TAG,"屏幕打开：" + new Date());
                IphoneActionDTO iphoneActionDTO = new IphoneActionDTO();
                iphoneActionDTO.setActionCode(ActionEnum.SCREEN_OPEN.getActionCode());
                iphoneActionDTO.setActionName(ActionEnum.SCREEN_OPEN.getActionName());
                long now = System.currentTimeMillis();
                iphoneActionDTO.setGmtCreate(now);
                iphoneActionDTO.setGmtModified(now);
                dataStore.saveIphoneAction(iphoneActionDTO);
            }

            @Override
            public void onScreenOff() {
                Log.d(TAG,"屏幕关闭："+ new Date());
                IphoneActionDTO iphoneActionDTO = new IphoneActionDTO();
                iphoneActionDTO.setActionCode(ActionEnum.SCREEN_CLOSE.getActionCode());
                iphoneActionDTO.setActionName(ActionEnum.SCREEN_CLOSE.getActionName());
                long now = System.currentTimeMillis();
                iphoneActionDTO.setGmtCreate(now);
                iphoneActionDTO.setGmtModified(now);
                dataStore.saveIphoneAction(iphoneActionDTO);
            }

            @Override
            public void onScreenLock() {
                Log.d(TAG,"屏幕解锁："+ new Date());
                IphoneActionDTO iphoneActionDTO = new IphoneActionDTO();
                iphoneActionDTO.setActionCode(ActionEnum.SCREEN_LOCK.getActionCode());
                iphoneActionDTO.setActionName(ActionEnum.SCREEN_LOCK.getActionName());
                long now = System.currentTimeMillis();
                iphoneActionDTO.setGmtCreate(now);
                iphoneActionDTO.setGmtModified(now);
                dataStore.saveIphoneAction(iphoneActionDTO);
            }
        });

        //安卓8.0系统的特殊处理
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = null;
        mChannel = new NotificationChannel(CHANNEL_ID_STRING, app_name, NotificationManager.IMPORTANCE_HIGH);
        //使通知静音
        mChannel.setSound(null,null);
        notificationManager.createNotificationChannel(mChannel);
        Notification notification = new Notification.Builder(getApplicationContext(), CHANNEL_ID_STRING).build();
        startForeground(NOTICE_ID, notification);
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TEST_ACTION.equals(action)) {
                i++;
                Log.d(TAG,"定时任务执行 :" + new Date()+"   i:"+i );
                wakeLock.acquire();
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + TIME_INTERVAL, pendingIntent);

                // 读取usage event
                loadNewUsageEvent();
                dataStore.flushAllData();
            }
        }
    };

    public void loadNewUsageEvent(){
        long current = System.currentTimeMillis();
        // 当天凌晨
        long defaultValue = current-(current+ TimeZone.getDefault().getRawOffset())%(1000*3600*24);

        long startTime = sp.getLong(Constant.LAST_USAGE_EVENT_TIME_KEY,defaultValue );
        Log.d(TAG,"last start time"+startTime);
        long endTime = System.currentTimeMillis();
        ArrayList<UsageEvents.Event> eventList = getEventList(startTime, endTime);
        try {
            dataStore.saveBatchUsageEvent(eventList);

            SharedPreferences.Editor editor = sp.edit();
            editor.putLong(Constant.LAST_USAGE_EVENT_TIME_KEY, endTime);
            editor.apply();
            Log.i(TAG,"[batchUploadUsageEvent] 上次上报时间"+ sp.getLong(Constant.LAST_USAGE_EVENT_TIME_KEY,123L));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<UsageEvents.Event> getEventList(long startTime, long endTime){
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

    @Override
    public void onDestroy() {
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
        super.onDestroy();
    }
}
