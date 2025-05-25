package com.example.todo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.todo.activities.AddTodosActivity;
import com.example.todo.activities.DetailsActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {
    private final ArrayList<Todo> todoList;
    private final Context context;
    private DatabaseReference databaseRef;
    private final String currentUserId;
    private final String directoryId;

    public TodoAdapter(ArrayList<Todo> todoList, Context context, String directoryId, String currentUserId) {
        this.todoList = todoList;
        this.context = context;
        this.directoryId = directoryId;
        this.currentUserId = currentUserId;
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
        holder.todoText.setVisibility(View.VISIBLE);
        holder.todoText.setText(todo.getContent());

        // Regular click opens the DetailsActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailsActivity.class);
            intent.putExtra("todoId", todo.getId());
            intent.putExtra("directoryId", directoryId); // assuming you have this field
            context.startActivity(intent);
        });

        // Long click still does edit/delete logic
        holder.itemView.setOnLongClickListener(v -> {
            if (todo.getImage() == null) {
                goToEditActivity(holder.getAdapterPosition(), todo);
            } else {
                showDeleteDialog(todo, holder);
            }
            return true;
        });

        databaseRef = FirebaseDatabase
                .getInstance("https://to-do-plus-plus-3bb3e-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("users");

        holder.editButton.setOnClickListener(v -> goToEditActivity(todo, position));
        holder.deleteButton.setOnClickListener(v -> showDeleteDialog(todo, holder));
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    private void goToEditActivity(int position, Todo todo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.edit_note);

        final EditText input = new EditText(context);
        input.setText(todo.getContent());
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            String newText = input.getText().toString().trim();
            if (!newText.isEmpty()) {
                todo.setContent(newText);
                updateTodoInFirebase(todo);
                notifyItemChanged(position);
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteDialog(Todo todo, RecyclerView.ViewHolder holder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_note);
        builder.setMessage(R.string.delete_note_confirmation);

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                deleteFromDatabase(todo, holder);
            }
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

    //delete with fixes
    private void deleteFromDatabase(Todo todo, RecyclerView.ViewHolder holder) {
        int position = holder.getAdapterPosition();

        databaseRef.child(currentUserId)
                .child("directories")
                .child(directoryId)
                .child("todos")
                .child(todo.getId())
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    // super defensive check
                    if (position != RecyclerView.NO_POSITION && position < todoList.size()) {
                        if (todoList.get(position).getId().equals(todo.getId())) {
                            todoList.remove(position);
                            notifyItemRemoved(position);
                        } else {
                            for (int i = 0; i < todoList.size(); i++) {
                                if (todoList.get(i).getId().equals(todo.getId())) {
                                    todoList.remove(i);
                                    notifyItemRemoved(i);
                                    break;
                                }
                            }
                        }
                    } else {
                        for (int i = 0; i < todoList.size(); i++) {
                            if (todoList.get(i).getId().equals(todo.getId())) {
                                todoList.remove(i);
                                notifyItemRemoved(i);
                                break;
                            }
                        }
                    }

                    Toast.makeText(context, "Todo deleted successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete note", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }



    private void goToEditActivity(Todo todo, int position) {
        Intent intent = new Intent(context, AddTodosActivity.class);
        intent.putExtra("directoryId", directoryId);
        intent.putExtra("userId", currentUserId);
        intent.putExtra("todoId", todo.getId());
        context.startActivity(intent);
    }
        static class TodoViewHolder extends RecyclerView.ViewHolder {
            TextView todoText;
            ImageButton editButton;
            ImageButton deleteButton;

            TodoViewHolder(View itemView) {
                super(itemView);
                todoText = itemView.findViewById(R.id.todoText);
                editButton = itemView.findViewById(R.id.editButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }
    }

