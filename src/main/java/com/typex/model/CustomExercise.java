package com.typex.model;

public class CustomExercise {
    public int id, userId;
    public String title, text, difficulty;

    @Override
    public String toString() {
        return title != null ? title : ("Exercise " + id);
    }
}
