package com.example.robbieginsburg.googleplace;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity implements PlaceSelectionListener {

    private GoogleMap googleMap;

    private Marker userMarker, targetMarker = null;

    private LatLng userLocation, lastUserLocation, targetLocation = null;

    private LocationManager lm;
    private MyLocationListener location;

    UpdateUserLocation updateUserLocation;

    private Handler updateLocation = new Handler();
    private Handler zoomToUser = new Handler();
    private Handler determineDistance = new Handler();

    // used to help zoom to the user's location the first time it has been found
    private Boolean zoom = false;

    private Toast toast;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(this);

        googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);


        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        location = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, location);

        // start asynctask that constantly updates the user's location
        updateUserLocation = new UpdateUserLocation();
        updateUserLocation.execute();

        // this handler is for updating the user's location
        updateLocation.post(UpdateLocation);

        // this handler is for zooming to the user's location the first time it has been found
        zoomToUser.post(ZoomToUser);

        // this handler constantly checks to see if the distance in meters between the user and
        // the place is less then or equal to 200 meters
        determineDistance.post(DetermineDistance);
    }

    // this runnable will constantly check to see if the distance in meters between the user and
    // the place is less then or equal to 200 meters
    Runnable DetermineDistance = new Runnable() {
        @Override
        public void run() {

            /*
            * This is the implementation Haversine Distance Algorithm between two places
            * R = earth’s radius (mean radius = 6,371km) = 6,371,000 meters
            * Δlat = lat2− lat1
            * Δlong = long2− long1
            * a = sin²(Δlat/2) + cos(lat1).cos(lat2).sin²(Δlong/2)
            * c = 2.atan2(√a, √(1−a))
            * d = R.c
            */

            // only calculates the distance if the user's location can be determined and if the
            // user has chosen a place on the google map
            if (userMarker != null && targetMarker != null) {

                // Radius of the earth in meters
                final int R = 6371000;

                Double lat1 = userLocation.latitude;
                Double long1 = userLocation.longitude;
                Double lat2 = targetLocation.latitude;
                Double long2 = targetLocation.longitude;

                Double latDistance = Math.toRadians(lat2 - lat1);
                Double longDistance = Math.toRadians(long2 - long1);

                Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                                Math.sin(longDistance / 2) * Math.sin(longDistance / 2);
                Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                Double distance = (R * c);

                // rounds the distance to two decimal places
                DecimalFormat twoDForm = new DecimalFormat("#.##");
                distance = Double.valueOf(twoDForm.format(distance));

                // if the user is within 200 meters of the place they chose on the map,
                // notify the with a toast message
                // the toast message will keep displaying until they move away from the location
                if(distance <= 200){

                    // if there is already a toast message cancel it
                    if (toast != null) {
                        toast.cancel();
                    }
                    // then display an updated toast message to the user if they are still within
                    // 200 meters of the location
                    toast = Toast.makeText(MainActivity.this, "The distance between your location " +
                            "and the target location is within 200 meters." + "\n" +
                            "The current distance between you and the target is: " +
                            distance.toString() + " meters", Toast.LENGTH_LONG);
                    toast.show();
                }
            }

            determineDistance.post(DetermineDistance);
        }
    };

    // this runnable will only run until has zoomed to the user's initial location
    // upon it being found
    Runnable ZoomToUser = new Runnable() {
        @Override
        public void run() {

            // zoom to the user's location the first time it has been found
            if((userLocation != null) && !zoom){

                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16));

                zoom = true;

                // do nothing and the handler will stop running
            }
            else{

                // keep running the handler until it has zoomed to the user's initial
                // location once it has been found
                zoomToUser.post(ZoomToUser);
            }
        }
    };

    // if the user's location has changed, then update the marker
    Runnable UpdateLocation = new Runnable() {
        @Override
        public void run() {
            // checks to make sure a location has been found
            // additionally checks to make sure the location is not the same
            // if the location hasn't changed, there is no need to update the marker
            if((userLocation != null) && (lastUserLocation != userLocation)){

                Log.d("Location has", "been changed");

                // updates the marker for the user's selected place
                if(userMarker != null) userMarker.remove();
                userMarker = googleMap.addMarker(new MarkerOptions()
                        .position(userLocation)
                        .icon(BitmapDescriptorFactory.
                                defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            }

            // updates the last recorded user location
            lastUserLocation = userLocation;

            updateLocation.post(UpdateLocation);
        }
    };

    @Override
    public void onPlaceSelected(Place place) {
        // get info about the selected place.
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 16));

        // updates the marker for the user's selected place
        if(targetMarker != null) targetMarker.remove();
        targetMarker = googleMap.addMarker(new MarkerOptions()
                .position(place.getLatLng())
                .icon(BitmapDescriptorFactory.
                        defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // updates the target location's coordinates
        targetLocation = place.getLatLng();
    }

    @Override
    public void onError(Status status) {
        // there was an error
        Log.d("Status", "" + status);
    }

    // async task that constantly gets the user's location
    private class UpdateUserLocation extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {return null;}

        @Override
        protected void onPostExecute(String result) {

            // updates the user's current location
            userLocation = location.getLatLong();

            // make the async task repeat itself so it can keep updating the user's location
            updateUserLocation = new UpdateUserLocation();
            updateUserLocation.execute();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        updateLocation.removeCallbacks(UpdateLocation);
        zoomToUser.removeCallbacks(ZoomToUser);
        determineDistance.removeCallbacks(DetermineDistance);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        lm.removeUpdates(location);
    }
}