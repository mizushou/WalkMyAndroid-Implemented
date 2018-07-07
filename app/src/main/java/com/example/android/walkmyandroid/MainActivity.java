/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.walkmyandroid;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity implements FetchAddressTask.OnTaskCompleted {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_LOCATION_PERMISSION = 0;
    private static final String TRACKING_LOCATION_KEY = "TrackingLocation";

    private Button mLocationButton;
    private Location mLastLocation;
    private TextView mLocationTextView;
    private ImageView mAndroidImageView;
    private AnimatorSet mRotateAnim;
    private FusedLocationProviderClient mFusedLocationClient;
    private Boolean mTrackingLocation = false;
    private LocationCallback mLocationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // restore tracking state
        if (savedInstanceState != null) {
            mTrackingLocation = savedInstanceState.getBoolean(TRACKING_LOCATION_KEY);
        }

        // set up location button
        mLocationButton = findViewById(R.id.button_location);
        mLocationButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!mTrackingLocation) {
                            startTrackingLocation();
                        } else {
                            stopTrackingLocation();
                        }
                    }
                });

        // set up location text view
        mLocationTextView = findViewById(R.id.textview_location);

        // set up FusedLocationClient
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // set up image view
        mAndroidImageView = findViewById(R.id.imageview_android);

        // set up rotate animator
        mRotateAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.rotate);
        mRotateAnim.setTarget(mAndroidImageView);

        mLocationCallback =
                new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        // If tracking is turned on, reverse geocode into an address
                        if (mTrackingLocation) {
                            new FetchAddressTask(MainActivity.this, MainActivity.this)
                                    .execute(locationResult.getLastLocation());
                        }
                    }
                };
    }

    @Override
    public void onTaskCompleted(String result) {
        // Update the UI
        if (mTrackingLocation) {
            mLocationTextView.setText(
                    getString(R.string.address_text, result, System.currentTimeMillis()));
        }
    }

    private void startTrackingLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            {
                ActivityCompat.requestPermissions(
                        this,
                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION_PERMISSION);
            }
        } else {
            mRotateAnim.start();
            mTrackingLocation = true;
            mLocationButton.setText(R.string.stop_tracking_location);
            mFusedLocationClient.requestLocationUpdates(
                    getLocationRequest(), mLocationCallback, null);
        }
    }

    private void stopTrackingLocation() {
        /**
         * Method that stops tracking the device. It removes the location updates, stops the
         * animation and reset the UI.
         */
        if (mTrackingLocation) {
            mTrackingLocation = false;
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            mLocationButton.setText(R.string.start_tracking_location);
            mLocationTextView.setText(R.string.text_view_hint);
            mRotateAnim.end();
        }
    }

    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startTrackingLocation();
                } else {
                    Toast.makeText(this, R.string.location_denied, Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume is called");
        if (mTrackingLocation) {
            startTrackingLocation();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause is called");
        if (mTrackingLocation) {
            stopTrackingLocation();
            mTrackingLocation = true;
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(TRACKING_LOCATION_KEY, mTrackingLocation);
        super.onSaveInstanceState(outState);
    }
}
