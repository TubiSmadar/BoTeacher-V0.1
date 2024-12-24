package com.example.boteacher.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment


@Composable
fun StudentPage(
    courses: List<Pair<String, String>>,
    fetchCourses: () -> Unit,
    handleSendMessage: (String, String, (String) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var selectedCourseId by remember { mutableStateOf<String?>(null) }
    var userMessage by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }

    // Load courses when the page is first displayed
    LaunchedEffect(Unit) {
        fetchCourses()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Button(onClick = onBack) { Text("Back") }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Text("Student Page", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Dropdown Menu
        DropdownMenuBox(
            courses = courses,
            selectedCourseId = selectedCourseId,
            onCourseSelected = { selectedCourseId = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Message Input
        TextField(
            value = userMessage,
            onValueChange = { userMessage = it },
            label = { Text("Ask a question") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            if (selectedCourseId != null) {
                isLoading.value = true
                handleSendMessage(selectedCourseId!!, userMessage) {
                    responseText = it
                    isLoading.value = false
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Send Question")
        }

        if (isLoading.value) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Text(responseText, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
fun DropdownMenuBox(
    courses: List<Pair<String, String>>,
    selectedCourseId: String?,
    onCourseSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedCourseName by remember { mutableStateOf("Select Course") }

    Box(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { expanded = true }) {
            Text(selectedCourseName)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (courses.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No courses available") },
                    onClick = { expanded = false }
                )
            } else {
                courses.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onCourseSelected(id)
                            selectedCourseName = name
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
