package com.lmwis.appusage.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.lmwis.appusage.rpc.DataCenterClient;
import com.lmwis.appusage.rpc.pojo.Constant;

import java.util.List;

public class UsageEventService {

    public static SharedPreferences sp;

    public static PackageManager pm;

    public static String getApplicationNameByPackageName(PackageManager pm, String packageName) {
        String name;
        try {
            name = pm.getApplicationLabel(pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            name = "";
        }
        return name;

    }
    public static void getAllApplications(){
        boolean initStatus = sp.getBoolean(Constant.APP_INIT_STATUS, false);

        if (initStatus){
            // 如果以及初始化则不再重新拉取
            return;
        }

        List<PackageInfo> installedPackages = pm.getInstalledPackages(0);
//        DataCenterClient.initApplication(installedPackages);
    }
}
