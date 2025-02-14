package com.example.myapplication;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.widget.EditText;
import android.widget.Switch;
import java.lang.reflect.Field;

import com.example.myapplication.Model.Database;
import com.example.myapplication.Model.User;
import com.example.myapplication.View.SignupActivity;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.Task;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;

@RunWith(MockitoJUnitRunner.class)
public class SignupUnitTest {

    @InjectMocks
    private SignupActivity signupActivity;

    @Mock
    private Database mockDatabase;

    @Mock
    private Task<AuthResult> mockAuthResultTask;

    @Test
    public void testCheckInput_ValidInput() throws Exception {
        setField(signupActivity, "signupEmail", mock(EditText.class));
        setField(signupActivity, "signupPassword", mock(EditText.class));

        EditText emailMock = (EditText) getField(signupActivity, "signupEmail");
        EditText passwordMock = (EditText) getField(signupActivity, "signupPassword");

        when(emailMock.getText().toString()).thenReturn("test@example.com");
        when(passwordMock.getText().toString()).thenReturn("password123");

        Method checkInputMethod = SignupActivity.class.getDeclaredMethod("checkInput");
        checkInputMethod.setAccessible(true);
        boolean result = (boolean) checkInputMethod.invoke(signupActivity);

        assertTrue(result);
    }

    @Test
    public void testCheckInput_InvalidInput() throws Exception {
        setField(signupActivity, "signupEmail", mock(EditText.class));
        setField(signupActivity, "signupPassword", mock(EditText.class));

        EditText emailMock = (EditText) getField(signupActivity, "signupEmail");
        EditText passwordMock = (EditText) getField(signupActivity, "signupPassword");

        when(emailMock.getText().toString()).thenReturn("");
        when(passwordMock.getText().toString()).thenReturn("123");

        Method checkInputMethod = SignupActivity.class.getDeclaredMethod("checkInput");
        checkInputMethod.setAccessible(true);
        boolean result = (boolean) checkInputMethod.invoke(signupActivity);

        assertFalse(result);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    public void testCheckUserExists_UserAlreadyExists() throws Exception {
        setField(signupActivity, "database", mockDatabase);

        Database.UserExistsCallback callback = mock(Database.UserExistsCallback.class);

        doAnswer(invocation -> {
            Database.UserExistsCallback cb = invocation.getArgument(1);
            cb.onUserExistsCheckComplete(true);
            return null;
        }).when(mockDatabase).checkUserExists(anyString(), any(Database.UserExistsCallback.class));

        signupActivity.getClass().getDeclaredMethod("checkUserExists", String.class, Database.UserExistsCallback.class)
                .invoke(signupActivity, "existing@example.com", callback);

        verify(callback).onUserExistsCheckComplete(true);
    }
}
