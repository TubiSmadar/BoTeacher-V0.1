package com.example.myapplication.View;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class StudentActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        // רשימת הקורסים
        String[] courses = {
                "חשבון אינפיניטסימלי",
                "מבוא לחישוב",
                "לוגיקה ותורת הקבוצות",
                "לינארית 1",
                "מערכות ספרתיות"
        };

        // איתור ה-ListView
        ListView courseListView = findViewById(R.id.courseListView);

        // הגדרת מתאם עבור ה-ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, courses);
        courseListView.setAdapter(adapter);

        // האזנה ללחיצה על קורס ברשימה
        courseListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedCourse = courses[position];

                // יצירת Intent למעבר למסך ChatActivity
                Intent intent = new Intent(StudentActivity.this, ChatActivity.class);

                // שליחת שם הקורס ל-ChatActivity
                intent.putExtra("courseName", selectedCourse);

                // מעבר ל-ChatActivity
                startActivity(intent);
            }
        });
    }
}
