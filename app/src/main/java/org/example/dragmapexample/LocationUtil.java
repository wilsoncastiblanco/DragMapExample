package org.example.dragmapexample;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.ArrayList;
import java.util.List;

public class LocationUtil implements com.google.android.gms.location.LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

  private static final String LOG_TAG = LocationUtil.class.getSimpleName();

  private GoogleApiClient mGoogleApiClient;

  public static LocationUtil instance;

  private Context context;

  public static LocationUtil getInstance(Context context) {
    return instance != null ? instance : new LocationUtil(context);
  }

  private List<Listener> pendingListeners = new ArrayList<>();

  LocationSettingsRequest.Builder builder;
  PendingResult<LocationSettingsResult> result;

  LocationRequest mLocationRequest;

  public LocationUtil(Context context) {
    instance = this;
    initializeGooglePlayServices(context);
  }

  private void initializeGooglePlayServices(Context context) {
    mGoogleApiClient = new GoogleApiClient.Builder(context.getApplicationContext())
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
  }

  public void getLocation(Listener listener, Context context) {
    this.context = context;
    pendingListeners.add(listener);
    mGoogleApiClient.connect();
  }

  @Override
  public void onConnected(Bundle bundle) {
    mLocationRequest = LocationRequest.create();
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    mLocationRequest.setInterval(GData.UPDATE_INTERVAL_IN_MILLISECONDS); // Update location every second
    mLocationRequest.setFastestInterval(GData.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      // TODO: Consider calling
      //    ActivityCompat#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for ActivityCompat#requestPermissions for more details.
      return;
    }
    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, LocationUtil.this);

    buildSettingsApi();
  }

  public void buildSettingsApi() {

    builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
    builder.setAlwaysShow(true);
    result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

    result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
      @Override
      public void onResult(LocationSettingsResult result) {
        final Status status = result.getStatus();
        switch (status.getStatusCode()) {
          case LocationSettingsStatusCodes.SUCCESS:
            // All location settings are satisfied. The client can initialize location
            // requests here.
            break;
          case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
            // Location settings are not satisfied. But could be fixed by showing the user
            // a dialog.
            try {
              status.startResolutionForResult((Activity) context, 999);
            } catch (IntentSender.SendIntentException e) {
              e.printStackTrace();
            }
            break;
          case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
            // Location settings are not satisfied. However, we have no way to fix the
            // settings so we won't show the dialog.
            break;
        }
      }
    });
  }

  @Override
  public void onConnectionSuspended(int i) {
    Log.i(LOG_TAG, "GoogleApiClient connection has been suspend");
  }

  @Override
  public void onConnectionFailed(ConnectionResult connectionResult) {
    Log.i(LOG_TAG, "GoogleApiClient connection has failed");
  }

  @Override
  public void onLocationChanged(Location location) {
    for (Listener listener : new ArrayList<>(pendingListeners)) {
      if (listener != null) {
        listener.onLocationObtained(location);
      }
    }
    pendingListeners.clear();
  }

  public void removeLocationUpdates(Listener listener) {
    pendingListeners.remove(listener);
    if (mGoogleApiClient.isConnected()) {
      stopLocationUpdates();
      mGoogleApiClient.disconnect();
    }
  }

  private void stopLocationUpdates() {
    if (mGoogleApiClient.isConnected()) {
      LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }
  }

  //region Listener
  public interface Listener {
    void onLocationObtained(Location location);
  }
  //endregion Listener
}
