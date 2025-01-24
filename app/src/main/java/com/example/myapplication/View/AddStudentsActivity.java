package com.example.myapplication.View;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Model.Course;
import com.example.myapplication.Model.Database;
import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

public class AddStudentsActivity extends AppCompatActivity {

    private List<String> studentsList;
    private ArrayAdapter<String> adapter;
    private EditText studentNameInput;
    private ListView studentsListView;
    private String courseId;
    private String teacherId;

    private Database database;

    private Course course;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_students);

        courseId = getIntent().getStringExtra("courseId");
        teacherId = getIntent().getStringExtra("teacherId");
        database = new Database();

        studentNameInput = findViewById(R.id.studentNameInput);
        Button addStudentButton = findViewById(R.id.btnAddStudent);
        Button finishButton = findViewById(R.id.btnFinish);
        studentsListView = findViewById(R.id.studentsListView);

        studentsList = new ArrayList<>(); // Initialize as an empty list
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, studentsList);
        studentsListView.setAdapter(adapter);

        database.fetchCourseInfo(teacherId, courseId, new Database.CourseInfoCallback() {
            @Override
            public void onSuccess(Course c) {
                AddStudentsActivity activity = AddStudentsActivity.this;
                if (activity.isFinishing() || activity.isDestroyed()) return;
                course = c;
                setTitle("Add Students to: " + course.getName());
                studentsList.clear();
                studentsList.addAll(course.getEnrolledStudents());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                AddStudentsActivity activity = AddStudentsActivity.this;
                if (activity.isFinishing() || activity.isDestroyed()) return;
                Toast.makeText(AddStudentsActivity.this, "Failed to fetch course: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });



        // הוספת סטודנט לרשימה
        addStudentButton.setOnClickListener(v -> {
            String studentId = studentNameInput.getText().toString();
            if (!studentId.isEmpty()) {
                studentsList.add(studentId);
                adapter.notifyDataSetChanged();
                database.addStudentsToCourse(studentsList, teacherId, courseId, new Database.AddStudentCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) return; // Check activity state
                        studentNameInput.setText("");
                        Toast.makeText(AddStudentsActivity.this, "Students added!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (isFinishing() || isDestroyed()) return; // Check activity state
                        Toast.makeText(AddStudentsActivity.this, "Failed to add students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            } else {
                Toast.makeText(this, "Please enter a student ID", Toast.LENGTH_SHORT).show();
            }
        });

        // לחיצה על כפתור Finish מחזירה ל-LecturerCoursesActivity
        finishButton.setOnClickListener(v -> {
            Intent intent = new Intent(AddStudentsActivity.this, LecturerCoursesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("courseId", courseId);
            intent.putExtra("teacherId", teacherId);
            startActivity(intent);
            finish();
        });
    }
}