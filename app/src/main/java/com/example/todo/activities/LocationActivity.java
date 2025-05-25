package com.example.todo.activities;

import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.example.todo.BaseActivity;
import com.example.todo.R;
import com.example.todo.SelectedLocation;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LocationActivity extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private List<Marker> markers = new ArrayList<>();

    private final List<SelectedLocation> selectedLocations = new ArrayList<>();

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

//    @Override
//    public void onMapReady(@NonNull GoogleMap googleMap) {
//        mMap = googleMap;
//
//        mMap.getUiSettings().setZoomControlsEnabled(true);
//        mMap.getUiSettings().setZoomGesturesEnabled(true);
//
//        // Uniwersytet Zielonogórski - Kampus A
//        LatLng campusA = new LatLng(51.941618, 15.529289);
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusA, 17)); // 17 = dobre przybliżenie budynku
//        mMap.addMarker(new MarkerOptions().position(campusA).title("UZ - Kampus A"));
//
//        mMap.setOnMapClickListener(latLng -> {
//            mMap.clear();
//            mMap.addMarker(new MarkerOptions().position(latLng).title("Wybrana lokalizacja"));
//            selectedLatLng = latLng;
//
//            Intent resultIntent = new Intent();
//            resultIntent.putExtra("lat", latLng.latitude);
//            resultIntent.putExtra("lng", latLng.longitude);
//            setResult(RESULT_OK, resultIntent);
//            finish();
//        });
//
//        //Geolocation button
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//            return;
//        }
//
//        mMap.setMyLocationEnabled(true);
//
//        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//        fusedLocationClient.getLastLocation()
//                .addOnSuccessListener(this, location -> {
//                    if (location != null) {
//                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
//                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
//                    }
//                });
//    }
@Override
public void onMapReady(@NonNull GoogleMap googleMap) {
    mMap = googleMap;

    mMap.getUiSettings().setZoomControlsEnabled(true);
    mMap.getUiSettings().setZoomGesturesEnabled(true);

    // Uniwersytet Zielonogórski - Kampus A
    LatLng campusA = new LatLng(51.941618, 15.529289);
    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusA, 17));
    mMap.addMarker(new MarkerOptions().position(campusA).title("UZ - Kampus A"));

    mMap.setOnMapClickListener(latLng -> {
        // No longer clearing the map to allow multiple selections
        Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title("Wybrana lokalizacja"));
        markers.add(marker); // Store marker for later use
        selectedLocations.add(new SelectedLocation(latLng, marker));

        // TODO: Show a dialog, bottom sheet, or some UI to enter additional info for this location

        // TODO: Create a data structure (e.g. custom class) to hold all info (lat, lng, user comment, etc.)

        // TODO: Eventually convert selectedLocations into a GeoJSON format and store it

        // NOTE: Don't finish() or return the result yet – user might want to add more locations
    });

    mMap.setOnMarkerClickListener(marker -> {
        // Show a dialog to confirm deletion
        new AlertDialog.Builder(LocationActivity.this)
                .setTitle("Usuń lokalizację")
                .setMessage("Czy chcesz usunąć tę lokalizację?")
                .setPositiveButton("Tak", (dialog, which) -> {
                    // Remove marker from map
                    marker.remove();

                    // Remove from list
                    for (int i = 0; i < selectedLocations.size(); i++) {
                        if (selectedLocations.get(i).getMarker().equals(marker)) {
                            selectedLocations.remove(i);
                            break;
                        }
                    }
                })
                .setNegativeButton("Anuluj", null)
                .show();

        return true;
    });

//    fusedLocationClient.getLastLocation()
//            .addOnSuccessListener(this, location -> {
//                if (location != null) {
//                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
//                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
//                }
//            });
    }
}

