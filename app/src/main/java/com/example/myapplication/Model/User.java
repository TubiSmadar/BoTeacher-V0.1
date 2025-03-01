package com.example.myapplication.Model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;

public class User extends FirebaseKey implements Serializable {
    private String firstname;
    private String email;
    private String lastname;
    private int account_type;

    private String myId;

    private String fcmToken;

    public User() {
        //this.account_type = 0;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String token) {
        this.fcmToken = token;
    }

    public String getMyId() {
        return myId;
    }

    public void setMyId(String setId) {
        this.myId = setId;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public int getAccount_type() {
        return account_type;
    }

    public void setAccount_type(int account_type) {
        this.account_type = account_type;
    }

    @Exclude
    public boolean isValid() {
        if (this.firstname == null || this.firstname.isEmpty()) return false;
        if (this.lastname == null || this.lastname.isEmpty()) return false;
        if (this.email == null || this.email.isEmpty()) return false;
        //if (this.password == null || this.password.isEmpty()) return false;
        if (this.myId == null || this.myId.isEmpty()) return false;
        return true;
    }

    public String getKey() {
        return key;
    }

    public void setKeyOn(String key) {
        this.key = key;
    }
}