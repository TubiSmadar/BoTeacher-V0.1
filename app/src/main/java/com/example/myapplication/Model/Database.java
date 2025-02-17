package com.example.myapplication.Model;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.myapplication.Controller.AuthCallBack;
import com.example.myapplication.Controller.UserCallBack;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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

    public void addStudentsToCourse(List<String> studentIds, String teacherKey, String courseId, AddStudentCallback callback) {
        // Step 1: Query users based on `myId` values
        db.collection(USERS_TABLE)
                .whereIn("myId", studentIds) // Query users whose `myId` matches any of the given IDs
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<String> uidList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            uidList.add(document.getId()); // Add user `uid` to the list
                        }

                        // Step 2: Update course with uids
                        DocumentReference courseRef = db.collection(USERS_TABLE)
                                .document(teacherKey)
                                .collection("courses")
                                .document(courseId);

                        courseRef.update("enrolledStudents", FieldValue.arrayUnion(studentIds.toArray()))
                                .addOnSuccessListener(aVoid -> {
                                    // Step 3: Update enrolledCourses for each student
                                    for (String uid : uidList) {
                                        /*DocumentReference studentDoc = db.collection(USERS_TABLE).document(uid);
                                        DocumentReference studentEnrolledCourseRef = studentDoc.collection("enrolledCourses").document(courseId);

                                        studentEnrolledCourseRef.get()
                                                .addOnCompleteListener(studentTask -> {
                                                    if (studentTask.isSuccessful()) {
                                                        List<DocumentReference> courseRefs;
                                                        if (studentTask.getResult() != null && studentTask.getResult().exists()) {
                                                            // Retrieve the existing list of course references
                                                            courseRefs = (List<DocumentReference>) studentTask.getResult().get("courseRefs");
                                                        } else {
                                                            courseRefs = new ArrayList<>();
                                                        }

                                                        // Add the new courseRef to the list
                                                        if (!courseRefs.contains(courseRef)) {
                                                            courseRefs.add(courseRef);
                                                        }

                                                        // Save the updated list back to Firestore
                                                        studentEnrolledCourseRef.set(Collections.singletonMap("courseRefs", courseRefs))
                                                                .addOnSuccessListener(unused -> callback.onSuccess())
                                                                .addOnFailureListener(callback::onFailure);
                                                    } else {
                                                        callback.onFailure(studentTask.getException() != null ? studentTask.getException() : new Exception("Failed to fetch student's enrolled courses document"));
                                                    }
                                                })
                                                .addOnFailureListener(callback::onFailure);*/

                                        db.collection(USERS_TABLE).document(uid).collection("enrolledCourses")
                                                .document(courseId)
                                                .set(Collections.singletonMap("courseRef", courseRef))
                                                .addOnFailureListener(callback::onFailure); // Handle failure for individual student
                                    }
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(callback::onFailure); // Handle failure for the course update
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException() : new Exception("Failed to fetch user documents"));
                    }
                });
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

    public void addCourse(String teacherKey, String courseName, String description, AddCourseCallback callback) {
        /*Map<String, Object> courseData = new HashMap<>();
        courseData.put("courseName", courseName);
        courseData.put("description", description);
        courseData.put("enrolledStudents", new ArrayList<>());
        courseData.put("files", new ArrayList<>());*/

        // Create a new Course object
        Course course = new Course();
        course.setName(courseName);
        course.setDescription(description);
        course.setEnrolledStudents(new ArrayList<>());
        course.setFiles(new ArrayList<>());

        db.collection(USERS_TABLE).document(teacherKey).collection("courses")
                .add(course)
                .addOnSuccessListener(documentReference -> callback.onSuccess(documentReference.getId()))
                .addOnFailureListener(callback::onFailure);
    }

    public void fetchCourseList(String teacherKey, CourseListCallback callback){
        db.collection(USERS_TABLE).document(teacherKey).collection("courses")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        ArrayList<Course> courses = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Course course = document.toObject(Course.class);
                            course.setId(document.getId()); // Set the document ID if needed
                            courses.add(course);
                        }
                        callback.onSuccess(courses);
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException() : new Exception("Failed to fetch courses"));
                    }
                });
    }

    public void fetchCourseInfo(String teacherKey, String courseId, CourseInfoCallback callback){
        db.collection(USERS_TABLE).document(teacherKey).collection("courses")
                .document(courseId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if(document.exists()){
                            Course course = document.toObject(Course.class);
                            if (course != null) {
                                course.setId(document.getId()); // Set the document ID if needed
                                callback.onSuccess(course);
                            } else {
                                callback.onFailure(new Exception("Course data invalid or null"));
                            }
                        } else {
                            callback.onFailure(new Exception("Course document doesn't exist"));
                        }

                    } else {
                        callback.onFailure(task.getException() != null ? task.getException() : new Exception("Failed to fetch courses"));
                    }
                });
    }

    /*public void fetchStudentCourses(String studentKey, CourseListCallback callback){
        // Step 1: Reference the student's enrolledCourses subcollection
        db.collection(USERS_TABLE)
                .document(studentKey)
                .collection("enrolledCourses")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        ArrayList<Course> courses = new ArrayList<>();

                        // Step 2: Iterate through the documents in the enrolledCourses subcollection
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Parse each course document into a Course object
                            //String courseId = document.getId();
                            if(document.exists()){
                                Course course = document.toObject(Course.class);
                                if (course != null) {
                                    course.setId(document.getId()); // Set the document ID if needed
                                    courses.add(course); // Add to the list
                                } else {
                                    callback.onFailure(new Exception("Course data invalid or null"));
                                }
                            } else {
                                callback.onFailure(new Exception("Course document doesn't exist"));
                            }
                        }

                        // Step 3: Invoke the callback with the list of courses
                        callback.onSuccess(courses);
                    } else {
                        // Handle failure
                        callback.onFailure(task.getException() != null ? task.getException() : new Exception("Failed to fetch enrolled courses"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }*/

    public void fetchStudentCourses(String studentKey, CourseListCallback callback) {
        // Step 1: Reference the student's enrolledCourses subcollection
        db.collection(USERS_TABLE)
                .document(studentKey)
                .collection("enrolledCourses")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        ArrayList<Course> courses = new ArrayList<>();
                        List<Task<DocumentSnapshot>> courseTasks = new ArrayList<>();

                        // Step 2: Iterate through the documents in the enrolledCourses subcollection
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Retrieve the reference to the course document
                            DocumentReference courseRef = document.getDocumentReference("courseRef");
                            if (courseRef != null) {
                                // Add fetch task for the course document to the task list
                                courseTasks.add(courseRef.get());
                            }
                        }

                        // Step 3: Fetch all course documents
                        Tasks.whenAllSuccess(courseTasks)
                                .addOnSuccessListener(results -> {
                                    for (Object result : results) {
                                        if (result instanceof DocumentSnapshot) {
                                            DocumentSnapshot courseDoc = (DocumentSnapshot) result;
                                            if (courseDoc.exists()) {
                                                // Convert to Course object
                                                Course course = courseDoc.toObject(Course.class);
                                                if (course != null) {
                                                    course.setId(courseDoc.getId());
                                                    courses.add(course);
                                                }
                                            }
                                        }
                                    }
                                    callback.onSuccess(courses); // Return the list of courses
                                })
                                .addOnFailureListener(callback::onFailure);
                    } else {
                        // Handle failure
                        callback.onFailure(task.getException() != null ? task.getException() : new Exception("Failed to fetch enrolled courses"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }


    public void uploadFile(String courseName, Uri fileUri, FileUploadCallback callback) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference fileRef = storageRef.child("courses/" + courseName + "/" + fileUri.getLastPathSegment());

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            // Store file metadata in Firestore
                            Map<String, Object> fileData = new HashMap<>();
                            fileData.put("name", fileUri.getLastPathSegment());
                            fileData.put("url", uri.toString());

                            db.collection("courses").document(courseName).collection("files")
                                    .add(fileData)
                                    .addOnSuccessListener(docRef -> callback.onSuccess(uri.toString()))
                                    .addOnFailureListener(callback::onFailure);
                        })
                        .addOnFailureListener(callback::onFailure))
                .addOnFailureListener(callback::onFailure);
    }

    public void getFiles(String teacherKey, String courseName, FilesCallback callback) {
        db.collection(USERS_TABLE).document(teacherKey).collection("courses").document(courseName).collection("files")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        ArrayList<String> files = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            files.add(document.getString("name")); // Assuming "name" is the field
                        }
                        callback.onSuccess(files);
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException() : new Exception("Failed to fetch files"));
                    }
                });
    }

    public interface FileUploadCallback {
        void onSuccess(String fileUrl);
        void onFailure(Exception e);
    }

    public interface FilesCallback {
        void onSuccess(ArrayList<String> files);
        void onFailure(Exception e);
    }

    public interface CourseInfoCallback {
        void onSuccess(Course c);
        void onFailure(Exception e);
    }

    public interface AddCourseCallback {
        void onSuccess(String courseId);
        void onFailure(Exception e);
    }

    public interface CourseListCallback {
        void onSuccess(ArrayList<Course> courses);
        void onFailure(Exception e);
    }

}
