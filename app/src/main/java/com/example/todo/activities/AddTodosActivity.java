package com.example.todo.activities;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.todo.BaseActivity;
import com.example.todo.GeoJsonUtils;
import com.example.todo.R;
import com.example.todo.SelectedLocation;
import com.example.todo.Todo;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AddTodosActivity extends BaseActivity {


    private static final int PICK_PDF_REQUEST = 1;
    private SharedPreferences sharedPreferences;
    private static final int DIRECTORY_MANAGEMENT_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    public static final int REQUEST_LOCATION = 1001;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private DatabaseReference todosRef;
    private String currentUserId;
    private String directoryId;
    private String todoId;
    private EditText inputTodo;
    private Bitmap capturedImage = null;
    private ImageView todoImageView;

    private FloatingActionButton deleteButton;

    private List<SelectedLocation> selectedLocations = null;

    private boolean drawRoute;

    private String pfdFile;

    private String pfdFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        directoryId = intent.getStringExtra("directoryId");
        todoId = intent.getStringExtra("todoId");
        drawRoute = getIntent().getBooleanExtra("drawRoute", false);
        //Move fetching  data to AddTODO Activity and move it via intent

        String firebaseURL = "https://to-do-plus-plus-3bb3e-default-rtdb.europe-west1.firebasedatabase.app";
        databaseRef = FirebaseDatabase.getInstance(firebaseURL).getReference("users");
        setContentView(R.layout.activity_add_todo);
        initializeViews();

        setupCameraButton();
        setupDeletePictureButton();

        mAuth = FirebaseAuth.getInstance();
        currentUserId = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : "anonymous";

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean("night_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isNightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        Button locationButton = findViewById(R.id.btn_select_location);
        locationButton.setOnClickListener(v -> {
            Intent newIntent = new Intent(AddTodosActivity.this, LocationActivity.class);
            newIntent.putExtra("directoryId", directoryId);
            newIntent.putExtra("todoId", todoId);
            newIntent.putExtra("drawRoute", drawRoute);
            if(!selectedLocations.isEmpty()){

                String geoJsonString = GeoJsonUtils.toGeoJsonString(selectedLocations);
                newIntent.putExtra("geojson",  geoJsonString);
            }
            startActivityForResult(newIntent, REQUEST_LOCATION);
        });

        todosRef = databaseRef.child(currentUserId).child("directories").child(directoryId).child("todos");

        if(todoExist()){
            setCurrentData();
        }
        setPictureVisibility();
        loadMarkersFromFirebase();
        loadPDFDataFromFirebase(todoId);

        findViewById(R.id.selectPdfButton).setOnClickListener(v -> pickPdf());

        Button openPdfButton = findViewById(R.id.openPdfButton);

        openPdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(pfdFilePath);
                if (!file.exists()) {
                    Toast.makeText(AddTodosActivity.this, "PDF file not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                Uri uri = FileProvider.getUriForFile(
                        AddTodosActivity.this,
                        getPackageName() + ".fileprovider",
                        file
                );

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(AddTodosActivity.this, "No PDF viewer found", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void pickPdf() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(intent, PICK_PDF_REQUEST);
    }

    private void savePdfToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            String fileName = "pdf_" + System.currentTimeMillis() + ".pdf";
            File file = new File(getFilesDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();

            pfdFile = fileName;
            pfdFilePath = file.getAbsolutePath();

//            saveToFirebase(fileName, file.getAbsolutePath());

        } catch (Exception e) {
            Log.e("LocationActivity", "Błąd zapisu PDF: " + e.getMessage());
        }
    }

        private void loadMarkersFromFirebase() {
            if(todoId != null) {
                todosRef.child(todoId).child("locationMarkers").addValueEventListener (new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        selectedLocations = new ArrayList<>();

                        String route = dataSnapshot.child("route").getValue(String.class);
                        if(route != null) {
                            if(route.equals("true")) {
                                drawRoute = true;
                            }else{
                                drawRoute = false;
                            }
                        }

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            try {
                                // Navigate to the fields
                                DataSnapshot geometrySnapshot = snapshot.child("geometry");
                                DataSnapshot coordinatesSnapshot = geometrySnapshot.child("coordinates");
                                DataSnapshot propertiesSnapshot = snapshot.child("properties");

                                double longitude = coordinatesSnapshot.child("0").getValue(Double.class);
                                double latitude = coordinatesSnapshot.child("1").getValue(Double.class);
                                LatLng latLng = new LatLng(latitude, longitude);

                                String title = propertiesSnapshot.child("title").getValue(String.class);
                                String snippet = propertiesSnapshot.child("snippet").getValue(String.class);

                                selectedLocations.add(new SelectedLocation(latLng, null, title, snippet));
                            } catch (Exception e) {
                                Log.e("LocationActivity", "Error parsing GeoJSON marker: " + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("LocationActivity", "Failed to load markers: " + databaseError.getMessage());
                        Toast.makeText(AddTodosActivity.this, "Failed to load markers.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
    }

    private boolean todoExist() {
        return todoId != null;
    }

    private void setCurrentData() {
        todosRef.child(todoId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String content = snapshot.child("content").getValue(String.class);
                inputTodo.setText(content);
                String capturedImageBase64 = snapshot.child("image").getValue(String.class);
                if (capturedImageBase64 != null && !capturedImageBase64.isEmpty()) {
                    capturedImage = decodeImage(capturedImageBase64);
                }
                setPictureVisibility();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setPictureVisibility() {
        if (capturedImage == null) {
            todoImageView.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        } else {
            todoImageView.setImageBitmap(capturedImage);
            todoImageView.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
        }
    }

    private void initializeViews() {
        inputTodo = findViewById(R.id.inputTodo);
        todoImageView = findViewById(R.id.todoImage);
        deleteButton = findViewById(R.id.deletePictureButton);
        Button addButton = findViewById(R.id.addButton);

        if(todoExist()){
            addButton.setText(R.string.edit);
        }

        addButton.setOnClickListener(v -> {
            String todoText = inputTodo.getText().toString().trim();
            if (!todoText.isEmpty()) {
                saveTodoWithOptionalImage();
                inputTodo.setText("");
            }
            onBackPressed();
        });
    }

    private void saveTodoWithOptionalImage() {
        if(todoId == null){
            todoId = UUID.randomUUID().toString();
        }

        String todoText = inputTodo.getText().toString().trim();
        Todo todo;

        // Convert the Bitmap image to Base64 if it exists
        if (capturedImage == null) {
            todo = new Todo(todoId, todoText); // No image, only text
        } else {
            String encodedImage = encodeImage(capturedImage); // Encode the image as Base64 string
            todo = new Todo(todoId, todoText, encodedImage); // Save the Base64 encoded image
        }

        todosRef.child(todoId)
                .setValue(todo)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Todo saved successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save todo.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });

        addMarkerToDatabase();
        if(pfdFile != null && pfdFilePath != null){
            savePDFToFirebase(pfdFile, pfdFilePath);
        }

        finish();
    }

    private void savePDFToFirebase(String name, String path) {
        Map<String, Object> pdfData = new HashMap<>();
        pdfData.put("name", name);
        pdfData.put("path", path);
        pdfData.put("timestamp", System.currentTimeMillis());

        todosRef.child(todoId).child("pdf").setValue(pdfData);
    }

    private void loadPDFDataFromFirebase(String todoId) {
        todosRef.child(todoId).child("pdf").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Pobieramy "name" i "path" jako String
                    pfdFile = snapshot.child("name").getValue(String.class);
                    pfdFilePath = snapshot.child("path").getValue(String.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebasePDF", "Failed to read PDF data", error.toException());
            }
        });
    }

    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream); // Compress to PNG format
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT); // Encode to Base64
    }

    private Bitmap decodeImage(String encodedImage) {
        byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String selectedLocationsString = data.getStringExtra("geojson");
        if(selectedLocationsString != null) {
            selectedLocations = GeoJsonUtils.fromGeoJsonString(selectedLocationsString);
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                if (imageBitmap != null) {
                    capturedImage = imageBitmap;
                    setPictureVisibility();
                    Toast.makeText(this, "Image captured.", Toast.LENGTH_SHORT).show();
                }
            }
        }

        drawRoute = data.getBooleanExtra("drawRoute", false);

        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri pdfUri = data.getData();
            savePdfToInternalStorage(pdfUri);
        }
    }

    private void setupCameraButton() {
        FloatingActionButton cameraButton = findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(v -> checkCameraPermission());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.camera_not_found), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void setupDeletePictureButton() {
        ImageView todoImage = findViewById(R.id.todoImage);

        deleteButton.setOnClickListener(v -> {
            capturedImage = null; // Clear the image from memory
            todoImage.setImageDrawable(null); // Optionally clear the drawable
            Toast.makeText(this, "Image removed.", Toast.LENGTH_SHORT).show();
            setPictureVisibility();
        });
    }

    private void addMarkerToDatabase() {
        if(selectedLocations != null) {
            todosRef.child(todoId).child("locationMarkers").removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firebase", "All location markers deleted.");
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firebase", "Failed to delete markers: " + e.getMessage());
                    });
            for(int i=0; i < selectedLocations.size(); i++ ){
                Map<String, Object> geoJson = selectedLocations.get(i).toGeoJsonFeature();
                todosRef.child(todoId)
                        .child("locationMarkers").push().setValue(geoJson);
            }
        }
        if(drawRoute){
            todosRef.child(todoId).child("locationMarkers").child("route").setValue("true");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}