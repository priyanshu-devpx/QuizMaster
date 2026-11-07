package com.quizmaster.app.ui.student

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// =========================================================
// COLORS
// =========================================================

private val QuizSuccess = Color(0xFF10B981)
private val QuizWarning = Color(0xFFF59E0B)

// =========================================================
// QUIZ MODEL
// =========================================================

private data class QuizItem(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val difficulty: String,
    val duration: Long,
    val passingScore: Long
)

// =========================================================
// STUDENT HOME
// =========================================================

@Composable
fun StudentHomeScreen(
    onLogout: () -> Unit,
    onQuizzesClick: () -> Unit,
    onResultsClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onQuizSelected: (String) -> Unit = {}
) {

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val currentUser = auth.currentUser

    var studentName by rememberSaveable {
        mutableStateOf("Student")
    }

    var studentEmail by rememberSaveable {
        mutableStateOf("")
    }

    var quizzes by remember {
        mutableStateOf<List<QuizItem>>(emptyList())
    }

    // =====================================================
    // PERFORMANCE STATS
    // =====================================================

    var attemptsCount by rememberSaveable {
        mutableStateOf(0)
    }

    var averageScore by rememberSaveable {
        mutableStateOf(0.0)
    }

    var passedCount by rememberSaveable {
        mutableStateOf(0)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var searchText by rememberSaveable {
        mutableStateOf("")
    }

    var selectedCategory by rememberSaveable {
        mutableStateOf("All")
    }

    var selectedDifficulty by rememberSaveable {
        mutableStateOf("All")
    }

    // =====================================================
    // LOAD USER + QUIZZES + PERFORMANCE
    // =====================================================

    LaunchedEffect(currentUser?.uid) {

        val userId = currentUser?.uid

        if (userId == null) {
            errorMessage = "Your session has expired. Please login again."
            isLoading = false
            return@LaunchedEffect
        }

        // -------------------------------------------------
        // LOAD STUDENT PROFILE
        // -------------------------------------------------

        firestore
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    studentName =
                        document.getString("name")
                            ?.trim()
                            ?.ifBlank { "Student" }
                            ?: "Student"

                    studentEmail =
                        document.getString("email")
                            ?.trim()
                            ?: currentUser.email.orEmpty()

                } else {

                    studentName = "Student"
                    studentEmail = currentUser.email.orEmpty()
                }
            }

        // -------------------------------------------------
        // LOAD PUBLISHED QUIZZES
        // -------------------------------------------------

        firestore
            .collection("quizzes")
            .whereEqualTo("status", "published")
            .get()
            .addOnSuccessListener { result ->

                quizzes = result.documents.mapNotNull { document ->

                    val title =
                        document.getString("title")
                            ?.trim()
                            ?: return@mapNotNull null

                    QuizItem(
                        id = document.id,

                        title = title,

                        description =
                            document.getString("description")
                                ?.trim()
                                ?: "Test your knowledge with this quiz.",

                        category =
                            document.getString("category")
                                ?.trim()
                                ?.ifBlank { "General" }
                                ?: "General",

                        difficulty =
                            document.getString("difficulty")
                                ?.trim()
                                ?.ifBlank { "Medium" }
                                ?: "Medium",

                        duration =
                            document.getLong("duration")
                                ?: 10L,

                        passingScore =
                            document.getLong("passingScore")
                                ?: 40L
                    )
                }

                isLoading = false
                errorMessage = ""
            }
            .addOnFailureListener { exception ->

                isLoading = false

                errorMessage =
                    exception.localizedMessage
                        ?: "Unable to load quizzes."
            }

        // -------------------------------------------------
        // LOAD STUDENT PERFORMANCE
        // -------------------------------------------------

        firestore
            .collection("attempts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->

                val attempts = result.documents

                attemptsCount = attempts.size

                passedCount = attempts.count { document ->
                    document.getBoolean("passed") == true
                }

                val percentages =
                    attempts.mapNotNull { document ->

                        val percentage =
                            document.get("percentage")

                        when (percentage) {
                            is Number ->
                                percentage.toDouble()

                            else -> null
                        }
                    }

                averageScore =
                    if (percentages.isNotEmpty()) {
                        percentages.average()
                    } else {
                        0.0
                    }
            }
            .addOnFailureListener {

                attemptsCount = 0
                averageScore = 0.0
                passedCount = 0
            }
    }

    // =====================================================
    // FILTER QUIZZES
    // =====================================================

    val categories =
        listOf("All") +
                quizzes
                    .map { it.category }
                    .distinct()
                    .sorted()

    val difficulties =
        listOf("All", "Easy", "Medium", "Hard")

    val filteredQuizzes =
        quizzes.filter { quiz ->

            val matchesSearch =
                searchText.isBlank() ||
                        quiz.title.contains(
                            searchText,
                            ignoreCase = true
                        ) ||
                        quiz.description.contains(
                            searchText,
                            ignoreCase = true
                        ) ||
                        quiz.category.contains(
                            searchText,
                            ignoreCase = true
                        )

            val matchesCategory =
                selectedCategory == "All" ||
                        quiz.category.equals(
                            selectedCategory,
                            ignoreCase = true
                        )

            val matchesDifficulty =
                selectedDifficulty == "All" ||
                        quiz.difficulty.equals(
                            selectedDifficulty,
                            ignoreCase = true
                        )

            matchesSearch &&
                    matchesCategory &&
                    matchesDifficulty
        }

    // =====================================================
    // SCAFFOLD
    // =====================================================

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,

        bottomBar = {
            StudentBottomNavigation(
                modifier = Modifier.navigationBarsPadding(),
                onQuizzesClick = onQuizzesClick,
                onResultsClick = onResultsClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            // =================================================
            // HEADER
            // =================================================

            item {

                StudentHeader(
                    name = studentName,
                    email = studentEmail,
                    onLogout = onLogout
                )
            }

            // =================================================
            // WELCOME CARD
            // =================================================

            item {

                WelcomeCard(
                    name = studentName
                )
            }

            // =================================================
            // PERFORMANCE
            // =================================================

            item {

                SectionTitle(
                    title = "Your Performance",
                    actionText = "View all",
                    onActionClick = onResultsClick
                )
            }

            item {

                PerformanceCards(
                    attempts = attemptsCount,
                    averageScore = averageScore,
                    passed = passedCount
                )
            }

            // =================================================
            // SEARCH
            // =================================================

            item {

                OutlinedTextField(
                    value = searchText,

                    onValueChange = {
                        searchText = it
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),

                    singleLine = true,

                    placeholder = {
                        Text(
                            text = "Search quizzes...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },

                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },

                    shape = RoundedCornerShape(12.dp),

                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor =
                                MaterialTheme.colorScheme.onSurface,

                            unfocusedTextColor =
                                MaterialTheme.colorScheme.onSurface,

                            cursorColor =
                                MaterialTheme.colorScheme.primary,

                            focusedBorderColor =
                                MaterialTheme.colorScheme.primary,

                            unfocusedBorderColor =
                                MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }

            // =================================================
            // CATEGORY FILTER
            // =================================================

            item {

                FilterSection(
                    title = "Categories",
                    options = categories,
                    selected = selectedCategory,
                    onSelected = {
                        selectedCategory = it
                    }
                )
            }

            // =================================================
            // DIFFICULTY FILTER
            // =================================================

            item {

                FilterSection(
                    title = "Difficulty",
                    options = difficulties,
                    selected = selectedDifficulty,
                    onSelected = {
                        selectedDifficulty = it
                    }
                )
            }

            // =================================================
            // AVAILABLE QUIZZES
            // =================================================

            item {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = "Available Quizzes",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,

                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${filteredQuizzes.size} available",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            // =================================================
            // LOADING
            // =================================================

            if (isLoading) {

                item {
                    LoadingQuizState()
                }

            }

            // =================================================
            // ERROR
            // =================================================

            else if (errorMessage.isNotBlank()) {

                item {

                    ErrorQuizState(
                        message = errorMessage
                    )
                }

            }

            // =================================================
            // EMPTY
            // =================================================

            else if (filteredQuizzes.isEmpty()) {

                item {

                    EmptyQuizState(
                        hasSearch =
                            searchText.isNotBlank() ||
                                    selectedCategory != "All" ||
                                    selectedDifficulty != "All"
                    )
                }

            }

            // =================================================
            // QUIZ LIST
            // =================================================

            else {

                items(
                    items = filteredQuizzes,
                    key = { it.id }
                ) { quiz ->

                    QuizCard(
                        quiz = quiz,

                        onStartQuiz = {
                            onQuizSelected(quiz.id)
                        }
                    )
                }
            }

            item {

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }
        }
    }
}

// =========================================================
// HEADER
// =========================================================

@Composable
private fun StudentHeader(
    name: String,
    email: String,
    onLogout: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 10.dp
            ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = "Hello,",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
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
                .background(MaterialTheme.colorScheme.primary),

            contentAlignment = Alignment.Center
        ) {

            Text(
                text = name
                    .firstOrNull()
                    ?.uppercase()
                    ?: "S",

                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.width(6.dp)
        )

        TextButton(
            onClick = onLogout
        ) {

            Text(
                text = "Logout",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// =========================================================
// WELCOME CARD (Enhanced Gradient)
// =========================================================

@Composable
private fun WelcomeCard(
    name: String
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

        colors =
            CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = gradientBrush)
                .padding(22.dp)
        ) {
            Column {
                Text(
                    text = "Ready for your next challenge?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text =
                        "Choose a quiz below and test your knowledge.",

                    color =
                        MaterialTheme.colorScheme.onPrimary
                            .copy(alpha = 0.85f),

                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    text = "Good luck, ${name.trim()}!",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// =========================================================
// SECTION TITLE
// =========================================================

@Composable
private fun SectionTitle(
    title: String,
    actionText: String,
    onActionClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,

            modifier = Modifier.weight(1f)
        )

        Text(
            text = actionText,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,

            modifier = Modifier.clickable(
                onClick = onActionClick
            )
        )
    }
}

// =========================================================
// PERFORMANCE CARDS
// =========================================================

@Composable
private fun PerformanceCards(
    attempts: Int,
    averageScore: Double,
    passed: Int
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),

        horizontalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        PerformanceCard(
            title = "Attempts",
            value = attempts.toString(),
            modifier = Modifier.weight(1f)
        )

        PerformanceCard(
            title = "Avg. Score",
            value = "${averageScore.toInt()}%",
            modifier = Modifier.weight(1f)
        )

        PerformanceCard(
            title = "Passed",
            value = passed.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

// =========================================================
// PERFORMANCE CARD
// =========================================================

@Composable
private fun PerformanceCard(
    title: String,
    value: String,
    modifier: Modifier
) {

    Card(
        modifier = modifier,

        shape = RoundedCornerShape(16.dp),

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

// =========================================================
// FILTER SECTION
// =========================================================

@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {

    Column {

        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,

            modifier = Modifier.padding(
                horizontal = 20.dp
            )
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        LazyRow(
            contentPadding =
                PaddingValues(
                    horizontal = 20.dp
                ),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            items(options) { option ->

                FilterChip(
                    text = option,
                    selected = option == selected,
                    onClick = {
                        onSelected(option)
                    }
                )
            }
        }
    }
}

// =========================================================
// FILTER CHIP
// =========================================================

@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .clickable(
                onClick = onClick
            )
            .padding(
                horizontal = 15.dp,
                vertical = 9.dp
            )
    ) {

        Text(
            text = text,

            color =
                if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },

            fontSize = 12.sp,

            fontWeight =
                if (selected) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                }
        )
    }
}

// =========================================================
// QUIZ CARD
// =========================================================

@Composable
private fun QuizCard(
    quiz: QuizItem,
    onStartQuiz: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),

        shape = RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.Top
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = quiz.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = quiz.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                DifficultyBadge(
                    difficulty = quiz.difficulty
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = quiz.category,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Text(
                    text = " • ",
                    color = MaterialTheme.colorScheme.outlineVariant,
                    fontSize = 13.sp
                )

                Text(
                    text = "${quiz.duration} min",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Text(
                    text = " • ",
                    color = MaterialTheme.colorScheme.outlineVariant,
                    fontSize = 13.sp
                )

                Text(
                    text = "Pass ${quiz.passingScore}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Button(
                onClick = onStartQuiz,

                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),

                shape = RoundedCornerShape(12.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            MaterialTheme.colorScheme.primary,

                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
            ) {

                Text(
                    text = "Start Quiz",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// =========================================================
// DIFFICULTY BADGE
// =========================================================

@Composable
private fun DifficultyBadge(
    difficulty: String
) {

    val normalized =
        difficulty.lowercase()

    val badgeColor =
        when (normalized) {
            "easy" -> QuizSuccess
            "hard" -> MaterialTheme.colorScheme.error
            else -> QuizWarning
        }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                badgeColor.copy(alpha = 0.12f)
            )
            .padding(
                horizontal = 9.dp,
                vertical = 5.dp
            )
    ) {

        Text(
            text = difficulty,
            color = badgeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// =========================================================
// LOADING STATE
// =========================================================

@Composable
private fun LoadingQuizState() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(30.dp),

        contentAlignment =
            Alignment.Center
    ) {

        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
    }
}

// =========================================================
// ERROR STATE
// =========================================================

@Composable
private fun ErrorQuizState(
    message: String
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),

        shape = RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.errorContainer
            )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = "Unable to load quizzes",
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

// =========================================================
// EMPTY STATE
// =========================================================

@Composable
private fun EmptyQuizState(
    hasSearch: Boolean
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),

        shape = RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    if (hasSearch) {
                        "No matching quizzes"
                    } else {
                        "No quizzes available"
                    },

                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text =
                    if (hasSearch) {
                        "Try changing your search or filters."
                    } else {
                        "Published quizzes will appear here."
                    },

                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

// =========================================================
// BOTTOM NAVIGATION
// =========================================================

@Composable
private fun StudentBottomNavigation(
    modifier: Modifier = Modifier,
    onQuizzesClick: () -> Unit,
    onResultsClick: () -> Unit,
    onProfileClick: () -> Unit
) {

    Card(
        modifier = modifier
            .fillMaxWidth(),

        shape = RoundedCornerShape(
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
                defaultElevation = 4.dp
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 8.dp
                ),

            horizontalArrangement =
                Arrangement.SpaceEvenly
        ) {

            BottomNavItem(
                label = "Home",
                active = true
            )

            BottomNavItem(
                label = "Quizzes",
                active = false,
                onClick = onQuizzesClick
            )

            BottomNavItem(
                label = "Results",
                active = false,
                onClick = onResultsClick
            )

            BottomNavItem(
                label = "Profile",
                active = false,
                onClick = onProfileClick
            )
        }
    }
}

// =========================================================
// BOTTOM NAV ITEM (Modern M3 Pill Indicator)
// =========================================================

@Composable
private fun BottomNavItem(
    label: String,
    active: Boolean,
    onClick: () -> Unit = {}
) {

    val icon = when (label) {
        "Home" -> Icons.Default.Home
        "Quizzes" -> Icons.Default.Quiz
        "Results" -> Icons.Default.Assessment
        else -> Icons.Default.Person
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
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = 12.dp,
                vertical = 4.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(width = 60.dp, height = 32.dp)
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
            modifier = Modifier.height(3.dp)
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
