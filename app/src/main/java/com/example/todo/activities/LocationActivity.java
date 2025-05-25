package com.example.todo.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.maps.android.PolyUtil;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
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
    private List<Marker> markers = new ArrayList<>();

    private final List<SelectedLocation> selectedLocations = new ArrayList<>();

    private LatLng selectedLatLng;

    private Polyline currentRoutePolyline;

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
                    Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(place.getName()));
                    markers.add(marker);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    selectedLatLng = latLng;
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

        // Intermediates (waypoints)
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

//    private void drawFullRouteWithWaypoints(List<SelectedLocation> locations) {
//        if (locations.size() < 2) {
//            Toast.makeText(this, "Potrzeba co najmniej 2 lokalizacji", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String apiKey = getString(R.string.google_maps_key); // or load from meta-data
//        LatLng origin = locations.get(0).getLatLng();
//        LatLng destination = locations.get(locations.size() - 1).getLatLng();
//
//        // Build waypoints string
//        StringBuilder waypoints = new StringBuilder();
//        for (int i = 1; i < locations.size() - 1; i++) {
//            LatLng point = locations.get(i).getLatLng();
//            waypoints.append(point.latitude).append(",").append(point.longitude);
//            if (i < locations.size() - 2) {
//                waypoints.append("|");
//            }
//        }
//
//        // Build request URL
//        String url = "https://maps.googleapis.com/maps/api/directions/json?"
//                + "origin=" + origin.latitude + "," + origin.longitude
//                + "&destination=" + destination.latitude + "," + destination.longitude
//                + (waypoints.length() > 0 ? "&waypoints=" + waypoints : "")
//                + "&key=" + apiKey;
//
//        Log.d("DIRECTIONS_URL", url);
//
//        new Thread(() -> {
//            try {
//                URL requestUrl = new URL(url);
//                HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
//                connection.connect();
//
//                InputStreamReader input = new InputStreamReader(connection.getInputStream());
//                BufferedReader reader = new BufferedReader(input);
//
//                StringBuilder sb = new StringBuilder();
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    sb.append(line);
//                }
//
//                JSONObject jsonResponse = new JSONObject(sb.toString());
//                JSONArray routes = jsonResponse.getJSONArray("routes");
//
//                if (routes.length() > 0) {
//                    JSONObject route = routes.getJSONObject(0);
//                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
//                    String encodedPolyline = overviewPolyline.getString("points");
//
//                    List<LatLng> points = PolyUtil.decode(encodedPolyline);
//
//                    runOnUiThread(() -> {
//                        mMap.addPolyline(new PolylineOptions()
//                                .addAll(points)
//                                .color(Color.BLUE)
//                                .width(10f));
//                    });
//                } else {
//                    runOnUiThread(() -> Toast.makeText(this, "Nie udało się wyznaczyć trasy", Toast.LENGTH_SHORT).show());
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                runOnUiThread(() -> Toast.makeText(this, "Błąd podczas pobierania trasy", Toast.LENGTH_SHORT).show());
//            }
//        }).start();
//    }

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
@SuppressLint("PotentialBehaviorOverride")
@Override
public void onMapReady(@NonNull GoogleMap googleMap) {
    mMap = googleMap;

    mMap.getUiSettings().setZoomControlsEnabled(true);
    mMap.getUiSettings().setZoomGesturesEnabled(true);

    // Uniwersytet Zielonogórski - Kampus A
    LatLng campusA = new LatLng(51.941618, 15.529289);
    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(campusA, 17));
//    mMap.addMarker(new MarkerOptions().position(campusA).title("UZ - Kampus A"));

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

