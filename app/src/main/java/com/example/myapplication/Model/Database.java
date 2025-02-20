package com.example.myapplication.Model;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.Controller.AuthCallBack;
import com.example.myapplication.Controller.UserCallBack;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Database {

    public static final String USERS_TABLE = "Users";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AuthCallBack authCallBack;
    private UserCallBack userCallBack;

    public Database() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void logout() {
        this.mAuth.signOut();
    }

    public void setAuthCallBack(AuthCallBack authCallBack) {
        this.authCallBack = authCallBack;
    }

    public void setUserCallBack(UserCallBack userCallBack) {
        this.userCallBack = userCallBack;
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    /*public void loginUser(String email, String password) {
        this.mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        authCallBack.onLoginComplete(task);
                    }
                });
    }*/

    public void loginUser(String email, String password) {
        this.mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (authCallBack != null) {
                        authCallBack.onLoginComplete(task);
                    }
                });
    }

    public void createAccount(String email, String password, User userData) {
        this.mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (getCurrentUser() != null) {
                            String userId = getCurrentUser().getUid();
                            userData.setKeyOn(userId);
                            saveUserData(userData);

                        } else {
                            authCallBack.onCreateAccountComplete(false, Objects.requireNonNull(task.getException()).getMessage());
                        }
                    }
                });
    }

    public void saveUserData(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("firstname", user.getFirstname());
        userData.put("lastname", user.getLastname());
        userData.put("email", user.getEmail());
        userData.put("myId", user.getMyId());
        userData.put("account_type", user.getAccount_type()); // == 1 ? "teacher" : "student");

        this.db.collection(USERS_TABLE)
                .document(user.getKey())
                .set(userData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful())
                            authCallBack.onCreateAccountComplete(true, "");
                        else
                            authCallBack.onCreateAccountComplete(false, Objects.requireNonNull(task.getException()).getMessage());
                    }
                });
    }


    public void checkUserExists(String email, final UserExistsCallback callback) {
        db.collection(USERS_TABLE)
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean exists = !task.getResult().isEmpty();
                        callback.onUserExistsCheckComplete(exists);
                    } else {
                        callback.onUserExistsCheckFailure(task.getException());
                    }
                });
    }

    public interface UserExistsCallback {
        void onUserExistsCheckComplete(boolean exists);

        void onUserExistsCheckFailure(Exception e);
    }

    public void addStudentsToCourse(List<String> studentIds, String teacherId, String courseId, AddStudentCallback callback) {
        DocumentReference courseRef = db.collection(USERS_TABLE).document(teacherId)
                .collection("courses").document(courseId);

        // Add students to the course's enrolledStudents list
        courseRef.update("enrolledStudents", FieldValue.arrayUnion(studentIds.toArray()))
                .addOnSuccessListener(aVoid -> {
                    // Add course reference to each student's enrolledCourses subcollection
                    for (String studentId : studentIds) {
                        db.collection(USERS_TABLE).document(studentId).collection("enrolledCourses")
                                .document(courseId)
                                .set(Collections.singletonMap("courseRef", courseRef))
                                .addOnFailureListener(callback::onFailure); // Handle failure for individual student
                    }
                    callback.onSuccess();
                })
                .addOnFailureListener(callback::onFailure); // Handle failure for the course update
    }


    // Callback Interface
    public interface AddStudentCallback {
        void onSuccess();
        void onFailure(Exception e);
    }


    //todo ensure this is storing the user correctly
    public void fetchUserData(String uid, UserFetchCallback callback) {
        db.collection(USERS_TABLE).document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                callback.onSuccess(user);
                            } else {
                                callback.onFailure(new Exception("User data is null"));
                            }
                        } else {
                            callback.onFailure(new Exception("Document does not exist"));
                        }
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    // Callback Interface
    public interface UserFetchCallback {
        void onSuccess(User user);
        void onFailure(Exception e);
    }

    public void addCourse(String teacherId, String courseName, String description, AddCourseCallback callback) {
        Map<String, Object> courseData = new HashMap<>();
        courseData.put("courseName", courseName);
        courseData.put("description", description);
        courseData.put("enrolledStudents", new ArrayList<>());
        courseData.put("files", new ArrayList<>());

        db.collection(USERS_TABLE).document(teacherId).collection("courses")
                .add(courseData)
                .addOnSuccessListener(documentReference -> callback.onSuccess(documentReference.getId()))
                .addOnFailureListener(callback::onFailure);
    }

    public interface AddCourseCallback {
        void onSuccess(String courseId);
        void onFailure(Exception e);
    }

}
