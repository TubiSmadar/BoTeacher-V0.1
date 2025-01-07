package com.example.myapplication.Model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;

public class FirebaseKey implements Serializable {
    protected String key;

    public FirebaseKey() {
    }

    @Exclude
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

}