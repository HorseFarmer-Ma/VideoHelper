package com.meizu.testdevVideo.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.meizu.testdevVideo.R;
import com.meizu.testdevVideo.constant.SettingPreferenceKey;
import com.meizu.testdevVideo.util.sharepreference.BaseData;
import com.meizu.testdevVideo.util.sharepreference.PrefWidgetOnOff;
import com.meizu.testdevVideo.util.wifi.WifiUtil;

import java.util.List;

/**
 * Author: jinghao
 * Date: 2015-04-10
 */
public class WifiLockService extends IntentService {

    private WifiManager mWifiManager;
    private SharedPreferences settingSharedPreferences;
    private String TAG = WifiLockService.class.getSimpleName();
    private int tryTime = 10;
    private boolean stopSelf = false;

    public WifiLockService() {
        super("wifi locker");
    }

    public void clearWifiConfig() {
        try {
            List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
            if (null == configs) {
                return;
            }
            for (WifiConfiguration config : configs) {
                if (!config.SSID.equals(("\"" + getSsid() + "\""))) {
                    mWifiManager.removeNetwork(config.networkId);
                }
            }
            mWifiManager.saveConfiguration();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (settingSharedPreferences.getBoolean(SettingPreferenceKey.LOCK_WIFI, true) && (!this.isWifiConnected()
                || !mWifiManager.isWifiEnabled() || !("\"" + getCurrentSsid() + "\"")
                .equals(mWifiManager.getConnectionInfo().getSSID()))) {
            connectToNet();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        settingSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "成功完成连接wifi");
        super.onDestroy();
        this.stopSelf = true;
    }

    public void connectToNet() {
        if (settingSharedPreferences.getBoolean(SettingPreferenceKey.LOCK_WIFI, true)) {
            Log.e(TAG, "wifi 锁收到新任务. SSID是" + getCurrentSsid());
            this.clearWifiConfig();
            String ssid;
            String usr = (this.getResources().getString(R.string.usr));
            String pwd = (this.getResources().getStringArray(R.array.pwd))[0];
            int connectTime = 0;
            while ((!("\"" + (ssid = getCurrentSsid()) + "\"").equals(mWifiManager.getConnectionInfo().getSSID())
                    || !this.isWifiConnected()
                    || !this.mWifiManager.isWifiEnabled()) && settingSharedPreferences.getBoolean(SettingPreferenceKey.LOCK_WIFI, true)) {
                // 大于尝试次数，连接默认WiFi
                if (connectTime > tryTime) {
                    Log.e(TAG, "超过尝试次数");
                } else {
                    connectTime++;
                }
                WifiConfiguration config;
                if ("".equals(ssid)) {
                    Log.e(TAG, "ssid is invalid, do nothing");
                    break;
                }

                if (isEapWifi(ssid)) {
                    config = WifiUtil.getEAPConfig(ssid, usr, pwd);
                }else{
                    config = WifiUtil.getWPAConfig(ssid, this.getPwd(ssid));
                }

                int netId = this.mWifiManager.addNetwork(config);
                this.mWifiManager.saveConfiguration();
                this.mWifiManager.disconnect();
                this.mWifiManager.enableNetwork(netId, true);
                this.mWifiManager.reconnect();
                this.mWifiManager.setWifiEnabled(true);
                try {
                    Log.e(TAG, "connect to wifi...");
                    Thread.sleep(7000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (stopSelf) {
                    break;
                }
            }
        }
    }


    private boolean isWifiConnected() {
        return WifiUtil.isWifiConnected(this);
    }


    private String getCurrentSsid(){
        if(settingSharedPreferences.getString(SettingPreferenceKey.LOCK_WIFI_TYPE, "MZ-MEIZU-5G").equals("自定义")){
            return settingSharedPreferences.getString(SettingPreferenceKey.DEFINED_WIFI_SSID, "MZ-Inweb-Test");
        }else{
            return settingSharedPreferences.getString(SettingPreferenceKey.LOCK_WIFI_TYPE, "MZ-MEIZU-5G");
        }
    }


    // 返回输入框的ssid
    private String getSsid(){
        return settingSharedPreferences.getString(SettingPreferenceKey.DEFINED_WIFI_SSID, "MZ-Inweb-Test");
    }


    // 返回当前连接的WIFI密码
    private String getPwd(String ssid) {
        if("MZ-Inweb-Test".equals(ssid)){
            return getResources().getStringArray(R.array.pwd)[1];
        }else{
            return settingSharedPreferences.getString(SettingPreferenceKey.DEFINED_WIFI_PSW, "Inweb@meizu.com");
        }

    }


    // 判断是否为加密的WIFI
    private boolean isEapWifi(String ssid) {
        return "MZ-MEIZU-5G".equals(ssid) || "MZ-MEIZU-2.4G".equals(ssid) || "MZ-Hkline-5G".equals(ssid) || "MZ-mgt".equals(ssid);
    }
}


