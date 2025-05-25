package com.example.todo.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.todo.BaseActivity;
import com.example.todo.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.checkerframework.checker.nullness.qual.NonNull;

public class DetailsActivity extends BaseActivity {
    private EditText inputTodo;
    private ImageView todoImageView;
    private String todoID, directoryID, currentUserId;
    private FirebaseAuth mAuth;
    private DatabaseReference todoRef;
    private Bitmap capturedImage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_todo);

        inputTodo = findViewById(R.id.inputTodo);
        todoImageView = findViewById(R.id.todoImage);
        Button editButton = findViewById(R.id.editButton);

        mAuth = FirebaseAuth.getInstance();
        currentUserId = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : "anonymous";

        Intent intent = getIntent();
        todoID = intent.getStringExtra("todoId");
        directoryID = intent.getStringExtra("directoryId");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        String firebaseURL = "https://to-do-plus-plus-3bb3e-default-rtdb.europe-west1.firebasedatabase.app";
        todoRef = FirebaseDatabase.getInstance(firebaseURL)
                .getReference("users")
                .child(currentUserId)
                .child("directories")
                .child(directoryID)
                .child("todos")
                .child(todoID);

        editButton.setOnClickListener(v -> {
            Intent editIntent = new Intent(this, AddTodosActivity.class);
            editIntent.putExtra("todoId", todoID);
            editIntent.putExtra("directoryId", directoryID);
            startActivity(editIntent);
        });
    }

    private void loadTodoDetails() {
        todoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String content = snapshot.child("content").getValue(String.class);
                String encodedImage = snapshot.child("image").getValue(String.class);

                inputTodo.setText(content);

                if (encodedImage != null && !encodedImage.isEmpty()) {
                    capturedImage = decodeImage(encodedImage);
                    todoImageView.setImageBitmap(capturedImage);
                    todoImageView.setVisibility(View.VISIBLE);
                } else {
                    todoImageView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DetailsActivity.this, "Failed to load todo details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap decodeImage(String encodedImage) {
        byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    @Override
    public boolean onOptionsItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            boolean isUpdated = data.getBooleanExtra("isUpdated", false);
            if (isUpdated) {
                recreate();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTodoDetails();
    }
}
