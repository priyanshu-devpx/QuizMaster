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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
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

private data class QuizDetails(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val difficulty: String = "Easy",
    val duration: Int = 0,
    val passingScore: Int = 0
)

@Composable
fun QuizDetailsScreen(
    quizId: String,
    onBack: () -> Unit,
    onStartQuiz: (String) -> Unit
) {

    BackHandler(onBack = onBack)

    val db = remember {
        FirebaseFirestore.getInstance()
    }

    var quiz by remember {
        mutableStateOf<QuizDetails?>(null)
    }

    var questionCount by remember {
        mutableStateOf(0)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    // ---------------------------------------------------------
    // LOAD QUIZ
    // ---------------------------------------------------------

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
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    errorMessage = "Quiz not found."
                    isLoading = false
                    return@addOnSuccessListener
                }

                val status =
                    document.getString("status")
                        ?.lowercase()
                        ?: "draft"

                // -------------------------------------------------
                // STUDENTS CAN ONLY OPEN PUBLISHED QUIZZES
                // -------------------------------------------------

                if (status != "published") {

                    errorMessage =
                        "This quiz is not currently available."

                    isLoading = false
                    return@addOnSuccessListener
                }

                quiz = QuizDetails(
                    title =
                        document.getString("title")
                            ?: "",

                    description =
                        document.getString("description")
                            ?: "",

                    category =
                        document.getString("category")
                            ?: "General",

                    difficulty =
                        document.getString("difficulty")
                            ?: "Easy",

                    duration =
                        (
                                document.getLong("duration")
                                    ?: 0L
                                ).toInt(),

                    passingScore =
                        (
                                document.getLong("passingScore")
                                    ?: 0L
                                ).toInt()
                )

                // -------------------------------------------------
                // LOAD QUESTION COUNT
                // -------------------------------------------------

                db.collection("publicQuestions")
                    .whereEqualTo("quizId", quizId)
                    .get()
                    .addOnSuccessListener { questions ->

                        questionCount =
                            questions.documents.size

                        isLoading = false
                    }
                    .addOnFailureListener {

                        // Quiz itself is still usable.
                        questionCount = 0
                        isLoading = false
                    }
            }
            .addOnFailureListener { exception ->

                errorMessage =
                    exception.message
                        ?: "Unable to load quiz."

                isLoading = false
            }
    }

    // ---------------------------------------------------------
    // SCREEN
    // ---------------------------------------------------------

    Scaffold { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {

            // =================================================
            // LOADING
            // =================================================

            if (isLoading) {

                CircularProgressIndicator(
                    modifier = Modifier.align(
                        Alignment.Center
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

            }

            // =================================================
            // ERROR
            // =================================================

            else if (errorMessage.isNotBlank()) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(
                        text = "Quiz unavailable",
                        fontSize = 22.sp,
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
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Go Back")
                    }
                }

            }

            // =================================================
            // DETAILS
            // =================================================

            else {

                val currentQuiz = quiz

                if (currentQuiz != null) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(
                                rememberScrollState()
                            )
                    ) {

                        // -----------------------------------------
                        // TOP BAR
                        // -----------------------------------------

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            IconButton(
                                onClick = onBack
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.ArrowBack,
                                    contentDescription =
                                        "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(
                                modifier = Modifier.width(4.dp)
                            )

                            Text(
                                text = "Quiz Details",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // -----------------------------------------
                        // CONTENT
                        // -----------------------------------------

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 20.dp,
                                    vertical = 12.dp
                                )
                        ) {

                            // -------------------------------------
                            // QUIZ HEADER CARD
                            // -------------------------------------

                            Card(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                shape =
                                    RoundedCornerShape(20.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.surface
                                    ),
                                elevation =
                                    CardDefaults.cardElevation(
                                        defaultElevation = 2.dp
                                    )
                            ) {

                                Column(
                                    modifier =
                                        Modifier.padding(20.dp)
                                ) {

                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {

                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(52.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        RoundedCornerShape(
                                                            14.dp
                                                        )
                                                    ),
                                            contentAlignment =
                                                Alignment.Center
                                        ) {

                                            Icon(
                                                imageVector =
                                                    Icons.Default.MenuBook,
                                                contentDescription =
                                                    null,
                                                tint =
                                                    MaterialTheme.colorScheme.primary,
                                                modifier =
                                                    Modifier.size(28.dp)
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
                                                text =
                                                    currentQuiz.title,
                                                fontSize = 22.sp,
                                                fontWeight =
                                                    FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            Spacer(
                                                modifier =
                                                    Modifier.height(5.dp)
                                            )

                                            Text(
                                                text =
                                                    "${currentQuiz.category} • ${currentQuiz.difficulty}",
                                                fontSize = 13.sp,
                                                color =
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (
                                        currentQuiz.description
                                            .isNotBlank()
                                    ) {

                                        Spacer(
                                            modifier =
                                                Modifier.height(18.dp)
                                        )

                                        Text(
                                            text =
                                                currentQuiz.description,
                                            fontSize = 15.sp,
                                            color =
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight =
                                                22.sp
                                        )
                                    }
                                }
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(20.dp)
                            )

                            // -------------------------------------
                            // QUIZ STATS
                            // -------------------------------------

                            Text(
                                text = "Quiz Information",
                                fontSize = 18.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(10.dp)
                            )

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(10.dp)
                            ) {

                                InfoCard(
                                    modifier =
                                        Modifier.weight(1f),
                                    icon =
                                        Icons.Default.Timer,
                                    value =
                                        "${currentQuiz.duration} min",
                                    label = "Duration"
                                )

                                InfoCard(
                                    modifier =
                                        Modifier.weight(1f),
                                    icon =
                                        Icons.Default.MenuBook,
                                    value =
                                        questionCount.toString(),
                                    label = "Questions"
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(10.dp)
                            )

                            Card(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                shape =
                                    RoundedCornerShape(16.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.surface
                                    ),
                                elevation =
                                    CardDefaults.cardElevation(
                                        defaultElevation = 1.dp
                                    )
                            ) {

                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.CheckCircle,
                                        contentDescription =
                                            null,
                                        tint =
                                            MaterialTheme.colorScheme.primary,
                                        modifier =
                                            Modifier.size(24.dp)
                                    )

                                    Spacer(
                                        modifier =
                                            Modifier.width(12.dp)
                                    )

                                    Column {

                                        Text(
                                            text =
                                                "Passing Score",
                                            fontSize = 13.sp,
                                            color =
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier.height(2.dp)
                                        )

                                        Text(
                                            text =
                                                "${currentQuiz.passingScore}%",
                                            fontSize = 18.sp,
                                            fontWeight =
                                                FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(24.dp)
                            )

                            // -------------------------------------
                            // INSTRUCTIONS
                            // -------------------------------------

                            Text(
                                text = "Before You Start",
                                fontSize = 18.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(10.dp)
                            )

                            InstructionRow(
                                text =
                                    "Complete the quiz within the given time."
                            )

                            InstructionRow(
                                text =
                                    "Read each question carefully before answering."
                            )

                            InstructionRow(
                                text =
                                    "Your result will be calculated after submission."
                            )

                            InstructionRow(
                                text =
                                    "Make sure you have a stable connection before starting."
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(28.dp)
                            )

                            // -------------------------------------
                            // START QUIZ
                            // -------------------------------------

                            Button(
                                onClick = {
                                    onStartQuiz(
                                        quizId
                                    )
                                },
                                enabled =
                                    questionCount > 0,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(54.dp),
                                shape =
                                    RoundedCornerShape(12.dp),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.primary,
                                        contentColor =
                                            Color.White,
                                        disabledContainerColor =
                                            MaterialTheme.colorScheme.outlineVariant,
                                        disabledContentColor =
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                            ) {

                                Text(
                                    text =
                                        if (
                                            questionCount > 0
                                        ) {
                                            "Start Quiz"
                                        } else {
                                            "No Questions Available"
                                        },
                                    fontSize = 15.sp,
                                    fontWeight =
                                        FontWeight.SemiBold
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
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
                .padding(16.dp)
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(23.dp)
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InstructionRow(
    text: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment =
            Alignment.Top
    ) {

        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(7.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(50)
                )
        )

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Text(
            text = text,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}
