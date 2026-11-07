package com.quizmaster.app.ui.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

private data class AdminStudentProfile(
    val name: String,
    val email: String,
    val role: String,
    val status: String = "active",
    val createdAt: Timestamp?
)

private data class AdminStudentAttempt(
    val id: String,
    val quizTitle: String,
    val percentage: Double,
    val score: Double,
    val totalMarks: Double,
    val passed: Boolean,
    val submittedAt: Timestamp?
)

@Composable
fun StudentDetailsScreen(
    studentId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }

    var profile by remember { mutableStateOf<AdminStudentProfile?>(null) }
    var attempts by remember { mutableStateOf<List<AdminStudentAttempt>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(studentId, auth.currentUser?.uid, reloadKey) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            errorMessage = "Your session has expired. Please login again."
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = ""

        firestore.collection("users")
            .document(studentId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    errorMessage = "Student profile was not found."
                    isLoading = false
                    return@addOnSuccessListener
                }

                val role = document.getString("role")
                    ?.trim()
                    ?.lowercase()
                    ?: ""

                if (role != "student") {
                    errorMessage = "This account is not a student account."
                    isLoading = false
                    return@addOnSuccessListener
                }

                profile = AdminStudentProfile(
                    name = document.getString("name")
                        ?.trim()
                        ?.ifBlank { "Unnamed Student" }
                        ?: "Unnamed Student",
                    email = document.getString("email")
                        ?.trim()
                        ?.ifBlank { "No email available" }
                        ?: "No email available",
                    role = role,
                    status = document.getString("status") ?: "active",
                    createdAt = document.getTimestamp("createdAt")
                )

                firestore.collection("attempts")
                    .whereEqualTo("userId", studentId)
                    .get()
                    .addOnSuccessListener { result ->
                        attempts = result.documents.map { attempt ->
                            AdminStudentAttempt(
                                id = attempt.id,
                                quizTitle = attempt.getString("quizTitle")
                                    ?.trim()
                                    ?.ifBlank { "Untitled Quiz" }
                                    ?: "Untitled Quiz",
                                percentage = attempt.getDouble("percentage") ?: 0.0,
                                score = attempt.getDouble("score") ?: 0.0,
                                totalMarks = attempt.getDouble("totalMarks") ?: 0.0,
                                passed = attempt.getBoolean("passed") ?: false,
                                submittedAt = attempt.getTimestamp("submittedAt")
                            )
                        }.sortedByDescending {
                            it.submittedAt?.toDate()?.time ?: 0L
                        }
                        isLoading = false
                    }
                    .addOnFailureListener { exception ->
                        errorMessage = exception.localizedMessage
                            ?: "Unable to load student activity."
                        isLoading = false
                    }
            }
            .addOnFailureListener { exception ->
                errorMessage = exception.localizedMessage
                    ?: "Unable to load student profile."
                isLoading = false
            }
    }

    val passedCount = attempts.count { it.passed }
    val failedCount = attempts.size - passedCount
    val averagePercentage = if (attempts.isEmpty()) {
        0.0
    } else {
        attempts.map { it.percentage }.average()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Student Details",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            if (profile != null) {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Student Name",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = {
                                errorMessage = ""
                                reloadKey++
                            }
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }

            profile != null -> {
                val currentProfile = profile!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 10.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        StudentProfileCard(profile = currentProfile)
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Account Access Status",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (currentProfile.status == "active") "Active - Student can log in" else "Deactivated - Login blocked",
                                        fontSize = 12.sp,
                                        color = if (currentProfile.status == "active") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }

                                Switch(
                                    checked = currentProfile.status == "active",
                                    onCheckedChange = { isChecked ->
                                        val newStatus = if (isChecked) "active" else "inactive"
                                        firestore.collection("users").document(studentId)
                                            .update(
                                                "status", newStatus,
                                                "updatedAt", FieldValue.serverTimestamp()
                                            )
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Student account status updated.", Toast.LENGTH_SHORT).show()
                                                reloadKey++
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, e.localizedMessage ?: "Status update failed", Toast.LENGTH_LONG).show()
                                            }
                                    }
                                )
                            }
                        }
                    }

                    item {
                        StudentActivityStats(
                            attempts = attempts.size,
                            passed = passedCount,
                            failed = failedCount,
                            average = averagePercentage
                        )
                    }

                    item {
                        Text(
                            text = "Quiz Activity",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (attempts.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = "This student has not attempted any quizzes yet.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                    } else {
                        items(
                            items = attempts,
                            key = { it.id }
                        ) { attempt ->
                            AttemptCard(attempt = attempt)
                        }
                    }
                }
            }
        }

        if (showEditDialog && profile != null) {
            var updatedName by remember { mutableStateOf(profile!!.name) }
            var isUpdating by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { if (!isUpdating) showEditDialog = false },
                title = { Text("Edit Student Name", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = updatedName,
                        onValueChange = { updatedName = it },
                        label = { Text("Student Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanName = updatedName.trim()
                            if (cleanName.length < 2) {
                                Toast.makeText(context, "Name must be at least 2 characters.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isUpdating = true
                            firestore.collection("users").document(studentId)
                                .update(
                                    "name", cleanName,
                                    "updatedAt", FieldValue.serverTimestamp()
                                )
                                .addOnSuccessListener {
                                    isUpdating = false
                                    showEditDialog = false
                                    Toast.makeText(context, "Student name updated.", Toast.LENGTH_SHORT).show()
                                    reloadKey++
                                }
                                .addOnFailureListener { e ->
                                    isUpdating = false
                                    Toast.makeText(context, e.localizedMessage ?: "Update failed", Toast.LENGTH_SHORT).show()
                                }
                        },
                        enabled = !isUpdating
                    ) {
                        Text(if (isUpdating) "Saving..." else "Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showEditDialog = false },
                        enabled = !isUpdating
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun StudentProfileCard(
    profile: AdminStudentProfile
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = profile.email,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Role: Student • Status: ${profile.status.replaceFirstChar { it.uppercase() }}",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                fontSize = 12.sp
            )

            profile.createdAt?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Registered: ${formatDate(it)}",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun StudentActivityStats(
    attempts: Int,
    passed: Int,
    failed: Int,
    average: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActivityStat(
            title = "Attempts",
            value = attempts.toString(),
            icon = Icons.Default.Quiz,
            modifier = Modifier.weight(1f)
        )
        ActivityStat(
            title = "Passed",
            value = passed.toString(),
            icon = Icons.Default.CheckCircle,
            modifier = Modifier.weight(1f)
        )
        ActivityStat(
            title = "Average",
            value = "${formatPercentage(average)}%",
            icon = Icons.Default.Assessment,
            modifier = Modifier.weight(1f)
        )
        ActivityStat(
            title = "Failed",
            value = failed.toString(),
            icon = Icons.Default.Close,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActivityStat(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(11.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun AttemptCard(
    attempt: AdminStudentAttempt
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (attempt.passed) {
                    Icons.Default.CheckCircle
                } else {
                    Icons.Default.Close
                },
                contentDescription = null,
                tint = if (attempt.passed) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(25.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = attempt.quizTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Score: ${formatNumber(attempt.score)} / ${formatNumber(attempt.totalMarks)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                attempt.submittedAt?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatDate(it),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatPercentage(attempt.percentage)}%",
                    color = if (attempt.passed) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (attempt.passed) "Passed" else "Failed",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun formatDate(timestamp: Timestamp): String {
    return SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    ).format(timestamp.toDate())
}

private fun formatPercentage(value: Double): String {
    return String.format(Locale.getDefault(), "%.1f", value)
}

private fun formatNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}
