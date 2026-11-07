package com.quizmaster.app.ui.admin

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class AttendanceStudent(
    val id: String,
    val name: String,
    val email: String,
    val status: String,
    val currentQuizTitle: String,
    val lastSeenMillis: Long?
)

@Composable
fun AttendanceScreen(
    onBack: () -> Unit
) {
    val firestore = remember { FirebaseFirestore.getInstance() }

    var students by remember { mutableStateOf<List<AttendanceStudent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        isLoading = true
        errorMessage = ""
    }

    DisposableEffect(firestore, refreshKey) {
        val listener: ListenerRegistration =
            firestore.collection("attendance")
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        errorMessage = exception.localizedMessage
                            ?: "Unable to load attendance."
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        errorMessage = "Unable to load attendance."
                        isLoading = false
                        return@addSnapshotListener
                    }

                    students = snapshot.documents
                        .map { document -> document.toAttendanceStudent() }
                        .sortedWith(
                            compareBy<AttendanceStudent> {
                                statusOrder(it.status)
                            }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                        )

                    isLoading = false
                }

        onDispose {
            listener.remove()
        }
    }

    val activeCount = students.count { it.status == "active" }
    val attemptingCount = students.count { it.status == "attempting" }
    val inactiveCount = students.count { it.status == "inactive" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Attendance",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Live student activity",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = { refreshKey++ }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh attendance",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AttendanceSummaryCard(
                    total = students.size,
                    active = activeCount,
                    attempting = attemptingCount,
                    inactive = inactiveCount
                )
            }

            when {
                isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                errorMessage.isNotBlank() -> {
                    item {
                        AttendanceError(
                            message = errorMessage,
                            onRetry = {
                                refreshKey++
                            }
                        )
                    }
                }

                students.isEmpty() -> {
                    item {
                        AttendanceEmptyState()
                    }
                }

                else -> {
                    item {
                        Text(
                            text = "Student activity",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                top = 4.dp,
                                bottom = 2.dp
                            )
                        )
                    }

                    items(
                        items = students,
                        key = { it.id }
                    ) { student ->
                        AttendanceStudentCard(student = student)
                    }
                }
            }
        }
    }
}

private fun DocumentSnapshot.toAttendanceStudent(): AttendanceStudent {
    val rawStatus = getString("status")
        ?.trim()
        ?.lowercase()

    val status = when (rawStatus) {
        "active" -> "active"
        "attempting" -> "attempting"
        else -> "inactive"
    }

    val name = getString("studentName")
        ?.trim()
        ?.ifBlank { "Unnamed Student" }
        ?: "Unnamed Student"

    val email = getString("email")
        ?.trim()
        ?.ifBlank { "" }
        ?: ""

    val quizTitle = getString("currentQuizTitle")
        ?.trim()
        ?.ifBlank { "" }
        ?: ""

    val lastSeen = getTimestamp("lastSeen")?.toDate()?.time
        ?: getTimestamp("updatedAt")?.toDate()?.time

    return AttendanceStudent(
        id = id,
        name = name,
        email = email,
        status = status,
        currentQuizTitle = quizTitle,
        lastSeenMillis = lastSeen
    )
}

private fun statusOrder(status: String): Int {
    return when (status) {
        "attempting" -> 0
        "active" -> 1
        else -> 2
    }
}

@Composable
private fun AttendanceSummaryCard(
    total: Int,
    active: Int,
    attempting: Int,
    inactive: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Live overview",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "$total registered students tracked",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttendanceCount(
                    label = "Active",
                    count = active,
                    color = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )

                AttendanceCount(
                    label = "Attempting",
                    count = attempting,
                    color = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f)
                )

                AttendanceCount(
                    label = "Inactive",
                    count = inactive,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AttendanceCount(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 12.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AttendanceStudentCard(
    student: AttendanceStudent
) {
    val statusColor = when (student.status) {
        "attempting" -> Color(0xFF2563EB)
        "active" -> Color(0xFF16A34A)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusLabel = when (student.status) {
        "attempting" -> "Quiz Attempting"
        "active" -> "Active"
        else -> "Inactive"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.name
                        .firstOrNull()
                        ?.uppercase()
                        ?: "S",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

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

                if (student.email.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = student.email,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (
                    student.status == "attempting" &&
                    student.currentQuizTitle.isNotBlank()
                ) {
                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Quiz: ${student.currentQuizTitle}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                student.lastSeenMillis?.let { lastSeen ->
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Last seen: ${formatLastSeen(lastSeen)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun formatLastSeen(timeMillis: Long): String {
    return SimpleDateFormat(
        "dd MMM, hh:mm a",
        Locale.getDefault()
    ).format(Date(timeMillis))
}

@Composable
private fun AttendanceError(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Unable to load attendance",
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

            OutlinedButton(
                onClick = onRetry
            ) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun AttendanceEmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
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
                text = "No attendance data yet",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Student activity will appear here after students log in.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}
