package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class LecturerCoursesActivity extends AppCompatActivity {

    private ArrayList<String> coursesList;
    private HashMap<String, ArrayList<String>> courseStudentsMap;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_course);

        ListView coursesListView = findViewById(R.id.coursesListView);
        Button addCourseButton = findViewById(R.id.addCourseButton);

        coursesList = new ArrayList<>();
        courseStudentsMap = new HashMap<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, coursesList);
        coursesListView.setAdapter(adapter);

        // הוספת קורס חדש
        addCourseButton.setOnClickListener(v -> {
            Intent intent = new Intent(LecturerCoursesActivity.this, LecturerActivity.class);
            startActivityForResult(intent, 1);
        });

        // מעבר למסך תוכן קורס כאשר לוחצים על קורס
        coursesListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCourse = coursesList.get(position);
            Intent intent = new Intent(LecturerCoursesActivity.this, CourseContentActivity.class);
            intent.putExtra("courseName", selectedCourse);
            intent.putStringArrayListExtra("studentsList", courseStudentsMap.getOrDefault(selectedCourse, new ArrayList<>()));
            startActivity(intent);
        });

        // קבלת נתונים ממסך AddStudentsActivity
        handleIncomingIntent(getIntent());
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null) {
            String newCourse = intent.getStringExtra("newCourseName");
            ArrayList<String> studentsList = intent.getStringArrayListExtra("studentsList");

            if (newCourse != null && !newCourse.isEmpty()) {
                coursesList.add(newCourse);
                courseStudentsMap.put(newCourse, studentsList != null ? studentsList : new ArrayList<>());
                adapter.notifyDataSetChanged();
                Toast.makeText(this, "Course '" + newCourse + "' added!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            handleIncomingIntent(data);
        }
    }
}
