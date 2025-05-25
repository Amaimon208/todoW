package com.example.todo;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.UUID;

public class SelectedLocation {
    public LatLng latLng;
    public Marker marker;

    public SelectedLocation(LatLng latLng, Marker marker) {
        this.latLng = latLng;
        this.marker = marker;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public Marker getMarker() {
        return marker;
    }
}
