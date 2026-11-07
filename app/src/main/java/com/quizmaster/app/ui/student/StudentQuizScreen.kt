package com.quizmaster.app.ui.student

import android.os.CountDownTimer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import com.google.firebase.firestore.FirebaseFirestore
import com.quizmaster.app.data.StudentActivityRepository

private data class StudentQuestion(
    val questionId: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val marks: Long = 1L,
    val correctAnswer: String = ""
)

@Composable
fun StudentQuizScreen(
    quizId: String,
    onBack: () -> Unit,
    onSubmit: (String, Map<String, String>, Boolean) -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }

    var questions by remember { mutableStateOf<List<StudentQuestion>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<String, String>() }
    val marked = remember { mutableStateMapOf<String, Boolean>() }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var quizTitle by remember { mutableStateOf("Quiz") }
    var durationMinutes by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(0) }
    var showNavigator by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    BackHandler(enabled = !submitted) {
        showExitDialog = true
    }

    LaunchedEffect(quizId) {
        if (quizId.isBlank()) {
            errorMessage = "Invalid quiz."
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = ""

        db.collection("quizzes")
            .document(quizId)
            .get()
            .addOnSuccessListener { quizDoc ->
                if (!quizDoc.exists()) {
                    errorMessage = "Quiz not found."
                    isLoading = false
                    return@addOnSuccessListener
                }

                val status = quizDoc.getString("status")?.lowercase() ?: "draft"
                if (status != "published") {
                    errorMessage = "This quiz is not currently available."
                    isLoading = false
                    return@addOnSuccessListener
                }

                quizTitle = quizDoc.getString("title") ?: "Quiz"
                durationMinutes = (quizDoc.getLong("duration") ?: 0L).toInt()
                remainingSeconds = durationMinutes * 60

                db.collection("publicQuestions")
                    .whereEqualTo("quizId", quizId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        questions = snapshot.documents.mapNotNull { doc ->
                            val text = doc.getString("question") ?: return@mapNotNull null
                            val rawOptions = doc.get("options") as? List<*> ?: emptyList<Any>()
                            val options = rawOptions.map { it.toString() }.filter { it.isNotBlank() }

                            if (text.isBlank() || options.size != 4) return@mapNotNull null

                            StudentQuestion(
                                questionId = doc.id,
                                question = text,
                                options = options,
                                marks = doc.getLong("marks") ?: 1L,
                                correctAnswer = ""
                            )
                        }.sortedBy { it.questionId }

                        if (questions.isEmpty()) {
                            errorMessage = "No questions are available for this quiz."
                        } else {
                            StudentActivityRepository.startQuiz(
                                quizId = quizId,
                                quizTitle = quizTitle,
                                onSuccess = {},
                                onError = {
                                    // Attendance failure must never block the quiz.
                                }
                            )
                        }

                        isLoading = false
                    }
                    .addOnFailureListener { exception ->
                        errorMessage = exception.localizedMessage ?: "Unable to load questions."
                        isLoading = false
                    }
            }
            .addOnFailureListener { exception ->
                errorMessage = exception.localizedMessage ?: "Unable to load quiz."
                isLoading = false
            }
    }

    DisposableEffect(questions, durationMinutes) {
        if (questions.isEmpty() || durationMinutes <= 0 || submitted) {
            onDispose { }
        } else {
            val timer = object : CountDownTimer(
                remainingSeconds.coerceAtLeast(1) * 1000L,
                1000L
            ) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingSeconds = (millisUntilFinished / 1000L).toInt()
                }

                override fun onFinish() {
                    remainingSeconds = 0
                    if (!submitted) {
                        submitted = true

                        StudentActivityRepository.finishQuiz(
                            onSuccess = {},
                            onError = {
                                // Attendance failure must never block timed submission.
                            }
                        )

                        onSubmit(quizId, answers.toMap(), true)
                    }
                }
            }
            timer.start()
            onDispose { timer.cancel() }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (errorMessage.isNotBlank()) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Unable to start quiz", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val question = questions[currentIndex]
    val selected = answers[question.questionId]
    val isMarked = marked[question.questionId] == true
    val answeredCount = answers.size
    val progress = (currentIndex + 1).toFloat() / questions.size.toFloat()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (!submitted) showExitDialog = true }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        quizTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Question ${currentIndex + 1} of ${questions.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .background(
                            if (remainingSeconds <= 60) Color(0xFFFFF7ED) else MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        null,
                        tint = if (remainingSeconds <= 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        formatTime(remainingSeconds),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingSeconds <= 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(5.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Question ${currentIndex + 1}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        onClick = {
                            marked[question.questionId] = !isMarked
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Flag,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isMarked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(if (isMarked) "Marked" else "Mark")
                    }
                }

                Spacer(Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        question.question,
                        modifier = Modifier.padding(20.dp),
                        fontSize = 20.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(16.dp))

                question.options.forEachIndexed { index, option ->
                    val letter = ('A'.code + index).toChar().toString()
                    val selectedThis = selected == letter

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .border(
                                width = if (selectedThis) 2.dp else 1.dp,
                                color = if (selectedThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                answers[question.questionId] = letter
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedThis) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        if (selectedThis) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        CircleShape
                                    )
                                    .border(
                                        1.dp,
                                        if (selectedThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedThis) {
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                                } else {
                                    Text(letter, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Text(
                                option,
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 21.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showNavigator = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Question Navigator")
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, null)
                        Spacer(Modifier.width(3.dp))
                        Text("Previous")
                    }

                    if (currentIndex < questions.lastIndex) {
                        Button(
                            onClick = { currentIndex++ },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(3.dp))
                            Icon(Icons.Default.KeyboardArrowRight, null)
                        }
                    } else {
                        Button(
                            onClick = { showSubmitDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Submit")
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "$answeredCount of ${questions.size} answered",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showNavigator) {
        AlertDialog(
            onDismissRequest = { showNavigator = false },
            title = {
                Text("Question Navigator", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column {
                    Text(
                        "Answered: $answeredCount/${questions.size}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.height(230.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(questions.indices.toList()) { index ->
                            val q = questions[index]
                            val answered = answers.containsKey(q.questionId)
                            val markedQuestion = marked[q.questionId] == true

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        when {
                                            index == currentIndex -> MaterialTheme.colorScheme.primary
                                            answered -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surface
                                        },
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (markedQuestion) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        currentIndex = index
                                        showNavigator = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (index == currentIndex) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNavigator = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    "Leave Quiz?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "Your current answers will not be submitted. Are you sure you want to leave?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false

                        StudentActivityRepository.finishQuiz(
                            onSuccess = {
                                onBack()
                            },
                            onError = {
                                // Leaving the quiz must still work if attendance
                                // update fails.
                                onBack()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Stay")
                }
            }
        )
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = {
                Text("Submit Quiz?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(
                    "You have answered $answeredCount of ${questions.size} questions. Are you sure you want to submit?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        submitted = true

                        StudentActivityRepository.finishQuiz(
                            onSuccess = {},
                            onError = {
                                // Attendance failure must never block result submission.
                            }
                        )

                        onSubmit(quizId, answers.toMap(), false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatTime(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return "%02d:%02d".format(minutes, seconds)
}
