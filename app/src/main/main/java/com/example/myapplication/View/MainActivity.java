package com.example.myapplication.View;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button studentButton = findViewById(R.id.btnStudent);
        Button lecturerButton = findViewById(R.id.btnLecturer);

        // Navigate to Student Activity
        studentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(MainActivity.this, StudentActivity.class);
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // Navigate to Lecturer Activity
        lecturerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(MainActivity.this, LecturerActivity.class);
                Intent intent = new Intent(MainActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }
}

/*package com.example.myapplication.View;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

import okhttp3.*;
import okhttp3.MediaType;

import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final String baseUrl = "http://10.0.2.2:5000"; // Replace with your Flask server URL

    private ActivityResultLauncher<String> filePickerLauncher;

    // Used to track the current 'mode' (null means not chosen yet)
    private Boolean isLecturer = null;

    // UI Elements
    private LinearLayout roleSelectionLayout;
    private LinearLayout lecturerLayout;
    private LinearLayout studentLayout;

    private TextView coursesTextView;
    private EditText messageEditText;
    private TextView responseTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the merged layout
        setContentView(R.layout.activity_main);

        // Initialize views
        roleSelectionLayout = findViewById(R.id.roleSelectionLayout);
        lecturerLayout = findViewById(R.id.lecturerLayout);
        studentLayout = findViewById(R.id.studentLayout);

        coursesTextView = findViewById(R.id.coursesTextView);
        messageEditText = findViewById(R.id.messageEditText);
        responseTextView = findViewById(R.id.responseTextView);

        // Set up buttons in role selection layout
        Button lecturerBtn = findViewById(R.id.lecturerBtn);
        Button studentBtn = findViewById(R.id.studentBtn);

        lecturerBtn.setOnClickListener(v -> {
            isLecturer = true;
            showLecturerScreen();
        });

        studentBtn.setOnClickListener(v -> {
            isLecturer = false;
            showStudentScreen();
            // Immediately fetch courses for student
            fetchCourses(this::displayCourses);
        });

        // File picker launcher
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleFileUpload(uri);
                    }
                }
        );

        // Show the role selection screen initially
        showRoleSelectionScreen();
    }

    // ------------------------------------------------------
    // UI Navigation
    // ------------------------------------------------------

    private void showRoleSelectionScreen() {
        // Toggle visibility
        roleSelectionLayout.setVisibility(View.VISIBLE);
        lecturerLayout.setVisibility(View.GONE);
        studentLayout.setVisibility(View.GONE);
    }

    private void showLecturerScreen() {
        // Toggle visibility
        roleSelectionLayout.setVisibility(View.GONE);
        lecturerLayout.setVisibility(View.VISIBLE);
        studentLayout.setVisibility(View.GONE);

        // Set up buttons in lecturer layout
        Button uploadFileBtn = findViewById(R.id.uploadFileBtn);
        Button backBtn = findViewById(R.id.backToRoleBtn);

        uploadFileBtn.setOnClickListener(v -> filePickerLauncher.launch("*/ /* *"));
        backBtn.setOnClickListener(v -> showRoleSelectionScreen());
    }


    // ------------------------------------------------------
    // Networking Methods
    // ------------------------------------------------------

    private void handleFileUpload(Uri uri) {
        String fileName = getFileName(uri);
        File tempFile = new File(getCacheDir(), fileName);

        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                saveInputStreamToFile(inputStream, tempFile);
            }
        } catch (IOException e) {
            showToast("Failed to read file: " + e.getMessage());
            return;
        }

        // Build multipart request body
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName,
                        RequestBody.create(tempFile, null))
                .addFormDataPart("course_name", fileName)
                .build();

        // POST request to /lecturer/upload_course
        Request request = new Request.Builder()
                .url(baseUrl + "/lecturer/upload_course")
                .post(requestBody)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        showToast("File upload failed: " + e.getMessage())
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        showToast("File uploaded successfully");
                    } else {
                        showToast("Error uploading file");
                    }
                });
            }
        });
    }

    private void fetchCourses(FetchCoursesCallback callback) {
        // Emulate a successful fetch of courses with hardcoded data
        List<Pair<String, String>> hardcodedCourses = new ArrayList<>();
        hardcodedCourses.add(new Pair<>("course101", "Introduction to Programming"));
        hardcodedCourses.add(new Pair<>("course102", "Data Structures"));
        hardcodedCourses.add(new Pair<>("course103", "Machine Learning"));

        // Call the callback immediately with the hardcoded courses
        runOnUiThread(() -> callback.onCoursesFetched(hardcodedCourses));
    }

    // ------------------------------------------------------
    // Helpers
    // ------------------------------------------------------

    private String getFileName(Uri uri) {
        String name = "uploaded_file";
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            name = cursor.getString(nameIndex);
            cursor.close();
        }
        return name;
    }

    private void saveInputStreamToFile(InputStream inputStream, File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------
    // Simple Pair class for Java (Kotlin has built-in Pair)
    // ------------------------------------------------------
    public static class Pair<F, S> {
        public final F first;
        public final S second;
        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }
    }

    // ------------------------------------------------------
    // Callback Interfaces
    // ------------------------------------------------------
    public interface FetchCoursesCallback {
        void onCoursesFetched(List<Pair<String, String>> courses);
    }

    public interface SendMessageCallback {
        void onResponse(String response);
    }
} */

