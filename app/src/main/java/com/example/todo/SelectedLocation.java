package com.example.todo;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SelectedLocation {
    private LatLng latLng;
    private Marker marker;
    private String title;
    private String snippet;

    public SelectedLocation() {}

    public SelectedLocation(LatLng latLng, Marker marker, String title, String snippet) {
        this.latLng = latLng;
        this.marker = marker;
        this.title = title;
        this.snippet = snippet;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public Marker getMarker() {
        return marker;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void recreateMarker(GoogleMap mMap) {
        if(latLng != null && title != null && snippet!= null){
            marker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .snippet(snippet));
        } else {
            Log.e("LocationActivity", "Missing latLng "
                    + latLng
                    +", title "
                    + title
                    + " or snippet "
                    + snippet);
        }
    }

    // Convert to GeoJSON feature as a Map<String,Object>
    public Map<String, Object> toGeoJsonFeature() {
        Map<String, Object> feature = new HashMap<>();
        feature.put("type", "Feature");

        Map<String, Object> geometry = new HashMap<>();
        geometry.put("type", "Point");
        geometry.put("coordinates", Arrays.asList(latLng.longitude, latLng.latitude));

        feature.put("geometry", geometry);

        Map<String, Object> properties = new HashMap<>();
        properties.put("title", title);
        properties.put("snippet", snippet);

        feature.put("properties", properties);

        return feature;
    }
}