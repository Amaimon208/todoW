package com.example.todo;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {
    private final ArrayList<Todo> todoList;
    private final Context context;
    private final DatabaseReference databaseRef;
    private final String currentUserId;
    private final String directoryId;

    public TodoAdapter(ArrayList<Todo> todoList, Context context, String directoryId, String currentUserId) {
        this.todoList = todoList;
        this.context = context;
        this.directoryId = directoryId;
        this.currentUserId = currentUserId;
        this.databaseRef = FirebaseDatabase.getInstance().getReference("users");
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_todo, parent, false);
        return new TodoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        Todo todo = todoList.get(position);
        holder.todoText.setVisibility(View.VISIBLE);  // Show the text view
        holder.todoText.setText(todo.getContent());  // Set the text content
        holder.itemView.setOnLongClickListener(v -> {
            if (todo.getImage() == null) {
                showEditDialog(position, todo);  // Show edit dialog if there's no image
            } else {
                showDeleteDialog(position, todo);  // Show delete dialog if there's an image
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    private void showEditDialog(int position, Todo todo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.edit_note);

        final EditText input = new EditText(context);
        input.setText(todo.getContent());
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            String newText = input.getText().toString().trim();
            if (!newText.isEmpty()) {
                todo.setContent(newText);
                updateTodoInFirebase(todo);  // Update the todo item in the Firebase database
                notifyItemChanged(position);  // Notify that the item at position has changed
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDeleteDialog(int position, Todo todo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_note);
        builder.setMessage(R.string.delete_note_confirmation);

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            deleteTodoFromFirebase(todo, position);  // Delete the todo item from Firebase
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateTodoInFirebase(Todo todo) {
        databaseRef.child(currentUserId)
                .child("directories")
                .child(directoryId)
                .child("todos")
                .child(todo.getId())
                .setValue(todo)  // Set the updated todo item to Firebase
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to update note", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteTodoFromFirebase(Todo todo, int position) {
        deleteFromDatabase(todo, position);  // Call the method to remove the todo from the database
    }

    private void deleteFromDatabase(Todo todo, int position) {
        databaseRef.child(currentUserId)
                .child("directories")
                .child(directoryId)
                .child("todos")
                .child(todo.getId())
                .removeValue()  // Remove the todo item from Firebase
                .addOnSuccessListener(aVoid -> {
                    todoList.remove(position);  // Remove the todo item from the list
                    notifyItemRemoved(position);  // Notify that the item has been removed
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete note", Toast.LENGTH_SHORT).show();
                });
    }

    static class TodoViewHolder extends RecyclerView.ViewHolder {
        TextView todoText;

        TodoViewHolder(View itemView) {
            super(itemView);
            todoText = itemView.findViewById(R.id.todoText);
        }
    }
}
