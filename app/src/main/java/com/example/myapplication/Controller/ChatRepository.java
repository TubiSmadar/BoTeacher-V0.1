package com.example.myapplication.Controller;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ChatRepository {

    private final OkHttpClient okHttpClient;
    private final String baseUrl;

    /**
     * Callback interface: returns a string that the UI can display.
     */
    public interface SendMessageCallback {
        void onResponse(String result);
    }

    public ChatRepository(OkHttpClient client, String baseUrl) {
        this.okHttpClient = client;
        this.baseUrl = baseUrl;
    }

    /**
     * Sends a chat message using the baseUrl + "/student/chat" endpoint.
     */
    public void sendMessage(String studentId, String courseId, String message, SendMessageCallback callback) {
        // Create JSON
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("student_id", studentId);
            jsonBody.put("course_id", courseId);
            jsonBody.put("message", message);
        } catch (Exception e) {
            // If JSON creation fails for any reason
            callback.onResponse("Failed to create JSON body");
            return;
        }

        RequestBody requestBody = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json")
        );

        // Build the HTTP request
        Request request = new Request.Builder()
                .url(baseUrl + "/student/chat")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build();

        // Enqueue an async call
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onResponse("Failed to send message: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (body == null) {
                        callback.onResponse("Server returned empty response");
                        return;
                    }
                    String responseString = body.string();
                    JSONObject jsonResponse = new JSONObject(responseString);
                    // if "response" key is missing, fallback to "No response"
                    String reply = jsonResponse.optString("response", "No response");

                    callback.onResponse(reply);
                } catch (Exception e) {
                    callback.onResponse("Failed to parse response: " + e.getMessage());
                }
            }
        });
    }
}
