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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Database {

    public static final String STUDENTS_TABLE = "Students";
    public static final String TEACHERS_TABLE = "Teachers";
    public static final String COURSES_TABLE = "Courses";
    public static final String PRE_APPROVED_EMAILS_TABLE = "PreApprovedEmails";

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

    public void loginUser(String email, String password) {
        this.mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
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

                            // Check if the account is a teacher account
                            if (userData.getAccount_type() == 1) {
                                // Add to the Teachers table
                                addTeacher(userId);
                            } else {
                                // Add to the Students table
                                db.collection(PRE_APPROVED_EMAILS_TABLE).document(email).get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {
                                                String teacherId = documentSnapshot.getString("teacherId");

                                                addStudentToTeacherList(userId, teacherId);

                                                removePreApprovedEmail(email, new PreApprovedEmailCallback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        Log.d("Database", "Pre-approved email removed successfully.");
                                                    }

                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.e("Database", "Failed to remove pre-approved email: " + e.getMessage());
                                                    }
                                                });

                                                authCallBack.onCreateAccountComplete(true, "");
                                            } else {
                                                authCallBack.onCreateAccountComplete(false, "Student's email not pre-approved.");
                                            }
                                        })
                                        .addOnFailureListener(e -> authCallBack.onCreateAccountComplete(false, e.getMessage()));
                            }
                        } else {
                            authCallBack.onCreateAccountComplete(false, Objects.requireNonNull(task.getException()).getMessage());
                        }
                    }
                });
    }

    private void addStudentToTeacherList(String userId, String teacherId) {
        db.collection(TEACHERS_TABLE).document(teacherId)
                .update("studentIds", FieldValue.arrayUnion(userId))
                .addOnSuccessListener(aVoid -> Log.d("Database", "Student " + userId + " added to teacher's list: " + teacherId))
                .addOnFailureListener(e -> Log.e("Database", "Error adding student to teacher's list", e));
    }

    public void checkAndCreateAccount(final String email, final String password, final User userData, final AuthCallBack callback) {
        if (userData.getAccount_type() == 1) {
            // This block is for creating an account directly if the user is a manager
            createAccount(email, password, userData);
        } else {
            // Check for pre-approved emails for employees
            db.collection(PRE_APPROVED_EMAILS_TABLE).document(email).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null && document.exists()) {
                        // Email is pre-approved, proceed to create account
                        createAccount(email, password, userData);
                    } else {
                        // Email not pre-approved, notify the user
                        Log.w("Database", "Email not pre-approved for account creation");
                        callback.onCreateAccountComplete(false, "Your email has not been approved by a manager.");
                    }
                } else {
                    Log.e("Database", "Error checking pre-approved emails", task.getException());
                    callback.onCreateAccountComplete(false, Objects.requireNonNull(task.getException()).getMessage());
                }
            });
        }
    }


    private void addTeacher(String teacherId) {
        Map<String, Object> teacherData = new HashMap<>();
        teacherData.put("studentIds", new ArrayList<>());

        db.collection(TEACHERS_TABLE).document(teacherId).set(teacherData)
                .addOnSuccessListener(aVoid -> Log.d("Database", "Teacher added"))
                .addOnFailureListener(e -> Log.w("Database", "Error adding teacher", e));
    }

    public void saveUserData(User user) {
        this.db.collection(user.getAccount_type() == 1 ? TEACHERS_TABLE : STUDENTS_TABLE)
                .document(user.getKey())
                .set(user)
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

    public void fetchUserData(String uid) {
        db.collection(STUDENTS_TABLE).document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    User user = document.toObject(User.class);
                    if (user != null) {
                        user.setKey(document.getId());
                        userCallBack.onUserFetchDataComplete(user);
                    }
                }
            }
        });
    }

    public void removePreApprovedEmail(String email, PreApprovedEmailCallback callback) {
        db.collection(PRE_APPROVED_EMAILS_TABLE).document(email).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public void checkUserExists(String email, final UserExistsCallback callback) {
        db.collection(STUDENTS_TABLE)
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

    public interface PreApprovedEmailCallback {
        void onSuccess();

        void onFailure(@NonNull Exception e);
    }

    public interface UserExistsCallback {
        void onUserExistsCheckComplete(boolean exists);

        void onUserExistsCheckFailure(Exception e);
    }

    public void addPreApprovedEmail(String email, String salary, String managerId, PreApprovedEmailCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("salary", salary);
        data.put("managerId", managerId);

        db.collection(PRE_APPROVED_EMAILS_TABLE).document(email).set(data)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
