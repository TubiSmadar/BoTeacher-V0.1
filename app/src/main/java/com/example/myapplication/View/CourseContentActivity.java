package com.example.myapplication.View;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Model.Course;
import com.example.myapplication.Model.Database;
import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

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
                filesList = course.getFiles();
                studentsList = course.getEnrolledStudents();
                courseDescription = course.getDescription();
                // עדכון תצוגת הערות כלליות
                updateNotesView();

                // עדכון רשימות
                updateFilesListView();
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



        // שמירת הערות חדשות
        /*btnSaveNotes.setOnClickListener(v -> {
            String newNotes = newNotesInput.getText().toString();
            if (!newNotes.isEmpty()) {
                generalNotes += newNotes + "\n";
                updateNotesView();
                newNotesInput.setText("");
                Toast.makeText(this, "הערות נשמרו בהצלחה", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "אנא הזן הערות חדשות", Toast.LENGTH_SHORT).show();
            }
        });*/

        // העלאת קובץ חדש
        btnUploadFile.setOnClickListener(v -> openFilePicker());

        //TODO send a notification to students list

        // מעבר למסך הוספת סטודנטים
        btnAddStudents.setOnClickListener(v -> {
            Intent intent = new Intent(CourseContentActivity.this, AddStudentsActivity.class);
            intent.putExtra("courseId", courseId);
            intent.putExtra("teacherId", teacherId);
            startActivityForResult(intent, REQUEST_ADD_STUDENTS);
        });
    }

    //TODO restrict to valid file types, use database storage function
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_FILE_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FILE_PICKER && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null && fileUri.getLastPathSegment() != null) {
                filesList.add(fileUri.getLastPathSegment());
                updateFilesListView();
                Toast.makeText(this, "קובץ הועלה בהצלחה: " + fileUri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
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

    //TODO retrieve files from file storage
    private void updateFilesListView() {
        ArrayAdapter<String> filesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filesList);
        filesListView.setAdapter(filesAdapter);
    }
}