package com.quizmaster.app.ui.admin

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.quizmaster.app.data.SubjectRepository
import com.quizmaster.app.data.model.Subject

private data class TeacherUser(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "teacher",
    val status: String = "active",
    val assignedSubjectId: String = ""
) {
    val isActive: Boolean get() = status.equals("active", ignoreCase = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTeachersScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }

    var teachers by remember { mutableStateOf<List<TeacherUser>>(emptyList()) }
    var subjectsMap by remember { mutableStateOf<Map<String, Subject>>(emptyMap()) }
    var activeSubjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTeacher by remember { mutableStateOf<TeacherUser?>(null) }

    fun loadData() {
        isLoading = true
        // Load active subjects
        SubjectRepository.getSubjects(
            onSuccess = { subList ->
                subjectsMap = subList.associateBy { it.subjectId }
                activeSubjects = subList.filter { it.isActive }

                // Load Teachers
                db.collection("users")
                    .whereEqualTo("role", "teacher")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        teachers = snapshot.documents.mapNotNull { doc ->
                            TeacherUser(
                                userId = doc.getString("userId") ?: doc.id,
                                name = doc.getString("name") ?: "",
                                email = doc.getString("email") ?: "",
                                role = doc.getString("role") ?: "teacher",
                                status = doc.getString("status") ?: "active",
                                assignedSubjectId = doc.getString("assignedSubjectId") ?: ""
                            )
                        }
                        isLoading = false
                        errorMessage = ""
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        errorMessage = e.localizedMessage ?: "Failed to load teachers."
                    }
            },
            onError = { msg ->
                isLoading = false
                errorMessage = msg
            }
        )
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val filteredTeachers = remember(teachers, searchQuery, subjectsMap) {
        if (searchQuery.isBlank()) {
            teachers
        } else {
            val query = searchQuery.trim().lowercase()
            teachers.filter { teacher ->
                val subjectName = subjectsMap[teacher.assignedSubjectId]?.name?.lowercase() ?: ""
                teacher.name.lowercase().contains(query) ||
                        teacher.email.lowercase().contains(query) ||
                        subjectName.contains(query)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTeacher = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Teacher")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (errorMessage.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Error loading data", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(errorMessage, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { loadData() }) { Text("Retry") }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "Manage instructors and assign exactly one academic subject to each teacher.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search teachers by name, email, or subject...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (filteredTeachers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isBlank()) "No teachers registered yet." else "No matching teachers found.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(filteredTeachers, key = { it.userId }) { teacher ->
                            val assignedSubject = subjectsMap[teacher.assignedSubjectId]
                            TeacherCard(
                                teacher = teacher,
                                assignedSubject = assignedSubject,
                                onEditSubject = {
                                    editingTeacher = teacher
                                    showAddDialog = true
                                },
                                onToggleStatus = { newStatus ->
                                    db.collection("users").document(teacher.userId)
                                        .update(
                                            "status", newStatus,
                                            "updatedAt", FieldValue.serverTimestamp()
                                        )
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Teacher status updated", Toast.LENGTH_SHORT).show()
                                            loadData()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, e.localizedMessage ?: "Update failed", Toast.LENGTH_LONG).show()
                                        }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        TeacherDialog(
            teacher = editingTeacher,
            activeSubjects = activeSubjects,
            onDismiss = { showAddDialog = false },
            onSave = { name, email, password, selectedSubjectId ->
                showAddDialog = false
                val target = editingTeacher
                if (target == null) {
                    // Create New Teacher safely using secondary Firebase Auth instance
                    createTeacherAccount(
                        name = name,
                        email = email,
                        pass = password,
                        assignedSubjectId = selectedSubjectId,
                        onSuccess = {
                            Toast.makeText(context, "Teacher account created successfully!", Toast.LENGTH_SHORT).show()
                            loadData()
                        },
                        onError = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    // Update Teacher Subject
                    db.collection("users").document(target.userId)
                        .update(
                            "assignedSubjectId", selectedSubjectId,
                            "updatedAt", FieldValue.serverTimestamp()
                        )
                        .addOnSuccessListener {
                            Toast.makeText(context, "Teacher assigned subject updated!", Toast.LENGTH_SHORT).show()
                            loadData()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, e.localizedMessage ?: "Failed to update teacher", Toast.LENGTH_LONG).show()
                        }
                }
            }
        )
    }
}

@Composable
private fun TeacherCard(
    teacher: TeacherUser,
    assignedSubject: Subject?,
    onEditSubject: () -> Unit,
    onToggleStatus: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Prof. ${teacher.name}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = teacher.email,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onEditSubject) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Subject", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Assigned Subject Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Assigned Subject: ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (assignedSubject != null) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${assignedSubject.name} (${assignedSubject.code})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Unassigned",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Account Status: ${if (teacher.isActive) "Active" else "Deactivated"}",
                    fontSize = 12.sp,
                    color = if (teacher.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )

                Switch(
                    checked = teacher.isActive,
                    onCheckedChange = { isChecked ->
                        onToggleStatus(if (isChecked) "active" else "inactive")
                    }
                )
            }
        }
    }
}

@Composable
private fun TeacherDialog(
    teacher: TeacherUser?,
    activeSubjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, pass: String, subjectId: String) -> Unit
) {
    var name by remember { mutableStateOf(teacher?.name ?: "") }
    var email by remember { mutableStateOf(teacher?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var selectedSubjectId by remember { mutableStateOf(teacher?.assignedSubjectId ?: "") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var validationError by remember { mutableStateOf("") }

    val isEditing = teacher != null

    val selectedSubjectName = activeSubjects.find { it.subjectId == selectedSubjectId }?.name ?: "Select Assigned Subject"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Teacher Subject" else "Register New Teacher",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isEditing) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; validationError = "" },
                        label = { Text("Teacher Full Name") },
                        placeholder = { Text("e.g. Alan Turing") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; validationError = "" },
                        label = { Text("Email Address") },
                        placeholder = { Text("teacher@school.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; validationError = "" },
                        label = { Text("Password (min 6 chars)") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "Hide" else "Show", fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "Teacher: Prof. ${teacher.name} (${teacher.email})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Assigned Subject Selector
                Text(
                    text = "Assign Subject (Required)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedSubjectName,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { dropdownExpanded = !dropdownExpanded }
                    )

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (activeSubjects.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No active subjects available", color = MaterialTheme.colorScheme.error) },
                                onClick = { dropdownExpanded = false }
                            )
                        } else {
                            activeSubjects.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text("${sub.name} (${sub.code})") },
                                    onClick = {
                                        selectedSubjectId = sub.subjectId
                                        dropdownExpanded = false
                                        validationError = ""
                                    },
                                    leadingIcon = {
                                        if (selectedSubjectId == sub.subjectId) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (validationError.isNotBlank()) {
                    Text(validationError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val cleanEmail = email.trim()

                    when {
                        !isEditing && cleanName.length < 2 -> {
                            validationError = "Enter a valid full name."
                        }
                        !isEditing && (!cleanEmail.contains("@") || !cleanEmail.contains(".")) -> {
                            validationError = "Enter a valid email address."
                        }
                        !isEditing && password.length < 6 -> {
                            validationError = "Password must be at least 6 characters."
                        }
                        selectedSubjectId.isBlank() -> {
                            validationError = "You must assign an active subject to the teacher."
                        }
                        else -> {
                            onSave(cleanName, cleanEmail, password, selectedSubjectId)
                        }
                    }
                }
            ) {
                Text(if (isEditing) "Save Assignment" else "Create Teacher")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Creates teacher Auth account using secondary FirebaseApp instance so current Admin is not logged out.
 */
private fun createTeacherAccount(
    name: String,
    email: String,
    pass: String,
    assignedSubjectId: String,
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
                onError("User creation failed.")
                return@addOnSuccessListener
            }

            val teacherProfile = hashMapOf(
                "userId" to uid,
                "name" to name,
                "email" to email,
                "role" to "teacher",
                "status" to "active",
                "assignedSubjectId" to assignedSubjectId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            mainDb.collection("users").document(uid)
                .set(teacherProfile)
                .addOnSuccessListener {
                    secondaryAuth.signOut()
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    secondaryAuth.signOut()
                    onError(e.localizedMessage ?: "Failed to save profile.")
                }
        }
        .addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Failed to create teacher Auth account.")
        }
}
