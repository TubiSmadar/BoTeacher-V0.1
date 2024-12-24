package com.example.boteacher

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import com.example.boteacher.screens.*
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val okHttpClient = OkHttpClient()
    private val baseUrl = "http://10.0.2.2:5000" // Replace with your Flask server URL
    private lateinit var filePickerLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // File picker for lecturer uploads
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleFileUpload(it) }
        }

        setContent {
            var isLecturer by remember { mutableStateOf<Boolean?>(null) }
            var courses by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

            // Fetch courses when in student mode
            LaunchedEffect(isLecturer) {
                if (isLecturer == false) fetchCourses { fetchedCourses -> courses = fetchedCourses }
            }

            when (isLecturer) {
                null -> RoleSelectionScreen { role -> isLecturer = role }
                true -> LecturerPage(filePickerLauncher) { isLecturer = null }
                false -> StudentPage(
                    courses = courses,
                    fetchCourses = { fetchCourses { fetchedCourses -> courses = fetchedCourses } },
                    handleSendMessage = { courseId, message, onResponse -> sendMessage(courseId, message, onResponse) },
                    onBack = { isLecturer = null }
                )
            }
        }
    }

    private fun handleFileUpload(uri: Uri) {
        val fileName = getFileName(uri)
        val tempFile = File(cacheDir, fileName)
        contentResolver.openInputStream(uri)?.use { inputStream ->
            saveInputStreamToFile(inputStream, tempFile)
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, RequestBody.create(null, tempFile))
            .addFormDataPart("course_name", fileName)
            .build()

        val request = Request.Builder().url("$baseUrl/lecturer/upload_course").post(requestBody).build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { showToast("File upload failed: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) showToast("File uploaded successfully")
                    else showToast("Error uploading file")
                }
            }
        })
    }

    private fun fetchCourses(onResult: (List<Pair<String, String>>) -> Unit) {
        val request = Request.Builder().url("$baseUrl/student/courses").get().build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    showToast("Failed to fetch courses: ${e.message}")
                    onResult(emptyList())
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.use { body ->
                    val bodyString = body.string()
                    try {
                        val json = JSONObject(bodyString)
                        val courseList = mutableListOf<Pair<String, String>>()
                        json.keys().forEach { key -> courseList.add(key to json.getString(key)) }
                        runOnUiThread { onResult(courseList) }
                    } catch (e: Exception) {
                        runOnUiThread { showToast("Invalid server response") }
                        onResult(emptyList())
                    }
                }
            }
        })
    }

    private fun sendMessage(courseId: String, message: String, onResponse: (String) -> Unit) {
        val jsonBody = JSONObject().apply {
            put("student_id", "student123") // Replace with the actual student ID
            put("course_id", courseId)
            put("message", message)
        }

        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody.toString())
        val request = Request.Builder()
            .url("$baseUrl/student/chat")
            .post(requestBody)
            .addHeader("Content-Type", "application/json") // Ensure the Content-Type header is set
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { onResponse("Failed to send message: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.use { body ->
                    val responseString = body.string()
                    val jsonResponse = JSONObject(responseString)
                    val reply = jsonResponse.optString("response", "No response")
                    runOnUiThread { onResponse(reply) }
                }
            }
        })
    }

    private fun getFileName(uri: Uri): String {
        var name = "uploaded_file"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            name = cursor.getString(nameIndex)
        }
        return name
    }

    private fun saveInputStreamToFile(inputStream: InputStream, file: File) {
        FileOutputStream(file).use { outputStream -> inputStream.copyTo(outputStream) }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
