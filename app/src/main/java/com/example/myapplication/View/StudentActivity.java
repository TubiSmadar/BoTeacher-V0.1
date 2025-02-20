package com.example.myapplication.View;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Model.Course;
import com.example.myapplication.Controller.CourseAdapter;
import com.example.myapplication.Controller.Database;
import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

import okhttp3.*;


public class StudentActivity extends AppCompatActivity {

    private ArrayList<Course> coursesList;
    private Database database;

    private CourseAdapter adapter; // Custom adapter for displaying courses

    private final String baseUrl = "http://10.0.2.2:5000"; // Flask server URL

    private final OkHttpClient okHttpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        database = new Database();

        // איתור ה-ListView
        ListView courseListView = findViewById(R.id.courseListView);
        // הגדרת מתאם עבור ה-ListView
        coursesList = new ArrayList<>();
        adapter = new CourseAdapter(this, coursesList);
        //ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, coursesList);
        courseListView.setAdapter(adapter);

        FirebaseUser fbUser = database.getCurrentUser();
        if (fbUser == null) {
            Toast.makeText(StudentActivity.this, "No user is signed in.", Toast.LENGTH_SHORT).show();
            return; // Stop further execution
        }
        String uid = fbUser.getUid();

        database.fetchStudentCourses(uid, new Database.CourseListCallback() {
            @Override
            public void onSuccess(ArrayList<Course> courses) {
                //Toast.makeText(StudentActivity.this, "Courses found.", Toast.LENGTH_SHORT).show();
                coursesList.clear();
                coursesList.addAll(courses);
                //Toast.makeText(StudentActivity.this, "Courses found: " + courses.get(0).getName(), Toast.LENGTH_SHORT).show();
                adapter.notifyDataSetChanged(); // Refresh the ListView
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(StudentActivity.this, "Failed to fetch courses: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        // האזנה ללחיצה על קורס ברשימה
        courseListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //String selectedCourse = courses[position];
                if (coursesList != null && position >= 0 && position < coursesList.size()) {
                    Course selectedCourse = coursesList.get(position);
                    //List<String> fileUrls = selectedCourse.getFiles();
                    String courseName = selectedCourse.getName();

                    /*if (fileUrls == null || fileUrls.isEmpty()) {
                        Toast.makeText(StudentActivity.this, "No files to upload for course: " + courseName, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    uploadCourse(courseName, fileUrls);*/

                    Toast.makeText(StudentActivity.this, "Selected course: " + courseName, Toast.LENGTH_SHORT).show();
                    // יצירת Intent למעבר למסך ChatActivity
                    Intent intent = new Intent(StudentActivity.this, ChatActivity.class);

                    // שליחת שם הקורס ל-ChatActivity
                    intent.putExtra("courseName", courseName);
                    intent.putExtra("courseId", selectedCourse.getId());
                    intent.putExtra("studentId", uid);

                    // מעבר ל-ChatActivity
                    startActivity(intent);
                } else {
                    Toast.makeText(StudentActivity.this, "Invalid course selection", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //TODO work
    /*private void fetchCourses(FetchCoursesCallback callback) {
        // GET request to /student/courses
        Request request = new Request.Builder()
                .url(baseUrl + "/student/courses")
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(StudentActivity.this, "Failed to fetch courses: " + e.getMessage(), Toast.LENGTH_SHORT).show();//showToast("Failed to fetch courses: " + e.getMessage());
                    callback.onCoursesFetched(new ArrayList<>()); // return empty list
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (body == null) {
                        runOnUiThread(() -> Toast.makeText(StudentActivity.this, "No response body", Toast.LENGTH_SHORT).show());//showToast("No response body"));
                        callback.onCoursesFetched(new ArrayList<>());
                        return;
                    }
                    String bodyString = body.string();
                    JSONObject json = new JSONObject(bodyString);
                    List<Pair<String, String>> courseList = new ArrayList<>();

                    Iterator<String> keys = json.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        String value = json.getString(key);
                        courseList.add(new Pair<>(key, value));
                    }

                    runOnUiThread(() -> callback.onCoursesFetched(courseList));
                } catch (Exception e) {
                    runOnUiThread(() -> showToast("Invalid server response"));
                    callback.onCoursesFetched(new ArrayList<>());
                }
            }
        });
    }

    // ------------------------------------------------------
    // Callback Interfaces
    // ------------------------------------------------------
    public interface FetchCoursesCallback {
        void onCoursesFetched(List<Pair<String, String>> courses);
    }*/

    /*public void uploadCourse(String courseName, List<String> fileUrls) {
        OkHttpClient client = new OkHttpClient();

        // Download files from URLs to temporary files
        MultipartBody.Builder multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("course_name", courseName);

        for (String fileUrl : fileUrls) {
            try {
                // Download file
                File tempFile = downloadFile(fileUrl);
                if (tempFile == null) {
                    Toast.makeText(StudentActivity.this, "Failed to download: " + fileUrl, Toast.LENGTH_SHORT).show();
                    continue;
                }

                // Add file to the multipart request
                multipartBody.addFormDataPart(
                        "file",
                        tempFile.getName(),
                        RequestBody.create(tempFile, MediaType.parse("application/octet-stream"))
                );
            } catch (Exception e) {
                Toast.makeText(StudentActivity.this, "Error processing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        // Build the request
        Request request = new Request.Builder()
                .url(baseUrl)
                .post(multipartBody.build())
                .build();

        // Make the asynchronous call
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(StudentActivity.this, "Loading course files failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(StudentActivity.this, "Loaded course files successfully", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(StudentActivity.this, "Server response unsuccessful: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private File downloadFile(String fileUrl) throws IOException {
        //todo debug
        //Toast.makeText(StudentActivity.this, "entered downloadfile", Toast.LENGTH_SHORT).show();

        URL url = new URL(fileUrl);

        //todo debug
        //Toast.makeText(StudentActivity.this, "created URL var", Toast.LENGTH_SHORT).show();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        //todo debug
        Toast.makeText(StudentActivity.this, "created connection var", Toast.LENGTH_SHORT).show();

        try {
            connection.connect();
            Toast.makeText(StudentActivity.this, "Connection successful", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(StudentActivity.this, "Connection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("FileDownload", "Connection failed: ", e);
            return null;
        }

        //todo debug
        Toast.makeText(StudentActivity.this, "connected", Toast.LENGTH_SHORT).show();

        // Check HTTP response
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            Toast.makeText(StudentActivity.this, "Failed to connect: " + responseCode, Toast.LENGTH_SHORT).show();
            Log.e("FileDownload", "Failed to connect. Response code: " + responseCode);
            return null;
        }

        // Extract file name and create a temporary file
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

        if (fileName.isEmpty()) {
            Toast.makeText(StudentActivity.this, "Invalid file name extracted from URL", Toast.LENGTH_SHORT).show();
            Log.e("FileDownload", "Invalid file name extracted from URL: " + fileUrl);
            return null;
        }

        //TODO dbugging
        Toast.makeText(StudentActivity.this, "filename: " + fileName, Toast.LENGTH_SHORT).show();

        File tempFile = File.createTempFile(fileName, null);

        try (InputStream input = connection.getInputStream();
             FileOutputStream output = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }

        connection.disconnect();
        return tempFile;
    }*/
}
