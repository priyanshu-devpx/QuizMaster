package com.quizmaster.app.ui.admin

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// =========================================================
// QUIZ DATA
// =========================================================

private data class QuizItem(
    val quizId: String = "",
    val title: String = "",
    val category: String = "",
    val difficulty: String = "",
    val status: String = ""
)

// =========================================================
// QUESTION DATA
// =========================================================

private data class QuestionItem(
    val questionId: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val marks: Long = 1L,
    val explanation: String = ""
)

// =========================================================
// MANAGE QUESTIONS SCREEN
// =========================================================

@Composable
fun ManageQuestionsScreen(
    teacherId: String? = null,
    onBack: () -> Unit,
    onAddQuestion: (quizId: String) -> Unit,
    onEditQuestion: (quizId: String, questionId: String) -> Unit
) {

    val db = remember {
        FirebaseFirestore.getInstance()
    }

    val context = LocalContext.current

    var quizzes by remember {
        mutableStateOf<List<QuizItem>>(emptyList())
    }

    var selectedQuiz by remember {
        mutableStateOf<QuizItem?>(null)
    }

    var questions by remember {
        mutableStateOf<List<QuestionItem>>(emptyList())
    }

    var isLoadingQuizzes by remember {
        mutableStateOf(true)
    }

    var isLoadingQuestions by remember {
        mutableStateOf(false)
    }

    var quizDropdownExpanded by remember {
        mutableStateOf(false)
    }

    var deleteQuestion by remember {
        mutableStateOf<QuestionItem?>(null)
    }

    var isDeleting by remember {
        mutableStateOf(false)
    }

    // =====================================================
    // LOAD QUIZZES
    // =====================================================

    LaunchedEffect(teacherId) {

        isLoadingQuizzes = true

        val quizzesQuery = if (teacherId != null && teacherId.isNotBlank()) {
            db.collection("quizzes").whereEqualTo("createdBy", teacherId)
        } else {
            db.collection("quizzes").orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )
        }

        quizzesQuery
            .get()
            .addOnSuccessListener { snapshot ->

                quizzes = snapshot.documents.mapNotNull { document ->

                    val title =
                        document.getString("title")
                            ?: return@mapNotNull null

                    QuizItem(
                        quizId = document.id,
                        title = title,
                        category =
                            document.getString("category")
                                ?: "",
                        difficulty =
                            document.getString("difficulty")
                                ?: "",
                        status =
                            document.getString("status")
                                ?: "draft"
                    )
                }

                if (
                    quizzes.isNotEmpty() &&
                    selectedQuiz == null
                ) {
                    selectedQuiz = quizzes.first()
                }

                isLoadingQuizzes = false
            }
            .addOnFailureListener {

                isLoadingQuizzes = false

                Toast.makeText(
                    context,
                    "Failed to load quizzes",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // =====================================================
    // LOAD QUESTIONS
    // =====================================================

    LaunchedEffect(selectedQuiz?.quizId) {

        val quizId = selectedQuiz?.quizId

        if (quizId.isNullOrBlank()) {

            questions = emptyList()

            return@LaunchedEffect
        }

        isLoadingQuestions = true

        db.collection("questions")
            .whereEqualTo(
                "quizId",
                quizId
            )
            .get()
            .addOnSuccessListener { snapshot ->

                questions =
                    snapshot.documents.map { document ->

                        val options =
                            document.get("options")
                                    as? List<*>
                                ?: emptyList<Any>()

                        QuestionItem(
                            questionId =
                                document.id,

                            question =
                                document.getString(
                                    "question"
                                ) ?: "",

                            options =
                                options.map {
                                    it.toString()
                                },

                            correctAnswer =
                                document.getString(
                                    "correctAnswer"
                                ) ?: "",

                            marks =
                                document.getLong(
                                    "marks"
                                ) ?: 1L,

                            explanation =
                                document.getString(
                                    "explanation"
                                ) ?: ""
                        )
                    }

                isLoadingQuestions = false
            }
            .addOnFailureListener {

                isLoadingQuestions = false
                questions = emptyList()

                Toast.makeText(
                    context,
                    "Failed to load questions",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // =====================================================
    // SCREEN
    // =====================================================

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // =================================================
            // TOP BAR
            // =================================================

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

                        tint =
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(4.dp)
                )

                Text(
                    text = "Manage Questions",

                    color =
                        MaterialTheme.colorScheme.onSurface,

                    fontSize =
                        MaterialTheme.typography
                            .titleLarge.fontSize,

                    fontWeight =
                        FontWeight.Bold
                )
            }

            // =================================================
            // CONTENT
            // =================================================

            LazyColumn(
                modifier =
                    Modifier.fillMaxSize(),

                contentPadding =
                    PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 24.dp
                    ),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                // =================================================
                // SELECT QUIZ
                // =================================================

                item {

                    Text(
                        text = "Select Quiz",

                        style =
                            MaterialTheme.typography
                                .titleMedium,

                        color =
                            MaterialTheme.colorScheme.onSurface,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    if (isLoadingQuizzes) {

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(70.dp),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(28.dp),

                                color =
                                    MaterialTheme.colorScheme.primary
                            )
                        }

                    } else if (quizzes.isEmpty()) {

                        EmptyQuizCard()

                    } else {

                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            OutlinedTextField(
                                value =
                                    selectedQuiz?.title ?: "",

                                onValueChange = {},

                                readOnly = true,

                                modifier =
                                    Modifier.fillMaxWidth(),

                                label = {
                                    Text(
                                        text =
                                            "Choose a quiz"
                                    )
                                },

                                trailingIcon = {

                                    Text(
                                        text =
                                            if (
                                                quizDropdownExpanded
                                            ) {
                                                "▲"
                                            } else {
                                                "▼"
                                            },

                                        color =
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },

                                singleLine = true,

                                shape =
                                    RoundedCornerShape(
                                        12.dp
                                    ),

                                colors =
                                    OutlinedTextFieldDefaults
                                        .colors(

                                            focusedTextColor =
                                                MaterialTheme.colorScheme.onSurface,

                                            unfocusedTextColor =
                                                MaterialTheme.colorScheme.onSurface,

                                            focusedLabelColor =
                                                MaterialTheme.colorScheme.primary,

                                            unfocusedLabelColor =
                                                MaterialTheme.colorScheme.onSurfaceVariant,

                                            focusedBorderColor =
                                                MaterialTheme.colorScheme.primary,

                                            unfocusedBorderColor =
                                                MaterialTheme.colorScheme.outline
                                        )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clickable {

                                        quizDropdownExpanded =
                                            !quizDropdownExpanded
                                    }
                            )

                            DropdownMenu(
                                expanded =
                                    quizDropdownExpanded,

                                onDismissRequest = {

                                    quizDropdownExpanded =
                                        false
                                },

                                modifier =
                                    Modifier.background(
                                        MaterialTheme.colorScheme.surface
                                    )
                            ) {

                                quizzes.forEach { quiz ->

                                    DropdownMenuItem(

                                        text = {

                                            Column {

                                                Text(
                                                    text =
                                                        quiz.title,

                                                    color =
                                                        MaterialTheme.colorScheme.onSurface,

                                                    fontWeight =
                                                        FontWeight
                                                            .Medium
                                                )

                                                Spacer(
                                                    modifier =
                                                        Modifier
                                                            .height(
                                                                2.dp
                                                            )
                                                )

                                                Text(
                                                    text =
                                                        buildQuizDetails(
                                                            quiz
                                                        ),

                                                    style =
                                                        MaterialTheme
                                                            .typography
                                                            .bodySmall,

                                                    color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },

                                        onClick = {

                                            selectedQuiz =
                                                quiz

                                            quizDropdownExpanded =
                                                false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // =================================================
                // QUESTIONS HEADER
                // =================================================

                if (selectedQuiz != null) {

                    item {

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),

                            verticalAlignment =
                                Alignment.CenterVertically,

                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            Column {

                                Text(
                                    text = "Questions",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleLarge,

                                    color =
                                        MaterialTheme.colorScheme.onSurface,

                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(2.dp)
                                )

                                Text(
                                    text =
                                        "${questions.size} question${
                                            if (
                                                questions.size == 1
                                            ) {
                                                ""
                                            } else {
                                                "s"
                                            }
                                        }",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyMedium,

                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {

                                    selectedQuiz?.let { quiz ->

                                        onAddQuestion(
                                            quiz.quizId
                                        )
                                    }
                                },

                                shape =
                                    RoundedCornerShape(
                                        12.dp
                                    ),

                                colors =
                                    ButtonDefaults
                                        .buttonColors(
                                            containerColor =
                                                MaterialTheme.colorScheme.primary,

                                            contentColor =
                                                MaterialTheme.colorScheme.onPrimary
                                        )
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Add,

                                    contentDescription =
                                        "Add question",

                                    modifier =
                                        Modifier.size(18.dp)
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(6.dp)
                                )

                                Text(
                                    text = "Add",

                                    fontWeight =
                                        FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isLoadingQuestions) {

                        item {

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(260.dp),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                CircularProgressIndicator(
                                    color =
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                    } else if (questions.isEmpty()) {

                        item {

                            EmptyQuestionState(
                                onAddQuestion = {

                                    selectedQuiz?.let { quiz ->

                                        onAddQuestion(
                                            quiz.quizId
                                        )
                                    }
                                }
                            )
                        }

                    } else {

                        items(
                            items = questions,

                            key = {
                                it.questionId
                            }
                        ) { question ->

                            QuestionCard(

                                question =
                                    question,

                                onEdit = {

                                    selectedQuiz?.let { quiz ->

                                        onEditQuestion(
                                            quiz.quizId,
                                            question.questionId
                                        )
                                    }
                                },

                                onDelete = {

                                    deleteQuestion =
                                        question
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    deleteQuestion?.let { question ->

        AlertDialog(

            onDismissRequest = {

                if (!isDeleting) {
                    deleteQuestion = null
                }
            },

            title = {

                Text(
                    text = "Delete Question?",

                    color =
                        MaterialTheme.colorScheme.onSurface,

                    fontWeight =
                        FontWeight.Bold
                )
            },

            text = {

                Text(
                    text =
                        "This question will be permanently deleted. This action cannot be undone.",

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            },

            confirmButton = {

                Button(

                    onClick = {

                        if (isDeleting) {
                            return@Button
                        }

                        isDeleting = true

                        val questionRef = db.collection("questions")
                            .document(question.questionId)

                        val publicQuestionRef =
                            db.collection("publicQuestions")
                                .document(question.questionId)

                        db.runBatch { batch ->

                            batch.delete(questionRef)

                            batch.delete(publicQuestionRef)
                        }
                            .addOnSuccessListener {

                                questions =
                                    questions.filter {
                                        it.questionId !=
                                                question.questionId
                                    }

                                isDeleting = false
                                deleteQuestion = null

                                Toast.makeText(
                                    context,
                                    "Question deleted",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { exception ->

                                isDeleting = false

                                Toast.makeText(
                                    context,
                                    exception.localizedMessage
                                        ?: "Failed to delete question",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    },

                    enabled =
                        !isDeleting,

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                MaterialTheme.colorScheme.error,

                            contentColor =
                                MaterialTheme.colorScheme.onError
                        )
                ) {

                    if (isDeleting) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(18.dp),

                            color =
                                MaterialTheme.colorScheme.onError,

                            strokeWidth =
                                2.dp
                        )

                    } else {

                        Text(
                            text = "Delete",

                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }
            },

            dismissButton = {

                OutlinedButton(

                    onClick = {
                        deleteQuestion = null
                    },

                    enabled =
                        !isDeleting
                ) {

                    Text(
                        text = "Cancel"
                    )
                }
            }
        )
    }
}

@Composable
private fun EmptyQuizCard() {

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

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector =
                    Icons.Default.Quiz,

                contentDescription = null,

                modifier =
                    Modifier.size(42.dp),

                tint =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Text(
                text =
                    "No quizzes found",

                color =
                    MaterialTheme.colorScheme.onSurface,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
                text =
                    "Create a quiz first before adding questions.",

                style =
                    MaterialTheme.typography.bodyMedium,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyQuestionState(
    onAddQuestion: () -> Unit
) {

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

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector =
                    Icons.Default.Quiz,

                contentDescription = null,

                modifier =
                    Modifier.size(48.dp),

                tint =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text =
                    "No questions yet",

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                color =
                    MaterialTheme.colorScheme.onSurface,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(
                text =
                    "Add the first question to this quiz.",

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Button(
                onClick =
                    onAddQuestion,

                shape =
                    RoundedCornerShape(12.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            MaterialTheme.colorScheme.primary,

                        contentColor =
                            MaterialTheme.colorScheme.onPrimary
                    )
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Add,

                    contentDescription =
                        "Add question"
                )

                Spacer(
                    modifier =
                        Modifier.width(6.dp)
                )

                Text(
                    text =
                        "Add Question",

                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: QuestionItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

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
                        question.question,

                    modifier =
                        Modifier.weight(1f),

                    color =
                        MaterialTheme.colorScheme.onSurface,

                    fontWeight =
                        FontWeight.SemiBold,

                    maxLines = 3,

                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier =
                        Modifier.width(4.dp)
                )

                IconButton(
                    onClick =
                        onEdit
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Edit,

                        contentDescription =
                            "Edit question",

                        tint =
                            MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick =
                        onDelete
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Delete,

                        contentDescription =
                            "Delete question",

                        tint =
                            MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            question.options.forEachIndexed {
                    index,
                    option ->

                val optionLetter =
                    ('A'.code + index).toChar()

                Text(
                    text =
                        "$optionLetter. $option",

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 3.dp
                            ),

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,

                    maxLines = 2,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Box(
                    modifier =
                        Modifier
                            .background(
                                color =
                                    MaterialTheme.colorScheme.primaryContainer,

                                shape =
                                    RoundedCornerShape(8.dp)
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            )
                ) {

                    Text(
                        text =
                            "Answer: ${question.correctAnswer}",

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium,

                        color =
                            MaterialTheme.colorScheme.onPrimaryContainer,

                        fontWeight =
                            FontWeight.Medium
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .background(
                                color =
                                    MaterialTheme.colorScheme.surfaceVariant,

                                shape =
                                    RoundedCornerShape(8.dp)
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            )
                ) {

                    Text(
                        text =
                            "${question.marks} mark${
                                if (
                                    question.marks == 1L
                                ) {
                                    ""
                                } else {
                                    "s"
                                }
                            }",

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (
                question.explanation.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Text(
                    text =
                        "Explanation: ${question.explanation}",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,

                    maxLines = 2,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun buildQuizDetails(
    quiz: QuizItem
): String {

    val category =
        quiz.category.ifBlank {
            "General"
        }

    val difficulty =
        quiz.difficulty.ifBlank {
            "Not set"
        }

    val status =
        quiz.status.ifBlank {
            "draft"
        }

    return "$category • $difficulty • $status"
}
