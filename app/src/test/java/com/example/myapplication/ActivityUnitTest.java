package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import com.example.myapplication.View.ChatActivity;
import com.example.myapplication.View.MainActivity;
import com.example.myapplication.View.StudentActivity;
import com.example.myapplication.View.LecturerActivity;


import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(
        manifest = Config.NONE
)
public class ActivityUnitTest {

    @Mock
    private ListView mockListView;

    @Mock
    private EditText mockEditText;

    @Mock
    private Uri mockUri;

    private MainActivity mainActivity;
    private StudentActivity studentActivity;
    private LecturerActivity lecturerActivity;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mainActivity = Robolectric.buildActivity(MainActivity.class).create().get();
        studentActivity = Robolectric.buildActivity(StudentActivity.class).create().get();
        lecturerActivity = Robolectric.buildActivity(LecturerActivity.class).create().get();
    }

    /**
     * Test 1: Tests the navigation from MainActivity to StudentActivity
     * Uses Robolectric to verify proper Intent creation and activity launch
     */
    @Test
    public void testMainActivityStudentNavigation() {
        // Setup
        Button studentButton = mainActivity.findViewById(R.id.btnStudent);

        // Action
        studentButton.performClick();

        // Verify
        ShadowActivity shadowActivity = shadowOf(mainActivity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        assertNotNull(startedIntent);
        assertEquals(StudentActivity.class.getName(), startedIntent.getComponent().getClassName());
    }

    /**
     * Test 2: Tests the course creation in LecturerActivity
     * Uses Mockito to mock EditText behavior
     */
    @Test
    public void testLecturerCourseCreation() {
        // Setup
        when(mockEditText.getText().toString()).thenReturn("Test Course");
        LecturerActivity spyLecturerActivity = spy(lecturerActivity);
        spyLecturerActivity.findViewById(R.id.courseNameInput);
        doReturn(mockEditText).when(spyLecturerActivity).findViewById(R.id.courseNameInput);

        Button createCourseButton = spyLecturerActivity.findViewById(R.id.btnCreateCourse);

        // Action
        createCourseButton.performClick();

        // Verify
        ShadowActivity shadowActivity = shadowOf(spyLecturerActivity);
        assertEquals(android.app.Activity.RESULT_OK, shadowActivity.getResultCode());

        Intent resultIntent = shadowActivity.getResultIntent();
        assertNotNull(resultIntent);
        assertEquals("Test Course", resultIntent.getStringExtra("newCourseName"));
    }

    /**
     * Test 3: Tests the course selection in StudentActivity
     * Tests the ListView adapter and item selection
     */
    @Test
    public void testStudentCourseSelection() {
        // Setup
        ListView courseListView = studentActivity.findViewById(R.id.courseListView);
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) courseListView.getAdapter();

        // Verify initial state
        assertNotNull(adapter);
        assertEquals(5, adapter.getCount());
        assertEquals("חשבון אינפיניטסימלי", adapter.getItem(0));

        // Action - simulate course selection
        courseListView.performItemClick(
                courseListView.getChildAt(0),
                0,
                courseListView.getAdapter().getItemId(0)
        );

        // Verify
        ShadowActivity shadowActivity = shadowOf(studentActivity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();

        assertNotNull(startedIntent);
        assertEquals(ChatActivity.class.getName(), startedIntent.getComponent().getClassName());
        assertEquals("חשבון אינפיניטסימלי", startedIntent.getStringExtra("courseName"));
    }
}