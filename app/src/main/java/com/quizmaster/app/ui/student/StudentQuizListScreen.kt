package com.quizmaster.app.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

private data class StudentQuiz(
    val quizId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val difficulty: String = "Easy",
    val duration: Int = 0,
    val passingScore: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentQuizListScreen(
    onBack: () -> Unit,
    onQuizSelected: (String) -> Unit
) {
    var quizzes by remember {
        mutableStateOf<List<StudentQuiz>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var reloadKey by remember {
        mutableStateOf(0)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var categoryFilter by remember {
        mutableStateOf("All")
    }

    var difficultyFilter by remember {
        mutableStateOf("All")
    }

    LaunchedEffect(reloadKey) {
        isLoading = true
        errorMessage = ""

        FirebaseFirestore.getInstance()
            .collection("quizzes")
            .whereEqualTo("status", "published")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->

                quizzes = result.documents.mapNotNull { document ->

                    try {
                        StudentQuiz(
                            quizId = document.getString("quizId")
                                ?: document.id,

                            title = document.getString("title")
                                ?: "",

                            description = document.getString("description")
                                ?: "",

                            category = document.getString("category")
                                ?: "",

                            difficulty = document.getString("difficulty")
                                ?: "Easy",

                            duration = (document.getLong("duration")
                                ?: 0L).toInt(),

                            passingScore = (document.getLong("passingScore")
                                ?: 0L).toInt()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                isLoading = false
                errorMessage = ""
            }
            .addOnFailureListener { exception ->
                isLoading = false
                errorMessage =
                    exception.message ?: "Unable to load quizzes."
            }
    }

    val categories = remember(quizzes) {
        listOf("All") +
                quizzes
                    .map { it.category.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sorted()
    }

    val difficulties = remember(quizzes) {
        listOf("All") +
                quizzes
                    .map { it.difficulty.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sorted()
    }

    val filteredQuizzes = remember(
        quizzes,
        searchQuery,
        categoryFilter,
        difficultyFilter
    ) {
        val query = searchQuery
            .trim()
            .lowercase()

        quizzes.filter { quiz ->
            val matchesSearch =
                query.isBlank() ||
                        quiz.title.lowercase().contains(query)

            val matchesCategory =
                categoryFilter == "All" ||
                        quiz.category.equals(
                            categoryFilter,
                            ignoreCase = true
                        )

            val matchesDifficulty =
                difficultyFilter == "All" ||
                        quiz.difficulty.equals(
                            difficultyFilter,
                            ignoreCase = true
                        )

            matchesSearch &&
                    matchesCategory &&
                    matchesDifficulty
        }
    }

    val hasActiveFilters =
        searchQuery.isNotBlank() ||
                categoryFilter != "All" ||
                difficultyFilter != "All"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Available Quizzes",
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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

        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {

                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(
                            Alignment.Center
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                errorMessage.isNotEmpty() -> {
                    ErrorContent(
                        message = errorMessage,
                        onRetry = {
                            reloadKey++
                        }
                    )
                }

                quizzes.isEmpty() -> {
                    EmptyQuizContent()
                }

                else -> {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = 20.dp,
                                vertical = 16.dp
                            ),

                        verticalArrangement =
                            Arrangement.spacedBy(14.dp)
                    ) {

                        item {
                            Text(
                                text = "Choose a quiz to get started",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    bottom = 2.dp
                                )
                            )
                        }

                        if (hasActiveFilters) {
                            item {
                                Text(
                                    text = "${filteredQuizzes.size} matching quiz${if (filteredQuizzes.size == 1) "" else "zes"}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        item {
                            androidx.compose.material3.OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = {
                                    Text("Search quiz")
                                },
                                placeholder = {
                                    Text("Search by quiz name")
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Category",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(
                                            rememberScrollState()
                                        ),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    categories.forEach { category ->
                                        FilterChip(
                                            selected =
                                                categoryFilter == category,
                                            onClick = {
                                                categoryFilter = category
                                            },
                                            label = {
                                                Text(category)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Difficulty",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(
                                            rememberScrollState()
                                        ),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {
                                    difficulties.forEach { difficulty ->
                                        FilterChip(
                                            selected =
                                                difficultyFilter == difficulty,
                                            onClick = {
                                                difficultyFilter = difficulty
                                            },
                                            label = {
                                                Text(difficulty)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (hasActiveFilters) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            searchQuery = ""
                                            categoryFilter = "All"
                                            difficultyFilter = "All"
                                        }
                                    ) {
                                        Text(
                                            text = "Clear filters",
                                            color =
                                                MaterialTheme.colorScheme.primary,
                                            fontWeight =
                                                FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredQuizzes.isEmpty()) {
                            item {
                                NoFilteredQuizContent(
                                    hasActiveFilters =
                                        hasActiveFilters
                                )
                            }
                        } else {
                            items(
                                items = filteredQuizzes,
                                key = { it.quizId }
                            ) { quiz ->

                                QuizListCard(
                                    quiz = quiz,
                                    onClick = {
                                        onQuizSelected(
                                            quiz.quizId
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
}

@Composable
private fun NoFilteredQuizContent(
    hasActiveFilters: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 24.dp,
                bottom = 24.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = if (hasActiveFilters) {
                "No results found"
            } else {
                "No quizzes available"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = if (hasActiveFilters) {
                "Try changing your search or filters."
            } else {
                "Published quizzes will appear here."
            },
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuizListCard(
    quiz: StudentQuiz,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),

                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(14.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = quiz.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (quiz.category.isNotBlank()) {

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = quiz.category,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DifficultyBadge(
                    difficulty = quiz.difficulty
                )
            }

            if (quiz.description.isNotBlank()) {

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Text(
                    text = quiz.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    maxLines = 3
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(5.dp)
                    )

                    Text(
                        text = "${quiz.duration} min",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(
                    modifier = Modifier.width(18.dp)
                )

                Text(
                    text = "Pass: ${quiz.passingScore}%",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    tint = Color(0xFF4F46E5),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun DifficultyBadge(
    difficulty: String
) {
    val textColor = when (difficulty.lowercase()) {
        "easy" -> Color(0xFF16A34A)
        "medium" -> Color(0xFFF59E0B)
        "hard" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    val badgeColor = textColor.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .background(
                color = badgeColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = 9.dp,
                vertical = 5.dp
            )
    ) {

        Text(
            text = difficulty,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun EmptyQuizContent() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(60.dp)
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "No quizzes available",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Published quizzes will appear here.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Something went wrong",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        TextButton(
            onClick = onRetry
        ) {
            Text(
                text = "Retry",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
