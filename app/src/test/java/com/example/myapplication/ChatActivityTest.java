package com.example.myapplication;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.example.myapplication.View.ChatActivity;
import com.example.myapplication.R;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.robolectric.annotation.Config;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowDialog;

import java.lang.reflect.Field;

import okhttp3.OkHttpClient;
@Config(
        manifest = Config.NONE,
        sdk=34,
        application = ChatActivityTest.TestApp.class

)
@RunWith(RobolectricTestRunner.class)
public class ChatActivityTest {
    public static class TestApp extends android.app.Application {
        @Override
        public void onCreate() {
            // Optionally do something before
            setTheme(R.style.Theme_MyApplication); // Force a theme, if needed

            // Call the real ChatActivity's onCreate
            super.onCreate();

            // Optionally do something after
        }
    }

    private ActivityController<ChatActivity> controller;
    private ChatActivity activity;

    // UI elements
    private EditText messageInput;
    private TextView chatHistoryTextView;
    private Button sendButton;
    private ImageView uploadImageButton;

    // MockWebServer to simulate HTTP responses
    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws Exception {
        // Start a mock web server
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // 1) Launch with the default theme
        controller =
                Robolectric.buildActivity(ChatActivity.class).setup();
        activity = controller.get();


        // Now we override the baseUrl field to point to our mockWebServer
        setPrivateField(activity, "baseUrl", mockWebServer.url("/").toString());

        // Also override the okHttpClient if needed
        OkHttpClient mockedClient = new OkHttpClient();
        setPrivateField(activity, "okHttpClient", mockedClient);

        // Re-fetch UI elements (now that we've replaced fields)
        messageInput = activity.findViewById(R.id.messageInput);
        chatHistoryTextView = activity.findViewById(R.id.chatHistoryTextView);
        sendButton = activity.findViewById(R.id.sendButton);
        uploadImageButton = activity.findViewById(R.id.uploadImageButton);
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    /**
     * Utility method to set a private field via reflection.
     */
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // ------------------------------------------------
    // 1) Test sending empty message
    // ------------------------------------------------
    @Test
    public void testSendMessage_emptyMessageShowsToast() {
        // Given
        messageInput.setText("");

        // When
        sendButton.performClick();

        // Then
        // We expect a Toast "אנא כתוב הודעה" and no request to the server
        assertEquals("",
                chatHistoryTextView.getText().toString()); // no updates to chatHistory

        // We can optionally check the latest Toast with Robolectric:
        // String latestToast = ShadowToast.getTextOfLatestToast();
        // assertEquals("אנא כתוב הודעה", latestToast);
    }

    // ------------------------------------------------
    // 2) Test sending valid message with successful response
    // ------------------------------------------------
    @Test
    public void testSendMessage_success() throws Exception {
        // Given
        messageInput.setText("Hello from test!");

        // Mock the server response
        String mockResponseBody = "{ \"response\": \"Hello from server\" }";
        mockWebServer.enqueue(new MockResponse().setBody(mockResponseBody).setResponseCode(200));

        // When
        sendButton.performClick();

        // Let Robolectric process background tasks
        // (In Robolectric 4.x, it's often enough to do nothing,
        //  but you can call ShadowLooper.runUiThreadTasks if needed)

        // Then
        // The chatHistory should be updated with "אתה: Hello from test!" and "בוט: Hello from server"
        String history = chatHistoryTextView.getText().toString();
        assertTrue(history.contains("אתה: Hello from test!"));
        assertTrue(history.contains("בוט: Hello from server"));
    }

    // ------------------------------------------------
    // 3) Test sending valid message but server fails
    // ------------------------------------------------
    @Test
    public void testSendMessage_failure() throws Exception {
        // Given
        messageInput.setText("This will fail");
        // Enqueue an error response
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Error"));

        // When
        sendButton.performClick();

        // Then
        String history = chatHistoryTextView.getText().toString();
        // We expect "Failed to send message: ..." or "Server returned empty response"
        // because the code tries to parse JSON, but with a 500 it might cause an exception
        // or read an empty body. Let's see:

        // Because your code does attempt to parse the body, you might get a parse error
        // or a catch block => "Failed to parse response: ..."
        // So let's check a simpler approach:
        assertTrue(history.contains("בוט: Failed to parse response")
                || history.contains("בוט: Server returned empty response")
                || history.contains("בוט: Failed to send message"));
    }

    // ------------------------------------------------
    // 4) Test camera intent
    // ------------------------------------------------
    @Test
    public void testChooseCameraInDialog_opensCameraIntent() {
        // 1) Click the "uploadImageButton" to show the AlertDialog
        uploadImageButton.performClick();

        // 2) Grab the latest Dialog displayed by Robolectric
        Dialog latestDialog = ShadowDialog.getLatestDialog();
        assertNotNull("The image picker dialog should be shown", latestDialog);

        // The dialog should be an AlertDialog
        AlertDialog alertDialog = (AlertDialog) latestDialog;
        // 3) The AlertDialog has a ListView with items: {"צלם תמונה", "בחר תמונה מהגלריה"}
        ListView listView = alertDialog.getListView();
        assertNotNull("AlertDialog should have a ListView", listView);

        // 4) Simulate clicking on index 0 => "צלם תמונה"
        listView.performItemClick(
                listView.getAdapter().getView(0, null, null),
                0,
                listView.getAdapter().getItemId(0)
        );

        // This should cause your ChatActivity to call `openCamera()` internally
        // 5) Check that an ACTION_IMAGE_CAPTURE Intent was started
        ShadowActivity shadowActivity = shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();
        assertNotNull("Camera intent should not be null", startedIntent);

        // Use MediaStore.ACTION_IMAGE_CAPTURE (correct constant) to compare
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, startedIntent.getAction());
    }


    // ------------------------------------------------
    // 5) Test gallery intent
    // ------------------------------------------------
    @Test
    public void testChooseGalleryInDialog_opensGalleryIntent() {
        // 1) Show the dialog
        uploadImageButton.performClick();

        // 2) Fetch the AlertDialog
        Dialog latestDialog = ShadowDialog.getLatestDialog();
        assertNotNull(latestDialog);

        AlertDialog alertDialog = (AlertDialog) latestDialog;
        ListView listView = alertDialog.getListView();
        assertNotNull(listView);

        // 3) Simulate click on item 1 => "בחר תמונה מהגלריה"
        listView.performItemClick(
                listView.getAdapter().getView(1, null, null),
                1,
                listView.getAdapter().getItemId(1)
        );

        // 4) Verify the next started Activity
        ShadowActivity shadowActivity = shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();
        assertNotNull(startedIntent);

        // 5) Check it's ACTION_PICK
        assertEquals(Intent.ACTION_PICK, startedIntent.getAction());
    }

}