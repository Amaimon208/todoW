package com.example.todo.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todo.BaseActivity;
import com.example.todo.R;
import com.example.todo.Todo;
import com.example.todo.TodoAdapter;
import com.example.todo.activities.Login.RegisterActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class TodosActivity extends BaseActivity {
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
        setContentView(R.layout.activity_todos);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            currentUserId = "anonymous";
        }

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
                Toast.makeText(TodosActivity.this, "Failed to load todos.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeViews() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        EditText inputTodo = findViewById(R.id.inputTodo);
        Button addButton = findViewById(R.id.addButton);

        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTodosActivity.class);
            intent.putExtra("directoryId", directoryId);
            intent.putExtra("directoryName", directoryName);
            intent.putExtra("userId", currentUserId);
            startActivity(intent);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}