package com.acer.batterycapacitydemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.regions.Regions;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends FragmentActivity {

    CallbackManager callbackManager;
    private AccessToken accessToken;
    private CognitoSyncManager client;
    private String TAG = "MainActivity";
    private MainFragment mainFragment;
    private PowerConnectionReceiver batteryReceiver;
    public static CognitoSyncClientManager clientManager = null;
    public static String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize the SDK before executing any other operations
        FacebookSdk.sdkInitialize(getApplicationContext());

        //TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            deviceId = Build.SERIAL;
        }
        Log.d("getDeviceId :", deviceId);
        clientManager = new CognitoSyncClientManager(this);
        clientManager.initClients();
        super.onCreate(savedInstanceState);

        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.acer.batterycapacitydemo",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }
//        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            // Add the fragment on initial activity setup
            mainFragment = new MainFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, mainFragment).commit();
        } else {
            // Or set the fragment from restored state info
            mainFragment = (MainFragment) getSupportFragmentManager()
                    .findFragmentById(android.R.id.content);
        }
        //RegisterReceiver();
        batteryReceiver = new PowerConnectionReceiver();
        registerReceiver(batteryReceiver, new IntentFilter( Intent.ACTION_BATTERY_CHANGED));

    }

    public void UnRegisterReceiver(){
        unregisterReceiver(this.batteryReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UnRegisterReceiver();
    }

}
