package com.example.todo.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todo.BaseActivity;
import com.example.todo.Directory;
import com.example.todo.DirectoryAdapter;
import com.example.todo.R;
import com.example.todo.activities.Login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends BaseActivity {
//    BaseActivity
    private DirectoryAdapter directoryAdapter;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private String currentUserId;
    private ArrayList<Directory> directoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        setContentView(R.layout.activity_main);
//        getLayoutInflater().inflate(R.layout.activity_main, findViewById(R.id.content_frame));
        setContentView(R.layout.activity_main);

//        Toolbar toolbar = findViewById(R.id.tool_bar);
//        setSupportActionBar(toolbar);

        // Setup toolbar once here
//        Toolbar toolbar = findViewById(R.id.tool_bar);
//        setSupportActionBar(toolbar);


        directoryList = new ArrayList<>();
        directoryAdapter = new DirectoryAdapter(directoryList, this);

        initializeViews();

        mAuth = FirebaseAuth.getInstance();
        String firebaseURL = "https://to-do-plus-plus-3bb3e-default-rtdb.europe-west1.firebasedatabase.app";
        databaseRef = FirebaseDatabase.getInstance(firebaseURL).getReference("users");
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            currentUserId = "anonymous";
        }
        fetchDirectories();
    }

    private void initializeViews() {
        RecyclerView recyclerView = findViewById(R.id.directoriesRecyclerView);
        Button addDirectoryButton = findViewById(R.id.addDirectoryButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(directoryAdapter);

        addDirectoryButton.setOnClickListener(v -> showAddDirectoryDialog());
    }

    @SuppressLint("NotifyDataSetChanged")
    private void showAddDirectoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.new_directory);

        final EditText input = new EditText(this);
        input.setHint(R.string.directory_name);
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            String directoryName = input.getText().toString().trim();
            if (!directoryName.isEmpty()) {
                saveDirectoryToFirebase(directoryName);
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveDirectoryToFirebase(String directoryName) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> directoryData = new HashMap<>();
        directoryData.put("id", id);
        directoryData.put("name", directoryName);

        databaseRef.child(currentUserId).child("directories").child(id).setValue(directoryData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Directory added!", Toast.LENGTH_SHORT).show();
                    fetchDirectories(); // 🔥 odśwież listę
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add directory.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

private void fetchDirectories() {
    if (databaseRef != null && currentUserId != null) {
        databaseRef.child(currentUserId).child("directories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                directoryList.clear();
                for (DataSnapshot directorySnapshot : snapshot.getChildren()) {
                    String id = directorySnapshot.child("id").getValue(String.class);
                    String name = directorySnapshot.child("name").getValue(String.class);
                    if (id != null && name != null) {
                        directoryList.add(new Directory(id, name));
                    }
                }
                directoryAdapter.notifyDataSetChanged(); // Update the UI
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load directories.", Toast.LENGTH_SHORT).show();
            }
        });
    } else {
        Toast.makeText(this, "Database reference or user ID " + currentUserId, Toast.LENGTH_SHORT).show();
    }
}
}
