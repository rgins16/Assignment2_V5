package com.example.robbieginsburg.googleplace;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import com.google.android.gms.maps.model.LatLng;

public class MyLocationListener implements LocationListener {

    LatLng latLong;

    public LatLng getLatLong(){
        return latLong;
    }

    @Override
    public void onLocationChanged(Location location) {

        try {
            latLong = new LatLng(location.getLatitude(), location.getLongitude());
        }
        catch (Exception e) {
            latLong = null;
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}