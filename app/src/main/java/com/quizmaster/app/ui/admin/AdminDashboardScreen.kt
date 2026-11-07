package com.quizmaster.app.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// =====================================================================
// ADMIN DASHBOARD
// =====================================================================

@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    onCreateQuiz: () -> Unit = {},
    onManageQuizzes: () -> Unit = {},
    onManageQuestions: () -> Unit = {},
    onManageSubjects: () -> Unit = {},
    onManageTeachers: () -> Unit = {},
    onViewStudents: () -> Unit = {},
    onViewResults: () -> Unit = {},
    onAttendanceClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {

    // -----------------------------------------------------------------
    // FIREBASE
    // -----------------------------------------------------------------

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val currentUser = auth.currentUser

    // -----------------------------------------------------------------
    // STATES
    // -----------------------------------------------------------------

    var adminName by rememberSaveable {
        mutableStateOf("Admin")
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var totalQuizzes by remember {
        mutableIntStateOf(0)
    }

    var publishedQuizzes by remember {
        mutableIntStateOf(0)
    }

    var totalStudents by remember {
        mutableIntStateOf(0)
    }

    var totalTeachers by remember {
        mutableIntStateOf(0)
    }

    var totalSubjects by remember {
        mutableIntStateOf(0)
    }

    var totalAttempts by remember {
        mutableIntStateOf(0)
    }

    var reloadKey by remember {
        mutableIntStateOf(0)
    }

    // -----------------------------------------------------------------
    // LOAD DASHBOARD DATA
    // -----------------------------------------------------------------

    LaunchedEffect(currentUser?.uid, reloadKey) {

        val userId = currentUser?.uid

        if (userId == null) {

            errorMessage =
                "Your session has expired. Please login again."

            isLoading = false

            return@LaunchedEffect
        }

        // -------------------------------------------------------------
        // ADMIN PROFILE
        // -------------------------------------------------------------

        firestore
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val role =
                        document
                            .getString("role")
                            ?.trim()
                            ?.lowercase()

                    if (role != "admin") {

                        errorMessage =
                            "This account does not have administrator access."

                        return@addOnSuccessListener
                    }

                    adminName =
                        document
                            .getString("name")
                            ?.trim()
                            ?.ifBlank {
                                "Admin"
                            }
                            ?: "Admin"
                }
            }
            .addOnFailureListener { exception ->

                errorMessage =
                    exception.localizedMessage
                        ?: "Unable to load administrator profile."
            }

        // -------------------------------------------------------------
        // QUIZ COUNT
        // -------------------------------------------------------------

        firestore
            .collection("quizzes")
            .get()
            .addOnSuccessListener { result ->

                totalQuizzes = result.size()

                publishedQuizzes =
                    result.documents.count { document ->

                        document
                            .getString("status")
                            ?.trim()
                            ?.lowercase() == "published"
                    }
            }
            .addOnFailureListener { exception ->

                errorMessage =
                    exception.localizedMessage
                        ?: "Unable to load quiz statistics."
            }

        // -------------------------------------------------------------
        // STUDENT COUNT
        // -------------------------------------------------------------

        firestore
            .collection("users")
            .whereEqualTo(
                "role",
                "student"
            )
            .get()
            .addOnSuccessListener { result ->

                totalStudents = result.size()
            }
            .addOnFailureListener { exception ->

                errorMessage =
                    exception.localizedMessage
                        ?: "Unable to load student statistics."
            }

        // -------------------------------------------------------------
        // TEACHER COUNT
        // -------------------------------------------------------------

        firestore
            .collection("users")
            .whereEqualTo(
                "role",
                "teacher"
            )
            .get()
            .addOnSuccessListener { result ->

                totalTeachers = result.size()
            }

        // -------------------------------------------------------------
        // SUBJECT COUNT
        // -------------------------------------------------------------

        firestore
            .collection("subjects")
            .get()
            .addOnSuccessListener { result ->

                totalSubjects = result.size()
            }

        // -------------------------------------------------------------
        // ATTEMPT COUNT
        // -------------------------------------------------------------

        firestore
            .collection("attempts")
            .get()
            .addOnSuccessListener { result ->

                totalAttempts = result.size()

                isLoading = false
            }
            .addOnFailureListener { exception ->

                errorMessage =
                    exception.localizedMessage
                        ?: "Unable to load attempt statistics."

                isLoading = false
            }
    }

    // =================================================================
    // SCREEN
    // =================================================================

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,

        bottomBar = {
            AdminBottomNavigation(
                modifier = Modifier.navigationBarsPadding(),
                onDashboard = {},
                onQuizzes = onManageQuizzes,
                onStudents = onViewStudents,
                onResults = onViewResults
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),

            contentPadding = PaddingValues(
                top = 18.dp,
                bottom = 22.dp
            ),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            // =========================================================
            // HEADER
            // =========================================================

            item {

                AdminHeader(
                    name = adminName,
                    onLogout = onLogout,
                    onSettingsClick = onSettingsClick
                )
            }

            // =========================================================
            // WELCOME CARD
            // =========================================================

            item {

                AdminWelcomeCard(
                    name = adminName
                )
            }

            // =========================================================
            // OVERVIEW
            // =========================================================

            item {

                SectionTitle(
                    title = "Overview",
                    subtitle =
                        "Your QuizMaster activity at a glance."
                )
            }

            // =========================================================
            // STATS
            // =========================================================

            item {

                AdminStatsGrid(
                    totalQuizzes = totalQuizzes,
                    publishedQuizzes = publishedQuizzes,
                    totalStudents = totalStudents,
                    totalTeachers = totalTeachers,
                    totalSubjects = totalSubjects,
                    totalAttempts = totalAttempts,
                    isLoading = isLoading
                )
            }

            // =========================================================
            // ERROR
            // =========================================================

            if (errorMessage.isNotBlank()) {

                item {

                    AdminErrorCard(
                        message = errorMessage,
                        onRetry = {
                            errorMessage = ""
                            isLoading = true
                            reloadKey++
                        }
                    )
                }
            }

            // =========================================================
            // QUICK ACTIONS TITLE
            // =========================================================

            item {

                SectionTitle(
                    title = "Quick Actions",
                    subtitle =
                        "Common tasks you can access quickly."
                )
            }

            // =========================================================
            // MANAGE TEACHERS
            // =========================================================

            item {

                QuickActionCard(
                    title = "Manage Teachers",
                    description =
                        "Register instructors and assign academic subjects.",
                    iconType =
                        QuickActionIcon.Teachers,
                    onClick = onManageTeachers
                )
            }

            // =========================================================
            // STUDENTS
            // =========================================================

            item {

                QuickActionCard(
                    title = "View Students",
                    description =
                        "View registered students and their activity.",
                    iconType =
                        QuickActionIcon.Students,
                    onClick = onViewStudents
                )
            }

            // =========================================================
            // MANAGE SUBJECTS
            // =========================================================

            item {

                QuickActionCard(
                    title = "Manage Subjects",
                    description =
                        "Create, edit and manage academic subjects.",
                    iconType =
                        QuickActionIcon.Subjects,
                    onClick = onManageSubjects
                )
            }

            // =========================================================
            // ALL QUIZZES
            // =========================================================

            item {

                QuickActionCard(
                    title = "All Quizzes",
                    description =
                        "View, edit, publish or delete quizzes across all teachers.",
                    iconType =
                        QuickActionIcon.AllQuizzes,
                    onClick = onManageQuizzes
                )
            }

            // =========================================================
            // ATTENDANCE
            // =========================================================

            item {

                QuickActionCard(
                    title = "Attendance & Activity",
                    description =
                        "View active, attempting and inactive students.",
                    iconType =
                        QuickActionIcon.Attendance,
                    onClick = onAttendanceClick
                )
            }

            // =========================================================
            // RESULTS
            // =========================================================

            item {

                QuickActionCard(
                    title = "View Results",
                    description =
                        "Review attempts, scores and performance.",
                    iconType =
                        QuickActionIcon.Results,
                    onClick = onViewResults
                )
            }

            // =========================================================
            // PUBLISHING
            // =========================================================

            item {

                PublishingSummaryCard(
                    totalQuizzes = totalQuizzes,
                    publishedQuizzes = publishedQuizzes
                )
            }

            // =========================================================
            // LOGOUT
            // =========================================================

            item {

                OutlinedButton(
                    onClick = onLogout,

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(52.dp),

                    shape = RoundedCornerShape(14.dp),

                    colors =
                        androidx.compose.material3.ButtonDefaults
                            .outlinedButtonColors(
                                contentColor =
                                    MaterialTheme.colorScheme.error
                            ),

                    border =
                        BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error
                                .copy(alpha = 0.30f)
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text(
                        text = "Logout",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// =====================================================================
// SECTION TITLE
// =====================================================================

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {

        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(3.dp)
        )

        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

// =====================================================================
// ADMIN HEADER
// =====================================================================

@Composable
private fun AdminHeader(
    name: String,
    onLogout: () -> Unit,
    onSettingsClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = "Administrator",
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = name,
                color =
                    MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primary
                ),

            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text =
                    name
                        .firstOrNull()
                        ?.uppercase()
                        ?: "A",

                color =
                    MaterialTheme.colorScheme.onPrimary,

                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.width(4.dp)
        )

        IconButton(
            onClick = onSettingsClick
        ) {

            Icon(
                imageVector =
                    Icons.Default.Settings,

                contentDescription = "Settings",

                tint =
                    MaterialTheme.colorScheme.onSurfaceVariant,

                modifier =
                    Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = onLogout
        ) {

            Icon(
                imageVector =
                    Icons.AutoMirrored.Filled.Logout,

                contentDescription = "Logout",

                tint =
                    MaterialTheme.colorScheme.onSurfaceVariant,

                modifier =
                    Modifier.size(20.dp)
            )
        }
    }
}

// =====================================================================
// WELCOME CARD
// =====================================================================

@Composable
private fun AdminWelcomeCard(
    name: String
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),

        shape = RoundedCornerShape(20.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.primary
            )
    ) {

        Column(
            modifier = Modifier.padding(22.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Welcome back, $name!",

                        color =
                            MaterialTheme.colorScheme.onPrimary,

                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "Everything you need to manage your quizzes is right here.",

                        color =
                            MaterialTheme.colorScheme.onPrimary
                                .copy(alpha = 0.82f),

                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            color =
                                MaterialTheme.colorScheme.onPrimary
                                    .copy(alpha = 0.12f),

                            shape =
                                RoundedCornerShape(14.dp)
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Assessment,

                        contentDescription = null,

                        tint =
                            MaterialTheme.colorScheme.onPrimary,

                        modifier =
                            Modifier.size(26.dp)
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color =
                            MaterialTheme.colorScheme.onPrimary
                                .copy(alpha = 0.10f),

                        shape =
                            RoundedCornerShape(11.dp)
                    )
                    .padding(
                        horizontal = 13.dp,
                        vertical = 10.dp
                    ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Check,

                    contentDescription = null,

                    tint =
                        MaterialTheme.colorScheme.onPrimary,

                    modifier =
                        Modifier.size(17.dp)
                )

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Text(
                    text = "QuizMaster Administration",

                    color =
                        MaterialTheme.colorScheme.onPrimary,

                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// =====================================================================
// STATS GRID
// =====================================================================

@Composable
private fun AdminStatsGrid(
    totalQuizzes: Int,
    publishedQuizzes: Int,
    totalStudents: Int,
    totalTeachers: Int,
    totalSubjects: Int,
    totalAttempts: Int,
    isLoading: Boolean
) {

    Column(
        modifier =
            Modifier.padding(horizontal = 20.dp),

        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            AdminStatCard(
                title = "Total Quizzes",

                value =
                    if (isLoading) {
                        "..."
                    } else {
                        totalQuizzes.toString()
                    },

                iconType =
                    StatIcon.Quizzes,

                accentColor =
                    MaterialTheme.colorScheme.primary,

                modifier =
                    Modifier.weight(1f)
            )

            AdminStatCard(
                title = "Published",

                value =
                    if (isLoading) {
                        "..."
                    } else {
                        publishedQuizzes.toString()
                    },

                iconType =
                    StatIcon.Published,

                accentColor =
                    MaterialTheme.colorScheme.primary,

                modifier =
                    Modifier.weight(1f)
            )
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            AdminStatCard(
                title = "Teachers",

                value =
                    if (isLoading) {
                        "..."
                    } else {
                        totalTeachers.toString()
                    },

                iconType =
                    StatIcon.Teachers,

                accentColor =
                    MaterialTheme.colorScheme.primary,

                modifier =
                    Modifier.weight(1f)
            )

            AdminStatCard(
                title = "Students",

                value =
                    if (isLoading) {
                        "..."
                    } else {
                        totalStudents.toString()
                    },

                iconType =
                    StatIcon.Students,

                accentColor =
                    MaterialTheme.colorScheme.primary,

                modifier =
                    Modifier.weight(1f)
            )
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            AdminStatCard(
                title = "Subjects",

                value =
                    if (isLoading) {
                        "..."
                    } else {
                        totalSubjects.toString()
                    },

                iconType =
                    StatIcon.Subjects,

                accentColor =
                    MaterialTheme.colorScheme.primary,

                modifier =
                    Modifier.weight(1f)
            )

            AdminStatCard(
                title = "Attempts",

                value =
                    if (isLoading) {
                        "..."
                    } else {
                        totalAttempts.toString()
                    },

                iconType =
                    StatIcon.Attempts,

                accentColor =
                    MaterialTheme.colorScheme.primary,

                modifier =
                    Modifier.weight(1f)
            )
        }
    }
}

// =====================================================================
// STAT ICON
// =====================================================================

private enum class StatIcon {
    Quizzes,
    Published,
    Students,
    Attempts,
    Teachers,
    Subjects
}

// =====================================================================
// STAT CARD
// =====================================================================

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    iconType: StatIcon,
    accentColor: Color,
    modifier: Modifier
) {

    Card(
        modifier = modifier,

        shape =
            RoundedCornerShape(17.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(37.dp)
                        .clip(
                            RoundedCornerShape(10.dp)
                        )
                        .background(
                            accentColor.copy(alpha = 0.10f)
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    StatIconView(
                        iconType = iconType,
                        color = accentColor
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(13.dp)
            )

            Text(
                text = value,

                color =
                    MaterialTheme.colorScheme.onSurface,

                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(
                text = title,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,

                fontSize = 11.sp,

                maxLines = 1,

                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}

// =====================================================================
// STAT ICON VIEW
// =====================================================================

@Composable
private fun StatIconView(
    iconType: StatIcon,
    color: Color
) {

    val icon = when (iconType) {

        StatIcon.Quizzes ->
            Icons.Default.Quiz

        StatIcon.Published ->
            Icons.Default.Check

        StatIcon.Students ->
            Icons.Default.Group

        StatIcon.Attempts ->
            Icons.Default.Assessment

        StatIcon.Teachers ->
            Icons.Default.School

        StatIcon.Subjects ->
            Icons.Default.Book
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(20.dp)
    )
}

// =====================================================================
// QUICK ACTION ICON
// =====================================================================

private enum class QuickActionIcon {
    Teachers,
    Students,
    Subjects,
    AllQuizzes,
    Attendance,
    Results
}

// =====================================================================
// QUICK ACTION CARD
// =====================================================================

@Composable
private fun QuickActionCard(
    title: String,
    description: String,
    iconType: QuickActionIcon,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(
                onClick = onClick
            ),

        shape =
            RoundedCornerShape(17.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(17.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(
                        RoundedCornerShape(13.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme.primaryContainer
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                QuickActionIconView(
                    iconType = iconType
                )
            }

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = title,

                    color =
                        MaterialTheme.colorScheme.onSurface,

                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text = description,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,

                    fontSize = 11.sp,
                    lineHeight = 16.sp,

                    maxLines = 2,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// =====================================================================
// QUICK ACTION ICON VIEW
// =====================================================================

@Composable
private fun QuickActionIconView(
    iconType: QuickActionIcon
) {

    val icon = when (iconType) {

        QuickActionIcon.Teachers ->
            Icons.Default.School

        QuickActionIcon.Students ->
            Icons.Default.Group

        QuickActionIcon.Subjects ->
            Icons.Default.Book

        QuickActionIcon.AllQuizzes ->
            Icons.Default.Quiz

        QuickActionIcon.Attendance ->
            Icons.Default.Person

        QuickActionIcon.Results ->
            Icons.Default.Leaderboard
    }

    Icon(
        imageVector = icon,
        contentDescription = null,

        tint =
            MaterialTheme.colorScheme.primary,

        modifier =
            Modifier.size(23.dp)
    )
}

// =====================================================================
// PUBLISHING SUMMARY
// =====================================================================

@Composable
private fun PublishingSummaryCard(
    totalQuizzes: Int,
    publishedQuizzes: Int
) {

    val draftQuizzes =
        (
                totalQuizzes -
                        publishedQuizzes
                )
            .coerceAtLeast(0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
    ) {

        Column(
            modifier =
                Modifier.padding(18.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "Quiz Publishing",

                        color =
                            MaterialTheme.colorScheme.onSurface,

                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(3.dp)
                    )

                    Text(
                        text =
                            "Current quiz publishing status",

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,

                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(
                            RoundedCornerShape(10.dp)
                        )
                        .background(
                            MaterialTheme.colorScheme.primaryContainer
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Assessment,

                        contentDescription = null,

                        tint =
                            MaterialTheme.colorScheme.primary,

                        modifier =
                            Modifier.size(20.dp)
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(17.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                PublishingItem(
                    label = "Published",
                    value = publishedQuizzes,
                    color =
                        MaterialTheme.colorScheme.primary
                )

                PublishingItem(
                    label = "Draft",
                    value = draftQuizzes,
                    color =
                        MaterialTheme.colorScheme.tertiary
                )

                PublishingItem(
                    label = "Total",
                    value = totalQuizzes,
                    color =
                        MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(
                modifier =
                    Modifier.height(15.dp)
            )

            HorizontalDivider(
                color =
                    MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text =
                    if (totalQuizzes == 0) {
                        "Create your first quiz to get started."
                    } else {
                        "$publishedQuizzes of $totalQuizzes quizzes are currently published."
                    },

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,

                fontSize = 11.sp
            )
        }
    }
}

// =====================================================================
// PUBLISHING ITEM
// =====================================================================

@Composable
private fun PublishingItem(
    label: String,
    value: Int,
    color: Color
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = value.toString(),

            color = color,

            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(3.dp)
        )

        Text(
            text = label,

            color =
                MaterialTheme.colorScheme.onSurfaceVariant,

            fontSize = 10.sp
        )
    }
}

// =====================================================================
// ERROR CARD
// =====================================================================

@Composable
private fun AdminErrorCard(
    message: String,
    onRetry: () -> Unit = {}
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),

        shape =
            RoundedCornerShape(15.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.error
                        .copy(alpha = 0.06f)
            ),

        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.error
                    .copy(alpha = 0.15f)
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(15.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(
                        RoundedCornerShape(9.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme.error
                            .copy(alpha = 0.10f)
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "!",

                    color =
                        MaterialTheme.colorScheme.error,

                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = "Dashboard data issue",

                    color =
                        MaterialTheme.colorScheme.error,

                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(
                    text = message,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,

                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                TextButton(
                    onClick = onRetry
                ) {
                    Text(
                        text = "Try Again",
                        color =
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// =====================================================================
// ADMIN BOTTOM NAVIGATION
// =====================================================================

@Composable
private fun AdminBottomNavigation(
    modifier: Modifier = Modifier,
    onDashboard: () -> Unit,
    onQuizzes: () -> Unit,
    onStudents: () -> Unit,
    onResults: () -> Unit
) {

    Card(
        modifier =
            modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 5.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 5.dp,
                        vertical = 7.dp
                    ),

            horizontalArrangement =
                Arrangement.SpaceEvenly
        ) {

            AdminBottomItem(
                label = "Dashboard",
                iconType =
                    BottomIcon.Dashboard,
                active = true,
                onClick = onDashboard
            )

            AdminBottomItem(
                label = "Quizzes",
                iconType =
                    BottomIcon.Quizzes,
                active = false,
                onClick = onQuizzes
            )

            AdminBottomItem(
                label = "Students",
                iconType =
                    BottomIcon.Students,
                active = false,
                onClick = onStudents
            )

            AdminBottomItem(
                label = "Results",
                iconType =
                    BottomIcon.Results,
                active = false,
                onClick = onResults
            )
        }
    }
}

// =====================================================================
// BOTTOM ICON
// =====================================================================

private enum class BottomIcon {
    Dashboard,
    Quizzes,
    Students,
    Results
}

// =====================================================================
// BOTTOM NAV ITEM
// =====================================================================

@Composable
private fun AdminBottomItem(
    label: String,
    iconType: BottomIcon,
    active: Boolean,
    onClick: () -> Unit
) {

    val icon = when (iconType) {
        BottomIcon.Dashboard ->
            Icons.Default.Dashboard

        BottomIcon.Quizzes ->
            Icons.Default.Quiz

        BottomIcon.Students ->
            Icons.Default.Group

        BottomIcon.Results ->
            Icons.Default.Leaderboard
    }

    val containerColor = if (active) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    val contentColor = if (active) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .clip(
                RoundedCornerShape(16.dp)
            )
            .clickable(
                onClick = onClick
            )
            .padding(
                horizontal = 10.dp,
                vertical = 4.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 32.dp)
                .background(
                    color = containerColor,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(
            modifier =
                Modifier.height(3.dp)
        )

        Text(
            text = label,

            color =
                if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },

            fontSize = 11.sp,

            fontWeight =
                if (active) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                }
        )
    }
}