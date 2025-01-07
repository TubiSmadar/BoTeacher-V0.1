package com.example.myapplication;

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

import java.util.ArrayList;

public class CourseContentActivity extends AppCompatActivity {

    private static final int REQUEST_FILE_PICKER = 1;
    private static final int REQUEST_ADD_STUDENTS = 2;

    private DatabaseHelper databaseHelper;
    private String courseName;

    private ArrayList<String> filesList = new ArrayList<>();
    private ArrayList<String> messagesList = new ArrayList<>();
    private ArrayList<String> studentsList = new ArrayList<>(); // רשימת סטודנטים

    private String generalNotes = "";

    private TextView existingNotesView;
    private ListView filesListView, messagesListView;
    private EditText newNotesInput, newMessageInput;
    private Button btnSaveNotes, btnUploadFile, btnSendMessage, btnAddStudents;
    private ArrayAdapter<String> messagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_content);

        databaseHelper = new DatabaseHelper(this);

        // קבלת שם הקורס
        courseName = getIntent().getStringExtra("courseName");
        setTitle("תוכן הקורס: " + courseName);

        // אתחול רכיבי ממשק
        existingNotesView = findViewById(R.id.existingNotesView);
        filesListView = findViewById(R.id.filesListView);
        messagesListView = findViewById(R.id.messagesListView);
        newNotesInput = findViewById(R.id.newNotesInput);
        newMessageInput = findViewById(R.id.newMessageInput);
        btnSaveNotes = findViewById(R.id.btnSaveNotes);
        btnUploadFile = findViewById(R.id.btnUploadFile);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnAddStudents = findViewById(R.id.btnAddStudents); // כפתור הוספת סטודנטים

        // טעינת הודעות קודמות ממסד הנתונים
        messagesList = databaseHelper.getMessages(courseName);
        messagesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messagesList);
        messagesListView.setAdapter(messagesAdapter);

        // עדכון תצוגת הערות כלליות
        updateNotesView();

        // עדכון רשימות
        updateFilesListView();

        // שמירת הערות חדשות
        btnSaveNotes.setOnClickListener(v -> {
            String newNotes = newNotesInput.getText().toString();
            if (!newNotes.isEmpty()) {
                generalNotes += newNotes + "\n";
                updateNotesView();
                newNotesInput.setText("");
                Toast.makeText(this, "הערות נשמרו בהצלחה", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "אנא הזן הערות חדשות", Toast.LENGTH_SHORT).show();
            }
        });

        // העלאת קובץ חדש
        btnUploadFile.setOnClickListener(v -> openFilePicker());

        // שליחת הודעה חדשה ושמירה במסד הנתונים
        btnSendMessage.setOnClickListener(v -> {
            String newMessage = newMessageInput.getText().toString();
            if (!newMessage.isEmpty()) {
                databaseHelper.insertMessage(courseName, newMessage);
                messagesList.add(newMessage);
                messagesAdapter.notifyDataSetChanged();
                newMessageInput.setText("");
                Toast.makeText(this, "הודעה נשמרה בהצלחה", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "אנא הזן הודעה", Toast.LENGTH_SHORT).show();
            }
        });

        // מעבר למסך הוספת סטודנטים
        btnAddStudents.setOnClickListener(v -> {
            Intent intent = new Intent(CourseContentActivity.this, AddStudentsActivity.class);
            intent.putExtra("courseName", courseName);
            startActivityForResult(intent, REQUEST_ADD_STUDENTS);
        });
    }

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
            if (fileUri != null) {
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
                Toast.makeText(this, "Students added to " + courseName + "!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateNotesView() {
        existingNotesView.setText(generalNotes.isEmpty() ? "אין הערות כלליות" : generalNotes);
    }

    private void updateFilesListView() {
        ArrayAdapter<String> filesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filesList);
        filesListView.setAdapter(filesAdapter);
    }
}
