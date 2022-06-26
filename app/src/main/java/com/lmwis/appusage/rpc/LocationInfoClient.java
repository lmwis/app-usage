package com.lmwis.appusage.rpc;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LocationInfoClient {

    private static final String TAG = LocationInfoClient.class.getSimpleName();

    private static final String LOCATION_INFO_URL = "http://10.111.169.20:9001/location";
    private static final String POST_METHOD = "POST";
    public static boolean saveToCenter(String latitude, String longitude){
        Log.d(TAG,"[saveToCenter] 开始保存当前经纬度: latitude 为 "+latitude
                +", longitude 为 "+longitude );

        Request request = new Request.Builder()
                .url(LOCATION_INFO_URL)
                .header("Authorization","165279332574941bfa612182749c098f16f1b3bc91d74")
                .post(new FormBody.Builder()
                        .add("latitude",latitude)
        .add("longitude",longitude).build())
                .build();

        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d(TAG,"[saveToCenter] 保存结果: res 为"+ response.body().string());
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG,"[onFailure] invoke error, e 为 "+e.getMessage());
            }
        });

        return true;
    }

    public static String sHA1(Context context){
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
            byte[] cert = info.signatures[0].toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert);
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < publicKey.length; i++) {
                String appendString = Integer.toHexString(0xFF & publicKey[i])
                        .toUpperCase(Locale.US);
                if (appendString.length() == 1)
                    hexString.append("0");
                hexString.append(appendString);
                hexString.append(":");
            }
            String result = hexString.toString();
            return result.substring(0, result.length()-1);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

}
