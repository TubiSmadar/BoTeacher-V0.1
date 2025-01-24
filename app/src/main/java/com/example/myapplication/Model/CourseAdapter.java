package com.example.myapplication.Model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.R;

import java.util.List;

public class CourseAdapter extends ArrayAdapter<Course> {

    private final Context context;
    private final List<Course> courses;

    public CourseAdapter(Context context, List<Course> courses) {
        super(context, R.layout.course_list_item, courses);
        this.context = context;
        this.courses = courses;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.course_list_item, parent, false);
        }

        Course course = courses.get(position);

        TextView courseName = convertView.findViewById(R.id.courseNameTextView);
        TextView courseDescription = convertView.findViewById(R.id.courseDescriptionTextView);

        courseName.setText(course.getName());
        courseDescription.setText(course.getDescription());

        return convertView;
    }
}
