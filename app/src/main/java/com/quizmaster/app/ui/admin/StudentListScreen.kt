package com.quizmaster.app.ui.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

private data class AdminStudent(
    val id: String,
    val name: String,
    val email: String,
    val status: String = "inactive",
    val currentQuizTitle: String = ""
)

@Composable
fun StudentListScreen(
    onBack: () -> Unit,
    onStudentSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    var students by remember { mutableStateOf<List<AdminStudent>>(emptyList()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var attendanceByStudentId by remember {
        mutableStateOf<Map<String, Pair<String, String>>>(emptyMap())
    }

    var attendanceListener by remember {
        mutableStateOf<ListenerRegistration?>(null)
    }

    LaunchedEffect(auth.currentUser?.uid, reloadKey) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            students = emptyList()
            errorMessage = "Your session has expired. Please login again."
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = ""

        firestore.collection("users")
            .whereEqualTo("role", "student")
            .get()
            .addOnSuccessListener { result ->
                students = result.documents.mapNotNull { document ->
                    val role = document.getString("role")
                        ?.trim()
                        ?.lowercase()

                    if (role != "student") {
                        return@mapNotNull null
                    }

                    AdminStudent(
                        id = document.id,
                        name = document.getString("name")
                            ?.trim()
                            ?.ifBlank { "Unnamed Student" }
                            ?: "Unnamed Student",
                        email = document.getString("email")
                            ?.trim()
                            ?.ifBlank { "No email available" }
                            ?: "No email available"
                    )
                }.sortedWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                )

                isLoading = false

                attendanceListener?.remove()

                attendanceListener = firestore.collection("attendance")
                    .addSnapshotListener { attendanceSnapshot, exception ->
                        if (exception != null || attendanceSnapshot == null) {
                            // Student list remains usable even if live attendance
                            // temporarily cannot be read.
                            return@addSnapshotListener
                        }

                        attendanceByStudentId = attendanceSnapshot.documents
                            .associate { document ->
                                val status = document.getString("status")
                                    ?.trim()
                                    ?.lowercase()
                                    ?.takeIf {
                                        it == "active" ||
                                                it == "attempting" ||
                                                it == "inactive"
                                    }
                                    ?: "inactive"

                                val quizTitle = document.getString("currentQuizTitle")
                                    ?.trim()
                                    ?.ifBlank { "" }
                                    ?: ""

                                document.id to Pair(status, quizTitle)
                            }
                    }
            }
            .addOnFailureListener { exception ->
                students = emptyList()
                errorMessage = exception.localizedMessage
                    ?: "Unable to load students."
                isLoading = false
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            attendanceListener?.remove()
            attendanceListener = null
        }
    }

    val normalizedSearch = searchQuery.trim()

    val filteredStudents = students.filter { student ->
        normalizedSearch.isBlank() ||
                student.name.contains(normalizedSearch, ignoreCase = true) ||
                student.email.contains(normalizedSearch, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Students",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isLoading) {
                        "Loading students..."
                    } else {
                        "${students.size} registered ${if (students.size == 1) "student" else "students"}"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = { showAddStudentDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Student",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            singleLine = true,
            label = { Text("Search students") },
            placeholder = { Text("Name or email") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    TextButton(
                        onClick = { searchQuery = "" }
                    ) {
                        Text("Clear")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            errorMessage.isNotBlank() -> {
                StudentListError(
                    message = errorMessage,
                    onRetry = {
                        errorMessage = ""
                        isLoading = true
                        reloadKey++
                    }
                )
            }

            students.isEmpty() -> {
                StudentEmptyState(
                    title = "No students yet",
                    message = "Registered student accounts will appear here."
                )
            }

            filteredStudents.isEmpty() -> {
                StudentEmptyState(
                    title = "No matching students",
                    message = "Try a different name or email."
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = filteredStudents,
                        key = { it.id }
                    ) { student ->
                        val attendance = attendanceByStudentId[student.id]
                        val status = attendance?.first ?: "inactive"
                        val currentQuizTitle = attendance?.second ?: ""

                        StudentCard(
                            student = student,
                            status = status,
                            currentQuizTitle = currentQuizTitle,
                            onClick = {
                                onStudentSelected(student.id)
                            }
                        )
                    }
                }
            }
        }

        if (showAddStudentDialog) {
            var newName by remember { mutableStateOf("") }
            var newEmail by remember { mutableStateOf("") }
            var newPassword by remember { mutableStateOf("") }
            var isCreating by remember { mutableStateOf(false) }
            var dialogError by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { if (!isCreating) showAddStudentDialog = false },
                title = { Text("Create Student Account", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Student Full Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = { newEmail = it },
                            label = { Text("Email Address") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("Password (min 6 chars)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (dialogError.isNotBlank()) {
                            Text(dialogError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanName = newName.trim()
                            val cleanEmail = newEmail.trim()
                            val cleanPass = newPassword.trim()
                            if (cleanName.length < 2) {
                                dialogError = "Name must be at least 2 characters."
                                return@Button
                            }
                            if (!cleanEmail.contains("@")) {
                                dialogError = "Enter a valid email address."
                                return@Button
                            }
                            if (cleanPass.length < 6) {
                                dialogError = "Password must be at least 6 characters."
                                return@Button
                            }
                            isCreating = true
                            dialogError = ""
                            createStudentAccount(
                                name = cleanName,
                                email = cleanEmail,
                                pass = cleanPass,
                                onSuccess = {
                                    isCreating = false
                                    showAddStudentDialog = false
                                    Toast.makeText(context, "Student created successfully", Toast.LENGTH_SHORT).show()
                                    reloadKey++
                                },
                                onError = { err ->
                                    isCreating = false
                                    dialogError = err
                                }
                            )
                        },
                        enabled = !isCreating
                    ) {
                        Text(if (isCreating) "Creating..." else "Create")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddStudentDialog = false },
                        enabled = !isCreating
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun StudentCard(
    student: AdminStudent,
    status: String,
    currentQuizTitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.name
                        .firstOrNull()
                        ?.uppercase()
                        ?: "S",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = student.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = student.email,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(7.dp))

                StudentActivityStatus(
                    status = status,
                    currentQuizTitle = currentQuizTitle
                )
            }

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Student details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StudentActivityStatus(
    status: String,
    currentQuizTitle: String
) {
    val normalizedStatus = status.trim().lowercase()

    val label: String
    val indicatorColor: Color

    when (normalizedStatus) {
        "active" -> {
            label = "Active"
            indicatorColor = Color(0xFF16A34A)
        }

        "attempting" -> {
            label = if (currentQuizTitle.isNotBlank()) {
                "Quiz Attempting • $currentQuizTitle"
            } else {
                "Quiz Attempting"
            }
            indicatorColor = Color(0xFF2563EB)
        }

        else -> {
            label = "Inactive"
            indicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )

        Spacer(modifier = Modifier.width(7.dp))

        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StudentListError(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.06f)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Unable to load students",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(onClick = onRetry) {
                    Text("Try Again")
                }
            }
        }
    }
}

@Composable
private fun StudentEmptyState(
    title: String,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Creates student Auth account using secondary FirebaseApp instance so current Admin is not logged out.
 */
private fun createStudentAccount(
    name: String,
    email: String,
    pass: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val context = FirebaseApp.getInstance().applicationContext

    val secondaryApp = try {
        FirebaseApp.getInstance("SecondaryApp")
    } catch (_: Exception) {
        val options = FirebaseApp.getInstance().options
        FirebaseApp.initializeApp(context, options, "SecondaryApp")
    }

    val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
    val mainDb = FirebaseFirestore.getInstance()

    secondaryAuth.createUserWithEmailAndPassword(email, pass)
        .addOnSuccessListener { authResult ->
            val uid = authResult.user?.uid
            if (uid == null) {
                onError("Student account creation failed.")
                return@addOnSuccessListener
            }

            val studentProfile = hashMapOf(
                "userId" to uid,
                "name" to name,
                "email" to email,
                "role" to "student",
                "status" to "active",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            mainDb.collection("users").document(uid)
                .set(studentProfile)
                .addOnSuccessListener {
                    secondaryAuth.signOut()
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    secondaryAuth.signOut()
                    onError(e.localizedMessage ?: "Failed to save student profile.")
                }
        }
        .addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Failed to create student Auth account.")
        }
}
