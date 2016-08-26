package com.acer.batterycapacitydemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PowerConnectionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
            Log.d("PowerConnectionReceiver", "ACTION_BATTERY_CHANGED received");
            int level = intent.getIntExtra("level", 0);
            int scale = intent.getIntExtra("Scale", 100);
            BatteryLevel.batteryLevel = String.valueOf(level*100 / scale);
        }
    }
}