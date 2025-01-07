package com.example.myapplication.View;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

import java.util.ArrayList;

public class AddStudentsActivity extends AppCompatActivity {

    private ArrayList<String> studentsList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private EditText studentNameInput;
    private ListView studentsListView;
    private String courseName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_students);

        courseName = getIntent().getStringExtra("courseName");
        setTitle("Add Students to: " + courseName);

        studentNameInput = findViewById(R.id.studentNameInput);
        Button addStudentButton = findViewById(R.id.btnAddStudent);
        Button finishButton = findViewById(R.id.btnFinish);
        studentsListView = findViewById(R.id.studentsListView);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, studentsList);
        studentsListView.setAdapter(adapter);

        // הוספת סטודנט לרשימה
        addStudentButton.setOnClickListener(v -> {
            String studentName = studentNameInput.getText().toString();
            if (!studentName.isEmpty()) {
                studentsList.add(studentName);
                adapter.notifyDataSetChanged();
                studentNameInput.setText("");
                Toast.makeText(this, "Student added!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enter a student ID", Toast.LENGTH_SHORT).show();
            }
        });

        // לחיצה על כפתור Finish מחזירה ל-LecturerCoursesActivity
        finishButton.setOnClickListener(v -> {
            Intent intent = new Intent(AddStudentsActivity.this, LecturerCoursesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("newCourseName", courseName);
            intent.putStringArrayListExtra("studentsList", studentsList);
            startActivity(intent);
            finish();
        });
    }
}