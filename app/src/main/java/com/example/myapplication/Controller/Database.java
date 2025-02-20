package com.example.myapplication.Controller;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.myapplication.Model.Course;
import com.example.myapplication.Model.User;
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
        userData.put("fcmToken", user.getFcmToken());
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

    /*public void addCourse(String teacherKey, String courseName, String description, Uri fileUri, AddCourseCallback callback, FileUploadCallback fileCallback) {
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

        uploadFile(teacherKey, , fileUri, fileCallback);
    }*/

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


    public void uploadFile(String teacherKey, String courseId, Uri fileUri, FileUploadCallback callback) {
        DocumentReference courseRef = db.collection(USERS_TABLE)
                .document(teacherKey)
                .collection("courses")
                .document(courseId);

        courseRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String courseName = documentSnapshot.getString("name"); // Assuming "name" is the course name field
                String storagePath = "courses/" + courseName + "/" + fileUri.getLastPathSegment();

                StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                StorageReference fileRef = storageRef.child(storagePath);

                fileRef.putFile(fileUri)
                        .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    courseRef.update("files", FieldValue.arrayUnion(uri.toString()))
                                            .addOnSuccessListener(docRef -> callback.onSuccess(uri.toString()))
                                            .addOnFailureListener(callback::onFailure);
                                })
                                .addOnFailureListener(callback::onFailure))
                        .addOnFailureListener(callback::onFailure);
            } else {
                callback.onFailure(new Exception("Course not found"));
            }
        }).addOnFailureListener(callback::onFailure);
    }

    public void addFileReferenceToCourse(String teacherKey, String courseId, String fileId, FileReferenceCallback callback) {
        // Reference to the specific course document
        DocumentReference courseRef = db.collection(USERS_TABLE)
                .document(teacherKey)
                .collection("courses")
                .document(courseId);

        // Update the 'files' array field with the new fileId
        courseRef.update("files", FieldValue.arrayUnion(fileId))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public void fetchFileNames(String teacherKey, String courseId, FileNameCallback callback) {
        // Reference to the course document
        DocumentReference courseRef = db.collection(USERS_TABLE)
                .document(teacherKey)
                .collection("courses")
                .document(courseId);

        // Get the document to retrieve the 'files' array field
        courseRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Get the list of file IDs from the 'files' array
                List<String> fileIds = (List<String>) documentSnapshot.get("files");

                // Check if files exist
                if (fileIds == null || fileIds.isEmpty()) {
                    callback.onSuccess(new ArrayList<>()); // Return empty list
                    return;
                }

                // Create a list to store file names
                List<String> fileNames = new ArrayList<>();
                // Counter to keep track of completed requests
                final int[] completedRequests = {0};

                // Loop through each file ID and fetch the file name
                for (String fileId : fileIds) {

                    // Check if fileId is a URL or an invalid path
                    if (fileId.contains("://") || fileId.contains("//")) {
                        //Log.w("fetchFileNames", "Invalid file reference, skipping: " + fileId);
                        completedRequests[0]++;
                        // Check if all requests are completed
                        if (completedRequests[0] == fileIds.size()) {
                            callback.onSuccess(fileNames);
                        }
                        continue; // Skip this entry
                    }

                    DocumentReference fileRef = db.collection("files").document(fileId);
                    fileRef.get().addOnSuccessListener(fileSnapshot -> {
                        if (fileSnapshot.exists()) {
                            String fileName = fileSnapshot.getString("name");
                            if (fileName != null) {
                                fileNames.add(fileName);
                            }
                        }
                        completedRequests[0]++;
                        // Check if all requests are completed
                        if (completedRequests[0] == fileIds.size()) {
                            callback.onSuccess(fileNames);
                        }
                    }).addOnFailureListener(e -> {
                        callback.onFailure(e);
                    });
                }
            } else {
                callback.onSuccess(new ArrayList<>()); // Return empty list if course doesn't exist
            }
        }).addOnFailureListener(callback::onFailure);
    }

    public void updateCourseDescription(String teacherKey, String courseId, String newDescription, DescUpdateCallback callback) {
        // Reference to the course document
        DocumentReference courseRef = db.collection(USERS_TABLE)
                .document(teacherKey)
                .collection("courses")
                .document(courseId);

        // Create a map to hold the updated field
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", newDescription);

        // Update the description field in Firestore
        courseRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e);
                });
    }

    public void updateFcmToken(String studentId, String newFcm, FcmUpdateCallback callback){
        DocumentReference courseRef = db.collection(USERS_TABLE)
                .document(studentId);

        // Create a map to hold the updated field
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", newFcm);

        courseRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e);
                });
    }

    public void getFcmToken(List<String> studentIds, FcmTokenCallback callback) {
        db.collection(USERS_TABLE)
                .whereIn("myId", studentIds)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<String> uidList = new ArrayList<>();
                        List<String> fcmTokens = new ArrayList<>(); //***CHANGE***

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            uidList.add(document.getId());
                        }

                        // If no users were found, return an empty list
                        if (uidList.isEmpty()) { //***CHANGE***
                            callback.onTokenReceived(fcmTokens); // Return empty list //***CHANGE***
                            return; // Exit early //***CHANGE***
                        }

                        // Fetch fcmToken for each uid
                        for (String uid : uidList) {
                            db.collection(USERS_TABLE).document(uid)
                                    .get()
                                    .addOnCompleteListener(innerTask -> {
                                        if (innerTask.isSuccessful() && innerTask.getResult() != null) {
                                            String token = innerTask.getResult().getString("fcmToken");
                                            if (token != null && !token.isEmpty()) {
                                                fcmTokens.add(token); //***CHANGE***
                                            }
                                        }

                                        // If all requests are completed, return the tokens
                                        if (fcmTokens.size() == uidList.size()) { //***CHANGE***
                                            callback.onTokenReceived(fcmTokens); //***CHANGE***
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        callback.onFailure(e); //***CHANGE***
                                    });
                        }
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException() : new Exception("Failed to fetch user documents")); //***CHANGE***
                    }
                });
    }


    public interface FcmTokenCallback {
        void onTokenReceived(List<String> tokens);
        void onFailure(Exception e);
    }

    public interface FcmUpdateCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface DescUpdateCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface FileNameCallback {
        void onSuccess(List<String> file_names);
        void onFailure(Exception e);
    }

    // Callback interface to handle success/failure
    public interface FileReferenceCallback {
        void onSuccess();
        void onFailure(Exception e);
    }


    public interface FileUploadCallback {
        void onSuccess(String fileUrl);
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
