package com.example.myapplication.Model;

import java.util.List;

public class Course {
    private String id;
    private String name;
    private String description;
    private List<String> enrolledStudents;

    private List<String> files;

    // Default constructor (required for Firestore)
    public Course() {}

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getEnrolledStudents() {
        return enrolledStudents;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setEnrolledStudents(List<String> enrolledStudents) {
        this.enrolledStudents = enrolledStudents;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }
}
