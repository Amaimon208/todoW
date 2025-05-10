package com.example.todo;

import android.graphics.Bitmap;

public class Todo {
    private String id;
    private String content;
    private String image;

    public Todo() {}

    public Todo(String id, String content) {
        this.id = id;
        this.content = content;
    }


    public Todo(String id, String content, String image) {
        this.id = id;
        this.content = content;
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}