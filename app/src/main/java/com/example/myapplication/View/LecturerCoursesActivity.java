package com.example.myapplication.View;

import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Model.Course;
import com.example.myapplication.Model.CourseAdapter;
import com.example.myapplication.Model.Database;
import com.example.myapplication.Model.User;
import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;

public class LecturerCoursesActivity extends AppCompatActivity {

    private ArrayList<Course> coursesList;

    //private HashMap<String, ArrayList<String>> courseStudentsMap;
    private CourseAdapter adapter; // Custom adapter for displaying courses

    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_course);

        database = new Database();

        ListView coursesListView = findViewById(R.id.coursesListView);
        Button addCourseButton = findViewById(R.id.addCourseButton);

        coursesList = new ArrayList<>();
        adapter = new CourseAdapter(this, coursesList);
        coursesListView.setAdapter(adapter);

        FirebaseUser fbUser = database.getCurrentUser();
        if (fbUser == null) {
            Toast.makeText(LecturerCoursesActivity.this, "No user is signed in.", Toast.LENGTH_SHORT).show();
            return; // Stop further execution
        }
        String uid = fbUser.getUid();
        fetchCourses(uid);

        // הוספת קורס חדש
        addCourseButton.setOnClickListener(v -> {
            Intent intent = new Intent(LecturerCoursesActivity.this, LecturerActivity.class);
            startActivityForResult(intent, 1);
        });

        // מעבר למסך תוכן קורס כאשר לוחצים על קורס
        coursesListView.setOnItemClickListener((parent, view, position, id) -> {
            if (coursesList != null && position >= 0 && position < coursesList.size()) {
                Course selectedCourse = coursesList.get(position);
                Toast.makeText(LecturerCoursesActivity.this, "Selected course: " + selectedCourse.getName(), Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(LecturerCoursesActivity.this, CourseContentActivity.class);
                intent.putExtra("courseId", selectedCourse.getId());
                intent.putExtra("teacherId", uid);
                startActivity(intent);
            } else {
                Toast.makeText(LecturerCoursesActivity.this, "Invalid course selection", Toast.LENGTH_SHORT).show();
            }
        });

        // קבלת נתונים ממסך AddStudentsActivity
        //handleIncomingIntent(getIntent());
    }

    private void fetchCourses(String uid) {
        database.fetchCourseList(uid, new Database.CourseListCallback() {
            @Override
            public void onSuccess(ArrayList<Course> courses) {
                if (courses.isEmpty()) {
                    Toast.makeText(LecturerCoursesActivity.this, "No courses found.", Toast.LENGTH_SHORT).show();
                }
                coursesList.clear();
                coursesList.addAll(courses);
                adapter.notifyDataSetChanged(); // Refresh the ListView
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(LecturerCoursesActivity.this, "Failed to fetch courses: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

//    private void handleIncomingIntent(Intent intent) {
//        /*if (intent != null) {
//            String newCourse = intent.getStringExtra("newCourseName");
//            ArrayList<String> studentsList = intent.getStringArrayListExtra("studentsList");
//
//            if (newCourse != null && !newCourse.isEmpty()) {
//                coursesList.add(newCourse);
//                courseStudentsMap.put(newCourse, studentsList != null ? studentsList : new ArrayList<>());
//                adapter.notifyDataSetChanged();
//                Toast.makeText(this, "Course '" + newCourse + "' added!", Toast.LENGTH_SHORT).show();
//            }
//        }*/
//        String newCourseName = intent.getStringExtra("newCourseName");
//        if (newCourseName != null && !newCourseName.isEmpty()) {
//            FirebaseUser fbUser = database.getCurrentUser();
//            if (fbUser == null) {
//                Toast.makeText(LecturerCoursesActivity.this, "No user is signed in.", Toast.LENGTH_SHORT).show();
//            }
//            String uid = fbUser.getUid();
//            fetchCourses(uid); // Reload courses after adding a new one
//        }
//    }
//
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
//            handleIncomingIntent(data);
//        }
//    }
}