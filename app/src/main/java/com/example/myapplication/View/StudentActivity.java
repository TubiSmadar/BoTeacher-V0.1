package com.example.myapplication.View;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.Model.Course;
import com.example.myapplication.Model.CourseAdapter;
import com.example.myapplication.Model.Database;
import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class StudentActivity extends AppCompatActivity {

    private ArrayList<Course> coursesList;
    private Database database;

    private CourseAdapter adapter; // Custom adapter for displaying courses

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
                    Toast.makeText(StudentActivity.this, "Selected course: " + selectedCourse.getName(), Toast.LENGTH_SHORT).show();
                    // יצירת Intent למעבר למסך ChatActivity
                    Intent intent = new Intent(StudentActivity.this, ChatActivity.class);

                    // שליחת שם הקורס ל-ChatActivity
                    intent.putExtra("courseName", selectedCourse.getName());

                    // מעבר ל-ChatActivity
                    startActivity(intent);
                } else {
                    Toast.makeText(StudentActivity.this, "Invalid course selection", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
