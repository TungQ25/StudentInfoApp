package com.example.studentinfoapp;

public class Post {
    private final int userId;
    private final int id;
    private final String title;
    private final String body;

    public Post(int userId, int id, String title, String body) {
        this.userId = userId;
        this.id = id;
        this.title = title != null ? title : "";
        this.body = body != null ? body : "";
    }

    public int getUserId() {
        return userId;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }
}
