package com.example.todo.activities;

import android.content.Intent;
import android.os.Bundle;

import com.example.todo.BaseActivity;
import com.example.todo.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.checkerframework.checker.nullness.qual.NonNull;

public class LocationActivity extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear(); // remove previous marker
            mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
            selectedLatLng = latLng;

            LatLng defaultLocation = new LatLng(37.7749, -122.4194); // San Francisco
            mMap.addMarker(new MarkerOptions().position(defaultLocation).title("Marker"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));

            // Return the selected location to ToDoActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("lat", latLng.latitude);
            resultIntent.putExtra("lng", latLng.longitude);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}

