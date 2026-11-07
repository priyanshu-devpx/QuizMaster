package com.quizmaster.app.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

private data class ReviewQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val selectedAnswer: String?,
    val marks: Int,
    val explanation: String,
    val questionNumber: Int
)

@Composable
fun AnswerReviewScreen(
    attemptId: String,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var quizTitle by remember { mutableStateOf("Answer Review") }
    var submittedAt by remember { mutableStateOf<Timestamp?>(null) }
    var timeUp by remember { mutableStateOf(false) }

    var reviewQuestions by remember {
        mutableStateOf<List<ReviewQuestion>>(emptyList())
    }

    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(attemptId, reloadKey) {

        if (attemptId.isBlank()) {
            errorMessage = "Invalid attempt."
            loading = false
            return@LaunchedEffect
        }

        FirebaseFirestore.getInstance()
            .collection("attempts")
            .document(attemptId)
            .get()
            .addOnSuccessListener { attemptDocument ->

                if (!attemptDocument.exists()) {
                    errorMessage = "Attempt not found."
                    loading = false
                    return@addOnSuccessListener
                }

                val userId = attemptDocument.getString("userId")
                val currentUserId =
                    com.google.firebase.auth.FirebaseAuth
                        .getInstance()
                        .currentUser
                        ?.uid

                if (
                    userId == null ||
                    currentUserId == null ||
                    userId != currentUserId
                ) {
                    errorMessage = "You are not allowed to view this attempt."
                    loading = false
                    return@addOnSuccessListener
                }

                quizTitle =
                    attemptDocument.getString("quizTitle")
                        ?: "Quiz Result"

                submittedAt =
                    attemptDocument.getTimestamp("submittedAt")

                timeUp =
                    attemptDocument.getBoolean("timeUp")
                        ?: false

                val quizId =
                    attemptDocument.getString("quizId")

                if (quizId.isNullOrBlank()) {
                    errorMessage = "Quiz information not found."
                    loading = false
                    return@addOnSuccessListener
                }

                val rawReviewData = attemptDocument.get("reviewData") as? List<*>
                if (!rawReviewData.isNullOrEmpty()) {
                    val parsedQuestions = rawReviewData.mapIndexedNotNull { index, item ->
                        val map = item as? Map<*, *> ?: return@mapIndexedNotNull null
                        val rawOptions = map["options"] as? List<*> ?: emptyList<Any>()
                        ReviewQuestion(
                            question = map["question"]?.toString() ?: "Question unavailable",
                            options = rawOptions.mapNotNull { it?.toString() },
                            correctAnswer = map["correctAnswer"]?.toString() ?: "",
                            selectedAnswer = map["selectedAnswer"]?.toString(),
                            marks = (map["marks"] as? Number)?.toInt() ?: 1,
                            explanation = map["explanation"]?.toString() ?: "",
                            questionNumber = index + 1
                        )
                    }
                    reviewQuestions = parsedQuestions
                    loading = false
                    errorMessage = ""
                    return@addOnSuccessListener
                }

                val answers =
                    attemptDocument.get("answers")
                            as? Map<*, *>
                        ?: emptyMap<Any, Any>()

                // Fallback for legacy attempts: load questions collection containing correctAnswer
                FirebaseFirestore.getInstance()
                    .collection("questions")
                    .whereEqualTo("quizId", quizId)
                    .get()
                    .addOnSuccessListener { questionDocuments ->
                        val targetDocs = if (!questionDocuments.isEmpty) {
                            questionDocuments.documents
                        } else {
                            emptyList()
                        }

                        val questions = targetDocs.mapIndexed { index, document ->
                            val questionId = document.id
                            val options = (document.get("options") as? List<*>)
                                ?.mapNotNull { it?.toString() }
                                ?: emptyList()
                            val correctAnswer = document.getString("correctAnswer") ?: ""
                            val selectedAnswer = answers[questionId]?.toString()

                            ReviewQuestion(
                                question = document.getString("question") ?: "Question unavailable",
                                options = options,
                                correctAnswer = correctAnswer,
                                selectedAnswer = selectedAnswer,
                                marks = (document.getLong("marks") ?: 1L).toInt(),
                                explanation = document.getString("explanation") ?: "",
                                questionNumber = index + 1
                            )
                        }

                        reviewQuestions = questions
                        loading = false
                    }
                    .addOnFailureListener { exception ->
                        errorMessage =
                            exception.message
                                ?: "Failed to load questions."
                        loading = false
                    }
            }
            .addOnFailureListener { exception ->
                errorMessage =
                    exception.message
                        ?: "Failed to load attempt."
                loading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        ReviewHeader(
            title = quizTitle,
            onBack = onBack
        )

        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Unable to load review",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = errorMessage ?: "",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        Button(
                            onClick = {
                                errorMessage = null
                                loading = true
                                reloadKey++
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }

            reviewQuestions.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No questions available.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement =
                        Arrangement.spacedBy(16.dp)
                ) {

                    item {

                        ReviewSummary(
                            questionCount =
                                reviewQuestions.size,
                            timeUp =
                                timeUp,
                            submittedAt =
                                submittedAt
                        )
                    }

                    items(
                        items = reviewQuestions,
                        key = {
                            "${it.questionNumber}-${it.question}"
                        }
                    ) { question ->

                        ReviewQuestionCard(
                            question = question
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewHeader(
    title: String,
    onBack: () -> Unit
) {
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
                imageVector =
                    Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(
            modifier = Modifier.padding(4.dp)
        )

        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun ReviewSummary(
    questionCount: Int,
    timeUp: Boolean,
    submittedAt: Timestamp?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = "Review your answers",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =
                    "$questionCount questions • Check your selected and correct answers below.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (timeUp) {

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )

                    Spacer(
                        modifier = Modifier.padding(4.dp)
                    )

                    Text(
                        text = "Submitted automatically when time ended.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (submittedAt != null) {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Attempt submitted • ${formatSubmittedDate(submittedAt)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReviewQuestionCard(
    question: ReviewQuestion
) {
    val selected =
        question.selectedAnswer

    val correct =
        question.correctAnswer

    val isUnanswered =
        selected.isNullOrBlank()

    val isCorrect =
        !isUnanswered &&
                selected.equals(
                    correct,
                    ignoreCase = true
                )

    val isWrong =
        !isUnanswered && !isCorrect

    val statusText =
        when {
            isCorrect -> "Correct"
            isWrong -> "Wrong"
            else -> "Unanswered"
        }

    val statusColor =
        when {
            isCorrect -> MaterialTheme.colorScheme.primary
            isWrong -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

                Text(
                    text = "Q${question.questionNumber}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(
                            horizontal = 9.dp,
                            vertical = 6.dp
                        )
                )

                Spacer(
                    modifier = Modifier.padding(5.dp)
                )

                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                    modifier = Modifier
                        .padding(top = 5.dp)
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${question.marks} mark",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Text(
                text = question.question,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            question.options.forEachIndexed { index, option ->

                val optionLetter =
                    ('A'.code + index).toChar().toString()

                val optionIsCorrect =
                    optionLetter.equals(
                        correct,
                        ignoreCase = true
                    )

                val optionIsSelected =
                    optionLetter.equals(
                        selected,
                        ignoreCase = true
                    )

                ReviewOption(
                    letter = optionLetter,
                    text = option,
                    isSelected = optionIsSelected,
                    isCorrect = optionIsCorrect
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            if (isUnanswered) {

                AnswerInfo(
                    title = "Your answer",
                    value = "Not answered",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            if (!isUnanswered) {

                val selectedIndex =
                    selected
                        .uppercase()
                        .firstOrNull()
                        ?.let {
                            it.code - 'A'.code
                        }

                val selectedText =
                    if (
                        selectedIndex != null &&
                        selectedIndex in question.options.indices
                    ) {
                        question.options[selectedIndex]
                    } else {
                        "Unknown option"
                    }

                AnswerInfo(
                    title = "Your answer",
                    value =
                        "$selected — $selectedText",
                    color =
                        if (isCorrect)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            val correctIndex =
                correct
                    .uppercase()
                    .firstOrNull()
                    ?.let {
                        it.code - 'A'.code
                    }

            val correctText =
                if (
                    correctIndex != null &&
                    correctIndex in question.options.indices
                ) {
                    question.options[correctIndex]
                } else {
                    "Unknown option"
                }

            AnswerInfo(
                title = "Correct answer",
                value =
                    "$correct — $correctText",
                color = MaterialTheme.colorScheme.primary
            )

            if (question.explanation.isNotBlank()) {

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {

                    Text(
                        text = "Explanation",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = question.explanation,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewOption(
    letter: String,
    text: String,
    isSelected: Boolean,
    isCorrect: Boolean
) {
    val backgroundColor =
        when {
            isCorrect -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            isSelected -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.background
        }

    val borderColor =
        when {
            isCorrect -> MaterialTheme.colorScheme.primary
            isSelected -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.outlineVariant
        }

    val icon =
        when {
            isCorrect ->
                Icons.Default.CheckCircle

            isSelected ->
                Icons.Default.Close

            else ->
                Icons.Default.RadioButtonUnchecked
        }

    val iconColor =
        when {
            isCorrect -> MaterialTheme.colorScheme.primary
            isSelected -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.outline
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor
        )

        Spacer(
            modifier = Modifier.padding(4.dp)
        )

        Text(
            text = "$letter.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(
            modifier = Modifier.padding(4.dp)
        )

        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        if (isCorrect) {

            Spacer(
                modifier = Modifier.padding(4.dp)
            )

            Text(
                text = "Correct",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        } else if (isSelected) {

            Spacer(
                modifier = Modifier.padding(4.dp)
            )

            Text(
                text = "Your answer",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun formatSubmittedDate(timestamp: Timestamp): String {
    val formatter = java.text.SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        java.util.Locale.getDefault()
    )
    return formatter.format(timestamp.toDate())
}

@Composable
private fun AnswerInfo(
    title: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {

        Text(
            text = "$title:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.weight(0.65f)
        )
    }
}
