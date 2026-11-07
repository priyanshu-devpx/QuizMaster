package com.quizmaster.app.ui.admin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Unpublished
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

private data class AdminQuiz(
    val quizId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val difficulty: String = "Easy",
    val duration: Int = 0,
    val passingScore: Int = 0,
    val status: String = "draft"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageQuizzesScreen(
    teacherId: String? = null,
    onBack: () -> Unit,
    onEditQuiz: (String) -> Unit
) {
    var quizzes by remember {
        mutableStateOf<List<AdminQuiz>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var quizToDelete by remember {
        mutableStateOf<AdminQuiz?>(null)
    }

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }
    var categoryFilter by remember { mutableStateOf("All") }
    var difficultyFilter by remember { mutableStateOf("All") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var difficultyMenuExpanded by remember { mutableStateOf(false) }

    val categories = remember(quizzes) {
        listOf("All") + quizzes
            .map { it.category.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    val difficulties = remember(quizzes) {
        listOf("All") + quizzes
            .map { it.difficulty.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    val filteredQuizzes by remember(
        quizzes,
        searchQuery,
        statusFilter,
        categoryFilter,
        difficultyFilter
    ) {
        derivedStateOf {
            val query = searchQuery.trim().lowercase()

            quizzes.filter { quiz ->
                val matchesSearch =
                    query.isEmpty() ||
                            quiz.title.lowercase().contains(query)

                val matchesStatus =
                    statusFilter == "All" ||
                            quiz.status.equals(statusFilter, ignoreCase = true)

                val matchesCategory =
                    categoryFilter == "All" ||
                            quiz.category.equals(categoryFilter, ignoreCase = true)

                val matchesDifficulty =
                    difficultyFilter == "All" ||
                            quiz.difficulty.equals(difficultyFilter, ignoreCase = true)

                matchesSearch &&
                        matchesStatus &&
                        matchesCategory &&
                        matchesDifficulty
            }
        }
    }

    val hasActiveFilters =
        searchQuery.isNotBlank() ||
                statusFilter != "All" ||
                categoryFilter != "All" ||
                difficultyFilter != "All"

    fun refreshQuizzes() {
        loadQuizzes(
            teacherId = teacherId,
            onSuccess = { loadedQuizzes ->
                quizzes = loadedQuizzes
                isLoading = false
                errorMessage = ""
            },
            onFailure = { message ->
                isLoading = false
                errorMessage = message
            }
        )
    }

    LaunchedEffect(teacherId) {
        refreshQuizzes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (teacherId != null) "My Quizzes" else "Manage Quizzes",
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },

        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {

                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(
                            Alignment.Center
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                errorMessage.isNotEmpty() -> {
                    ErrorState(
                        message = errorMessage
                    )
                }

                quizzes.isEmpty() -> {
                    EmptyQuizState()
                }

                else -> {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = 20.dp,
                                vertical = 16.dp
                            ),

                        verticalArrangement =
                            Arrangement.spacedBy(14.dp)
                    ) {

                        item {
                            Text(
                                text = "Manage all quizzes created by the administrator.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    bottom = 2.dp
                                )
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Search quiz") },
                                placeholder = { Text("Search by quiz name") },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                )
                            )
                        }

                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Status",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("All", "Draft", "Published").forEach { status ->
                                        FilterChip(
                                            selected = statusFilter == status,
                                            onClick = { statusFilter = status },
                                            label = { Text(status) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    FilterSelector(
                                        label = "Category",
                                        selectedValue = categoryFilter,
                                        expanded = categoryMenuExpanded,
                                        options = categories,
                                        onExpandedChange = {
                                            categoryMenuExpanded = it
                                        },
                                        onOptionSelected = {
                                            categoryFilter = it
                                            categoryMenuExpanded = false
                                        }
                                    )
                                }

                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    FilterSelector(
                                        label = "Difficulty",
                                        selectedValue = difficultyFilter,
                                        expanded = difficultyMenuExpanded,
                                        options = difficulties,
                                        onExpandedChange = {
                                            difficultyMenuExpanded = it
                                        },
                                        onOptionSelected = {
                                            difficultyFilter = it
                                            difficultyMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (hasActiveFilters) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            searchQuery = ""
                                            statusFilter = "All"
                                            categoryFilter = "All"
                                            difficultyFilter = "All"
                                            categoryMenuExpanded = false
                                            difficultyMenuExpanded = false
                                        }
                                    ) {
                                        Text(
                                            text = "Clear filters",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredQuizzes.isEmpty()) {
                            item {
                                NoFilteredResultsState(
                                    hasActiveFilters = hasActiveFilters
                                )
                            }
                        } else {
                            items(
                                items = filteredQuizzes,
                                key = { it.quizId }
                            ) { quiz ->

                                AdminQuizCard(
                                    quiz = quiz,

                                    onPublish = {
                                        updateQuizStatus(
                                            quiz = quiz,
                                            newStatus = "published",
                                            onSuccess = {
                                                refreshQuizzes()
                                            },
                                            onFailure = {
                                                errorMessage = it
                                            }
                                        )
                                    },

                                    onUnpublish = {
                                        updateQuizStatus(
                                            quiz = quiz,
                                            newStatus = "draft",
                                            onSuccess = {
                                                refreshQuizzes()
                                            },
                                            onFailure = {
                                                errorMessage = it
                                            }
                                        )
                                    },

                                    onEdit = {
                                        onEditQuiz(quiz.quizId)
                                    },

                                    onDelete = {
                                        quizToDelete = quiz
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (quizToDelete != null) {

        AlertDialog(
            onDismissRequest = {
                quizToDelete = null
            },

            title = {
                Text(
                    text = "Delete Quiz?"
                )
            },

            text = {
                Text(
                    text = "Are you sure you want to delete \"${quizToDelete!!.title}\"? This action and all associated questions will be permanently deleted."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        val quiz = quizToDelete

                        if (quiz != null) {
                            val db = FirebaseFirestore.getInstance()

                            db.collection("questions")
                                .whereEqualTo("quizId", quiz.quizId)
                                .get()
                                .addOnSuccessListener { questionsSnapshot ->
                                    db.collection("publicQuestions")
                                        .whereEqualTo("quizId", quiz.quizId)
                                        .get()
                                        .addOnSuccessListener { publicQuestionsSnapshot ->
                                            db.runBatch { batch ->
                                                batch.delete(db.collection("quizzes").document(quiz.quizId))
                                                for (doc in questionsSnapshot.documents) {
                                                    batch.delete(doc.reference)
                                                }
                                                for (doc in publicQuestionsSnapshot.documents) {
                                                    batch.delete(doc.reference)
                                                }
                                            }.addOnSuccessListener {
                                                quizzes = quizzes.filter { it.quizId != quiz.quizId }
                                                quizToDelete = null
                                                errorMessage = ""
                                            }.addOnFailureListener { exception ->
                                                errorMessage = exception.message ?: "Unable to delete quiz."
                                                quizToDelete = null
                                            }
                                        }
                                        .addOnFailureListener { exception ->
                                            errorMessage = exception.message ?: "Unable to delete quiz questions."
                                            quizToDelete = null
                                        }
                                }
                                .addOnFailureListener { exception ->
                                    errorMessage = exception.message ?: "Unable to delete quiz questions."
                                    quizToDelete = null
                                }
                        }
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        quizToDelete = null
                    }
                ) {
                    Text(
                        text = "Cancel"
                    )
                }
            }
        )
    }
}

private fun loadQuizzes(
    teacherId: String?,
    onSuccess: (List<AdminQuiz>) -> Unit,
    onFailure: (String) -> Unit
) {
    val collection = FirebaseFirestore.getInstance().collection("quizzes")
    val query = if (teacherId != null && teacherId.isNotBlank()) {
        collection.whereEqualTo("createdBy", teacherId)
    } else {
        collection.orderBy("createdAt", Query.Direction.DESCENDING)
    }

    query.get()
        .addOnSuccessListener { result ->

            val loadedQuizzes =
                result.documents.mapNotNull { document ->

                    try {

                        AdminQuiz(
                            quizId =
                                document.getString("quizId")
                                    ?: document.id,

                            title =
                                document.getString("title")
                                    ?: "",

                            description =
                                document.getString("description")
                                    ?: "",

                            category =
                                document.getString("category")
                                    ?: "",

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
                                        ).toInt(),

                            status =
                                document.getString("status")
                                    ?: "draft"
                        )

                    } catch (e: Exception) {
                        null
                    }
                }

            onSuccess(loadedQuizzes)
        }
        .addOnFailureListener { exception ->

            onFailure(
                exception.message
                    ?: "Unable to load quizzes."
            )
        }
}

private fun updateQuizStatus(
    quiz: AdminQuiz,
    newStatus: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    FirebaseFirestore
        .getInstance()
        .collection("quizzes")
        .document(quiz.quizId)
        .update(
            "status",
            newStatus
        )
        .addOnSuccessListener {
            onSuccess()
        }
        .addOnFailureListener { exception ->

            onFailure(
                exception.message
                    ?: "Unable to update quiz status."
            )
        }
}

@Composable
private fun FilterSelector(
    label: String,
    selectedValue: String,
    expanded: Boolean,
    options: List<String>,
    onExpandedChange: (Boolean) -> Unit,
    onOptionSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            label = { Text(label) }
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    onExpandedChange(!expanded)
                }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                onExpandedChange(false)
            },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun NoFilteredResultsState(
    hasActiveFilters: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 24.dp,
                bottom = 24.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = if (hasActiveFilters) {
                "No results found"
            } else {
                "No quizzes found"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = if (hasActiveFilters) {
                "Try changing your search or filters."
            } else {
                "Create a quiz from the admin dashboard."
            },
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AdminQuizCard(
    quiz: AdminQuiz,
    onPublish: () -> Unit,
    onUnpublish: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(16.dp),

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
                .padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),

                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.MenuBook,
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
                        text = quiz.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "${quiz.category} • ${quiz.difficulty}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(
                    status = quiz.status
                )
            }

            if (quiz.description.isNotBlank()) {

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = quiz.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    maxLines = 3
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "${quiz.duration} min",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.width(16.dp)
                )

                Text(
                    text = "Pass: ${quiz.passingScore}%",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Quiz",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Quiz",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(
                    modifier = Modifier.width(4.dp)
                )

                if (quiz.status.lowercase() == "published") {

                    Button(
                        onClick = onUnpublish,

                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),

                        shape = RoundedCornerShape(10.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.Unpublished,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(5.dp)
                        )

                        Text(
                            text = "Unpublish",
                            fontSize = 13.sp
                        )
                    }

                } else {

                    Button(
                        onClick = onPublish,

                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),

                        shape = RoundedCornerShape(10.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(5.dp)
                        )

                        Text(
                            text = "Publish",
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: String
) {
    val isPublished =
        status.lowercase() == "published"

    val backgroundColor =
        if (isPublished) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        }

    val textColor =
        if (isPublished) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onTertiaryContainer
        }

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = 9.dp,
                vertical = 5.dp
            )
    ) {

        Text(
            text = status.replaceFirstChar {
                it.uppercase()
            },

            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun EmptyQuizState() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(60.dp)
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "No quizzes found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Create a quiz from the admin dashboard.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(
    message: String
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Something went wrong",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
