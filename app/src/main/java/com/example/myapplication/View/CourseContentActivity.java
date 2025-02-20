package com.example.myapplication.View;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Model.Course;
import com.example.myapplication.Controller.Database;
import com.example.myapplication.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.*;
import java.io.File;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class CourseContentActivity extends AppCompatActivity {

    private static final int REQUEST_FILE_PICKER = 1;
    private static final int REQUEST_ADD_STUDENTS = 2;

    private Database database;
    private String courseId;
    private String teacherId;
    private Course course;

    private List<String> filesList;
    private List<String> studentsList; // רשימת סטודנטים

    private String courseDescription;

    private TextView existingNotesView;
    private ListView filesListView;
    private EditText newMessageInput, newNotesInput;
    private Button btnSaveNotes, btnUploadFile, btnSendMessage, btnAddStudents;
    private ArrayAdapter<String> messagesAdapter;

    private final String baseUrl = "http://10.0.2.2:5000"; // Flask server URL
    private final OkHttpClient okHttpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_content);

        courseId = getIntent().getStringExtra("courseId");
        teacherId = getIntent().getStringExtra("teacherId");
        database = new Database();

        // אתחול רכיבי ממשק
        existingNotesView = findViewById(R.id.existingNotesView);
        filesListView = findViewById(R.id.filesListView);
        newNotesInput = findViewById(R.id.newNotesInput);
        //newMessageInput = findViewById(R.id.newMessageInput);
        btnSaveNotes = findViewById(R.id.btnSaveNotes);
        btnUploadFile = findViewById(R.id.btnUploadFile);
        //btnSendMessage = findViewById(R.id.btnSendMessage);
        btnAddStudents = findViewById(R.id.btnAddStudents); // כפתור הוספת סטודנטים

        // קבלת פרטי הקורס
        database.fetchCourseInfo(teacherId, courseId, new Database.CourseInfoCallback() {
            @Override
            public void onSuccess(Course c) {
                course = c;
                setTitle("תוכן הקורס: " + course.getName());
                database.fetchFileNames(teacherId, courseId, new Database.FileNameCallback(){

                    @Override
                    public void onSuccess(List<String> file_names) {
                        filesList = file_names;
                        studentsList = course.getEnrolledStudents();
                        courseDescription = course.getDescription();
                        // עדכון תצוגת הערות כלליות
                        updateNotesView();

                        // עדכון רשימות
                        updateFilesListView();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        filesList = new ArrayList<>(); //course.getFiles();
                        studentsList = course.getEnrolledStudents();
                        courseDescription = course.getDescription();
                        // עדכון תצוגת הערות כלליות
                        updateNotesView();

                        // עדכון רשימות
                        updateFilesListView();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(CourseContentActivity.this, "Failed to fetch course: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                setTitle("תוכן הקורס: " + "Default");
                filesList = new ArrayList<>();
                studentsList = new ArrayList<>();
                courseDescription = "";
                // עדכון תצוגת הערות כלליות
                updateNotesView();

                // עדכון רשימות
                updateFilesListView();
            }
        });

        //TODO update notes

        // שמירת הערות חדשות
        btnSaveNotes.setOnClickListener(v -> {
            String newDescription = newNotesInput.getText().toString().trim();

            if (!newDescription.isEmpty()) {
                database.updateCourseDescription(teacherId, courseId, newDescription, new Database.DescUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        // Update local variable and the TextView
                        courseDescription = newDescription;
                        updateNotesView();
                        newNotesInput.setText(""); // Clear input field
                        Toast.makeText(CourseContentActivity.this, "הערות נשמרו בהצלחה", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(CourseContentActivity.this, "שגיאה בשמירת ההערות: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(CourseContentActivity.this, "אנא הזן הערות חדשות", Toast.LENGTH_SHORT).show();
            }
        });


        // העלאת קובץ חדש
        btnUploadFile.setOnClickListener(v -> openFilePicker());

        // מעבר למסך הוספת סטודנטים
        btnAddStudents.setOnClickListener(v -> {
            Intent intent = new Intent(CourseContentActivity.this, AddStudentsActivity.class);
            intent.putExtra("courseId", courseId);
            intent.putExtra("teacherId", teacherId);
            startActivityForResult(intent, REQUEST_ADD_STUDENTS);
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, REQUEST_FILE_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FILE_PICKER && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null && fileUri.getLastPathSegment() != null) {
                handleFileUpload(fileUri,course.getName());

                /*database.uploadFile(teacherId, courseId, fileUri, new Database.FileUploadCallback() {
                    @Override
                    public void onSuccess(String fileUrl) {
                        filesList.add(fileUri.getLastPathSegment());
                        updateFilesListView();
                        Toast.makeText(CourseContentActivity.this, "קובץ הועלה בהצלחה: " + fileUri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(CourseContentActivity.this, "Failed to save file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });*/


            } else {
                Toast.makeText(this, "שגיאה בהעלאת הקובץ", Toast.LENGTH_SHORT).show();
            }
        }

        // קליטת רשימת הסטודנטים ממסך AddStudentsActivity
        if (requestCode == REQUEST_ADD_STUDENTS && resultCode == RESULT_OK && data != null) {
            ArrayList<String> newStudentsList = data.getStringArrayListExtra("studentsList");
            if (newStudentsList != null && !newStudentsList.isEmpty()) {
                studentsList.addAll(newStudentsList);
                Toast.makeText(this, "Students added to " + course.getName() + "!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateNotesView() {
        existingNotesView.setText(courseDescription.isEmpty() ? "אין הערות כלליות" : courseDescription);
    }

    private void updateFilesListView() {
        ArrayAdapter<String> filesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filesList);
        filesListView.setAdapter(filesAdapter);
    }

    /*private void uploadToFlaskServer(Uri fileUri, String courseName) {
        try {
            // Get the file's input stream
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                Toast.makeText(this, "Failed to read the selected file", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a temporary file from the input stream
            File tempFile = new File(getCacheDir(), "upload_temp_file");
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }

            OkHttpClient client = new OkHttpClient();

            // Build the multipart request body
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("course_name", courseName)
                    .addFormDataPart("file", tempFile.getName(),
                            RequestBody.create(tempFile, MediaType.parse("application/octet-stream")))
                    .build();

            // Build the POST request
            Request request = new Request.Builder()
                    .url(baseUrl + "/lecturer/upload_course")
                    .post(requestBody)
                    .build();

            // Execute the request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(CourseContentActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> Toast.makeText(CourseContentActivity.this, "File uploaded successfully!", Toast.LENGTH_SHORT).show());
                    } else {
                        runOnUiThread(() -> Toast.makeText(CourseContentActivity.this, "Upload failed: " + response.message(), Toast.LENGTH_SHORT).show());
                    }
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Error uploading file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }*/

    private void handleFileUpload(Uri uri, String courseName) {
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
                        RequestBody.create(tempFile, null)) // No media type for .pdf, etc.
                .addFormDataPart("file_name", fileName)
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
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = null;
                        try {
                            responseBody = response.body().string();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String fileId = jsonResponse.getString("file_id");

                            runOnUiThread(() -> {
                                showToast("File uploaded successfully.");
                                // Store reference to the file in the course document:
                                database.addFileReferenceToCourse(teacherId, courseId, fileId, new Database.FileReferenceCallback() {
                                    @Override
                                    public void onSuccess() {
                                        runOnUiThread(() -> Toast.makeText(CourseContentActivity.this, "File reference added successfully!", Toast.LENGTH_SHORT).show());
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        runOnUiThread(() -> Toast.makeText(CourseContentActivity.this, "Failed to add file reference: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    }
                                });
                            });
                        } catch (JSONException e) {
                            runOnUiThread(() ->
                                    showToast("Error parsing response: " + e.getMessage())
                            );
                        }
                    } else {
                        showToast("Error uploading file");
                    }
                });
            }
        });
    }

    //HELPERS

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
}