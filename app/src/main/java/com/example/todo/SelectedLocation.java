package com.example.todo;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

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