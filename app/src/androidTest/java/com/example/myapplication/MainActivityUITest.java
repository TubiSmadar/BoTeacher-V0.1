package com.example.myapplication;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

@RunWith(AndroidJUnit4.class)
public class MainActivityUITest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule =
            new ActivityTestRule<>(MainActivity.class);

    /**
     * Test: Clicking the student button leads to the student screen     */
    @Test
    public void testStudentButtonNavigation() {
        // Check that the "Student" button exists
        onView(withId(R.id.btnStudent)).check(matches(isDisplayed()));

        // Clicking the "Student" button
        onView(withId(R.id.btnStudent)).perform(click());

        // Check that we have reached the student screen
        onView(withId(R.id.courseListView)).check(matches(isDisplayed()));
    }

    /**
     * Test: Clicking the lecturer button leads to the course selection screen.
     */
    @Test
    public void testLecturerButtonNavigation() {
        // Check that the "Lecturer" button exists
        onView(withId(R.id.btnLecturer)).check(matches(isDisplayed()));

        // Clicking the "Lecturer" button
        onView(withId(R.id.btnLecturer)).perform(click());

        // Check that we have reached the course list screen
        onView(withId(R.id.coursesListView)).check(matches(isDisplayed()));
    }
}
