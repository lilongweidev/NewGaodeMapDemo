package com.llw.newmapdemo;

import android.app.Application;
import android.content.Context;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.services.core.ServiceSettings;

public class App  extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Context mContext = this;
        // 定位隐私政策同意
        AMapLocationClient.updatePrivacyShow(mContext,true,true);
        AMapLocationClient.updatePrivacyAgree(mContext,true);
        // 地图隐私政策同意
        MapsInitializer.updatePrivacyShow(mContext,true,true);
        MapsInitializer.updatePrivacyAgree(mContext,true);
        // 搜索隐私政策同意
        ServiceSettings.updatePrivacyShow(mContext,true,true);
        ServiceSettings.updatePrivacyAgree(mContext,true);
    }
}
