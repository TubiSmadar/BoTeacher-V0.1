package com.example.myapplication.Model;

import android.util.Log;

import com.example.myapplication.Controller.Database;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
private Database database = new Database();
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d("FCM", "New Token: " + token);

        // Send token to your server
        sendTokenToServer(token);


    }

    private void sendTokenToServer(String token) {
        // TODO: Implement API call to send token to your backend database
        Log.d("FCM", "Token sent to server: " + token);
    }
}
