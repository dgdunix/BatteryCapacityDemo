package com.acer.batterycapacitydemo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainFragment extends Fragment {

    public static String identityID;
    private String TAG = "MainFragment";
    private TextView mTextDetails;
    private CallbackManager mCallbackManager;
    private AccessTokenTracker mTokenTracker;
    private ProfileTracker mProfileTracker;
    private Handler handler = new Handler();

    private FacebookCallback<LoginResult> mFacebookCallback = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            Log.d(TAG, "Facebook access token got");
            setFacebookSession(loginResult.getAccessToken());
            new initIdentityID().execute();
            Profile profile = Profile.getCurrentProfile();
            mTextDetails.setText(constructWelcomeMessage(profile));
        }

        @Override
        public void onCancel() {
            Toast.makeText(getActivity(), "Facebook login cancelled",
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public void onError(FacebookException exception) {
            Log.d(TAG, "Error in Facebook login " + exception.getMessage());
            Toast.makeText(getActivity(), "Error in Facebook login " +
                    exception.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    };


    public MainFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCallbackManager = CallbackManager.Factory.create();
        setupTokenTracker();
        setupProfileTracker();

        mTokenTracker.startTracking();
        mProfileTracker.startTracking();
    }

    private class initIdentityID extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            identityID = CognitoSyncClientManager.getCredentialsProvider().getIdentityId();
            Log.d(TAG, "id " + identityID);
            return "Success";
        }
        @Override
        protected void onPostExecute(String response) {
            Profile profile = Profile.getCurrentProfile();
            mTextDetails.setText(constructWelcomeMessage(profile));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_main, container, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(updateTimer);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setupTextDetails(view);
        setupLoginButton(view);

        Profile profile = Profile.getCurrentProfile();
        if (profile != null) {
            new initIdentityID().execute();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupProfileTracker();
        Profile profile = Profile.getCurrentProfile();
        mTextDetails.setText(constructWelcomeMessage(profile));

    }

    @Override
    public void onStop() {
        super.onStop();
        mTokenTracker.stopTracking();
        mProfileTracker.stopTracking();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void setupTextDetails(View view) {
        mTextDetails = (TextView) view.findViewById(R.id.text_details);
    }

    private void setupTokenTracker() {
        mTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                Log.d(TAG, "currentAccessToken" + currentAccessToken);
            }
        };
    }

    private void setupProfileTracker() {
        mProfileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                Log.d(TAG, "currentProfile" + currentProfile);
                mTextDetails.setText(constructWelcomeMessage(currentProfile));
            }
        };
    }

    private void setupLoginButton(View view) {
        LoginButton mButtonLogin = (LoginButton) view.findViewById(R.id.login_button);
        mButtonLogin.setFragment(this);
        mButtonLogin.setReadPermissions("user_friends");
        mButtonLogin.registerCallback(mCallbackManager, mFacebookCallback);
    }

    private String constructWelcomeMessage(Profile profile) {
        StringBuffer stringBuffer = new StringBuffer();
        Log.d(TAG,"Enter constructWelcomeMessage" + profile);
        handler.removeCallbacks(updateTimer);
        if (profile != null) {
            // Check id
            stringBuffer.append("Welcome " + profile.getName() + " ID " + identityID);
            handler.postDelayed(updateTimer, 5000);
        }
        return stringBuffer.toString();
    }

    private void setFacebookSession(AccessToken accessToken) {
        Log.i(TAG, "facebook token: " + accessToken.getToken());
        CognitoSyncClientManager.addLogins("graph.facebook.com",
                accessToken.getToken());
    }

    private Runnable updateTimer = new Runnable() {
        public void run() {
            Toast.makeText(getActivity(), "Battery update at " + System.currentTimeMillis() + " for " + identityID,
                    Toast.LENGTH_LONG).show();
            new DynamoDBManagerTask()
                    .execute(DynamoDBManagerType.UPLOAD_BATTERYINFO);
            //Profile profile = Profile.getCurrentProfile();
            //if (profile != null) {
                handler.postDelayed(this, 10000);
            //}
        }
    };

    private enum DynamoDBManagerType {
        UPLOAD_BATTERYINFO
    }

    private class DynamoDBManagerTask extends
            AsyncTask<DynamoDBManagerType, Void, DynamoDBManagerTaskResult> {

        protected DynamoDBManagerTaskResult doInBackground(
                DynamoDBManagerType... types) {

            String tableStatus = DynamoDBManager.getTestTableStatus();

            DynamoDBManagerTaskResult result = new DynamoDBManagerTaskResult();
            result.setTableStatus(tableStatus);
            result.setTaskType(types[0]);

            if (types[0] == DynamoDBManagerType.UPLOAD_BATTERYINFO) {
                if (tableStatus.equalsIgnoreCase("ACTIVE")) {
                    DynamoDBManager.uploadBatteryInfo();
                }
            }
            return result;
        }

        protected void onPostExecute(DynamoDBManagerTaskResult result) {
            if (!result.getTableStatus().equalsIgnoreCase("ACTIVE")) {
                Toast.makeText(
                        getActivity(),
                        "The test table is not ready yet.\nTable Status: "
                                + result.getTableStatus(), Toast.LENGTH_LONG)
                        .show();
            } else if (result.getTableStatus().equalsIgnoreCase("ACTIVE")
                    && result.getTaskType() == DynamoDBManagerType.UPLOAD_BATTERYINFO) {
                Toast.makeText(getActivity(),
                        "Battery Info upload successfully!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class DynamoDBManagerTaskResult {
        private DynamoDBManagerType taskType;
        private String tableStatus;

        public DynamoDBManagerType getTaskType() {
            return taskType;
        }

        public void setTaskType(DynamoDBManagerType taskType) {
            this.taskType = taskType;
        }

        public String getTableStatus() {
            return tableStatus;
        }

        public void setTableStatus(String tableStatus) {
            this.tableStatus = tableStatus;
        }
    }
}
