package com.quizmaster.app.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

// =====================================================================
// EDIT QUESTION SCREEN
// =====================================================================

@Composable
fun EditQuestionScreen(
    quizId: String,
    questionId: String,
    onBack: () -> Unit,
    onQuestionUpdated: () -> Unit
) {

    // -----------------------------------------------------------------
    // FIREBASE
    // -----------------------------------------------------------------

    val db = remember {
        FirebaseFirestore.getInstance()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val context = LocalContext.current

    // -----------------------------------------------------------------
    // FORM STATES
    // -----------------------------------------------------------------

    var questionText by remember {
        mutableStateOf("")
    }

    var optionA by remember {
        mutableStateOf("")
    }

    var optionB by remember {
        mutableStateOf("")
    }

    var optionC by remember {
        mutableStateOf("")
    }

    var optionD by remember {
        mutableStateOf("")
    }

    var correctAnswer by remember {
        mutableStateOf("")
    }

    var marks by remember {
        mutableStateOf("1")
    }

    var explanation by remember {
        mutableStateOf("")
    }

    // -----------------------------------------------------------------
    // UI STATES
    // -----------------------------------------------------------------

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var showErrors by remember {
        mutableStateOf(false)
    }

    var correctAnswerExpanded by remember {
        mutableStateOf(false)
    }

    // =================================================================
    // LOAD EXISTING QUESTION
    // =================================================================

    LaunchedEffect(questionId) {

        isLoading = true

        db.collection("questions")
            .document(questionId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    Toast.makeText(
                        context,
                        "Question not found.",
                        Toast.LENGTH_LONG
                    ).show()

                    isLoading = false
                    onBack()

                    return@addOnSuccessListener
                }

                val savedQuizId =
                    document.getString("quizId")
                        ?: ""

                if (savedQuizId != quizId) {

                    Toast.makeText(
                        context,
                        "This question does not belong to the selected quiz.",
                        Toast.LENGTH_LONG
                    ).show()

                    isLoading = false
                    onBack()

                    return@addOnSuccessListener
                }

                questionText =
                    document.getString("question")
                        ?: ""

                val savedOptions =
                    document.get("options")
                            as? List<*>
                        ?: emptyList<Any>()

                optionA =
                    savedOptions
                        .getOrNull(0)
                        ?.toString()
                        ?: ""

                optionB =
                    savedOptions
                        .getOrNull(1)
                        ?.toString()
                        ?: ""

                optionC =
                    savedOptions
                        .getOrNull(2)
                        ?.toString()
                        ?: ""

                optionD =
                    savedOptions
                        .getOrNull(3)
                        ?.toString()
                        ?: ""

                correctAnswer =
                    document.getString("correctAnswer")
                        ?.uppercase()
                        ?: ""

                marks =
                    (
                            document.getLong("marks")
                                ?: 1L
                            ).toString()

                explanation =
                    document.getString("explanation")
                        ?: ""

                isLoading = false
            }
            .addOnFailureListener { exception ->

                isLoading = false

                Toast.makeText(
                    context,
                    exception.localizedMessage
                        ?: "Failed to load question.",
                    Toast.LENGTH_LONG
                ).show()

                onBack()
            }
    }

    // =================================================================
    // CLEAN VALUES
    // =================================================================

    val cleanQuestion =
        questionText.trim()

    val cleanOptionA =
        optionA.trim()

    val cleanOptionB =
        optionB.trim()

    val cleanOptionC =
        optionC.trim()

    val cleanOptionD =
        optionD.trim()

    val cleanExplanation =
        explanation.trim()

    val marksValue =
        marks.toIntOrNull()

    // =================================================================
    // VALIDATION
    // =================================================================

    val questionValid =
        cleanQuestion.length >= 5

    val optionAValid =
        cleanOptionA.isNotBlank()

    val optionBValid =
        cleanOptionB.isNotBlank()

    val optionCValid =
        cleanOptionC.isNotBlank()

    val optionDValid =
        cleanOptionD.isNotBlank()

    val optionsValid =
        optionAValid &&
                optionBValid &&
                optionCValid &&
                optionDValid

    val correctAnswerValid =
        when (correctAnswer) {
            "A" -> optionAValid
            "B" -> optionBValid
            "C" -> optionCValid
            "D" -> optionDValid
            else -> false
        }

    val marksValid =
        marksValue != null &&
                marksValue in 1..100

    val formValid =
        questionValid &&
                optionsValid &&
                correctAnswerValid &&
                marksValid

    // =================================================================
    // MAIN SCREEN
    // =================================================================

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
    ) {

        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {

            // =========================================================
            // HEADER
            // =========================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = {

                        if (!isSaving) {
                            onBack()
                        }
                    },

                    enabled =
                        !isSaving
                ) {

                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Filled.ArrowBack,

                        contentDescription =
                            "Back",

                        tint =
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Edit Question",

                        style =
                            MaterialTheme.typography.titleLarge,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )

                    Text(
                        text =
                            "Update the existing question",

                        style =
                            MaterialTheme.typography.bodySmall,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(11.dp)
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Save,

                        contentDescription =
                            null,

                        tint =
                            MaterialTheme.colorScheme.primary,

                        modifier =
                            Modifier.size(20.dp)
                    )
                }
            }

            // =========================================================
            // CONTENT
            // =========================================================

            if (isLoading) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.background
                        ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator(
                        color =
                            MaterialTheme.colorScheme.primary,

                        strokeWidth = 3.dp
                    )
                }

            } else {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(
                            horizontal = 18.dp,
                            vertical = 18.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(16.dp)
                ) {

                    // =================================================
                    // INTRO CARD
                    // =================================================

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(18.dp),

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
                                    .padding(18.dp),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(12.dp)
                                    ),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Save,

                                    contentDescription =
                                        null,

                                    tint =
                                        MaterialTheme.colorScheme.primary,

                                    modifier =
                                        Modifier.size(23.dp)
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
                                        "Question Details",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,

                                    fontWeight =
                                        FontWeight.Bold,

                                    color =
                                        MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(3.dp)
                                )

                                Text(
                                    text =
                                        "Edit the question, answer options, correct answer and marks.",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall,

                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // =================================================
                    // QUESTION
                    // =================================================

                    SectionCard(
                        title =
                            "Question",

                        subtitle =
                            "Edit the question students will see."
                    ) {

                        StyledTextField(
                            value =
                                questionText,

                            onValueChange = {
                                questionText = it
                            },

                            label =
                                "Question",

                            placeholder =
                                "Enter your question",

                            singleLine =
                                false,

                            minLines =
                                4,

                            maxLines =
                                6,

                            isError =
                                showErrors &&
                                        !questionValid
                        )

                        if (
                            showErrors &&
                            !questionValid
                        ) {

                            ErrorText(
                                text =
                                    "Question must contain at least 5 characters."
                            )
                        }
                    }

                    // =================================================
                    // ANSWER OPTIONS
                    // =================================================

                    SectionCard(
                        title =
                            "Answer Options",

                        subtitle =
                            "Edit all four possible answers."
                    ) {

                        OptionField(
                            option = "A",

                            value = optionA,

                            onValueChange = {
                                optionA = it
                            },

                            isError =
                                showErrors &&
                                        !optionAValid
                        )

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        OptionField(
                            option = "B",

                            value = optionB,

                            onValueChange = {
                                optionB = it
                            },

                            isError =
                                showErrors &&
                                        !optionBValid
                        )

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        OptionField(
                            option = "C",

                            value = optionC,

                            onValueChange = {
                                optionC = it
                            },

                            isError =
                                showErrors &&
                                        !optionCValid
                        )

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        OptionField(
                            option = "D",

                            value = optionD,

                            onValueChange = {
                                optionD = it
                            },

                            isError =
                                showErrors &&
                                        !optionDValid
                        )

                        if (
                            showErrors &&
                            !optionsValid
                        ) {

                            ErrorText(
                                text =
                                    "All four options are required."
                            )
                        }
                    }

                    // =================================================
                    // CORRECT ANSWER
                    // =================================================

                    SectionCard(
                        title =
                            "Correct Answer",

                        subtitle =
                            "Select the option that is correct."
                    ) {

                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            OutlinedTextField(
                                value =
                                    if (
                                        correctAnswer.isBlank()
                                    ) {
                                        ""
                                    } else {
                                        "Option $correctAnswer"
                                    },

                                onValueChange = {},

                                readOnly = true,

                                modifier =
                                    Modifier.fillMaxWidth(),

                                label = {
                                    Text(
                                        text =
                                            "Correct Answer"
                                    )
                                },

                                placeholder = {
                                    Text(
                                        text =
                                            "Choose A, B, C or D"
                                    )
                                },

                                trailingIcon = {

                                    Icon(
                                        imageVector =
                                            Icons.Default.KeyboardArrowDown,

                                        contentDescription =
                                            "Select correct answer",

                                        tint =
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },

                                shape =
                                    RoundedCornerShape(12.dp),

                                isError =
                                    showErrors &&
                                            !correctAnswerValid,

                                colors =
                                    editQuestionTextFieldColors()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clickable(
                                        enabled =
                                            !isSaving
                                    ) {

                                        correctAnswerExpanded =
                                            !correctAnswerExpanded
                                    }
                            )

                            DropdownMenu(
                                expanded =
                                    correctAnswerExpanded,

                                onDismissRequest = {

                                    correctAnswerExpanded =
                                        false
                                },

                                modifier =
                                    Modifier.background(
                                        MaterialTheme.colorScheme.surface
                                    )
                            ) {

                                DropdownMenuItem(
                                    enabled =
                                        optionAValid,

                                    text = {

                                        AnswerDropdownText(
                                            letter = "A",

                                            answer = cleanOptionA,

                                            enabled = optionAValid
                                        )
                                    },

                                    onClick = {

                                        correctAnswer = "A"

                                        correctAnswerExpanded = false

                                        showErrors = false
                                    },

                                    leadingIcon = {

                                        if (
                                            correctAnswer == "A"
                                        ) {

                                            Icon(
                                                imageVector =
                                                    Icons.Default.Check,

                                                contentDescription =
                                                    "Selected",

                                                tint =
                                                    MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    enabled =
                                        optionBValid,

                                    text = {

                                        AnswerDropdownText(
                                            letter = "B",

                                            answer = cleanOptionB,

                                            enabled = optionBValid
                                        )
                                    },

                                    onClick = {

                                        correctAnswer = "B"

                                        correctAnswerExpanded = false

                                        showErrors = false
                                    },

                                    leadingIcon = {

                                        if (
                                            correctAnswer == "B"
                                        ) {

                                            Icon(
                                                imageVector =
                                                    Icons.Default.Check,

                                                contentDescription =
                                                    "Selected",

                                                tint =
                                                    MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    enabled =
                                        optionCValid,

                                    text = {

                                        AnswerDropdownText(
                                            letter = "C",

                                            answer = cleanOptionC,

                                            enabled = optionCValid
                                        )
                                    },

                                    onClick = {

                                        correctAnswer = "C"

                                        correctAnswerExpanded = false

                                        showErrors = false
                                    },

                                    leadingIcon = {

                                        if (
                                            correctAnswer == "C"
                                        ) {

                                            Icon(
                                                imageVector =
                                                    Icons.Default.Check,

                                                contentDescription =
                                                    "Selected",

                                                tint =
                                                    MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    enabled =
                                        optionDValid,

                                    text = {

                                        AnswerDropdownText(
                                            letter = "D",

                                            answer = cleanOptionD,

                                            enabled = optionDValid
                                        )
                                    },

                                    onClick = {

                                        correctAnswer = "D"

                                        correctAnswerExpanded = false

                                        showErrors = false
                                    },

                                    leadingIcon = {

                                        if (
                                            correctAnswer == "D"
                                        ) {

                                            Icon(
                                                imageVector =
                                                    Icons.Default.Check,

                                                contentDescription =
                                                    "Selected",

                                                tint =
                                                    MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        if (
                            showErrors &&
                            !correctAnswerValid
                        ) {

                            ErrorText(
                                text =
                                    if (
                                        correctAnswer.isBlank()
                                    ) {
                                        "Please select the correct answer."
                                    } else {
                                        "The selected correct answer cannot be empty."
                                    }
                            )
                        }
                    }

                    // =================================================
                    // MARKS
                    // =================================================

                    SectionCard(
                        title =
                            "Marks",

                        subtitle =
                            "Set the marks for this question."
                    ) {

                        StyledTextField(
                            value =
                                marks,

                            onValueChange = { value ->

                                if (
                                    value.isEmpty() ||
                                    value.all {
                                        it.isDigit()
                                    }
                                ) {

                                    marks = value
                                }
                            },

                            label =
                                "Marks",

                            placeholder =
                                "e.g. 1",

                            singleLine =
                                true,

                            isError =
                                showErrors &&
                                        !marksValid,

                            keyboardType =
                                KeyboardType.Number
                        )

                        if (
                            showErrors &&
                            !marksValid
                        ) {

                            ErrorText(
                                text =
                                    "Marks must be between 1 and 100."
                            )
                        }
                    }

                    // =================================================
                    // EXPLANATION
                    // =================================================

                    SectionCard(
                        title =
                            "Explanation",

                        subtitle =
                            "Optional explanation shown after answering."
                    ) {

                        StyledTextField(
                            value =
                                explanation,

                            onValueChange = {
                                explanation = it
                            },

                            label =
                                "Explanation (Optional)",

                            placeholder =
                                "Explain why this answer is correct.",

                            singleLine =
                                false,

                            minLines =
                                4,

                            maxLines =
                                6,

                            isError =
                                false
                        )
                    }

                    // =================================================
                    // UPDATE BUTTON
                    // =================================================

                    Button(
                        onClick = {

                            showErrors =
                                true

                            if (!formValid) {
                                return@Button
                            }

                            if (isSaving) {
                                return@Button
                            }

                            val adminUser =
                                auth.currentUser

                            if (
                                adminUser == null
                            ) {

                                Toast.makeText(
                                    context,
                                    "Admin session expired. Please login again.",
                                    Toast.LENGTH_LONG
                                ).show()

                                onBack()

                                return@Button
                            }

                            if (
                                quizId.isBlank() ||
                                questionId.isBlank()
                            ) {

                                Toast.makeText(
                                    context,
                                    "Invalid question information.",
                                    Toast.LENGTH_LONG
                                ).show()

                                onBack()

                                return@Button
                            }

                            val finalMarks =
                                marksValue

                            if (
                                finalMarks == null
                            ) {

                                Toast.makeText(
                                    context,
                                    "Invalid marks.",
                                    Toast.LENGTH_LONG
                                ).show()

                                return@Button
                            }

                            isSaving =
                                true

                            val updatedData =
                                hashMapOf<String, Any>(

                                    "question" to
                                            cleanQuestion,

                                    "options" to
                                            listOf(
                                                cleanOptionA,
                                                cleanOptionB,
                                                cleanOptionC,
                                                cleanOptionD
                                            ),

                                    "correctAnswer" to
                                            correctAnswer,

                                    "marks" to
                                            finalMarks,

                                    "explanation" to
                                            cleanExplanation,

                                    "updatedBy" to
                                            adminUser.uid,

                                    "updatedAt" to
                                            FieldValue.serverTimestamp()
                                )

                            val publicQuestionData =
                                hashMapOf<String, Any>(
                                    "questionId" to questionId,
                                    "quizId" to quizId,
                                    "question" to cleanQuestion,
                                    "options" to listOf(
                                        cleanOptionA,
                                        cleanOptionB,
                                        cleanOptionC,
                                        cleanOptionD
                                    ),
                                    "marks" to finalMarks,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )

                            val questionRef =
                                db.collection("questions")
                                    .document(questionId)

                            val publicQuestionRef =
                                db.collection("publicQuestions")
                                    .document(questionId)

                            val batch =
                                db.batch()

                            batch.update(
                                questionRef,
                                updatedData
                            )

                            batch.set(
                                publicQuestionRef,
                                publicQuestionData
                            )

                            batch.commit()
                                .addOnSuccessListener {

                                    isSaving =
                                        false

                                    Toast.makeText(
                                        context,
                                        "Question updated successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    onQuestionUpdated()
                                }

                                .addOnFailureListener { exception ->

                                    isSaving =
                                        false

                                    Toast.makeText(
                                        context,
                                        exception.localizedMessage
                                            ?: "Failed to update question.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),

                        enabled =
                            !isSaving,

                        shape =
                            RoundedCornerShape(14.dp),

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    MaterialTheme.colorScheme.primary,

                                contentColor =
                                    MaterialTheme.colorScheme.onPrimary,

                                disabledContainerColor =
                                    MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.65f
                                    )
                            )
                    ) {

                        if (isSaving) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(21.dp),

                                color =
                                    MaterialTheme.colorScheme.onPrimary,

                                strokeWidth =
                                    2.dp
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(10.dp)
                            )

                            Text(
                                text =
                                    "Updating Question...",

                                fontWeight =
                                    FontWeight.SemiBold
                            )

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Default.Save,

                                contentDescription =
                                    null,

                                modifier =
                                    Modifier.size(20.dp)
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(9.dp)
                            )

                            Text(
                                text =
                                    "Update Question",

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )
                }
            }
        }
    }
}

// =====================================================================
// SECTION CARD
// =====================================================================

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(17.dp),

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            Text(
                text =
                    title,

                style =
                    MaterialTheme.typography.titleMedium,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(
                text =
                    subtitle,

                style =
                    MaterialTheme.typography.bodySmall,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(15.dp)
            )

            content()
        }
    }
}

// =====================================================================
// TEXT FIELD
// =====================================================================

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean,
    isError: Boolean,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {

    OutlinedTextField(
        value =
            value,

        onValueChange =
            onValueChange,

        modifier =
            Modifier.fillMaxWidth(),

        label = {
            Text(
                text =
                    label
            )
        },

        placeholder = {
            Text(
                text =
                    placeholder,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        },

        singleLine =
            singleLine,

        minLines =
            minLines,

        maxLines =
            maxLines,

        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    keyboardType
            ),

        shape =
            RoundedCornerShape(12.dp),

        isError =
            isError,

        colors =
            editQuestionTextFieldColors()
    )
}

// =====================================================================
// OPTION FIELD
// =====================================================================

@Composable
private fun OptionField(
    option: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color =
                        MaterialTheme.colorScheme.primaryContainer,

                    shape =
                        RoundedCornerShape(10.dp)
                ),

            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text =
                    option,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme.colorScheme.primary
            )
        }

        Spacer(
            modifier =
                Modifier.width(10.dp)
        )

        OutlinedTextField(
            value =
                value,

            onValueChange =
                onValueChange,

            modifier =
                Modifier.weight(1f),

            label = {
                Text(
                    text =
                        "Option $option"
                )
            },

            placeholder = {
                Text(
                    text =
                        "Enter option $option",

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            },

            singleLine =
                true,

            shape =
                RoundedCornerShape(12.dp),

            isError =
                isError,

            colors =
                editQuestionTextFieldColors()
        )
    }
}

// =====================================================================
// ANSWER DROPDOWN TEXT
// =====================================================================

@Composable
private fun AnswerDropdownText(
    letter: String,
    answer: String,
    enabled: Boolean
) {

    val titleColor =
        if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.55f
            )
        }

    val answerColor =
        if (enabled) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.45f
            )
        }

    Column {

        Text(
            text =
                "Option $letter",

            fontWeight =
                FontWeight.SemiBold,

            color =
                titleColor
        )

        if (
            answer.isNotBlank()
        ) {

            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )

            Text(
                text =
                    answer,

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                color =
                    answerColor
            )
        } else {

            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )

            Text(
                text =
                    "Not filled",

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                color =
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

// =====================================================================
// ERROR TEXT
// =====================================================================

@Composable
private fun ErrorText(
    text: String
) {

    Text(
        text =
            text,

        style =
            MaterialTheme.typography.bodySmall,

        color =
            MaterialTheme.colorScheme.error,

        modifier =
            Modifier.padding(
                start = 4.dp,
                top = 5.dp
            )
    )
}

// =====================================================================
// TEXT FIELD COLORS
// =====================================================================

@Composable
private fun editQuestionTextFieldColors() =
    OutlinedTextFieldDefaults.colors(

        focusedTextColor =
            MaterialTheme.colorScheme.onSurface,

        unfocusedTextColor =
            MaterialTheme.colorScheme.onSurface,

        disabledTextColor =
            MaterialTheme.colorScheme.onSurfaceVariant,

        errorTextColor =
            MaterialTheme.colorScheme.onSurface,

        cursorColor =
            MaterialTheme.colorScheme.primary,

        errorCursorColor =
            MaterialTheme.colorScheme.error,

        focusedLabelColor =
            MaterialTheme.colorScheme.primary,

        unfocusedLabelColor =
            MaterialTheme.colorScheme.onSurfaceVariant,

        errorLabelColor =
            MaterialTheme.colorScheme.error,

        focusedBorderColor =
            MaterialTheme.colorScheme.primary,

        unfocusedBorderColor =
            MaterialTheme.colorScheme.outlineVariant,

        errorBorderColor =
            MaterialTheme.colorScheme.error,

        focusedPlaceholderColor =
            MaterialTheme.colorScheme.onSurfaceVariant,

        unfocusedPlaceholderColor =
            MaterialTheme.colorScheme.onSurfaceVariant,

        errorPlaceholderColor =
            MaterialTheme.colorScheme.onSurfaceVariant
    )
