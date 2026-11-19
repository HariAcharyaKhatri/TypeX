package com.typex.model;

public class User {
    public int id;
    public String username;
    public String avatarPath;
    public String language = "en";
    public String skillLevel = "Beginner";
    public int dailyGoal = 30;

    @Override
    public String toString() {
        return username;
    }
}
