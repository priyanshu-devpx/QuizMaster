package com.quizmaster.app.ui.admin

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.quizmaster.app.ui.theme.DarkBackground
import com.quizmaster.app.ui.theme.QuizErrorDark
import com.quizmaster.app.ui.theme.QuizErrorLight
import com.quizmaster.app.ui.theme.QuizSuccessDark
import com.quizmaster.app.ui.theme.QuizSuccessLight
import com.quizmaster.app.ui.theme.QuizWarningDark
import com.quizmaster.app.ui.theme.QuizWarningLight
import java.text.SimpleDateFormat
import java.util.Locale

// =========================================================
// DATA MODELS
// =========================================================

private data class AdminAttemptDetails(
    val attemptId: String,
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

private data class StudentProfileDetails(
    val name: String,
    val email: String
)

private data class AnswerDetails(
    val questionId: String,
    val questionText: String,
    val selectedAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val unanswered: Boolean,
    val explanation: String
)

// =========================================================
// ADMIN RESULT DETAILS SCREEN
// =========================================================

@Composable
fun AdminResultDetailsScreen(
    attemptId: String,
    onBack: () -> Unit
) {

    val db = remember {
        FirebaseFirestore.getInstance()
    }

    var attempt by remember {
        mutableStateOf<AdminAttemptDetails?>(null)
    }

    var student by remember {
        mutableStateOf<StudentProfileDetails?>(null)
    }

    var answers by remember {
        mutableStateOf<List<AnswerDetails>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    // =====================================================
    // LOAD RESULT DETAILS
    // =====================================================

    fun loadDetails() {

        if (attemptId.isBlank()) {
            errorMessage = "Invalid result."
            isLoading = false
            return
        }

        isLoading = true
        errorMessage = ""

        db.collection("attempts")
            .document(attemptId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    errorMessage = "Result not found."
                    isLoading = false
                    return@addOnSuccessListener
                }

                val userId =
                    document.getString("userId")
                        ?: ""

                val quizId =
                    document.getString("quizId")
                        ?: ""

                val loadedAttempt =
                    AdminAttemptDetails(
                        attemptId = document.id,

                        userId = userId,

                        quizId = quizId,

                        quizTitle =
                            document.getString("quizTitle")
                                ?: "Quiz",

                        score =
                            document.getLong("score")
                                ?: 0L,

                        totalMarks =
                            document.getLong("totalMarks")
                                ?: 0L,

                        percentage =
                            document.getDouble("percentage")
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

                attempt = loadedAttempt

                val answersMap =
                    document.get("answers") as? Map<*, *>
                        ?: emptyMap<String, Any>()

                val attemptQuestionIds =
                    answersMap.keys.mapNotNull {
                        it?.toString()
                    }

                // =================================================
                // LOAD STUDENT
                // =================================================

                fun loadStudent() {

                    if (userId.isBlank()) {

                        student =
                            StudentProfileDetails(
                                name = "Unknown Student",
                                email = ""
                            )

                        loadQuestions(
                            db = db,
                            quizId = quizId,
                            answersMap = answersMap,
                            questionIds = attemptQuestionIds,
                            onComplete = {
                                answers = it
                                isLoading = false
                            }
                        )

                        return
                    }

                    db.collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { userDocument ->

                            student =
                                StudentProfileDetails(
                                    name =
                                        userDocument
                                            .getString("name")
                                            ?.ifBlank {
                                                "Student"
                                            }
                                            ?: "Student",

                                    email =
                                        userDocument
                                            .getString("email")
                                            ?: ""
                                )

                            loadQuestions(
                                db = db,
                                quizId = quizId,
                                answersMap = answersMap,
                                questionIds = attemptQuestionIds,
                                onComplete = {
                                    answers = it
                                    isLoading = false
                                }
                            )
                        }
                        .addOnFailureListener {

                            student =
                                StudentProfileDetails(
                                    name = "Student",
                                    email = ""
                                )

                            loadQuestions(
                                db = db,
                                quizId = quizId,
                                answersMap = answersMap,
                                questionIds = attemptQuestionIds,
                                onComplete = {
                                    answers = it
                                    isLoading = false
                                }
                            )
                        }
                }

                loadStudent()
            }
            .addOnFailureListener { exception ->

                errorMessage =
                    exception.localizedMessage
                        ?: "Unable to load result details."

                isLoading = false
            }
    }

    // =====================================================
    // LOAD WHEN ATTEMPT CHANGES
    // =====================================================

    LaunchedEffect(attemptId) {
        loadDetails()
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
        // TOP HEADER
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
                    text = "Result Details",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Detailed attempt information",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // =================================================
        // LOADING
        // =================================================

        if (isLoading) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }

        }

        // =================================================
        // ERROR
        // =================================================

        else if (errorMessage.isNotBlank()) {

            ErrorContent(
                message = errorMessage,
                onRetry = {
                    loadDetails()
                }
            )

        }

        // =================================================
        // CONTENT
        // =================================================

        else {

            val currentAttempt = attempt

            if (currentAttempt == null) {

                ErrorContent(
                    message = "Result information is unavailable.",
                    onRetry = {
                        loadDetails()
                    }
                )

            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),

                    contentPadding =
                        PaddingValues(
                            horizontal = 20.dp,
                            vertical = 16.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {

                    // =========================================
                    // STUDENT
                    // =========================================

                    item {

                        StudentCard(
                            student = student
                        )
                    }

                    // =========================================
                    // QUIZ SUMMARY
                    // =========================================

                    item {

                        QuizSummaryCard(
                            attempt = currentAttempt
                        )
                    }

                    // =========================================
                    // PERFORMANCE
                    // =========================================

                    item {

                        PerformanceCard(
                            attempt = currentAttempt
                        )
                    }

                    // =========================================
                    // ANSWER SUMMARY
                    // =========================================

                    item {

                        Text(
                            text = "Answer Summary",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // =========================================
                    // NO ANSWERS
                    // =========================================

                    if (answers.isEmpty()) {

                        item {

                            Card(
                                modifier =
                                    Modifier.fillMaxWidth(),

                                shape =
                                    RoundedCornerShape(16.dp),

                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            MaterialTheme
                                                .colorScheme
                                                .surface
                                    )
                            ) {

                                Text(
                                    text =
                                        "No answer details available for this attempt.",

                                    modifier =
                                        Modifier.padding(16.dp),

                                    fontSize = 14.sp,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }

                    }

                    // =========================================
                    // ANSWERS
                    // =========================================

                    else {

                        items(
                            items = answers,
                            key = {
                                it.questionId
                            }
                        ) { answer ->

                            AnswerCard(
                                answer = answer
                            )
                        }
                    }

                    // =========================================
                    // ATTEMPT ID
                    // =========================================

                    item {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "Attempt ID: ${currentAttempt.attemptId}",

                            fontSize = 11.sp,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// =========================================================
// LOAD QUESTIONS
// =========================================================

private fun loadQuestions(
    db: FirebaseFirestore,
    quizId: String,
    answersMap: Map<*, *>,
    questionIds: List<String>,
    onComplete: (List<AnswerDetails>) -> Unit
) {

    if (quizId.isBlank()) {
        onComplete(emptyList())
        return
    }

    db.collection("questions")
        .whereEqualTo("quizId", quizId)
        .get()
        .addOnSuccessListener { snapshot ->

            val questionDocuments =
                snapshot.documents.associateBy {
                    it.id
                }

            val orderedIds =
                if (questionIds.isEmpty()) {
                    questionDocuments.keys.toList()
                } else {
                    questionIds
                }

            val result =
                orderedIds.mapNotNull { questionId ->

                    val questionDocument =
                        questionDocuments[questionId]
                            ?: return@mapNotNull null

                    val questionText =
                        questionDocument
                            .getString("question")
                            ?: questionDocument
                                .getString("questionText")
                            ?: "Question"

                    val correctAnswer =
                        questionDocument
                            .getString("correctAnswer")
                            ?: ""

                    val selectedAnswer =
                        answersMap[questionId]
                            ?.toString()
                            ?: ""

                    val unanswered =
                        selectedAnswer.isBlank()

                    val isCorrect =
                        !unanswered &&
                                selectedAnswer ==
                                correctAnswer

                    AnswerDetails(
                        questionId = questionId,

                        questionText = questionText,

                        selectedAnswer = selectedAnswer,

                        correctAnswer = correctAnswer,

                        isCorrect = isCorrect,

                        unanswered = unanswered,

                        explanation =
                            questionDocument
                                .getString("explanation")
                                ?: ""
                    )
                }

            onComplete(result)
        }
        .addOnFailureListener {
            onComplete(emptyList())
        }
}

// =========================================================
// STUDENT CARD
// =========================================================

@Composable
private fun StudentCard(
    student: StudentProfileDetails?
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(
                text = "Student",
                fontSize = 13.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(
                text =
                    student?.name
                        ?: "Unknown Student",

                fontSize = 19.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )

            if (!student?.email.isNullOrBlank()) {

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(
                    text =
                        student?.email.orEmpty(),

                    fontSize = 13.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

// =========================================================
// QUIZ SUMMARY CARD
// =========================================================

@Composable
private fun QuizSummaryCard(
    attempt: AdminAttemptDetails
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(
                text = "Quiz",
                fontSize = 13.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
                text = attempt.quizTitle,

                fontSize = 19.sp,

                fontWeight =
                    FontWeight.Bold,

                maxLines = 2,

                overflow =
                    TextOverflow.Ellipsis,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Text(
                text =
                    "Submitted: ${formatTimestamp(attempt.submittedAt)}",

                fontSize = 13.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            if (attempt.timeUp) {

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        "Submitted because time expired.",

                    fontSize = 13.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

// =========================================================
// PERFORMANCE CARD
// =========================================================

@Composable
private fun PerformanceCard(
    attempt: AdminAttemptDetails
) {

    /*
     * The application has a fixed Light/Dark palette.
     * We determine the active palette from the current
     * MaterialTheme background instead of using
     * isSystemInDarkTheme(), because QuizMaster supports
     * a manually selected Dark Mode setting.
     */

    val isDarkTheme =
        MaterialTheme.colorScheme.background ==
                DarkBackground

    val statusColor =
        if (attempt.passed) {

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
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "Performance",

                    fontSize = 18.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )

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
                            if (attempt.passed) {
                                "PASSED"
                            } else {
                                "FAILED"
                            },

                        modifier =
                            Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 7.dp
                            ),

                        fontSize = 11.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color = statusColor
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                PerformanceMetric(
                    value =
                        "${attempt.score}/${attempt.totalMarks}",

                    label = "Score"
                )

                PerformanceMetric(
                    value =
                        "${attempt.percentage.toInt()}%",

                    label = "Percentage"
                )

                PerformanceMetric(
                    value =
                        attempt.correctCount.toString(),

                    label = "Correct"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                PerformanceMetric(
                    value =
                        attempt.wrongCount.toString(),

                    label = "Wrong"
                )

                PerformanceMetric(
                    value =
                        attempt.unansweredCount.toString(),

                    label = "Unanswered"
                )

                PerformanceMetric(
                    value =
                        if (attempt.timeUp) {
                            "Yes"
                        } else {
                            "No"
                        },

                    label = "Time Up"
                )
            }
        }
    }
}

// =========================================================
// PERFORMANCE METRIC
// =========================================================

@Composable
private fun PerformanceMetric(
    value: String,
    label: String
) {

    Column {

        Text(
            text = value,

            fontSize = 17.sp,

            fontWeight =
                FontWeight.Bold,

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
// ANSWER CARD
// =========================================================

@Composable
private fun AnswerCard(
    answer: AnswerDetails
) {

    val isDarkTheme =
        MaterialTheme.colorScheme.background ==
                DarkBackground

    val statusColor =
        when {

            answer.unanswered -> {
                if (isDarkTheme) {
                    QuizWarningDark
                } else {
                    QuizWarningLight
                }
            }

            answer.isCorrect -> {
                if (isDarkTheme) {
                    QuizSuccessDark
                } else {
                    QuizSuccessLight
                }
            }

            else -> {
                if (isDarkTheme) {
                    QuizErrorDark
                } else {
                    QuizErrorLight
                }
            }
        }

    val statusText =
        when {

            answer.unanswered ->
                "UNANSWERED"

            answer.isCorrect ->
                "CORRECT"

            else ->
                "WRONG"
        }

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
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

                Text(
                    text =
                        answer.questionText,

                    modifier =
                        Modifier.weight(1f),

                    fontSize = 15.sp,

                    fontWeight =
                        FontWeight.SemiBold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )

                Spacer(
                    modifier =
                        Modifier.padding(4.dp)
                )

                Surface(
                    shape =
                        RoundedCornerShape(50),

                    color =
                        statusColor.copy(
                            alpha = 0.10f
                        )
                ) {

                    Text(
                        text = statusText,

                        modifier =
                            Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 5.dp
                            ),

                        fontSize = 9.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color = statusColor
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text = "Your answer",

                fontSize = 11.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Text(
                text =
                    if (answer.unanswered) {
                        "Not answered"
                    } else {
                        answer.selectedAnswer
                    },

                fontSize = 14.sp,

                fontWeight =
                    FontWeight.Medium,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )

            if (answer.correctAnswer.isNotBlank()) {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text = "Correct answer",

                    fontSize = 11.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Text(
                    text =
                        answer.correctAnswer,

                    fontSize = 14.sp,

                    fontWeight =
                        FontWeight.Medium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )
            }

            if (answer.explanation.isNotBlank()) {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text = "Explanation",

                    fontSize = 11.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Text(
                    text =
                        answer.explanation,

                    fontSize = 13.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )
            }
        }
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
            text =
                "Unable to load result",

            fontSize = 18.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
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
// TIMESTAMP FORMATTER
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