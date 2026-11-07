package com.quizmaster.app.ui.student

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

private data class QuizResult(
    val quizTitle: String = "Quiz Result",
    val score: Long = 0L,
    val totalMarks: Long = 0L,
    val percentage: Double = 0.0,
    val correctCount: Long = 0L,
    val wrongCount: Long = 0L,
    val unansweredCount: Long = 0L,
    val timeUp: Boolean = false,
    val passed: Boolean = false
)

@Composable
fun ResultScreen(
    attemptId: String,
    onBackToQuizzes: () -> Unit,
    onReviewAnswers: () -> Unit
) {
    var result by remember {
        mutableStateOf<QuizResult?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    LaunchedEffect(attemptId) {

        if (attemptId.isBlank()) {
            errorMessage = "Invalid result."
            isLoading = false
            return@LaunchedEffect
        }

        FirebaseFirestore.getInstance()
            .collection("attempts")
            .document(attemptId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    errorMessage = "Result not found."
                    isLoading = false
                    return@addOnSuccessListener
                }

                result = QuizResult(
                    quizTitle = document.getString("quizTitle")
                        ?: "Quiz Result",

                    score = document.getLong("score")
                        ?: 0L,

                    totalMarks = document.getLong("totalMarks")
                        ?: 0L,

                    percentage = document.getDouble("percentage")
                        ?: 0.0,

                    correctCount = document.getLong("correctCount")
                        ?: 0L,

                    wrongCount = document.getLong("wrongCount")
                        ?: 0L,

                    unansweredCount = document.getLong("unansweredCount")
                        ?: 0L,

                    timeUp = document.getBoolean("timeUp")
                        ?: false,

                    passed = document.getBoolean("passed")
                        ?: false
                )

                isLoading = false
            }
            .addOnFailureListener { exception ->

                errorMessage = exception.localizedMessage
                    ?: "Failed to load result."

                isLoading = false
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

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
                        modifier = Modifier.height(16.dp)
                    )

                    Text(
                        text = "Loading result...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
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
                        text = "Unable to Load Result",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = errorMessage,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )

                    Button(
                        onClick = onBackToQuizzes,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Back to Quizzes"
                        )
                    }
                }
            }

            result != null -> {

                ResultContent(
                    result = result!!,
                    onBackToQuizzes = onBackToQuizzes,
                    onReviewAnswers = onReviewAnswers
                )
            }
        }
    }
}

@Composable
private fun ResultContent(
    result: QuizResult,
    onBackToQuizzes: () -> Unit,
    onReviewAnswers: () -> Unit
) {

    val statusColor = if (result.passed) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    val statusBackground = if (result.passed) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    val statusText = if (result.passed) {
        "PASSED"
    } else {
        "FAILED"
    }

    val formattedPercentage = String.format(
        Locale.US,
        "%.2f",
        result.percentage
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                horizontal = 20.dp,
                vertical = 24.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Icon(
            imageVector = if (result.passed) {
                Icons.Default.CheckCircle
            } else {
                Icons.Default.Close
            },
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(72.dp)
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Quiz Completed",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = result.quizTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(statusBackground)
                .padding(
                    horizontal = 20.dp,
                    vertical = 10.dp
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = statusText,
                color = statusColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Your Score",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "${result.score} / ${result.totalMarks}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "$formattedPercentage%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ResultStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckCircle,
                value = result.correctCount.toString(),
                label = "Correct",
                iconColor = MaterialTheme.colorScheme.primary
            )

            ResultStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Close,
                value = result.wrongCount.toString(),
                label = "Wrong",
                iconColor = MaterialTheme.colorScheme.error
            )

            ResultStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.TaskAlt,
                value = result.unansweredCount.toString(),
                label = "Skipped",
                iconColor = MaterialTheme.colorScheme.tertiary
            )
        }

        if (result.timeUp) {

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(
                        modifier = Modifier.size(12.dp)
                    )

                    Column {

                        Text(
                            text = "Time's Up",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        Text(
                            text = "The quiz was automatically submitted when the timer ended.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        Button(
            onClick = onReviewAnswers,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.TaskAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )

            Spacer(
                modifier = Modifier.size(8.dp)
            )

            Text(
                text = "Review Answers",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Button(
            onClick = onBackToQuizzes,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )

            Spacer(
                modifier = Modifier.size(8.dp)
            )

            Text(
                text = "Back to Quizzes",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ResultStatCard(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
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
                .padding(
                    vertical = 16.dp,
                    horizontal = 8.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
