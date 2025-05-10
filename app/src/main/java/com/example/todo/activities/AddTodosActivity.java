package com.example.todo.activities;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.todo.BaseActivity;
import com.example.todo.R;
import com.example.todo.Todo;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class AddTodosActivity extends BaseActivity {
    private SharedPreferences sharedPreferences;
    private static final int DIRECTORY_MANAGEMENT_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private DatabaseReference todosRef;

    private String currentUserId;
    private String directoryId;
    private String todoID;
    private EditText inputTodo;

    private Bitmap capturedImage = null;
    private ImageView todoImageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        directoryId = intent.getStringExtra("directoryId");
        todoID = intent.getStringExtra("todoID");

        String firebaseURL = "https://to-do-plus-plus-3bb3e-default-rtdb.europe-west1.firebasedatabase.app";
        databaseRef = FirebaseDatabase.getInstance(firebaseURL).getReference("users");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_todo);
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

        todosRef = databaseRef.child(currentUserId).child("directories").child(directoryId).child("todos");

        initializeViews();

        if(todoID != null){
            setCurrentData();
        }
    }

    private void setCurrentData() {
        todosRef.child(todoID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("content").getValue(String.class);
                inputTodo.setText(name);

                String capturedImageBase64 = snapshot.child("image").getValue(String.class);
                if (capturedImageBase64 != null && !capturedImageBase64.isEmpty()) {
                    capturedImage = decodeImage(capturedImageBase64);
                    todoImageView.setImageBitmap(capturedImage);
                    todoImageView.setVisibility(View.VISIBLE);
                } else {
                    todoImageView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeViews() {
        inputTodo = findViewById(R.id.inputTodo);
        todoImageView = findViewById(R.id.todoImage);
        Button addButton = findViewById(R.id.addButton);

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
        if(todoID == null){
            todoID = UUID.randomUUID().toString();
        }

        String todoText = inputTodo.getText().toString().trim();
        Todo todo;

        // Convert the Bitmap image to Base64 if it exists
        if (capturedImage == null) {
            todo = new Todo(todoID, todoText); // No image, only text
        } else {
            String encodedImage = encodeImage(capturedImage); // Encode the image as Base64 string
            todo = new Todo(todoID, todoText, encodedImage); // Save the Base64 encoded image
        }

        todosRef.child(todoID)
                .setValue(todo)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Todo saved successfully!", Toast.LENGTH_SHORT).show();
                    capturedImage = null; // Clear the captured image after saving
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save todo.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
        finish();
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

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                if (imageBitmap != null) {
                    capturedImage = imageBitmap;
                    todoImageView.setImageBitmap(capturedImage);
                    todoImageView.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Image captured.", Toast.LENGTH_SHORT).show();
                }
            }
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
        FloatingActionButton deleteButton = findViewById(R.id.deletePictureButton);
        ImageView todoImage = findViewById(R.id.todoImage);

        deleteButton.setOnClickListener(v -> {
            capturedImage = null; // Clear the image from memory
            todoImage.setVisibility(View.GONE); // Hide the ImageView
            todoImage.setImageDrawable(null); // Optionally clear the drawable
            Toast.makeText(this, "Image removed.", Toast.LENGTH_SHORT).show();
        });
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