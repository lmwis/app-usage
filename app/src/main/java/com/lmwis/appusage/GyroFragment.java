package com.lmwis.appusage;

import static android.content.Context.SENSOR_SERVICE;
import static android.util.Half.EPSILON;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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

import java.text.CollationElementIterator;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;

public class GyroFragment extends Fragment implements SensorEventListener {
    private static final String TAG = GyroFragment.class.getSimpleName();

    Button mSwitchLocationButton;

    private SensorManager sensorManager = null;
    private Sensor gyroSensor = null;
    private TextView vX;
    private TextView vY;
    private TextView vZ;
    private TextView gX;
    private TextView gY;
    private TextView gZ;
    private TextView v;
    private Button button;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private float[] angle = new float[3];

    private float mTimestamp; // 记录上次的时间戳
    private float mAngle[] = new float[3]; // 记录xyz三个方向上的旋转角度
    private TextView mPhoneAzTv;
    private TextView mPhonePitchTv;
    private TextView mPhoneRollTv;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gyro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        ServiceSettings.updatePrivacyShow(getContext(), true, true);
        ServiceSettings.updatePrivacyAgree(getContext(),true);

        mSwitchLocationButton = (Button) getActivity().findViewById(R.id.switch_location);

        mSwitchLocationButton.setOnClickListener(v -> {
            FragmentManager manager = getActivity().getSupportFragmentManager();
            FragmentTransaction transaction=manager.beginTransaction();
            transaction.replace(R.id.container,MainActivity.locationFragment);
            transaction.commit();
        });

        angle[0] = 0;
        angle[1] = 0;
        angle[2] = 0;
        timestamp = 0;

        vX = (TextView) getActivity().findViewById(R.id.vx);
        vY = (TextView)getActivity().findViewById(R.id.vy);
        vZ = (TextView)getActivity().findViewById(R.id.vz);

        gX = (TextView) getActivity().findViewById(R.id.gx);
        gY = (TextView)getActivity().findViewById(R.id.gy);
        gZ = (TextView)getActivity().findViewById(R.id.gz);

        mPhoneAzTv = (TextView) getActivity().findViewById(R.id.iphonex);
        mPhonePitchTv = (TextView)getActivity().findViewById(R.id.iphoney);
        mPhoneRollTv = (TextView)getActivity().findViewById(R.id.iphonez);


        sensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        gyroSensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        initUtils();
        initPhoneSensors();

    }


    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        sensorManager.unregisterListener(this); // 解除监听器注册
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        sensorManager.registerListener(this, gyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);  //为传感器注册监听器
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }
    private void initUtils() {
        ButterKnife.bind(getActivity());
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                vX.setText(String.format(Locale.CHINA, "acc x : %f", event.values[0]));
                vY.setText(String.format(Locale.CHINA, "acc y : %f", event.values[1]));
                vZ.setText(String.format(Locale.CHINA, "acc z : %f", event.values[2]));
                System.arraycopy(event.values, 0, mAccValues, 0, mAccValues.length);// 获取数据
                break;
            case Sensor.TYPE_GYROSCOPE:
                gX.setText(String.format(Locale.CHINA, "PhoneGyro x : %f", event.values[0]));
                gY.setText(String.format(Locale.CHINA, "PhoneGyro y : %f", event.values[1]));
                gZ.setText(String.format(Locale.CHINA, "PhoneGyro z : %f", event.values[2]));
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, mMagValues, 0, mMagValues.length);// 获取数据
                break;
        }
        SensorManager.getRotationMatrix(mRMatrix, null, mAccValues, mMagValues);
        SensorManager.getOrientation(mRMatrix, mPhoneAngleValues);// 此时获取到了手机的角度信息
        mPhoneAzTv.setText(String.format(Locale.CHINA, "Azimuth(地平经度): %f", Math.toDegrees(mPhoneAngleValues[0])));
        mPhonePitchTv.setText(String.format(Locale.CHINA, "Pitch: %f", Math.toDegrees(mPhoneAngleValues[1])));
        mPhoneRollTv.setText(String.format(Locale.CHINA, "Roll: %f", Math.toDegrees(mPhoneAngleValues[2])));

//        vX.setText("Orientation X: " + event.values[0] * 10);
//        vY.setText("Orientation Y: " + event.values[1] * 10);
//        vZ.setText("Orientation Z: " + event.values[2] * 10);

    }
    // 加速度传感器数据
    float mAccValues[] = new float[3];
    // 地磁传感器数据
    float mMagValues[] = new float[3];
    // 旋转矩阵，用来保存磁场和加速度的数据
    float mRMatrix[] = new float[9];
    // 存储方向传感器的数据（原始数据为弧度）
    float mPhoneAngleValues[] = new float[3];

    private Sensor mGyroSensor;
    private Sensor mAccSensor;
    private Sensor mMagSensor;
    private void initPhoneSensors() {
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensorList) {
            Log.d(TAG, String.format(Locale.CHINA, "[Sensor] name: %s \tvendor:%s",
                    sensor.getName(), sensor.getVendor()));
        }
        // 获取传感器
        mGyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, mGyroSensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, mAccSensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, mMagSensor, SensorManager.SENSOR_DELAY_UI);
    }


}
