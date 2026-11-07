package com.quizmaster.app.ui.teacher

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quizmaster.app.data.model.Subject

private data class TeacherQuiz(
    val quizId: String = "",
    val title: String = "",
    val category: String = "",
    val difficulty: String = "Medium",
    val duration: Int = 10,
    val status: String = "draft"
)

@Composable
fun TeacherDashboardScreen(
    onLogout: () -> Unit,
    onCreateQuiz: () -> Unit,
    onManageQuizzes: () -> Unit,
    onManageQuestions: () -> Unit,
    onViewResults: () -> Unit
) {
    BackHandler {
        // Prevent accidental exit
    }

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    val currentUser = auth.currentUser

    var teacherName by rememberSaveable { mutableStateOf("Teacher") }
    var assignedSubjectId by rememberSaveable { mutableStateOf("") }
    var assignedSubject by remember { mutableStateOf<Subject?>(null) }
    var quizzes by remember { mutableStateOf<List<TeacherQuiz>>(emptyList()) }
    var totalQuestions by rememberSaveable { mutableStateOf(0) }
    var totalAttempts by rememberSaveable { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect

        // Load Teacher Profile
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    teacherName = doc.getString("name")?.ifBlank { "Teacher" } ?: "Teacher"
                    assignedSubjectId = doc.getString("assignedSubjectId") ?: ""

                    if (assignedSubjectId.isNotBlank()) {
                        db.collection("subjects").document(assignedSubjectId).get()
                            .addOnSuccessListener { subDoc ->
                                if (subDoc.exists()) {
                                    assignedSubject = Subject(
                                        subjectId = subDoc.id,
                                        name = subDoc.getString("name") ?: "",
                                        code = subDoc.getString("code") ?: "",
                                        description = subDoc.getString("description") ?: "",
                                        status = subDoc.getString("status") ?: "active"
                                    )
                                }
                            }
                    }
                }

                // Load Teacher's Quizzes (owned by this teacher)
                db.collection("quizzes")
                    .whereEqualTo("createdBy", uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        quizzes = snapshot.documents.mapNotNull { d ->
                            TeacherQuiz(
                                quizId = d.id,
                                title = d.getString("title") ?: "",
                                category = d.getString("category") ?: "General",
                                difficulty = d.getString("difficulty") ?: "Medium",
                                duration = (d.getLong("duration") ?: 10L).toInt(),
                                status = d.getString("status") ?: "draft"
                            )
                        }
                        isLoading = false
                    }
                    .addOnFailureListener {
                        isLoading = false
                    }

                // Load Questions Count for this teacher
                db.collection("questions")
                    .whereEqualTo("createdBy", uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        totalQuestions = snapshot.size()
                    }

                // Load Attempts Count for this teacher's quizzes
                db.collection("attempts")
                    .whereEqualTo("teacherId", uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        totalAttempts = snapshot.size()
                    }
                    .addOnFailureListener {
                        totalAttempts = 0
                    }
            }
    }

    val hasValidAssignment = assignedSubjectId.isNotBlank() && assignedSubject != null && assignedSubject!!.isActive

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            TeacherBottomNavigation(
                modifier = Modifier.navigationBarsPadding(),
                onDashboard = {},
                onQuizzes = onManageQuizzes,
                onQuestions = onManageQuestions,
                onResults = onViewResults
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TeacherHeader(
                    name = teacherName,
                    onLogout = onLogout
                )
            }

            item {
                TeacherWelcomeCard(
                    assignedSubject = assignedSubject
                )
            }

            if (!hasValidAssignment && !isLoading) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Subject Assignment Required",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You have not been assigned an active academic subject by the administrator. Quiz creation is currently locked until your subject is assigned.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            item {
                TeacherStatsGrid(
                    quizCount = quizzes.size,
                    questionCount = totalQuestions,
                    attemptCount = totalAttempts
                )
            }

            item {
                Text(
                    text = "Instructor Actions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TeacherActionCard(
                        title = "Create New Quiz",
                        subtitle = if (hasValidAssignment) "Build a test for ${assignedSubject?.name}" else "Locked (No subject assigned)",
                        icon = Icons.Default.Add,
                        enabled = hasValidAssignment,
                        onClick = onCreateQuiz
                    )

                    TeacherActionCard(
                        title = "Manage My Quizzes",
                        subtitle = "Publish, draft, or edit your assigned subject quizzes",
                        icon = Icons.Default.Quiz,
                        onClick = onManageQuizzes
                    )

                    TeacherActionCard(
                        title = "Question Bank",
                        subtitle = "Add & edit questions in your subject pool",
                        icon = Icons.Default.Book,
                        onClick = onManageQuestions
                    )

                    TeacherActionCard(
                        title = "Student Results & Performance",
                        subtitle = "View student scores and test analysis",
                        icon = Icons.Default.Leaderboard,
                        onClick = onViewResults
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Subject Quizzes",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(onClick = onManageQuizzes) {
                        Text("View all", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (quizzes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No quizzes created yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Create New Quiz' to make your first test.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(quizzes.take(4)) { quiz ->
                    TeacherQuizItemCard(
                        quiz = quiz,
                        onEdit = onManageQuizzes
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// =========================================================
// HEADER
// =========================================================

@Composable
private fun TeacherHeader(
    name: String,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Instructor Portal",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Prof. $name",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        TextButton(onClick = onLogout) {
            Text("Logout", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

// =========================================================
// WELCOME CARD WITH EXACT ASSIGNED SUBJECT
// =========================================================

@Composable
private fun TeacherWelcomeCard(
    assignedSubject: Subject?
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = gradientBrush)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Exclusive Subject Workspace",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Assigned Teaching Subject:",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (assignedSubject != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${assignedSubject.name} (${assignedSubject.code})",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No subject assigned yet.",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// =========================================================
// STATS GRID
// =========================================================

@Composable
private fun TeacherStatsGrid(
    quizCount: Int,
    questionCount: Int,
    attemptCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TeacherStatCard("My Quizzes", quizCount.toString(), Modifier.weight(1f))
        TeacherStatCard("Questions", questionCount.toString(), Modifier.weight(1f))
        TeacherStatCard("Test Attempts", attemptCount.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun TeacherStatCard(title: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// =========================================================
// ACTION CARD
// =========================================================

@Composable
private fun TeacherActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// =========================================================
// QUIZ ITEM CARD
// =========================================================

@Composable
private fun TeacherQuizItemCard(
    quiz: TeacherQuiz,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(quiz.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${quiz.category} • ${quiz.duration} min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// =========================================================
// BOTTOM NAVIGATION
// =========================================================

@Composable
private fun TeacherBottomNavigation(
    modifier: Modifier = Modifier,
    onDashboard: () -> Unit,
    onQuizzes: () -> Unit,
    onQuestions: () -> Unit,
    onResults: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TeacherNavItem("Dashboard", Icons.Default.Dashboard, active = true, onClick = onDashboard)
            TeacherNavItem("Quizzes", Icons.Default.Quiz, active = false, onClick = onQuizzes)
            TeacherNavItem("Questions", Icons.Default.Book, active = false, onClick = onQuestions)
            TeacherNavItem("Results", Icons.Default.Leaderboard, active = false, onClick = onResults)
        }
    }
}

@Composable
private fun TeacherNavItem(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .background(containerColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
        )
    }
}
