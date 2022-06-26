package com.lmwis.appusage;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import com.lmwis.appusage.rpc.LocationInfoClient;

public class LocationFragment extends Fragment {
    private static final String TAG = LocationFragment.class.getSimpleName();


    MapView mapView;

    private AMap aMap;

    Button mSwitchAppButton;

    Button mSwitchGyroButton;


    //地图中定位的类
    private LocationSource.OnLocationChangedListener mListener = null;
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;
    private final Long interval = 4000L;//定位时间间隔

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        ServiceSettings.updatePrivacyShow(getContext(), true, true);
        ServiceSettings.updatePrivacyAgree(getContext(),true);

        mSwitchAppButton = (Button) getActivity().findViewById(R.id.switch_location);
        mSwitchGyroButton = (Button) getActivity().findViewById(R.id.switch_gyro);

        mSwitchAppButton.setOnClickListener(v -> {
            FragmentManager manager = getActivity().getSupportFragmentManager();
            FragmentTransaction transaction=manager.beginTransaction();
            transaction.replace(R.id.container,MainActivity.appUsageStatisticsFragment);
            transaction.commit();
        });
        mSwitchGyroButton.setOnClickListener(v -> {
            FragmentManager manager = getActivity().getSupportFragmentManager();
            FragmentTransaction transaction=manager.beginTransaction();
            transaction.replace(R.id.container,MainActivity.gyroFragment);
            transaction.commit();
        });
        mapView = getView().findViewById(R.id.mapView);

        mapView.onCreate(savedInstanceState);// 此方法必须重写

        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
    }
    /**
     * 开始定位。
     */
    private void startLocation() {
        if (mLocationClient == null) {
            try {
                mLocationClient = new AMapLocationClient(getContext());
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
            Log.d(TAG, "[SportMapActivity]纬度信息为"
                    + aMapLocation.getLatitude() + "\n经度信息为"
                    + aMapLocation.getLongitude());
//            LatLng locationValue = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
            // 写入数据中心
//            LocationInfoClient.saveToCenter(aMapLocation.getLatitude()+"",aMapLocation.getLongitude()+"");
            //定位成功
            updateLocation(aMapLocation);

        } else {
            String errText = "定位失败," + aMapLocation.getErrorCode() + ": " + aMapLocation.getErrorInfo();
            Log.e(TAG, "AmapErr "+errText);
        }
    };

    private void updateLocation(AMapLocation aMapLocation){
//        record.addpoint(new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude()));
        if (mListener != null)
            mListener.onLocationChanged(aMapLocation);// 显示系统小蓝点
//            aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(getBounds(mSportLatLngs), 18));
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude()), 18));

    }

    private LocationSource locationSource = new LocationSource() {
        @Override
        public void activate(OnLocationChangedListener onLocationChangedListener) {
            mListener = onLocationChangedListener;
            try {
                startLocation();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void deactivate() {
            mListener = null;
            if (mLocationClient != null) {
                mLocationClient.stopLocation();
                mLocationClient.onDestroy();
            }
            mLocationClient = null;
        }
    };

    /**
     * 设置一些amap的属性
     */
    private void setUpMap() {
        aMap.setLocationSource(locationSource);// 设置定位监听
        // 自定义系统定位小蓝点
        MyLocationStyle myLocationStyle = new MyLocationStyle();
//        myLocationStyle.myLocationIcon(BitmapDescriptorFactory
//                .fromResource(R.drawable.mylocation_point));// 设置小蓝点的图标
        myLocationStyle.strokeColor(Color.TRANSPARENT);// 设置圆形的边框颜色
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));// 设置圆形的填充颜色
        // myLocationStyle.anchor(int,int)//设置小蓝点的锚点
        // 设置定位的类型为定位模式 ，定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
//        myLocationStyle.interval(interval);//设置发起定位请求的时间间隔
//        myLocationStyle.showMyLocation(true);//设置是否显示定位小蓝点，true 显示，false不显示
        myLocationStyle.strokeWidth(1.0f);// 设置圆形的边框粗细
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.getUiSettings().setZoomControlsEnabled(false);// 设置默认缩放按钮是否显示
        aMap.getUiSettings().setCompassEnabled(false);// 设置默认指南针是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
    }

}
