package com.example.todo.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todo.BaseActivity;
import com.example.todo.R;
import com.example.todo.Todo;
import com.example.todo.TodoAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class AddTodosActivity extends BaseActivity {
    private TodoAdapter todoAdapter;
    private SharedPreferences sharedPreferences;
    private static final int DIRECTORY_MANAGEMENT_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private DatabaseReference todosRef;
    private String directoryId;
    private String directoryName;
    private String currentUserId;
    private final ArrayList<Todo> allNotes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        directoryId = intent.getStringExtra("directoryId");
        directoryName = intent.getStringExtra("directoryName");

        String firebaseURL = "https://to-do-plus-plus-3bb3e-default-rtdb.europe-west1.firebasedatabase.app";
        databaseRef = FirebaseDatabase.getInstance(firebaseURL).getReference("users");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_todo);
        setupCameraButton();

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            currentUserId = "anonymous";
        }

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

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        todoAdapter = new TodoAdapter(allNotes, this, directoryId, currentUserId);
        recyclerView.setAdapter(todoAdapter);

        loadTodosFromFirebase();
        initializeViews();
    }

    private void loadTodosFromFirebase() {
        todosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allNotes.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Todo todo = snapshot.getValue(Todo.class);
                    if (todo != null) {
                        allNotes.add(todo);
                    }
                }

                todoAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("AddTodosActivity", "Failed to load todos: " + databaseError.getMessage());
                Toast.makeText(AddTodosActivity.this, "Failed to load todos.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeViews() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        EditText inputTodo = findViewById(R.id.inputTodo);
        Button addButton = findViewById(R.id.addButton);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch themeToggleBtn = findViewById(R.id.themeToggleBtn);
        boolean isNightMode = sharedPreferences.getBoolean("night_mode", false);
        themeToggleBtn.setText(isNightMode ? getString(R.string.light_mode) : getString(R.string.dark_mode));
        themeToggleBtn.setChecked(isNightMode);
        themeToggleBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("night_mode", isChecked);
            editor.apply();
            themeToggleBtn.setText(isChecked ? getString(R.string.light_mode) : getString(R.string.dark_mode));

            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );

            recreate();
        });

        addButton.setOnClickListener(v -> {
            String todoText = inputTodo.getText().toString().trim();
            if (!todoText.isEmpty()) {
                saveTodoToFirebase(todoText);
                inputTodo.setText("");
            }
        });
    }

    private void saveTodoToFirebase(String todoText) {
        String todoId = databaseRef.push().getKey();
        if (todoId != null) {
            Todo todo = new Todo(todoId, "text", todoText);

            databaseRef.child(currentUserId)
                    .child("directories")
                    .child(directoryId)
                    .child("todos")
                    .child(todoId)
                    .setValue(todo)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Todo added successfully!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to add todo.", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    });
        }
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
                    saveImageToFirebase(imageBitmap);
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

    private void saveImageToFirebase(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);

            String todoId = UUID.randomUUID().toString();
            Todo todo = new Todo(todoId, "image", "data:image/jpeg;base64," + base64Image);

            databaseRef.child(currentUserId)
                    .child("directories")
                    .child(directoryId)
                    .child("todos")
                    .child(todoId)
                    .setValue(todo)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firebase", "Image saved successfully");
                        Toast.makeText(this, "Image saved successfully!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firebase", "Error saving image: " + e.getMessage());
                        Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Log.e("Firebase", "Error processing image: " + e.getMessage());
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}