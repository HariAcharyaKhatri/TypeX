package com.typex.model;

public class Lesson {
    public int id, categoryId, level, orderIndex;
    public String title, objective, instructions, practiceText;
    public double minWpm, minAccuracy;

    @Override
    public String toString() {
        return title != null ? title : ("Lesson " + id);
    }
}
