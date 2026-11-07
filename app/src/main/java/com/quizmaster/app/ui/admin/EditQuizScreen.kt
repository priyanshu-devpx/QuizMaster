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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun EditQuizScreen(
    quizId: String,
    onBack: () -> Unit,
    onQuizUpdated: () -> Unit
) {

    val context = LocalContext.current

    val db = remember {
        FirebaseFirestore.getInstance()
    }

    // ---------------------------------------------------------
    // FORM FIELDS
    // ---------------------------------------------------------

    var title by remember {
        mutableStateOf("")
    }

    var description by remember {
        mutableStateOf("")
    }

    var category by remember {
        mutableStateOf("")
    }

    var difficulty by remember {
        mutableStateOf("Easy")
    }

    var duration by remember {
        mutableStateOf("")
    }

    var passingScore by remember {
        mutableStateOf("")
    }

    // ---------------------------------------------------------
    // UI STATES
    // ---------------------------------------------------------

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var validationMessage by remember {
        mutableStateOf("")
    }

    var difficultyExpanded by remember {
        mutableStateOf(false)
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

                } else {

                    title =
                        document.getString("title") ?: ""

                    description =
                        document.getString("description") ?: ""

                    category =
                        document.getString("category") ?: ""

                    difficulty =
                        document.getString("difficulty")
                            ?: "Easy"

                    duration =
                        (
                                document.getLong("duration")
                                    ?: 0L
                                ).toString()

                    passingScore =
                        (
                                document.getLong("passingScore")
                                    ?: 0L
                                ).toString()
                }

                isLoading = false
            }
            .addOnFailureListener { exception ->

                errorMessage =
                    exception.message
                        ?: "Unable to load quiz."

                isLoading = false
            }
    }

    // ---------------------------------------------------------
    // UPDATE QUIZ
    // ---------------------------------------------------------

    fun updateQuiz() {

        validationMessage = ""

        val cleanTitle = title.trim()
        val cleanDescription = description.trim()
        val cleanCategory = category.trim()

        val durationValue =
            duration.trim().toIntOrNull()

        val passingScoreValue =
            passingScore.trim().toIntOrNull()

        when {

            cleanTitle.length < 3 -> {
                validationMessage =
                    "Quiz title must be at least 3 characters."
                return
            }

            cleanDescription.length < 10 -> {
                validationMessage =
                    "Description must be at least 10 characters."
                return
            }

            cleanCategory.length < 2 -> {
                validationMessage =
                    "Category must be at least 2 characters."
                return
            }

            durationValue == null -> {
                validationMessage =
                    "Enter a valid duration."
                return
            }

            durationValue !in 1..300 -> {
                validationMessage =
                    "Duration must be between 1 and 300 minutes."
                return
            }

            passingScoreValue == null -> {
                validationMessage =
                    "Enter a valid passing score."
                return
            }

            passingScoreValue !in 1..100 -> {
                validationMessage =
                    "Passing score must be between 1 and 100."
                return
            }
        }

        isSaving = true

        val updatedQuiz = hashMapOf<String, Any>(
            "title" to cleanTitle,
            "description" to cleanDescription,
            "category" to cleanCategory,
            "difficulty" to difficulty,
            "duration" to durationValue,
            "passingScore" to passingScoreValue,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("quizzes")
            .document(quizId)
            .update(updatedQuiz)
            .addOnSuccessListener {

                isSaving = false

                Toast.makeText(
                    context,
                    "Quiz updated successfully",
                    Toast.LENGTH_SHORT
                ).show()

                onQuizUpdated()
            }
            .addOnFailureListener { exception ->

                isSaving = false

                validationMessage =
                    exception.message
                        ?: "Unable to update quiz."
            }
    }

    // ---------------------------------------------------------
    // SCREEN
    // ---------------------------------------------------------

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            if (isLoading) {

                CircularProgressIndicator(
                    modifier = Modifier.align(
                        Alignment.Center
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

            } else if (errorMessage.isNotBlank()) {

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
                        text = "Unable to load quiz",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Go Back")
                    }
                }

            } else {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {

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
                            onClick = {
                                if (!isSaving) {
                                    onBack()
                                }
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(4.dp)
                        )

                        Text(
                            text = "Edit Quiz",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 20.dp,
                                vertical = 8.dp
                            )
                    ) {

                        Text(
                            text = "Quiz Details",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        Text(
                            text = "Update the information of this quiz.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(
                            modifier = Modifier.height(24.dp)
                        )

                        Text(
                            text = "Quiz Title",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(
                            modifier = Modifier.height(7.dp)
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                validationMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Enter quiz title")
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors()
                        )

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        Text(
                            text = "Description",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(
                            modifier = Modifier.height(7.dp)
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                                validationMessage = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            placeholder = {
                                Text(
                                    "Enter quiz description"
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors()
                        )

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        Text(
                            text = "Category",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(
                            modifier = Modifier.height(7.dp)
                        )

                        OutlinedTextField(
                            value = category,
                            onValueChange = {
                                category = it
                                validationMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Example: Science")
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors()
                        )

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        Text(
                            text = "Difficulty",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(
                            modifier = Modifier.height(7.dp)
                        )

                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            OutlinedTextField(
                                value = difficulty,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text("Select difficulty")
                                },
                                trailingIcon = {
                                    Text(
                                        text =
                                            if (difficultyExpanded) {
                                                "▲"
                                            } else {
                                                "▼"
                                            },
                                        color =
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clickable {
                                        difficultyExpanded = !difficultyExpanded
                                    }
                            )

                            DropdownMenu(
                                expanded = difficultyExpanded,
                                onDismissRequest = {
                                    difficultyExpanded = false
                                },
                                modifier = Modifier.background(
                                    MaterialTheme.colorScheme.surface
                                )
                            ) {

                                listOf(
                                    "Easy",
                                    "Medium",
                                    "Hard"
                                ).forEach { level ->

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = level,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {

                                            difficulty = level

                                            difficultyExpanded =
                                                false

                                            validationMessage = ""
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        Text(
                            text = "Duration",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(
                            modifier = Modifier.height(7.dp)
                        )

                        OutlinedTextField(
                            value = duration,
                            onValueChange = {
                                if (
                                    it.length <= 3 &&
                                    it.all { char ->
                                        char.isDigit()
                                    }
                                ) {
                                    duration = it
                                    validationMessage = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Enter duration in minutes")
                            },
                            suffix = {
                                Text("min")
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors()
                        )

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        Text(
                            text = "Passing Score",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(
                            modifier = Modifier.height(7.dp)
                        )

                        OutlinedTextField(
                            value = passingScore,
                            onValueChange = {
                                if (
                                    it.length <= 3 &&
                                    it.all { char ->
                                        char.isDigit()
                                    }
                                ) {
                                    passingScore = it
                                    validationMessage = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "Enter passing score"
                                )
                            },
                            suffix = {
                                Text("%")
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Number
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors()
                        )

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Text(
                            text = "Passing score should be between 1% and 100%.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (validationMessage.isNotBlank()) {

                            Spacer(
                                modifier = Modifier.height(14.dp)
                            )

                            Text(
                                text = validationMessage,
                                fontSize = 13.sp,
                                fontWeight =
                                    FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(28.dp)
                        )

                        Button(
                            onClick = {
                                updateQuiz()
                            },
                            enabled = !isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor =
                                    MaterialTheme.colorScheme.primary,
                                contentColor =
                                    MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor =
                                    MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.6f
                                    )
                            )
                        ) {

                            if (isSaving) {

                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(8.dp)
                                )

                                Text(
                                    text = "Saving..."
                                )

                            } else {

                                Icon(
                                    imageVector =
                                        Icons.Default.Save,
                                    contentDescription =
                                        "Save",
                                    modifier =
                                        Modifier.size(19.dp)
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(7.dp)
                                )

                                Text(
                                    text = "Save Changes",
                                    fontWeight =
                                        FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun fieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        cursorColor = MaterialTheme.colorScheme.primary
    )
