package com.quizmaster.app.ui.admin

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.quizmaster.app.data.preferences.AdminSettingsStorage

// =====================================================================
// CREATE QUIZ SCREEN
// =====================================================================

@Composable
fun CreateQuizScreen(
    onBack: () -> Unit,
    onQuizCreated: (String) -> Unit
) {

    val firestore = remember {
        FirebaseFirestore.getInstance()
    }

    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val context = LocalContext.current

    val defaultDuration = remember {
        AdminSettingsStorage.getDefaultDuration(context)
    }

    val defaultPassingScore = remember {
        AdminSettingsStorage.getDefaultPassingScore(context)
    }

    // -----------------------------------------------------------------
    // FORM STATES
    // -----------------------------------------------------------------

    var title by rememberSaveable {
        mutableStateOf("")
    }

    var description by rememberSaveable {
        mutableStateOf("")
    }

    var category by rememberSaveable {
        mutableStateOf("")
    }

    var difficulty by rememberSaveable {
        mutableStateOf("Medium")
    }

    var duration by rememberSaveable {
        mutableStateOf(defaultDuration.toString())
    }

    var passingScore by rememberSaveable {
        mutableStateOf(defaultPassingScore.toString())
    }

    var assignedSubjectId by rememberSaveable {
        mutableStateOf("")
    }

    var assignedSubjectName by rememberSaveable {
        mutableStateOf("")
    }

    androidx.compose.runtime.LaunchedEffect(auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        firestore.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val subId = doc.getString("assignedSubjectId") ?: ""
                assignedSubjectId = subId
                if (subId.isNotBlank()) {
                    firestore.collection("subjects").document(subId).get().addOnSuccessListener { subDoc ->
                        if (subDoc.exists()) {
                            val subName = subDoc.getString("name") ?: ""
                            assignedSubjectName = subName
                            if (category.isBlank()) {
                                category = subName
                            }
                        }
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // ERROR STATES
    // -----------------------------------------------------------------

    var titleError by rememberSaveable {
        mutableStateOf(false)
    }

    var descriptionError by rememberSaveable {
        mutableStateOf(false)
    }

    var categoryError by rememberSaveable {
        mutableStateOf(false)
    }

    var durationError by rememberSaveable {
        mutableStateOf(false)
    }

    var passingScoreError by rememberSaveable {
        mutableStateOf(false)
    }

    // -----------------------------------------------------------------
    // OTHER STATES
    // -----------------------------------------------------------------

    var isSaving by remember {
        mutableStateOf(false)
    }

    var errorMessage by rememberSaveable {
        mutableStateOf("")
    }

    var difficultyExpanded by remember {
        mutableStateOf(false)
    }

    val difficulties = listOf(
        "Easy",
        "Medium",
        "Hard"
    )

    // -----------------------------------------------------------------
    // SAVE QUIZ
    // -----------------------------------------------------------------

    fun saveQuiz() {

        val cleanTitle = title.trim()
        val cleanDescription = description.trim()
        val cleanCategory = category.trim()

        val durationValue =
            duration.trim().toLongOrNull()

        val passingScoreValue =
            passingScore.trim().toLongOrNull()

        titleError =
            cleanTitle.length < 3

        descriptionError =
            cleanDescription.length < 10

        categoryError =
            cleanCategory.length < 2

        durationError =
            durationValue == null ||
                    durationValue < 1 ||
                    durationValue > 300

        passingScoreError =
            passingScoreValue == null ||
                    passingScoreValue < 1 ||
                    passingScoreValue > 100

        if (
            titleError ||
            descriptionError ||
            categoryError ||
            durationError ||
            passingScoreError
        ) {
            errorMessage = ""
            return
        }

        val adminUser =
            auth.currentUser

        if (adminUser == null) {

            errorMessage =
                "Your session has expired. Please login again."

            return
        }

        isSaving = true
        errorMessage = ""

        val quizDocument =
            firestore
                .collection("quizzes")
                .document()

        val quizId =
            quizDocument.id

        val quizData =
            hashMapOf(
                "quizId" to quizId,
                "title" to cleanTitle,
                "description" to cleanDescription,
                "category" to cleanCategory,
                "subjectId" to assignedSubjectId,
                "difficulty" to difficulty,
                "duration" to durationValue,
                "passingScore" to passingScoreValue,
                "status" to "draft",
                "createdBy" to adminUser.uid,
                "createdAt" to FieldValue.serverTimestamp()
            )

        quizDocument
            .set(quizData)
            .addOnSuccessListener {

                isSaving = false

                onQuizCreated(quizId)
            }
            .addOnFailureListener { exception ->

                isSaving = false

                errorMessage =
                    exception.localizedMessage
                        ?: "Unable to create quiz. Please try again."
            }
    }

    // =================================================================
    // SCREEN
    // =================================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // =============================================================
        // TOP HEADER
        // =============================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .padding(
                    horizontal = 14.dp,
                    vertical = 11.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = {
                    if (!isSaving) {
                        onBack()
                    }
                },
                enabled = !isSaving
            ) {

                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(
                modifier = Modifier.width(6.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Create Quiz",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = "Set up your new quiz",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        // =============================================================
        // MAIN SCROLLABLE CONTENT
        // =============================================================

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

            // =========================================================
            // INTRO CARD
            // =========================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Quiz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
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
                            text = "New Quiz",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "Enter the basic details of your quiz. You can add questions after creating it.",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // =========================================================
            // BASIC INFORMATION
            // =========================================================

            SectionCard(
                title = "Basic Information",
                subtitle =
                    "Tell students what this quiz is about."
            ) {

                QuizTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                        errorMessage = ""
                    },
                    label = "Quiz Title",
                    placeholder =
                        "e.g. Biology Fundamentals",
                    isError = titleError
                )

                if (titleError) {

                    FieldError(
                        text =
                            "Title must contain at least 3 characters."
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                QuizTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionError = false
                        errorMessage = ""
                    },
                    label = "Description",
                    placeholder =
                        "Briefly describe what this quiz covers.",
                    isError = descriptionError,
                    minLines = 3
                )

                if (descriptionError) {

                    FieldError(
                        text =
                            "Description must contain at least 10 characters."
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                QuizTextField(
                    value = category,
                    onValueChange = {
                        if (assignedSubjectName.isBlank()) {
                            category = it
                            categoryError = false
                            errorMessage = ""
                        }
                    },
                    label = if (assignedSubjectName.isNotBlank()) "Assigned Subject" else "Category",
                    placeholder =
                        "e.g. Biology",
                    isError = categoryError
                )

                if (categoryError) {

                    FieldError(
                        text =
                            "Enter a valid category."
                    )
                }
            }

            // =========================================================
            // QUIZ SETTINGS
            // =========================================================

            SectionCard(
                title = "Quiz Settings",
                subtitle =
                    "Configure difficulty, duration and passing score."
            ) {

                Text(
                    text = "Difficulty",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape =
                                    RoundedCornerShape(12.dp)
                            )
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape =
                                    RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                if (!isSaving) {
                                    difficultyExpanded =
                                        !difficultyExpanded
                                }
                            }
                            .padding(
                                horizontal = 15.dp,
                                vertical = 15.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text = difficulty,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector =
                                Icons.Default.KeyboardArrowDown,
                            contentDescription =
                                "Select difficulty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = difficultyExpanded,
                        onDismissRequest = {
                            difficultyExpanded = false
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {

                        difficulties.forEach { option ->

                            DropdownMenuItem(
                                text = {

                                    Text(
                                        text = option,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {

                                    difficulty = option

                                    difficultyExpanded = false
                                },
                                leadingIcon = {

                                    if (
                                        difficulty == option
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.Default.Check,
                                            contentDescription =
                                                null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        QuizTextField(
                            value = duration,
                            onValueChange = {

                                if (
                                    it.isEmpty() ||
                                    (
                                            it.all { char ->
                                                char.isDigit()
                                            } &&
                                                    it.length <= 3
                                            )
                                ) {

                                    duration = it
                                    durationError = false
                                    errorMessage = ""
                                }
                            },
                            label = "Duration",
                            placeholder = defaultDuration.toString(),
                            isError = durationError,
                            keyboardType =
                                KeyboardType.Number,
                            leadingIconType =
                                LeadingIconType.Duration
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "Minutes",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(
                                start = 3.dp
                            )
                        )

                        if (durationError) {

                            FieldError(
                                text = "1–300 min."
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        QuizTextField(
                            value = passingScore,
                            onValueChange = {

                                if (
                                    it.isEmpty() ||
                                    (
                                            it.all { char ->
                                                char.isDigit()
                                            } &&
                                                    it.length <= 3
                                            )
                                ) {

                                    passingScore = it
                                    passingScoreError = false
                                    errorMessage = ""
                                }
                            },
                            label = "Passing Score",
                            placeholder = defaultPassingScore.toString(),
                            isError =
                                passingScoreError,
                            keyboardType =
                                KeyboardType.Number,
                            leadingIconType =
                                LeadingIconType.Score
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "Percentage",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(
                                start = 3.dp
                            )
                        )

                        if (passingScoreError) {

                            FieldError(
                                text = "1–100%."
                            )
                        }
                    }
                }
            }

            // =========================================================
            // DRAFT CARD
            // =========================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {

                Column(
                    modifier = Modifier.padding(17.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape =
                                        RoundedCornerShape(9.dp)
                                ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier =
                                    Modifier.size(19.dp)
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(10.dp)
                        )

                        Text(
                            text = "Draft mode",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "This quiz will be saved as a draft. You can add questions and publish it later.",
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // =========================================================
            // ERROR CARD
            // =========================================================

            if (errorMessage.isNotBlank()) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {

                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // =========================================================
            // CREATE BUTTON
            // =========================================================

            Button(
                onClick = {

                    if (!isSaving) {
                        saveQuiz()
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor =
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.60f),
                    disabledContentColor =
                        MaterialTheme.colorScheme.onPrimary
                )
            ) {

                if (isSaving) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(21.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Text(
                        text = "Creating Quiz...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                } else {

                    Icon(
                        imageVector =
                            Icons.Default.Quiz,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(9.dp)
                    )

                    Text(
                        text = "Create Quiz",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier
                    .height(10.dp)
                    .navigationBarsPadding()
            )
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
        modifier = Modifier.fillMaxWidth(),
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

            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            content()
        }
    }
}

// =====================================================================
// LEADING ICON TYPE
// =====================================================================

private enum class LeadingIconType {
    None,
    Duration,
    Score
}

// =====================================================================
// QUIZ TEXT FIELD
// =====================================================================

@Composable
private fun QuizTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    leadingIconType: LeadingIconType = LeadingIconType.None
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = label
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        isError = isError,
        minLines = minLines,
        maxLines =
            if (minLines > 1) {
                5
            } else {
                1
            },
        singleLine =
            minLines == 1,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        leadingIcon = {

            when (leadingIconType) {

                LeadingIconType.Duration -> {

                    Icon(
                        imageVector =
                            Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                LeadingIconType.Score -> {

                    Icon(
                        imageVector =
                            Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }

                LeadingIconType.None -> {
                    // No icon
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                errorTextColor = MaterialTheme.colorScheme.onSurface,

                cursorColor = MaterialTheme.colorScheme.primary,
                errorCursorColor = MaterialTheme.colorScheme.error,

                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = MaterialTheme.colorScheme.error,

                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                errorBorderColor = MaterialTheme.colorScheme.error,

                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
    )
}

// =====================================================================
// FIELD ERROR
// =====================================================================

@Composable
private fun FieldError(
    text: String
) {

    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        modifier = Modifier.padding(
            start = 3.dp,
            top = 4.dp
        )
    )
}
