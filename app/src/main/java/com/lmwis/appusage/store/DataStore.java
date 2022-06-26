package com.lmwis.appusage.store;

import android.app.usage.UsageEvents;
import android.location.Location;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmwis.appusage.rpc.DataCenterClient;
import com.lmwis.appusage.rpc.pojo.IphoneActionDTO;
import com.lmwis.appusage.rpc.pojo.IphonePostureDTO;
import com.lmwis.appusage.rpc.pojo.LocationInfoDTO;
import com.lmwis.appusage.service.GyroService;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据仓库，封装已经产生但是还未上报到数据中心的数据
 */
public class DataStore {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private static final String TAG = DataStore.class.getSimpleName();
    final static List<LocationInfoDTO> locationStore = new ArrayList<>();
    final static List<IphoneActionDTO> iphoneActionStore = new ArrayList<>();
    final static List<IphonePostureDTO> iphonePostureStore = new ArrayList<>();
    final static List<UsageEvents.Event> usageEventsStore = new ArrayList<>();

    long lastUploadAction ;

    public void saveLocationInfo(LocationInfoDTO locationInfoDTO){
        locationStore.add(locationInfoDTO);
        try {
            Log.d(TAG," 保存手机定位数据 :"+ objectMapper.writeValueAsString(locationInfoDTO));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void saveIphoneAction(IphoneActionDTO iphoneActionDTO){
        iphoneActionStore.add(iphoneActionDTO);
        try {
            Log.d(TAG," 保存手机行为 :"+ objectMapper.writeValueAsString(iphoneActionDTO));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


    public void saveIphonePosture(IphonePostureDTO iphonePostureDTO){
        long now =  System.currentTimeMillis();
        if ( now - lastUploadAction < 1000){
            // 频率限制
            return;
        }
        lastUploadAction = now;
        iphonePostureStore.add(iphonePostureDTO);
        try {
            Log.d(TAG," 保存手机姿态数据 :"+ objectMapper.writeValueAsString(iphonePostureDTO));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void flushAllData(){
        // 写入iphonePosture数据
        List<IphonePostureDTO> currentPosture = new ArrayList<>(iphonePostureStore);
        try {
            DataCenterClient.batchUploadIphonePosture(currentPosture);
            // 去除已经上报的数据
            iphonePostureStore.removeAll(currentPosture);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        // 写iphoneAction数据
        List<IphoneActionDTO> currentAction = new ArrayList<>(iphoneActionStore);
        try {
            DataCenterClient.batchUploadIphoneAction(currentAction);
            // 去除已经上报的数据
            iphoneActionStore.removeAll(currentAction);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        // 写location数据
        List<LocationInfoDTO> currentLocation = new ArrayList<>(locationStore);
        try {
            DataCenterClient.batchUploadLocationInfo(currentLocation);
            // 去除已经上报的数据
            locationStore.removeAll(currentLocation);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        // 写usageEvent
        List<UsageEvents.Event> currentUsageEvent = new ArrayList<>(usageEventsStore);
        try {
            DataCenterClient.batchUploadUsageEvent(currentUsageEvent);
            // 去除已经上报的数据
            usageEventsStore.removeAll(currentUsageEvent);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void saveBatchUsageEvent(ArrayList<UsageEvents.Event> eventList) {
        if (eventList!=null && eventList.size()>0){
            usageEventsStore.addAll(eventList);
        }
        try {
            Log.d(TAG," 保存APP事件数据 :"+ objectMapper.writeValueAsString(usageEventsStore));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
