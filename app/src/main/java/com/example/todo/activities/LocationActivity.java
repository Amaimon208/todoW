package com.example.todo.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.example.todo.BaseActivity;
import com.example.todo.GeoJsonUtils;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.PolyUtil;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LocationActivity extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private List<SelectedLocation> selectedLocations = new ArrayList<>();

    private LatLng selectedLatLng;

    private Polyline currentRoutePolyline;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private DatabaseReference databaseRef;
    private DatabaseReference markerRef;

    private String directoryId;
    private String todoId;

    private Marker lastClickedMarker = null;
    private long lastClickTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Intent intent = getIntent();
        directoryId = intent.getStringExtra("directoryId");
        todoId = intent.getStringExtra("todoId");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        String firebaseURL = "https://to-do-plus-plus-3bb3e-default-rtdb.europe-west1.firebasedatabase.app";
        databaseRef = FirebaseDatabase.getInstance(firebaseURL).getReference("users");

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            currentUserId = "anonymous";
        }

        markerRef = databaseRef.child(currentUserId).child("directories").child(directoryId).child("todos").child(todoId).child("locationMarkers");

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
                    addMarkerWithAddress(latLng);
                }
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(LocationActivity.this, "Błąd: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        Button btnDrawRoute = findViewById(R.id.btn_draw_route);
        btnDrawRoute.setOnClickListener(v -> {
            requestRoute(selectedLocations);
        });


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void loadMarkersFromIntent() {
        Intent intent = getIntent();
        String geojson = intent.getStringExtra("geojson");
        if(geojson!= null) {
            selectedLocations = GeoJsonUtils.fromGeoJsonString(geojson);
        }

        for (int i = 0; i < selectedLocations.size(); i++) {
            selectedLocations.get(i).recreateMarker(mMap);
        }
    }

    private JSONObject buildRoutesApiRequestBody(List<SelectedLocation> locations) throws JSONException, JSONException {
        JSONObject root = new JSONObject();

        JSONObject origin = new JSONObject();
        JSONObject originLocation = new JSONObject();
        JSONObject originLatLng = new JSONObject();
        originLatLng.put("latitude", locations.get(0).getLatLng().latitude);
        originLatLng.put("longitude", locations.get(0).getLatLng().longitude);
        originLocation.put("latLng", originLatLng);
        origin.put("location", originLocation);
        root.put("origin", origin);

        JSONObject destination = new JSONObject();
        JSONObject destLocation = new JSONObject();
        JSONObject destLatLng = new JSONObject();
        destLatLng.put("latitude", locations.get(locations.size() - 1).getLatLng().latitude);
        destLatLng.put("longitude", locations.get(locations.size() - 1).getLatLng().longitude);
        destLocation.put("latLng", destLatLng);
        destination.put("location", destLocation);
        root.put("destination", destination);

        JSONArray intermediates = new JSONArray();
        for (int i = 1; i < locations.size() - 1; i++) {
            JSONObject intermediate = new JSONObject();
            JSONObject intermediateLocation = new JSONObject();
            JSONObject intermediateLatLng = new JSONObject();
            intermediateLatLng.put("latitude", locations.get(i).getLatLng().latitude);
            intermediateLatLng.put("longitude", locations.get(i).getLatLng().longitude);
            intermediateLocation.put("latLng", intermediateLatLng);
            intermediate.put("location", intermediateLocation);
            intermediates.put(intermediate);
        }
        root.put("intermediates", intermediates);

        root.put("travelMode", "DRIVE");
        root.put("routingPreference", "TRAFFIC_AWARE");
        root.put("computeAlternativeRoutes", false);

        return root;
    }

    private void requestRoute(List<SelectedLocation> locations) {
        if (locations.size() < 2) {
            Toast.makeText(this, "Potrzeba co najmniej 2 lokalizacji", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                String apiKey = getString(R.string.google_maps_key);
                URL url = new URL("https://routes.googleapis.com/directions/v2:computeRoutes?key=" + apiKey);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("X-Goog-FieldMask", "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline");
                conn.setDoOutput(true);

                JSONObject requestBody = buildRoutesApiRequestBody(locations);

                try(OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();

                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject jsonResponse = new JSONObject(response.toString());

                if (responseCode == 200) {
                    // Parse routes and polylines here
                    JSONArray routes = jsonResponse.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        JSONObject polyline = route.getJSONObject("polyline");
                        String encodedPolyline = polyline.getString("encodedPolyline");

                        List<LatLng> points = PolyUtil.decode(encodedPolyline);

                        runOnUiThread(() -> {
                            if (currentRoutePolyline != null) {
                                currentRoutePolyline.remove();
                            }
                            currentRoutePolyline = mMap.addPolyline(new PolylineOptions()
                                    .addAll(points)
                                    .color(Color.BLUE)
                                    .width(10f));
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Nie znaleziono trasy", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    String errorMsg = jsonResponse.optString("errorMessage", "Błąd wyznaczania trasy");
                    runOnUiThread(() -> Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show());
                }

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Błąd sieci: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        loadMarkersFromIntent();

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        // Uniwersytet Zielonogórski - Kampus A
        LatLng campusA = new LatLng(51.941618, 15.529289);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusA, 17));

        mMap.setOnMapClickListener(latLng -> {
            addMarkerWithAddress(latLng);
        });

        mMap.setOnMarkerClickListener(marker -> {
            long currentTime = System.currentTimeMillis();

            if (marker.equals(lastClickedMarker) && (currentTime - lastClickTime < 1500)) {
                // Double-tap detected — delete marker
                new AlertDialog.Builder(LocationActivity.this)
                        .setTitle("Usuń lokalizację")
                        .setMessage("Czy chcesz usunąć tę lokalizację?")
                        .setPositiveButton("Tak", (dialog, which) -> {
                            marker.remove();

                            // Remove from selectedLocations
                            for (int i = 0; i < selectedLocations.size(); i++) {
                                if (selectedLocations.get(i).getLatLng().equals(marker.getPosition())) {
                                    selectedLocations.remove(i);
                                    break;
                                }
                            }

                            lastClickedMarker = null; // Reset
                        })
                        .setNegativeButton("Anuluj", null)
                        .show();
            } else {
                // First tap — show info window
                marker.showInfoWindow();
                lastClickedMarker = marker;
                lastClickTime = currentTime;
            }

            return true;
        });
    }

    private void addMarkerWithAddress(LatLng latLng) {
        if (latLng == null || mMap == null) return;
        String title = "Localization " + (selectedLocations.size() + 1);
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String snippetText;

        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                snippetText = address.getAddressLine(0);
            } else {
                snippetText = "Brak adresu. Współrzędne: " + latLng.latitude + ", " + latLng.longitude;
                Toast.makeText(this, "Nie udało się pobrać adresu", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            snippetText = "Współrzędne: " + latLng.latitude + ", " + latLng.longitude;
            Toast.makeText(this, "Błąd sieci — nie można pobrać adresu", Toast.LENGTH_SHORT).show();
        }

        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet(snippetText));

        if (marker != null) {
            marker.showInfoWindow();
            selectedLocations.add(new SelectedLocation(latLng, marker, title, snippetText));
        }
    }
    @Override
    public boolean onOptionsItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Create a new Intent to hold result data
            Intent resultIntent = new Intent();

            if(!selectedLocations.isEmpty()){

                String geoJsonString = GeoJsonUtils.toGeoJsonString(selectedLocations);
                resultIntent.putExtra("geojson",  geoJsonString);
            }

            // Set the result for the calling activity
            setResult(RESULT_OK, resultIntent);

            // Finish and return
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

