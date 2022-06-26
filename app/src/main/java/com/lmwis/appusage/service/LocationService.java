package com.lmwis.appusage.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.ServiceSettings;
import com.lmwis.appusage.MainActivity;
import com.lmwis.appusage.R;
import com.lmwis.appusage.rpc.LocationInfoClient;
import com.lmwis.appusage.rpc.pojo.LocationInfoDTO;
import com.lmwis.appusage.store.DataStore;

public class LocationService extends Service {

    DataStore dataStore;

    public static final int NOTICE_ID = 101;

    public static final String CHANNEL_ID_STRING = "lmwis002";

    public static String app_name="app-usage";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.dataStore = MainActivity.dataStore;

        try {
            startLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }
        backgroundInit();
    }

    private void backgroundInit(){
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

    private static final String TAG = LocationService.class.getSimpleName();

    //地图中定位的类
    private LocationSource.OnLocationChangedListener mListener = null;
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;
    private final Long interval = 4000L;//定位时间间隔

    /**
     * 开始定位。
     */
    private void startLocation() {
        if (mLocationClient == null) {
            try {
                mLocationClient = new AMapLocationClient(getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
            //设置定位属性
            mLocationOption = new AMapLocationClientOption();
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
            mLocationOption.setGpsFirst(false);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
            mLocationOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
            mLocationOption.setInterval(interval);//可选，设置定位间隔。默认为2秒
            mLocationOption.setNeedAddress(false);//可选，设置是否返回逆地理地址信息。默认是true
            mLocationOption.setOnceLocation(false);//可选，设置是否单次定位。默认是false
            mLocationOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
            AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
            mLocationOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
            mLocationOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
            mLocationOption.setLocationCacheEnable(true); //可选，设置是否使用缓存定位，默认为true
            mLocationOption.setGeoLanguage(AMapLocationClientOption.GeoLanguage.ZH);//可选，设置逆地理信息的语言，默认值为默认语言（根据所在地区选择语言）
            mLocationClient.setLocationOption(mLocationOption);

            // 设置定位监听
            mLocationClient.setLocationListener(aMapLocationListener);
            //开始定位
            mLocationClient.startLocation();
        }
    }
    /**
     * 定位结果回调
     *
     * @param aMapLocation 位置信息类
     */
    private AMapLocationListener aMapLocationListener = aMapLocation -> {
        if (null == aMapLocation)
            return;
        if (aMapLocation.getErrorCode() == 0) {
            //先暂时获得经纬度信息，并将其记录在List中
            Log.d(TAG, "[LocationService]纬度信息为"
                    + aMapLocation.getLatitude() + "\n经度信息为"
                    + aMapLocation.getLongitude());
//            LatLng locationValue = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
            // 写入数据中心
//            LocationInfoClient.saveToCenter(aMapLocation.getLatitude()+"",aMapLocation.getLongitude()+"");
            //定位成功
            LocationInfoDTO locationInfoDTO = new LocationInfoDTO();
            long now = System.currentTimeMillis();
            locationInfoDTO.setLatitude(String.valueOf(aMapLocation.getLatitude()));
            locationInfoDTO.setLongitude(String.valueOf(aMapLocation.getLongitude()));
            locationInfoDTO.setGmtModified(now);
            locationInfoDTO.setGmtCreate(now);
            dataStore.saveLocationInfo(locationInfoDTO);

        } else {
            String errText = "定位失败," + aMapLocation.getErrorCode() + ": " + aMapLocation.getErrorInfo();
            Log.e(TAG, "AmapErr "+errText);
        }
    };

    @Override
    public void onDestroy() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
        super.onDestroy();
    }

}
