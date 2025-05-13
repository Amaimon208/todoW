package com.example.todo.activities;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.example.todo.BaseActivity;
import com.example.todo.R;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.Locale;

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



        if (!Places.isInitialized()) {
            try {
                ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
                Bundle bundle = ai.metaData;
                String apiKey = bundle.getString("com.google.android.geo.API_KEY");

                if (!Places.isInitialized()) {
                    Places.initialize(getApplicationContext(), apiKey, Locale.getDefault());
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                LatLng latLng = place.getLatLng();
                if (latLng != null) {
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(latLng).title(place.getName()));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    selectedLatLng = latLng;
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(LocationActivity.this, "Błąd: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        // Uniwersytet Zielonogórski - Kampus A
        LatLng campusA = new LatLng(51.941618, 15.529289);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusA, 17)); // 17 = dobre przybliżenie budynku
        mMap.addMarker(new MarkerOptions().position(campusA).title("UZ - Kampus A"));

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title("Wybrana lokalizacja"));
            selectedLatLng = latLng;

            Intent resultIntent = new Intent();
            resultIntent.putExtra("lat", latLng.latitude);
            resultIntent.putExtra("lng", latLng.longitude);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}

