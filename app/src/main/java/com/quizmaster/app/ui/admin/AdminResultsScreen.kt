package com.quizmaster.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.quizmaster.app.ui.theme.QuizErrorDark
import com.quizmaster.app.ui.theme.QuizErrorLight
import com.quizmaster.app.ui.theme.QuizSuccessDark
import com.quizmaster.app.ui.theme.QuizSuccessLight

// =========================================================
// RESULT MODEL
// =========================================================

private data class AdminResultItem(
    val attemptId: String,
    val studentName: String,
    val userId: String,
    val quizId: String,
    val quizTitle: String,
    val score: Long,
    val totalMarks: Long,
    val percentage: Double,
    val correctCount: Long,
    val wrongCount: Long,
    val unansweredCount: Long,
    val passed: Boolean,
    val timeUp: Boolean,
    val submittedAt: Timestamp?
)

// =========================================================
// ADMIN RESULTS SCREEN
// =========================================================

@Composable
fun AdminResultsScreen(
    teacherId: String? = null,
    onBack: () -> Unit,
    onResultSelected: (String) -> Unit
) {

    val db = remember {
        FirebaseFirestore.getInstance()
    }

    var results by remember {
        mutableStateOf<List<AdminResultItem>>(emptyList())
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var resultFilter by remember {
        mutableStateOf("All")
    }

    var dateFilter by remember {
        mutableStateOf("All Time")
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    // =====================================================
    // LOAD RESULTS
    // =====================================================

    fun loadResults() {

        isLoading = true
        errorMessage = ""

        db.collection("users")
            .get()
            .addOnSuccessListener { usersSnapshot ->

                val studentNames =
                    usersSnapshot.documents.associate { document ->

                        val name =
                            document.getString("name")
                                ?.trim()
                                ?.takeIf { it.isNotEmpty() }

                        val email =
                            document.getString("email")
                                ?.trim()
                                ?.takeIf { it.isNotEmpty() }

                        document.id to (
                                name
                                    ?: email
                                    ?: "Unknown Student"
                                )
                    }

                val attemptsQuery = if (teacherId != null && teacherId.isNotBlank()) {
                    db.collection("attempts").whereEqualTo("teacherId", teacherId)
                } else {
                    db.collection("attempts")
                }

                attemptsQuery
                    .get()
                    .addOnSuccessListener { snapshot ->

                        results =
                            snapshot.documents
                                .mapNotNull { document ->

                                    val userId =
                                        document.getString("userId")
                                            ?.trim()
                                            ?.takeIf { it.isNotEmpty() }
                                            ?: return@mapNotNull null

                                    AdminResultItem(

                                        attemptId = document.id,

                                        studentName =
                                            studentNames[userId]
                                                ?: "Unknown Student",

                                        userId = userId,

                                        quizId =
                                            document.getString("quizId")
                                                ?: "",

                                        quizTitle =
                                            document.getString("quizTitle")
                                                ?.trim()
                                                ?.takeIf { it.isNotEmpty() }
                                                ?: "Quiz",

                                        score =
                                            document.getLong("score")
                                                ?: 0L,

                                        totalMarks =
                                            document.getLong("totalMarks")
                                                ?: 0L,

                                        percentage =
                                            (document.get("percentage")
                                                    as? Number)
                                                ?.toDouble()
                                                ?: 0.0,

                                        correctCount =
                                            document.getLong("correctCount")
                                                ?: 0L,

                                        wrongCount =
                                            document.getLong("wrongCount")
                                                ?: 0L,

                                        unansweredCount =
                                            document.getLong("unansweredCount")
                                                ?: 0L,

                                        passed =
                                            document.getBoolean("passed")
                                                ?: false,

                                        timeUp =
                                            document.getBoolean("timeUp")
                                                ?: false,

                                        submittedAt =
                                            document.getTimestamp("submittedAt")
                                    )
                                }
                                .sortedByDescending {
                                    it.submittedAt?.seconds ?: 0L
                                }

                        isLoading = false
                    }
                    .addOnFailureListener { exception ->

                        errorMessage =
                            exception.localizedMessage
                                ?: "Failed to load results."

                        isLoading = false
                    }
            }
            .addOnFailureListener { exception ->

                errorMessage =
                    exception.localizedMessage
                        ?: "Failed to load student information."

                isLoading = false
            }
    }

    // =====================================================
    // INITIAL LOAD
    // =====================================================

    LaunchedEffect(teacherId) {
        loadResults()
    }

    // =====================================================
    // FILTERED RESULTS
    // =====================================================

    val filteredResults =
        remember(
            results,
            searchText,
            resultFilter,
            dateFilter
        ) {

            val query =
                searchText
                    .trim()
                    .lowercase(Locale.getDefault())

            results.filter { result ->

                val matchesSearch =
                    query.isBlank() ||
                            result.studentName
                                .lowercase(Locale.getDefault())
                                .contains(query) ||
                            result.quizTitle
                                .lowercase(Locale.getDefault())
                                .contains(query) ||
                            result.quizId
                                .lowercase(Locale.getDefault())
                                .contains(query)

                val matchesResult =
                    when (resultFilter) {

                        "Passed" ->
                            result.passed

                        "Failed" ->
                            !result.passed

                        else ->
                            true
                    }

                val matchesDate =
                    matchesDateFilter(
                        timestamp = result.submittedAt,
                        dateFilter = dateFilter
                    )

                matchesSearch &&
                        matchesResult &&
                        matchesDate
            }
        }

    val hasActiveFilters =
        searchText.isNotBlank() ||
                resultFilter != "All" ||
                dateFilter != "All Time"

    // =====================================================
    // STATISTICS
    // =====================================================

    val totalAttempts =
        results.size

    val passedAttempts =
        results.count {
            it.passed
        }

    val failedAttempts =
        results.count {
            !it.passed
        }

    val averagePercentage =
        if (results.isEmpty()) {
            0.0
        } else {
            results
                .map {
                    it.percentage
                }
                .average()
        }

    // =====================================================
    // MAIN SCREEN
    // =====================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
    ) {

        // =================================================
        // HEADER
        // =================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface
                )
                .statusBarsPadding()
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 6.dp,
                    bottom = 12.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Results",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Monitor quiz performance",
                    fontSize = 13.sp,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    loadResults()
                }
            ) {

                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // =================================================
        // CONTENT
        // =================================================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // =================================================
            // STAT CARDS
            // =================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                AdminResultStatCard(
                    modifier = Modifier.weight(1f),
                    value = totalAttempts.toString(),
                    label = "Attempts"
                )

                AdminResultStatCard(
                    modifier = Modifier.weight(1f),
                    value = passedAttempts.toString(),
                    label = "Passed"
                )

                AdminResultStatCard(
                    modifier = Modifier.weight(1f),
                    value = "${averagePercentage.toInt()}%",
                    label = "Average"
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // =================================================
            // SEARCH
            // =================================================

            OutlinedTextField(
                value = searchText,

                onValueChange = {
                    searchText = it
                },

                modifier = Modifier.fillMaxWidth(),

                singleLine = true,

                placeholder = {
                    Text(
                        text =
                            "Search student name, quiz name or quiz ID"
                    )
                },

                trailingIcon = {

                    if (searchText.isNotEmpty()) {

                        IconButton(
                            onClick = {
                                searchText = ""
                            }
                        ) {

                            Text(
                                text = "×",
                                fontSize = 22.sp
                            )
                        }
                    }
                },

                shape = RoundedCornerShape(12.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            // =================================================
            // RESULT FILTER
            // =================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    ),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                listOf(
                    "All",
                    "Passed",
                    "Failed"
                ).forEach { filter ->

                    FilterChip(

                        selected =
                            resultFilter == filter,

                        onClick = {
                            resultFilter = filter
                        },

                        label = {
                            Text(filter)
                        }
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            // =================================================
            // DATE LABEL
            // =================================================

            Text(
                text = "Date",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            // =================================================
            // DATE FILTER
            // =================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    ),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                listOf(
                    "All Time",
                    "Today",
                    "Last 7 Days",
                    "Last 30 Days"
                ).forEach { filter ->

                    FilterChip(

                        selected =
                            dateFilter == filter,

                        onClick = {
                            dateFilter = filter
                        },

                        label = {
                            Text(filter)
                        }
                    )
                }
            }

            // =================================================
            // CLEAR FILTERS
            // =================================================

            if (hasActiveFilters) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.End
                ) {

                    TextButton(
                        onClick = {

                            searchText = ""
                            resultFilter = "All"
                            dateFilter = "All Time"
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

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            // =================================================
            // STATES
            // =================================================

            if (isLoading) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),

                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator(
                        color =
                            MaterialTheme.colorScheme.primary
                    )
                }

            } else if (errorMessage.isNotBlank()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),

                    contentAlignment =
                        Alignment.Center
                ) {

                    ErrorContent(
                        message = errorMessage,
                        onRetry = {
                            loadResults()
                        }
                    )
                }

            } else if (results.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),

                    contentAlignment =
                        Alignment.Center
                ) {

                    EmptyResultsContent()
                }

            } else if (filteredResults.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),

                    contentAlignment =
                        Alignment.Center
                ) {

                    NoMatchContent()
                }

            } else {

                // =================================================
                // RESULTS LIST
                // =================================================

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp),

                    contentPadding =
                        PaddingValues(
                            bottom = 24.dp
                        )
                ) {

                    item {

                        Text(
                            text =
                                if (!hasActiveFilters) {
                                    "$totalAttempts attempts • $failedAttempts failed"
                                } else {
                                    "${filteredResults.size} matching results"
                                },

                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }

                    items(
                        items = filteredResults,

                        key = {
                            it.attemptId
                        }
                    ) { result ->

                        AdminResultCard(
                            result = result,

                            onClick = {
                                onResultSelected(
                                    result.attemptId
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// =========================================================
// STAT CARD
// =========================================================

@Composable
private fun AdminResultStatCard(
    modifier: Modifier,
    value: String,
    label: String
) {

    Card(
        modifier = modifier,

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        border =
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme.colorScheme.outlineVariant
            )
    ) {

        Column(
            modifier =
                Modifier.padding(14.dp)
        ) {

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = label,
                fontSize = 12.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

// =========================================================
// RESULT CARD
// =========================================================

@Composable
private fun AdminResultCard(
    result: AdminResultItem,
    onClick: () -> Unit
) {

    /*
     * Detect the currently active Material theme rather than
     * relying on the device's system theme.
     *
     * This keeps the status colors correct even when QuizMaster's
     * own Dark Mode preference controls the theme.
     */
    val isDarkTheme =
        MaterialTheme
            .colorScheme
            .background
            .luminance() < 0.5f

    val statusColor =
        if (result.passed) {

            if (isDarkTheme) {
                QuizSuccessDark
            } else {
                QuizSuccessLight
            }

        } else {

            if (isDarkTheme) {
                QuizErrorDark
            } else {
                QuizErrorLight
            }
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            ),

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        border =
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color =
                    MaterialTheme.colorScheme.outlineVariant
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.Top
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = result.quizTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface,

                        maxLines = 2,

                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text = result.studentName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,

                        maxLines = 1,

                        overflow =
                            TextOverflow.Ellipsis
                    )
                }

                // =================================================
                // STATUS BADGE
                // =================================================

                Surface(
                    shape =
                        RoundedCornerShape(50),

                    color =
                        statusColor.copy(
                            alpha = 0.10f
                        )
                ) {

                    Text(
                        text =
                            if (result.passed) {
                                "PASSED"
                            } else {
                                "FAILED"
                            },

                        modifier =
                            Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            ),

                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            // =================================================
            // METRICS
            // =================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                ResultMetric(
                    label = "Score",
                    value =
                        "${result.score}/${result.totalMarks}"
                )

                ResultMetric(
                    label = "Percentage",
                    value =
                        "${result.percentage.toInt()}%"
                )

                ResultMetric(
                    label = "Correct",
                    value =
                        result.correctCount.toString()
                )

                ResultMetric(
                    label = "Wrong",
                    value =
                        result.wrongCount.toString()
                )
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text = buildString {

                    append(
                        formatTimestamp(
                            result.submittedAt
                        )
                    )

                    if (result.timeUp) {
                        append(" • Time up")
                    }
                },

                fontSize = 12.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

// =========================================================
// RESULT METRIC
// =========================================================

@Composable
private fun ResultMetric(
    label: String,
    value: String
) {

    Column {

        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )

        Text(
            text = label,
            fontSize = 11.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

// =========================================================
// ERROR CONTENT
// =========================================================

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {

    Column(
        modifier =
            Modifier.fillMaxSize(),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "Unable to load results",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color =
                MaterialTheme
                    .colorScheme
                    .onBackground
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text = message,
            fontSize = 13.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Button(
            onClick = onRetry
        ) {

            Text(
                text = "Retry"
            )
        }
    }
}

// =========================================================
// EMPTY RESULTS
// =========================================================

@Composable
private fun EmptyResultsContent() {

    Column(
        modifier =
            Modifier.fillMaxSize(),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "No quiz results yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color =
                MaterialTheme
                    .colorScheme
                    .onBackground
        )

        Spacer(
            modifier =
                Modifier.height(6.dp)
        )

        Text(
            text =
                "Student attempts will appear here.",

            fontSize = 13.sp,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

// =========================================================
// NO MATCH CONTENT
// =========================================================

@Composable
private fun NoMatchContent() {

    Column(
        modifier =
            Modifier.fillMaxSize(),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "No results found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color =
                MaterialTheme
                    .colorScheme
                    .onBackground
        )

        Spacer(
            modifier =
                Modifier.height(6.dp)
        )

        Text(
            text =
                "Try changing your search or filters.",

            fontSize = 13.sp,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

// =========================================================
// DATE FILTER
// =========================================================

private fun matchesDateFilter(
    timestamp: Timestamp?,
    dateFilter: String
): Boolean {

    if (dateFilter == "All Time") {
        return true
    }

    val submittedAtMillis =
        timestamp
            ?.toDate()
            ?.time
            ?: return false

    val now =
        System.currentTimeMillis()

    return when (dateFilter) {

        "Today" -> {

            val calendar =
                Calendar.getInstance().apply {

                    set(
                        Calendar.HOUR_OF_DAY,
                        0
                    )

                    set(
                        Calendar.MINUTE,
                        0
                    )

                    set(
                        Calendar.SECOND,
                        0
                    )

                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }

            submittedAtMillis >=
                    calendar.timeInMillis &&
                    submittedAtMillis <= now
        }

        "Last 7 Days" -> {

            submittedAtMillis >=
                    now -
                    7L *
                    24L *
                    60L *
                    60L *
                    1000L &&

                    submittedAtMillis <= now
        }

        "Last 30 Days" -> {

            submittedAtMillis >=
                    now -
                    30L *
                    24L *
                    60L *
                    60L *
                    1000L &&

                    submittedAtMillis <= now
        }

        else -> true
    }
}

// =========================================================
// TIMESTAMP FORMAT
// =========================================================

private fun formatTimestamp(
    timestamp: Timestamp?
): String {

    if (timestamp == null) {
        return "Date unavailable"
    }

    return SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    ).format(
        timestamp.toDate()
    )
}