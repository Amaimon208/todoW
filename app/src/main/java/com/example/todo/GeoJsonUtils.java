package com.example.todo;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeoJsonUtils {
    public static String toGeoJsonString(List<SelectedLocation> locations) {
        try {
            JSONArray featuresArray = new JSONArray();

            for (SelectedLocation location : locations) {
                Map<String, Object> featureMap = location.toGeoJsonFeature();
                JSONObject featureJson = new JSONObject(featureMap);
                featuresArray.put(featureJson);
            }

            JSONObject featureCollection = new JSONObject();
            featureCollection.put("type", "FeatureCollection");
            featureCollection.put("features", featuresArray);

            return featureCollection.toString();

        } catch (JSONException e) {
            e.printStackTrace();
            return null; // or handle the error accordingly
        }
    }

    public static List<SelectedLocation> fromGeoJsonString(String geoJson) {
        List<SelectedLocation> locations = new ArrayList<>();

        try {
            JSONObject featureCollection = new JSONObject(geoJson);

            if (!"FeatureCollection".equals(featureCollection.optString("type"))) {
                return locations; // or throw error — not a FeatureCollection
            }

            JSONArray features = featureCollection.optJSONArray("features");
            if (features == null) return locations;

            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);

                JSONObject geometry = feature.optJSONObject("geometry");
                if (geometry == null || !"Point".equals(geometry.optString("type"))) continue;

                JSONArray coords = geometry.optJSONArray("coordinates");
                if (coords == null || coords.length() < 2) continue;

                double longitude = coords.getDouble(0);
                double latitude = coords.getDouble(1);
                LatLng latLng = new LatLng(latitude, longitude);

                JSONObject properties = feature.optJSONObject("properties");
                String title = "";
                String snippet = "";

                if (properties != null) {
                    title = properties.optString("title", "");
                    snippet = properties.optString("snippet", "");
                }

                // Create SelectedLocation with latLng, no marker (null), title and snippet
                SelectedLocation location = new SelectedLocation(latLng, null, title, snippet);
                locations.add(location);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return locations;
    }
}
