package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class LecturerCoursesActivity extends AppCompatActivity {

    private ArrayList<String> coursesList; // רשימת הקורסים
    private HashMap<String, ArrayList<String>> courseFilesMap; // מיפוי קורסים לקבצים
    private HashMap<String, ArrayList<String>> courseMessagesMap; // מיפוי קורסים להודעות
    private ArrayAdapter<String> adapter; // מתאם לרשימה

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_course);

        // איתור רכיבי הממשק
        ListView coursesListView = findViewById(R.id.coursesListView);
        Button addCourseButton = findViewById(R.id.addCourseButton);

        // אתחול רשימת הקורסים והמיפויים
        coursesList = new ArrayList<>();
        courseFilesMap = new HashMap<>();
        courseMessagesMap = new HashMap<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, coursesList);
        coursesListView.setAdapter(adapter);

        // לחיצה על כפתור יצירת קורס חדש
        addCourseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LecturerCoursesActivity.this, LecturerActivity.class);
                startActivityForResult(intent, 1); // בקשת תוצאה מ-LecturerActivity
            }
        });

        // האזנה ללחיצה על פריט ברשימה
        coursesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedCourse = coursesList.get(position);

                // מעבר למסך תוכן הקורס
                Intent intent = new Intent(LecturerCoursesActivity.this, CourseContentActivity.class);
                intent.putExtra("courseName", selectedCourse);

                // העברת נתוני הקבצים וההודעות
                intent.putStringArrayListExtra("filesList", courseFilesMap.getOrDefault(selectedCourse, new ArrayList<>()));
                intent.putStringArrayListExtra("messagesList", courseMessagesMap.getOrDefault(selectedCourse, new ArrayList<>()));

                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            String newCourse = data.getStringExtra("newCourseName");
            if (newCourse != null && !newCourse.isEmpty()) {
                coursesList.add(newCourse); // הוספת הקורס לרשימה
                courseFilesMap.put(newCourse, new ArrayList<>()); // אתחול קבצים לקורס החדש
                courseMessagesMap.put(newCourse, new ArrayList<>()); // אתחול הודעות לקורס החדש
                adapter.notifyDataSetChanged(); // עדכון תצוגת הרשימה
                Toast.makeText(this, "Course '" + newCourse + "' added!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
