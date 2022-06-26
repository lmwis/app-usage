package com.lmwis.appusage.rpc;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmwis.appusage.rpc.pojo.BatchUploadIphoneActionDTO;
import com.lmwis.appusage.rpc.pojo.BatchUploadIphonePostureDTO;
import com.lmwis.appusage.rpc.pojo.BatchUploadLocationInfoDTO;
import com.lmwis.appusage.rpc.pojo.IphoneActionDTO;
import com.lmwis.appusage.rpc.pojo.IphonePostureDTO;
import com.lmwis.appusage.rpc.pojo.LocationInfoDTO;
import com.lmwis.appusage.service.UsageEventService;
import com.lmwis.appusage.rpc.pojo.AppBaseDTO;
import com.lmwis.appusage.rpc.pojo.AppUsagesDTO;
import com.lmwis.appusage.rpc.pojo.BatchInitAppBaseDTO;
import com.lmwis.appusage.rpc.pojo.BatchUploadAppUsagesDTO;
import com.lmwis.appusage.rpc.pojo.BatchUploadUsageEventDTO;
import com.lmwis.appusage.rpc.pojo.CommonReturnType;
import com.lmwis.appusage.rpc.pojo.Constant;
import com.lmwis.appusage.rpc.pojo.UsageEventDTO;
import com.lmwis.appusage.service.FileService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DataCenterClient {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    final static String PORT = "9001";
    final static String HOST = "101.43.95.32";
    final static String TOKEN = "165279332574941bfa612182749c098f16f1b3bc91d74";
    // 10分钟
    final static int CONNECT_TIME_OUT_SECONDS = 600;

    private static final String TAG = DataCenterClient.class.getSimpleName();
    private static final String APP_USAGE_URL = "/app/usage";
    private static final String USAGE_EVENT_URL = "/app/event";
    private static final String IPHONE_POSTURE_URL = "/iphone/posture";
    private static final String IPHONE_ACTION_URL = "/iphone/action";
    private static final String LOCATION_BATCH_URL = "/location/batch";

    private static final String APP_INIT_URL = "/app/init";
    private static final String FILE_STRING_URL = "/file/string";
    private static final String POST_METHOD = "POST";
    public static PackageManager pm;
    public static SharedPreferences sp;
    public static boolean batchUploadAppUsage(List<UsageStats> list, long startTime, long endTime) throws JsonProcessingException {

        Log.d(TAG,"[batchUploadAppUsage] 开始保存UsageStats: startTime 为 "+startTime
                +", endTime 为 "+endTime );
        List<AppUsagesDTO> appUsagesDTOList = convertToAppUsagesDTOList(list);
        BatchUploadAppUsagesDTO batchUploadAppUsagesDTO = new BatchUploadAppUsagesDTO();
        batchUploadAppUsagesDTO.setList(appUsagesDTOList);
        batchUploadAppUsagesDTO.setStartTime(startTime);
        batchUploadAppUsagesDTO.setEndTime(endTime);
        String json = objectMapper.writeValueAsString(batchUploadAppUsagesDTO);
        MediaType JSON = MediaType.parse("application/json;charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(assembleUrl(APP_USAGE_URL))
                .header("Authorization","165279332574941bfa612182749c098f16f1b3bc91d74")
                .header("Content-Type","application/json;charset=utf-8")
                .post(requestBody)
                .build();

        OkHttpClient okHttpClient = buildOkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.i(TAG,"[batchUploadAppUsage] 保存结果: res 为"+ response.body().string());

            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG,"[batchUploadAppUsage] invoke error, e 为 "+e.getMessage());

            }
        });

        return true;
    }

    private static List<AppUsagesDTO> convertToAppUsagesDTOList(List<UsageStats> list) {
        if (list==null|| list.size()==0){
            return null;
        }
        List<AppUsagesDTO> res = new ArrayList<>();
        list.forEach(k->{
            AppUsagesDTO appUsagesDTO = new AppUsagesDTO();
            appUsagesDTO.setPackageName(k.getPackageName());
            appUsagesDTO.setDescribeContents(k.describeContents());
            appUsagesDTO.setLastTimeStamp(k.getLastTimeStamp());
            appUsagesDTO.setLastTimeUsed(k.getLastTimeUsed());
            appUsagesDTO.setLastTimeVisible(k.getLastTimeVisible());
            appUsagesDTO.setLastTimeForegroundServiceUsed(k.getLastTimeForegroundServiceUsed());
            appUsagesDTO.setTotalTimeForegroundServiceUsed(k.getTotalTimeForegroundServiceUsed());
            appUsagesDTO.setTotalTimeInForeground(k.getTotalTimeInForeground());
            appUsagesDTO.setTotalTimeVisible(k.getTotalTimeVisible());
            res.add(appUsagesDTO);
        });
        return res;
    }

    public static void batchUploadUsageEvent(List<UsageEvents.Event> eventList) throws JsonProcessingException {

        if (eventList == null || eventList.size()==0){
            Log.d(TAG,"[batchUploadUsageEvent] 无新增事件 ");
            return;
        }
        Log.d(TAG,"[batchUploadUsageEvent] 开始保存UsageStats 数量: " + eventList.size());
        List<UsageEventDTO> appUsagesDTOList = convertToUsageEventList(eventList);
        BatchUploadUsageEventDTO batchUploadUsageEventDTO = new BatchUploadUsageEventDTO();
        batchUploadUsageEventDTO.setList(appUsagesDTOList);
        String json = objectMapper.writeValueAsString(batchUploadUsageEventDTO);
        MediaType JSON = MediaType.parse("application/json;charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(assembleUrl(USAGE_EVENT_URL))
                .header("Authorization","165279332574941bfa612182749c098f16f1b3bc91d74")
                .header("Content-Type","application/json;charset=utf-8")
                .post(requestBody)
                .build();

        OkHttpClient okHttpClient = buildOkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.i(TAG,"[batchUploadUsageEvent] 保存结果: res 为"+ response.body().string());

            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG,"[batchUploadUsageEvent] invoke error, e 为 "+e.getMessage());
            }
        });



    }

    private static void batchInitAppBase(BatchInitAppBaseDTO batchInitAppBaseDTO){

        Log.d(TAG,"[batchInitAppBase] 开始保存 batchInitAppBaseDTO size:"+batchInitAppBaseDTO.getList().size() );
        String json = null;
        try {
            json = objectMapper.writeValueAsString(batchInitAppBaseDTO);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        MediaType JSON = MediaType.parse("application/json;charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(assembleUrl(APP_INIT_URL))
                .header("Authorization","165279332574941bfa612182749c098f16f1b3bc91d74")
                .header("Content-Type","application/json;charset=utf-8")
                .post(requestBody)
                .build();

        OkHttpClient okHttpClient = buildOkHttpClient();
        okHttpClient.connectTimeoutMillis();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d(TAG,"[batchInitAppBase] 保存结果: res 为"+ response.body().string());

            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG,"[batchInitAppBase] invoke error, e 为 "+e.getMessage());
                boolean flag = sp.getBoolean(Constant.APP_INIT_STATUS, false);
                if (flag){
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putBoolean(Constant.APP_INIT_STATUS, false);
                    editor.apply();
                }
            }
        });
        // 初始化成功后标记初始化状态
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(Constant.APP_INIT_STATUS, true);
        editor.apply();
        Log.d(TAG,"[batchInitAppBase] 初始化状态为"+ sp.getBoolean(Constant.APP_INIT_STATUS,false));
    }
    private static List<UsageEventDTO> convertToUsageEventList(List<UsageEvents.Event> eventList) {
        if (eventList==null|| eventList.size()==0){
            return null;
        }
        List<UsageEventDTO> res = new ArrayList<>();
        eventList.forEach(k->{
            UsageEventDTO usageEventDTO = new UsageEventDTO();
            usageEventDTO.setPackageName(k.getPackageName());
            usageEventDTO.setEventType(k.getEventType());
            usageEventDTO.setTimeStamp(k.getTimeStamp());
            res.add(usageEventDTO);
        });
        return res;
    }

    public static void initApplication(List<PackageInfo> installedPackages) {
        if (installedPackages==null){
            return;
        }

        Log.d(TAG,"[initApplication] all installed packages size:"+ installedPackages.size());
        BatchInitAppBaseDTO batchInitAppBase = new BatchInitAppBaseDTO();
        List<AppBaseDTO> list = new ArrayList<>();

        installedPackages.forEach(l->{

            AppBaseDTO appBaseDTO = convertAppBaseDTO(l);
            try {
                String str = objectMapper.writeValueAsString(appBaseDTO);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            list.add(appBaseDTO);
        });
        batchInitAppBase.setList(list);

        batchInitAppBase(batchInitAppBase);
    }

    public static String fileUpload(String fileString) {

        Request request = new Request.Builder()
                .url(assembleUrl(FILE_STRING_URL))
                .header("Authorization", "165279332574941bfa612182749c098f16f1b3bc91d74")
                .post(new FormBody.Builder()
                        .add("file", fileString)
                        .build())
                .build();

        OkHttpClient okHttpClient = buildOkHttpClient();
        final String[] fileUrl = {""};
        // 同步方式
        Call call = okHttpClient.newCall(request);
        try {
            Response response = call.execute();
            String result = response.body().string();
            CommonReturnType commonReturnType = objectMapper.readValue(result, CommonReturnType.class);
            fileUrl[0] = commonReturnType.getData().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileUrl[0];
    }

    private static AppBaseDTO convertAppBaseDTO(PackageInfo packageInfo){
        if (pm==null){
            return null;
        }
        AppBaseDTO appBaseDTO = new AppBaseDTO();
        appBaseDTO.setPackageName(packageInfo.packageName);
        appBaseDTO.setName(UsageEventService.getApplicationNameByPackageName(pm,packageInfo.packageName));
        appBaseDTO.setVersion(packageInfo.versionName);
        Drawable appIcon = packageInfo.applicationInfo.loadIcon(pm);
        String fileString = FileService.DrawableToString(appIcon);
        appBaseDTO.setPictureUrl( fileUpload(fileString));
        return appBaseDTO;
    }

    private static String assembleUrl(String flag){
        return "http://"+HOST +":"+PORT+flag;
    }

    private static OkHttpClient buildOkHttpClient(){
        OkHttpClient client = new OkHttpClient().newBuilder().
                connectTimeout(CONNECT_TIME_OUT_SECONDS, TimeUnit.SECONDS)
                .build();
        return client;
    }

    public static void batchUploadIphonePosture(List<IphonePostureDTO> list) throws JsonProcessingException {

        if (list == null || list.size()==0){
            Log.d(TAG,"[batchUploadIphonePosture] 无新数据 ");
            return;
        }

        Log.d(TAG,"[batchUploadIphonePosture] 开始保存IphonePosture 数量:"+list.size());

        BatchUploadIphonePostureDTO batchUploadIphonePostureDTO = new BatchUploadIphonePostureDTO();
        batchUploadIphonePostureDTO.setList(list);
        String json = objectMapper.writeValueAsString(batchUploadIphonePostureDTO);
        MediaType JSON = MediaType.parse("application/json;charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(assembleUrl(IPHONE_POSTURE_URL))
                .header("Authorization",TOKEN)
                .header("Content-Type","application/json;charset=utf-8")
                .post(requestBody)
                .build();

        OkHttpClient okHttpClient = buildOkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.i(TAG,"[batchUploadIphonePosture] 保存结果: res 为"+ response.body().string());

            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG,"[batchUploadIphonePosture] invoke error, e 为 "+e.getMessage());
            }
        });

    }
    public static void batchUploadIphoneAction(List<IphoneActionDTO> list) throws JsonProcessingException {

        if (list == null || list.size()==0){
            Log.d(TAG,"[batchUploadIphoneAction] 无新数据 ");
            return;
        }

        Log.d(TAG,"[batchUploadIphoneAction] 开始保存IphoneAction 数量:"+list.size());

        BatchUploadIphoneActionDTO batchUploadIphoneActionDTO = new BatchUploadIphoneActionDTO();
        batchUploadIphoneActionDTO.setList(list);
        String json = objectMapper.writeValueAsString(batchUploadIphoneActionDTO);
        MediaType JSON = MediaType.parse("application/json;charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(assembleUrl(IPHONE_ACTION_URL))
                .header("Authorization",TOKEN)
                .header("Content-Type","application/json;charset=utf-8")
                .post(requestBody)
                .build();

        OkHttpClient okHttpClient = buildOkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.i(TAG,"[batchUploadIphoneAction] 保存结果: res 为"+ response.body().string());

            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG,"[batchUploadIphoneAction] invoke error, e 为 "+e.getMessage());
            }
        });

    }

    public static void batchUploadLocationInfo(List<LocationInfoDTO> list) throws JsonProcessingException {

        if (list == null || list.size()==0){
            Log.d(TAG,"[batchUploadLocationInfo] 无新数据 ");
            return;
        }

        Log.d(TAG,"[batchUploadLocationInfo] 开始保存locationInfo 数量:"+list.size());

        BatchUploadLocationInfoDTO batchUploadLocationInfoDTO = new BatchUploadLocationInfoDTO();
        batchUploadLocationInfoDTO.setList(list);
        String json = objectMapper.writeValueAsString(batchUploadLocationInfoDTO);
        MediaType JSON = MediaType.parse("application/json;charset=utf-8");
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(assembleUrl(LOCATION_BATCH_URL))
                .header("Authorization",TOKEN)
                .header("Content-Type","application/json;charset=utf-8")
                .post(requestBody)
                .build();

        OkHttpClient okHttpClient = buildOkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.i(TAG,"[batchUploadLocationInfo] 保存结果: res 为"+ response.body().string());

            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG,"[batchUploadLocationInfo] invoke error, e 为 "+e.getMessage());
            }
        });

    }


}
