/*
 * Created by Sujoy Datta. Copyright (c) 2020. All rights reserved.
 *
 * To the person who is reading this..
 * When you finally understand how this works, please do explain it to me too at sujoydatta26@gmail.com
 * P.S.: In case you are planning to use this without mentioning me, you will be met with mean judgemental looks and sarcastic comments.
 */

package com.cooperativeai.activities;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.cooperativeai.BuildConfig;
import com.cooperativeai.R;
import com.cooperativeai.receivers.TransitionReceiver;
import com.cooperativeai.utils.Constants;
import com.cooperativeai.utils.DateTimeManager;
import com.cooperativeai.utils.SharedPreferenceManager;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private final int ACTIVITY_PERMISSION_CODE = 11;
    @BindView(R.id.textView_main_date)
    TextView textViewDate;
    @BindView(R.id.main_bottom_nav)
    BottomNavigationView bottomNavigationView;
    @BindView(R.id.textView_main_total_coins)
    TextView textViewTotalCoins;
    @BindView(R.id.textView_main_user_level)
    TextView textViewLevel;

    private boolean isPermissionsEnabled;
    private ActivityTransition activityTransition;
    private TransitionReceiver transitionReceiver;
    // Action fired when transitions are triggered.
    private final String TRANSITIONS_RECEIVER_ACTION =
            BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";
    private ArrayList<ActivityTransition> activityTransitions;
    private PendingIntent mActivityTransitionsPendingIntent;

    private Date lastUsedDate;
    private String lastUsedDateAsString;
    private Date currentDate;
    private String currentDateAsString;
    private int userCurrentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

//        setupActivityTracking();

        // we get the current date and check it with the previously accessed date to determine
        // the current level of user. This is done always when the app starts so as to maintain exact difference of specified hours
        currentDate = new Date();
        currentDateAsString = DateTimeManager.converDateToString(currentDate);

        lastUsedDateAsString = SharedPreferenceManager.getLastUsedDate(this);
        if (lastUsedDateAsString.isEmpty())
            lastUsedDateAsString = currentDateAsString;
        lastUsedDate = DateTimeManager.convertStringToDate(lastUsedDateAsString);

        userCurrentLevel = SharedPreferenceManager.getUserLevel(this);
        if (DateTimeManager.diffInDate(currentDate, lastUsedDate) > Constants.LEVEL_CHECK_DELAY) {
            userCurrentLevel = reduceLevelCount();
            SharedPreferenceManager.setUserLevel(this, userCurrentLevel);
        }

        setupViews();

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_camera) {
                    startActivity(new Intent(MainActivity.this, CameraActivity.class));
                    finish();
                } else if (item.getItemId() == R.id.menu_profile) {
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                    finish();
                }

                return true;
            }
        });
    }

    private int reduceLevelCount(){
        if (userCurrentLevel == 1){
            return userCurrentLevel;
        }
        else{
            long reduceCount = DateTimeManager.diffInDate(currentDate, lastUsedDate);
            userCurrentLevel -= reduceCount;

            if (userCurrentLevel <= 1)
                userCurrentLevel = 1;

            return userCurrentLevel;
        }
    }

    private void setupActivityTracking() {
        if (isPermissionRequired())
            getPermission();

        if (isPermissionRequired()) {
            if (isPermissionsEnabled)
                Toast.makeText(this, "Permission required and enabled", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Permission required. Not enabled", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Permission Not required", Toast.LENGTH_SHORT).show();
        }

        if (!isPermissionRequired() || isPermissionsEnabled) {
            // initialise the array of activities that need to be detected
            initTransitionsArray();

            // PendingIntent will be triggered when a certain activity from the arrayList is detected
            Intent intent = new Intent(TRANSITIONS_RECEIVER_ACTION);
            mActivityTransitionsPendingIntent =
                    PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

            // The receiver listens for the PendingIntent above that is triggered by the system when an
            // activity transition occurs.
            transitionReceiver = new TransitionReceiver();
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    transitionReceiver,
                    new IntentFilter(TRANSITIONS_RECEIVER_ACTION)
            );
        }

        enableActivityTransitions();
    }

    private void initTransitionsArray() {
        activityTransitions = new ArrayList<>();
        activityTransitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());

        activityTransitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        activityTransitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());

        activityTransitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
    }

    private void getPermission() {
        if(PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_PERMISSION_CODE);
            }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTIVITY_PERMISSION_CODE){
            isPermissionsEnabled = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean isPermissionRequired() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    private void setupViews() {
        textViewDate.setText(DateTimeManager.getMonthNameWithDate());
        textViewTotalCoins.setText(SharedPreferenceManager.getUserCoins(MainActivity.this));
        textViewLevel.setText("Level " + userCurrentLevel);
    }

    private void enableActivityTransitions(){
        ActivityTransitionRequest request = new ActivityTransitionRequest(activityTransitions);

        Task<Void> task = ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, mActivityTransitionsPendingIntent);

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(MainActivity.this, "Transactions Api was successfully registered", Toast.LENGTH_SHORT).show();
            }
        });
        
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Transactions API could not be registered", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if (transitionReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(transitionReceiver);
            transitionReceiver = null;
        }
        super.onStop();
    }
}
