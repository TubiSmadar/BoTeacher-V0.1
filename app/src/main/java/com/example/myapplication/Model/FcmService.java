package com.example.myapplication.Model;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.auth.oauth2.GoogleCredentials;
import okhttp3.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class FcmService {

    private static final String PROJECT_ID = "boteacher-b9652"; // Replace with your Firebase Project ID

    public static void getAccessToken(AccessTokenCallback callback) {
        new Thread(() -> {
            try {
                InputStream serviceAccount = FcmService.class.getClassLoader().getResourceAsStream("assets/service-account.json");

                if (serviceAccount == null) {
                    callback.onFailure(new FileNotFoundException("Service account file not found!"));
                    return;
                }

                GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount)
                        .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

                credentials.refreshIfExpired();
                String token = credentials.getAccessToken().getTokenValue();
                callback.onSuccess(token);
            } catch (IOException e) {
                e.printStackTrace();
                callback.onFailure(e);
            }
        }).start();
    }

    // ✅ Callback interface for async handling
    public interface AccessTokenCallback {
        void onSuccess(String token);

        void onFailure(Exception e);
    }


    public static void sendPushNotification(List<String> tokens, String title, String message) {
        getAccessToken(new AccessTokenCallback() {
            @Override
            public void onSuccess(String accessToken) {
                OkHttpClient client = new OkHttpClient();

                for (String token : tokens) {
                    String jsonBody = "{"
                            + "\"message\": {"
                            + "\"token\": \"" + token + "\","
                            + "\"notification\": {"
                            + "\"title\": \"" + title + "\","
                            + "\"body\": \"" + message + "\""
                            + "}"
                            + "}"
                            + "}";

                    RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));
                    Request request = new Request.Builder()
                            .url("https://fcm.googleapis.com/v1/projects/boteacher-b9652/messages:send")
                            .post(body)
                            .addHeader("Authorization", "Bearer " + accessToken)
                            .addHeader("Content-Type", "application/json")
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, IOException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            if (response.isSuccessful()) {
                                Log.d("FCMService", "Notification sent to: " + token);
                            } else {
                                Log.e("FCMservice", "Failed to send notification: " + response.message());
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("FCMService", "Failed to retrieve access token", e);
            }
        });
    }
}

