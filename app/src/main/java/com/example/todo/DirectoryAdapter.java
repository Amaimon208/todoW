package com.example.todo;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todo.activities.TodosActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class DirectoryAdapter extends RecyclerView.Adapter<DirectoryAdapter.DirectoryViewHolder> {
    public static final int DIRECTORY_NOTES_REQUEST = 2;
    private final ArrayList<Directory> directories;
    private final Context context;
    private DatabaseReference databaseRef;
    private FirebaseAuth mAuth;
    private String currentUserId;

    public DirectoryAdapter(ArrayList<Directory> directories, Context context) {
        this.directories = directories;
        this.context = context;
    }

    @NonNull
    @Override
    public DirectoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.directory_item, parent, false);
        String firebaseURL = "https://to-do-plus-plus-3bb3e-default-rtdb.europe-west1.firebasedatabase.app";
        databaseRef = FirebaseDatabase.getInstance(firebaseURL).getReference("users");
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            currentUserId = "anonymous";
        }

        return new DirectoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DirectoryViewHolder holder, int position) {
        Directory directory = directories.get(position);
        holder.directoryName.setText(directory.getName());
        holder.notesCount.setText(context.getString(R.string.notes_count, directory.getNotes().size()));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TodosActivity.class);
            intent.putExtra("directoryPosition", position);
            intent.putExtra("directoryName", directory.getName());
            intent.putExtra("directoryId", directory.getId());
            intent.putStringArrayListExtra("notes", directory.getNotes());
            ((Activity) context).startActivityForResult(intent, DIRECTORY_NOTES_REQUEST);
        });

        holder.editButton.setOnClickListener(v -> showEditDialog(directory, position));
        holder.deleteButton.setOnClickListener(v -> showDeleteDialog(position, directory.getId()));
    }

    @Override
    public int getItemCount() {
        return directories.size();
    }

    private void showEditDialog(Directory directory, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.edit_directory);

        final EditText input = new EditText(context);
        input.setText(directory.getName());
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                String directoryId = directory.getId();
                databaseRef
                        .child(currentUserId)
                        .child("directories")
                        .child(directoryId)
                        .child("name")
                        .setValue(newName)
                        .addOnSuccessListener(aVoid -> {
                            directory.setName(newName);

                            notifyItemChanged(position);

                            Toast.makeText(context, "Directory updated!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Failed to update directory.", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        });
            } else {
                Toast.makeText(context, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDeleteDialog(int position, String directoryId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_directory);
        builder.setMessage(R.string.delete_directory_hint);

        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            databaseRef
                    .child(currentUserId)
                    .child("directories")
                    .child(directoryId)
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        directories.remove(position);
                        notifyItemRemoved(position);
                        Toast.makeText(context, "Directory deleted!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to delete directory.", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    });
        });

        builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel());
        builder.show();
    }

//    private void showDeleteDialog(int position, String directoryId) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setTitle(R.string.delete_directory);
//        builder.setMessage(R.string.delete_directory_hint);
//
//        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
//            databaseRef
//                    .child(currentUserId)
//                    .child("directories")
//                    .child(directoryId)
//                    .removeValue()
//                    .addOnSuccessListener(aVoid -> {
//                        if (position >= 0 && position < directories.size()) {
//                            directories.remove(position);
//                            notifyItemRemoved(position);
//                            notifyItemRangeChanged(position, directories.size());
//                        }
//                        Toast.makeText(context, R.string.delete_directory, Toast.LENGTH_SHORT).show();
//                    })
//                    .addOnFailureListener(e -> {
//                        Toast.makeText(context, R.string.delete_failed, Toast.LENGTH_SHORT).show();
//                        e.printStackTrace();
//                    });
//        });
//
//        builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
//        builder.show();
//    }

    static class DirectoryViewHolder extends RecyclerView.ViewHolder {
        TextView directoryName;
        TextView notesCount;
        ImageButton editButton;
        ImageButton deleteButton;

        DirectoryViewHolder(View itemView) {
            super(itemView);
            directoryName = itemView.findViewById(R.id.directoryName);
            notesCount = itemView.findViewById(R.id.notesCount);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
