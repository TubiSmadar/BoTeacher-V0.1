package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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

    private ArrayList<String> filesList;
    private ArrayList<String> messagesList;
    private String generalNotes;

    private ListView filesListView, messagesListView;
    private EditText newMessageInput, newNotesInput;
    private Button uploadFileButton, sendMessageButton, saveNotesButton;
    private TextView existingNotesView, filesTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_content);

        // קבלת נתונים מתוך ה-Intent
        String courseName = getIntent().getStringExtra("courseName");
        setTitle("תוכן הקורס: " + courseName);

        filesList = getIntent().getStringArrayListExtra("filesList");
        if (filesList == null) filesList = new ArrayList<>();

        messagesList = getIntent().getStringArrayListExtra("messagesList");
        if (messagesList == null) messagesList = new ArrayList<>();

        generalNotes = getIntent().getStringExtra("generalNotes");
        if (generalNotes == null) generalNotes = "";

        // איתור רכיבי הממשק
        filesListView = findViewById(R.id.filesListView);
        messagesListView = findViewById(R.id.messagesListView);
        newMessageInput = findViewById(R.id.newMessageInput);
        newNotesInput = findViewById(R.id.newNotesInput);
        uploadFileButton = findViewById(R.id.btnUploadFile);
        sendMessageButton = findViewById(R.id.btnSendMessage);
        saveNotesButton = findViewById(R.id.btnSaveNotes);
        existingNotesView = findViewById(R.id.existingNotesView);
        filesTitle = findViewById(R.id.filesTitle);

        // הצגת הערות כלליות קיימות
        existingNotesView.setText(generalNotes);

        // מאזין ללחיצה על "קבצים שהועלו"
        filesTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filesList.isEmpty()) {
                    Toast.makeText(CourseContentActivity.this, "אין קבצים שהועלו", Toast.LENGTH_SHORT).show();
                } else {
                    updateFilesListView();
                    Toast.makeText(CourseContentActivity.this, "מציג את הקבצים שהועלו", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // העלאת קובץ
        uploadFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });

        // שליחת הודעה חדשה
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newMessage = newMessageInput.getText().toString();
                if (!newMessage.isEmpty()) {
                    messagesList.add(newMessage);
                    updateMessagesListView();
                    newMessageInput.setText("");
                } else {
                    Toast.makeText(CourseContentActivity.this, "אנא כתוב הודעה", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // שמירת הערות כלליות חדשות
        saveNotesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newNotes = newNotesInput.getText().toString();
                if (!newNotes.isEmpty()) {
                    generalNotes += "\n" + newNotes;
                    existingNotesView.setText(generalNotes);
                    newNotesInput.setText("");
                    Toast.makeText(CourseContentActivity.this, "הערות נשמרו בהצלחה", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CourseContentActivity.this, "אנא כתוב הערות", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // הצגת רשימות ראשוניות
        updateFilesListView();
        updateMessagesListView();
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
    }

    private void updateFilesListView() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filesList);
        filesListView.setAdapter(adapter);
    }

    private void updateMessagesListView() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messagesList);
        messagesListView.setAdapter(adapter);
    }
}
