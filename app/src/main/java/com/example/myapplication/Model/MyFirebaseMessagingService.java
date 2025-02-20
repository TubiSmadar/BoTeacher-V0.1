package com.example.myapplication.Model;

import android.util.Log;

import androidx.annotation.NonNull;
import com.example.myapplication.Controller.Database;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private Database database;
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "Refreshed token: " + token);

        database = new Database();

        // Get the current user ID
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId != null) {
            database.updateFcmToken(userId, token, new Database.FcmUpdateCallback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailure(Exception e) {

                }
            });
        }
    }
}
