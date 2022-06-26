package com.lmwis.appusage.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.lmwis.appusage.GyroFragment;
import com.lmwis.appusage.MainActivity;
import com.lmwis.appusage.rpc.pojo.IphonePostureDTO;
import com.lmwis.appusage.store.DataStore;

import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;

public class GyroService extends Service implements SensorEventListener {

    private static final String TAG = GyroService.class.getSimpleName();

    DataStore dataStore;

    private SensorManager sensorManager = null;
    private Sensor gyroSensor = null;

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

    public static final int NOTICE_ID = 103;

    public static final String CHANNEL_ID_STRING = "lmwis003";

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

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        gyroSensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(this, gyroSensor,
                SensorManager.SENSOR_DELAY_NORMAL);  //为传感器注册监听器

        initPhoneSensors();
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
//                vX.setText(String.format(Locale.CHINA, "acc x : %f", event.values[0]));
//                vY.setText(String.format(Locale.CHINA, "acc y : %f", event.values[1]));
//                vZ.setText(String.format(Locale.CHINA, "acc z : %f", event.values[2]));
                System.arraycopy(event.values, 0, mAccValues, 0, mAccValues.length);// 获取数据
                break;
            case Sensor.TYPE_GYROSCOPE:
//                gX.setText(String.format(Locale.CHINA, "PhoneGyro x : %f", event.values[0]));
//                gY.setText(String.format(Locale.CHINA, "PhoneGyro y : %f", event.values[1]));
//                gZ.setText(String.format(Locale.CHINA, "PhoneGyro z : %f", event.values[2]));
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, mMagValues, 0, mMagValues.length);// 获取数据
                break;
        }
        SensorManager.getRotationMatrix(mRMatrix, null, mAccValues, mMagValues);
        SensorManager.getOrientation(mRMatrix, mPhoneAngleValues);// 此时获取到了手机的角度信息
//        mPhoneAzTv.setText(String.format(Locale.CHINA, "Azimuth(地平经度): %f", Math.toDegrees(mPhoneAngleValues[0])));
//        mPhonePitchTv.setText(String.format(Locale.CHINA, "Pitch: %f", Math.toDegrees(mPhoneAngleValues[1])));
//        mPhoneRollTv.setText(String.format(Locale.CHINA, "Roll: %f", Math.toDegrees(mPhoneAngleValues[2])));
        IphonePostureDTO iphonePostureDTO = new IphonePostureDTO();
        iphonePostureDTO.setAzimuth(Math.toDegrees(mPhoneAngleValues[0]));
        iphonePostureDTO.setPitch(Math.toDegrees(mPhoneAngleValues[1]));
        iphonePostureDTO.setRoll(Math.toDegrees(mPhoneAngleValues[2]));
        long now = System.currentTimeMillis();
        iphonePostureDTO.setGmtCreate(now);
        iphonePostureDTO.setGmtModified(now);
        dataStore.saveIphonePosture(iphonePostureDTO);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this); // 解除监听器注册
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }
}
