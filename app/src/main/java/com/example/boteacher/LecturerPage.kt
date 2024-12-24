package com.example.boteacher.screens

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LecturerPage(filePicker: ActivityResultLauncher<String>, onBack: () -> Unit) {
    var courseName by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Button(onClick = onBack) { Text("Back") }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text("Lecturer Page", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = courseName,
            onValueChange = { courseName = it },
            label = { Text("Enter Course Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { filePicker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
            Text("Upload Course File")
        }
    }
}
