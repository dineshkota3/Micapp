package com.example.dineshkota.randd.utils;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

/**
 * Created by dineshkota on 5/9/15.
 */
public class utils {

    private String getLocalIpAddress(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip;
    }
}
