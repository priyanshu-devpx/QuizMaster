package com.quizmaster.app.ui.admin

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaster.app.data.SubjectRepository
import com.quizmaster.app.data.model.Subject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSubjectsScreen(
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current

    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }

    fun loadSubjects() {
        isLoading = true
        SubjectRepository.getSubjects(
            onSuccess = { list ->
                subjects = list
                isLoading = false
                errorMessage = ""
            },
            onError = { msg ->
                isLoading = false
                errorMessage = msg
            }
        )
    }

    LaunchedEffect(Unit) {
        loadSubjects()
    }

    val filteredSubjects = remember(subjects, searchQuery) {
        if (searchQuery.isBlank()) {
            subjects
        } else {
            val query = searchQuery.trim().lowercase()
            subjects.filter {
                it.name.lowercase().contains(query) ||
                        it.code.lowercase().contains(query) ||
                        it.description.lowercase().contains(query)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Manage Subjects",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSubject = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Subject"
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                errorMessage.isNotBlank() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Something went wrong",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { loadSubjects() }) {
                            Text("Retry")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = "Create and manage academic subjects for quizzes and teacher assignments.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Search subjects by name or code...") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        if (filteredSubjects.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(30.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (searchQuery.isBlank()) "No subjects created yet." else "No matching subjects found.",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(
                                items = filteredSubjects,
                                key = { it.subjectId }
                            ) { subject ->
                                SubjectCard(
                                    subject = subject,
                                    onEdit = {
                                        editingSubject = subject
                                        showDialog = true
                                    },
                                    onToggleStatus = { newStatus ->
                                        SubjectRepository.setSubjectStatus(
                                            subjectId = subject.subjectId,
                                            status = newStatus,
                                            onSuccess = {
                                                Toast.makeText(
                                                    context,
                                                    "Status updated",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                loadSubjects()
                                            },
                                            onError = { msg ->
                                                Toast.makeText(
                                                    context,
                                                    msg,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        SubjectDialog(
            subject = editingSubject,
            onDismiss = { showDialog = false },
            onSave = { name, code, description ->
                showDialog = false
                val target = editingSubject
                if (target == null) {
                    SubjectRepository.createSubject(
                        name = name,
                        code = code,
                        description = description,
                        onSuccess = {
                            Toast.makeText(context, "Subject created successfully", Toast.LENGTH_SHORT).show()
                            loadSubjects()
                        },
                        onError = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    SubjectRepository.updateSubject(
                        subjectId = target.subjectId,
                        name = name,
                        code = code,
                        description = description,
                        onSuccess = {
                            Toast.makeText(context, "Subject updated successfully", Toast.LENGTH_SHORT).show()
                            loadSubjects()
                        },
                        onError = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun SubjectCard(
    subject: Subject,
    onEdit: () -> Unit,
    onToggleStatus: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = subject.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (subject.code.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = subject.code,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (subject.isActive) "Active" else "Inactive",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (subject.isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Subject",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (subject.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = subject.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: ${if (subject.isActive) "Active" else "Disabled"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Switch(
                    checked = subject.isActive,
                    onCheckedChange = { isChecked ->
                        onToggleStatus(if (isChecked) "active" else "inactive")
                    }
                )
            }
        }
    }
}

@Composable
private fun SubjectDialog(
    subject: Subject?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(subject?.name ?: "") }
    var code by remember { mutableStateOf(subject?.code ?: "") }
    var description by remember { mutableStateOf(subject?.description ?: "") }
    var validationError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (subject == null) "Add New Subject" else "Edit Subject",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = ""
                    },
                    label = { Text("Subject Name") },
                    placeholder = { Text("e.g. Mathematics") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        validationError = ""
                    },
                    label = { Text("Subject Code") },
                    placeholder = { Text("e.g. MATH101") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        validationError = ""
                    },
                    label = { Text("Description") },
                    placeholder = { Text("Brief description of the subject...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                if (validationError.isNotBlank()) {
                    Text(
                        text = validationError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().length < 2) {
                        validationError = "Subject name must be at least 2 characters."
                    } else {
                        onSave(name, code, description)
                    }
                }
            ) {
                Text(if (subject == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
