package com.example.boteacher

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.boteacher.ui.theme.BoTeacherTheme
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)

class MainActivity : ComponentActivity() {

    private val okHttpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // File Picker Contract
        val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleFileUpload(it) }
        }

        setContent {
            BoTeacherTheme {
                var userMessage by remember { mutableStateOf("") }
                var responseText by remember { mutableStateOf("") }
                val isLoading = remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("BoTeacher") }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            // Text Field for User Input
                            BasicTextField(
                                value = userMessage,
                                onValueChange = { userMessage = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                decorationBox = { innerTextField ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = MaterialTheme.shapes.small,
                                        tonalElevation = 1.dp
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(12.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (userMessage.isEmpty()) {
                                                Text("Type your message...")
                                            }
                                            innerTextField()
                                        }
                                    }
                                }
                            )

                            // Button to Send Message
                            Button(
                                onClick = {
                                    isLoading.value = true
                                    handleSendMessage(userMessage) {
                                        responseText = it
                                        isLoading.value = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text("Send")
                            }
                        }

                        // Loading Indicator
                        if (isLoading.value) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            // Display API Response
                            Text(
                                text = responseText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        // Button to Upload File
                        Button(
                            onClick = { pickFile.launch("*/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text("Upload File")
                        }
                    }
                }
            }
        }
    }

    // Function to Handle File Upload
    private fun handleFileUpload(uri: Uri) {
        val tempFile = File(cacheDir, "uploaded_file")
        contentResolver.openInputStream(uri)?.use { inputStream ->
            saveInputStreamToFile(inputStream, tempFile)
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                tempFile.name,
                RequestBody.create("application/octet-stream".toMediaTypeOrNull(), tempFile)
            )
            .build()






        val request = Request.Builder()
            .url("http://10.0.2.2:5000/upload") // Replace with your Flask server URL
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "File upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "File uploaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Error: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // Function to Save InputStream to File
    private fun saveInputStreamToFile(inputStream: InputStream, file: File) {
        FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }

    // Function to Send Message to Flask API
    private fun handleSendMessage(message: String, onResponse: (String) -> Unit) {
        val jsonBody = JSONObject()
        jsonBody.put("message", message)

        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody.toString())
        val request = Request.Builder()
            .url("http://10.0.2.2:5000/generate") // Replace with your Flask server URL
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { onResponse("Request failed: ${e.message}") }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        onResponse(responseBody ?: "No response from server")
                    } else {
                        onResponse("Error: ${response.code}")
                    }
                }
            }
        })
    }
}
