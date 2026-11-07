package com.quizmaster.app.ui.student

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

private data class ResultHistoryItem(
    val attemptId: String = "",
    val quizTitle: String = "Quiz",
    val score: Long = 0L,
    val totalMarks: Long = 0L,
    val percentage: Double = 0.0,
    val correctCount: Long = 0L,
    val wrongCount: Long = 0L,
    val unansweredCount: Long = 0L,
    val passed: Boolean = false,
    val timeUp: Boolean = false,
    val submittedAt: Timestamp? = null
)

@Composable
fun ResultsHistoryScreen(
    onBack: () -> Unit,
    onResultClick: (String) -> Unit
) {

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val db = remember {
        FirebaseFirestore.getInstance()
    }

    BackHandler {
        onBack()
    }

    var results by remember {
        mutableStateOf<List<ResultHistoryItem>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    fun loadResults() {

        val user = auth.currentUser

        if (user == null) {
            errorMessage = "Your session has expired. Please login again."
            isLoading = false
            return
        }

        isLoading = true
        errorMessage = ""

        db.collection("attempts")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->

                results = snapshot.documents
                    .mapNotNull { document ->

                        val attemptId = document.id

                        val quizTitle =
                            document.getString("quizTitle")
                                ?: "Quiz"

                        val score =
                            document.getLong("score")
                                ?: 0L

                        val totalMarks =
                            document.getLong("totalMarks")
                                ?: 0L

                        val percentage =
                            document.getDouble("percentage")
                                ?: 0.0

                        val correctCount =
                            document.getLong("correctCount")
                                ?: 0L

                        val wrongCount =
                            document.getLong("wrongCount")
                                ?: 0L

                        val unansweredCount =
                            document.getLong("unansweredCount")
                                ?: 0L

                        val passed =
                            document.getBoolean("passed")
                                ?: false

                        val timeUp =
                            document.getBoolean("timeUp")
                                ?: false

                        val submittedAt =
                            document.getTimestamp("submittedAt")

                        ResultHistoryItem(
                            attemptId = attemptId,
                            quizTitle = quizTitle,
                            score = score,
                            totalMarks = totalMarks,
                            percentage = percentage,
                            correctCount = correctCount,
                            wrongCount = wrongCount,
                            unansweredCount = unansweredCount,
                            passed = passed,
                            timeUp = timeUp,
                            submittedAt = submittedAt
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
                        ?: "Failed to load your results."

                isLoading = false
            }
    }

    LaunchedEffect(Unit) {
        loadResults()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // ---------------------------------------------------------
            // HEADER
            // ---------------------------------------------------------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(
                        start = 8.dp,
                        end = 12.dp,
                        top = 6.dp,
                        bottom = 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
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
                        text = "Your quiz performance",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        if (!isLoading) {
                            loadResults()
                        }
                    },
                    enabled = !isLoading
                ) {

                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ---------------------------------------------------------
            // CONTENT
            // ---------------------------------------------------------

            when {

                isLoading -> {

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        Text(
                            text = "Loading your results...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                errorMessage.isNotBlank() -> {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(56.dp)
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        Text(
                            text = "Something went wrong",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = errorMessage,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(
                            modifier = Modifier.height(20.dp)
                        )

                        Button(
                            onClick = {
                                loadResults()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {

                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(
                                modifier = Modifier.size(8.dp)
                            )

                            Text(
                                text = "Try Again"
                            )
                        }
                    }
                }

                results.isEmpty() -> {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        Text(
                            text = "No Results Yet",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "Complete a quiz to see your results here.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 20.dp,
                            vertical = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        item {

                            Text(
                                text = "${results.size} attempt${
                                    if (results.size == 1) "" else "s"
                                }",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    bottom = 2.dp
                                )
                            )
                        }

                        items(
                            items = results,
                            key = {
                                it.attemptId
                            }
                        ) { result ->

                            ResultHistoryCard(
                                result = result,
                                onClick = {
                                    onResultClick(result.attemptId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultHistoryCard(
    result: ResultHistoryItem,
    onClick: () -> Unit
) {

    val statusColor =
        if (result.passed) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        }

    val statusBackground =
        if (result.passed) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        }

    val statusText =
        if (result.passed) {
            "PASSED"
        } else {
            "FAILED"
        }

    val formattedPercentage =
        String.format(
            Locale.US,
            "%.2f",
            result.percentage
        )

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            // ---------------------------------------------------------
            // TITLE + STATUS
            // ---------------------------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = result.quizTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = formatSubmittedDate(
                            result.submittedAt
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = statusBackground,
                            shape = RoundedCornerShape(50.dp)
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        )
                ) {

                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // ---------------------------------------------------------
            // SCORE
            // ---------------------------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Score",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "${result.score} / ${result.totalMarks}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {

                    Text(
                        text = "Percentage",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "$formattedPercentage%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            // ---------------------------------------------------------
            // STATISTICS
            // ---------------------------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                MiniResultStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    value = result.correctCount.toString(),
                    label = "Correct",
                    color = MaterialTheme.colorScheme.primary
                )

                MiniResultStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Close,
                    value = result.wrongCount.toString(),
                    label = "Wrong",
                    color = MaterialTheme.colorScheme.error
                )

                MiniResultStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.TaskAlt,
                    value = result.unansweredCount.toString(),
                    label = "Skipped",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // ---------------------------------------------------------
            // TIME UP
            // ---------------------------------------------------------

            if (result.timeUp) {

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 8.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(
                        modifier = Modifier.size(7.dp)
                    )

                    Text(
                        text = "Automatically submitted when time ended",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniResultStat(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {

    Row(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.background,
                RoundedCornerShape(10.dp)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(17.dp)
        )

        Spacer(
            modifier = Modifier.size(5.dp)
        )

        Column {

            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatSubmittedDate(
    timestamp: Timestamp?
): String {

    if (timestamp == null) {
        return "Date unavailable"
    }

    val date = timestamp.toDate()

    val formatter = SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    )

    return formatter.format(date)
}
