package com.quizmaster.app

import android.os.Bundle
import android.util.Patterns
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.quizmaster.app.ui.auth.ForgotPasswordScreen
import com.quizmaster.app.ui.auth.RegisterScreen
import com.quizmaster.app.ui.auth.RoleSelectionScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.School
import com.quizmaster.app.ui.admin.AdminDashboardScreen
import com.quizmaster.app.ui.admin.CreateQuizScreen
import com.quizmaster.app.ui.admin.ManageQuestionsScreen
import com.quizmaster.app.ui.admin.ManageQuizzesScreen
import com.quizmaster.app.ui.admin.ManageSubjectsScreen
import com.quizmaster.app.ui.admin.ManageTeachersScreen
import com.quizmaster.app.ui.admin.AddQuestionScreen
import com.quizmaster.app.ui.admin.EditQuestionScreen
import com.quizmaster.app.ui.admin.EditQuizScreen
import com.quizmaster.app.ui.admin.StudentListScreen
import com.quizmaster.app.ui.admin.StudentDetailsScreen
import com.quizmaster.app.ui.admin.AdminResultsScreen
import com.quizmaster.app.ui.admin.AdminResultDetailsScreen
import com.quizmaster.app.ui.admin.AdminSettingsScreen
import com.quizmaster.app.ui.admin.AttendanceScreen
import com.quizmaster.app.ui.student.StudentHomeScreen
import com.quizmaster.app.ui.student.StudentQuizListScreen
import com.quizmaster.app.ui.student.QuizDetailsScreen
import com.quizmaster.app.ui.student.StudentQuizScreen
import com.quizmaster.app.ui.student.ResultScreen
import com.quizmaster.app.ui.student.ResultsHistoryScreen
import com.quizmaster.app.ui.student.AnswerReviewScreen
import com.quizmaster.app.ui.student.StudentProfileScreen
import com.quizmaster.app.ui.student.StudentSettingsScreen
import com.quizmaster.app.ui.teacher.TeacherDashboardScreen
import com.quizmaster.app.data.AttemptRepository
import com.quizmaster.app.data.StudentActivityRepository
import com.quizmaster.app.data.preferences.UserPreferences
import com.quizmaster.app.ui.theme.QuizMasterTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

// =========================================================
// APP SCREENS
// =========================================================

private enum class AppScreen {
    Splash,
    RoleSelection,
    Login,
    Register,
    ForgotPassword,
    StudentHome,
    TeacherDashboard,
    AdminDashboard,
    CreateQuiz,
    ManageQuestions,
    ManageSubjects,
    ManageTeachers,
    AddQuestion,
    EditQuestion,
    StudentQuizList,
    ResultsHistory,
    QuizDetails,
    StudentQuiz,
    ManageQuizzes,
    EditQuiz,
    Result,
    AnswerReview,
    StudentProfile,
    StudentSettings,
    StudentList,
    StudentDetails,
    AdminResults,
    AdminResultDetails,
    AdminSettings,
    Attendance,
}

// =========================================================
// LOGIN ROLE
// =========================================================

private enum class LoginRole {
    Student,
    Teacher,
    Admin
}

// =========================================================
// MAIN ACTIVITY
// =========================================================

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val userPreferences = remember {
                UserPreferences(applicationContext)
            }

            val darkModeEnabled by userPreferences.darkModeEnabled
                .collectAsState(initial = false)

            QuizMasterTheme(
                darkTheme = darkModeEnabled
            ) {
                QuizMasterApp()
            }
        }
    }
}

// =========================================================
// MAIN APP
// =========================================================

@Composable
private fun QuizMasterApp() {

    val context = LocalContext.current

    var currentScreen by rememberSaveable {
        mutableStateOf(AppScreen.Splash)
    }

    var selectedRole by rememberSaveable {
        mutableStateOf(LoginRole.Student)
    }

    var selectedQuizId by rememberSaveable {
        mutableStateOf("")
    }

    var selectedQuestionId by rememberSaveable {
        mutableStateOf("")
    }

    var selectedAttemptId by rememberSaveable {
        mutableStateOf("")
    }

    var selectedStudentId by rememberSaveable {
        mutableStateOf("")
    }

    // -----------------------------------------------------
    // SPLASH + SESSION RESTORE
    // -----------------------------------------------------

    LaunchedEffect(Unit) {
        delay(1800.milliseconds)

        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val user = auth.currentUser

        if (user == null) {
            currentScreen = AppScreen.RoleSelection
            return@LaunchedEffect
        }

        firestore
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                val status = document.getString("status")?.trim()?.lowercase() ?: "active"

                if (status == "inactive") {
                    auth.signOut()
                    currentScreen = AppScreen.RoleSelection
                    Toast.makeText(
                        context,
                        "Your account has been deactivated. Please contact the administrator.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                val role = document
                    .getString("role")
                    ?.trim()
                    ?.lowercase()

                when (role) {
                    "student" -> {
                        StudentActivityRepository.setActive(
                            onSuccess = {
                                currentScreen = AppScreen.StudentHome
                            },
                            onError = {
                                // Attendance must never block a valid login/session restore.
                                currentScreen = AppScreen.StudentHome
                            }
                        )
                    }
                    "teacher" -> currentScreen = AppScreen.TeacherDashboard
                    "admin" -> currentScreen = AppScreen.AdminDashboard
                    else -> {
                        auth.signOut()
                        currentScreen = AppScreen.RoleSelection
                    }
                }
            }
            .addOnFailureListener {
                auth.signOut()
                currentScreen = AppScreen.RoleSelection

                Toast.makeText(
                    context,
                    "Session could not be restored. Please log in again.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // -----------------------------------------------------
    // SYSTEM BACK NAVIGATION
    // -----------------------------------------------------

    BackHandler(
        enabled = currentScreen != AppScreen.Splash &&
                currentScreen != AppScreen.RoleSelection &&
                currentScreen != AppScreen.StudentHome &&
                currentScreen != AppScreen.TeacherDashboard &&
                currentScreen != AppScreen.AdminDashboard
    ) {
        currentScreen = when (currentScreen) {
            AppScreen.Splash -> AppScreen.Splash
            AppScreen.RoleSelection -> AppScreen.RoleSelection
            AppScreen.Login -> AppScreen.RoleSelection
            AppScreen.Register -> AppScreen.Login
            AppScreen.ForgotPassword -> AppScreen.Login

            AppScreen.StudentHome -> AppScreen.StudentHome
            AppScreen.TeacherDashboard -> AppScreen.TeacherDashboard
            AppScreen.StudentQuizList -> AppScreen.StudentHome
            AppScreen.ResultsHistory -> AppScreen.StudentHome
            AppScreen.StudentProfile -> AppScreen.StudentHome
            AppScreen.StudentSettings -> AppScreen.StudentProfile
            AppScreen.StudentList -> AppScreen.AdminDashboard
            AppScreen.StudentDetails -> AppScreen.StudentList
            AppScreen.AdminResults -> if (selectedRole == LoginRole.Teacher) AppScreen.TeacherDashboard else AppScreen.AdminDashboard
            AppScreen.AdminResultDetails -> AppScreen.AdminResults
            AppScreen.AdminSettings -> AppScreen.AdminDashboard
            AppScreen.Attendance -> AppScreen.AdminDashboard
            AppScreen.QuizDetails -> AppScreen.StudentQuizList
            AppScreen.StudentQuiz -> AppScreen.QuizDetails
            AppScreen.Result -> AppScreen.StudentQuizList
            AppScreen.AnswerReview -> AppScreen.Result

            AppScreen.AdminDashboard -> AppScreen.AdminDashboard
            AppScreen.CreateQuiz -> if (selectedRole == LoginRole.Teacher) AppScreen.TeacherDashboard else AppScreen.AdminDashboard
            AppScreen.ManageQuizzes -> if (selectedRole == LoginRole.Teacher) AppScreen.TeacherDashboard else AppScreen.AdminDashboard
            AppScreen.ManageSubjects -> AppScreen.AdminDashboard
            AppScreen.ManageTeachers -> AppScreen.AdminDashboard
            AppScreen.EditQuiz -> AppScreen.ManageQuizzes
            AppScreen.ManageQuestions -> if (selectedRole == LoginRole.Teacher) AppScreen.TeacherDashboard else AppScreen.AdminDashboard
            AppScreen.AddQuestion -> AppScreen.ManageQuestions
            AppScreen.EditQuestion -> AppScreen.ManageQuestions
        }
    }

    // -----------------------------------------------------
    // MAIN SURFACE
    // -----------------------------------------------------

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentScreen) {

            // =================================================
            // SPLASH
            // =================================================

            AppScreen.Splash -> {

                SplashScreen()
            }

            // =================================================
            // LOGIN
            // =================================================
            // ROLE SELECTION ("Who are you?")
            // =================================================

            AppScreen.RoleSelection -> {

                RoleSelectionScreen(
                    onSelectStudent = {
                        selectedRole = LoginRole.Student
                        currentScreen = AppScreen.Login
                    },
                    onSelectTeacher = {
                        selectedRole = LoginRole.Teacher
                        currentScreen = AppScreen.Login
                    },
                    onSelectAdmin = {
                        selectedRole = LoginRole.Admin
                        currentScreen = AppScreen.Login
                    }
                )
            }

            // =================================================
            // LOGIN
            // =================================================

            AppScreen.Login -> {

                LoginScreen(
                    selectedRole = selectedRole,
                    onChangeRole = {
                        currentScreen = AppScreen.RoleSelection
                    },
                    onLogin = { email, password ->

                        loginUser(
                            email = email,
                            password = password,
                            selectedRole = selectedRole,

                            onStudentLogin = {

                                Toast.makeText(
                                    context,
                                    "Login successful!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                StudentActivityRepository.setActive(
                                    onSuccess = {
                                        currentScreen = AppScreen.StudentHome
                                    },
                                    onError = {
                                        // Attendance failure must not break the existing login flow.
                                        currentScreen = AppScreen.StudentHome
                                    }
                                )
                            },

                            onTeacherLogin = {

                                Toast.makeText(
                                    context,
                                    "Teacher login successful!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                currentScreen = AppScreen.TeacherDashboard
                            },

                            onAdminLogin = {

                                Toast.makeText(
                                    context,
                                    "Admin login successful!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                currentScreen = AppScreen.AdminDashboard
                            },

                            onError = { errorMessage ->

                                Toast.makeText(
                                    context,
                                    errorMessage,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    },

                    onCreateAccount = {
                        currentScreen = AppScreen.Register
                    },

                    onForgotPassword = {
                        currentScreen = AppScreen.ForgotPassword
                    }
                )
            }

            // =================================================
            // REGISTER
            // =================================================

            AppScreen.Register -> {

                RegisterScreen(
                    onRegister = { name, email, password ->

                        registerUser(
                            name = name,
                            email = email,
                            password = password,

                            onSuccess = {

                                Toast.makeText(
                                    context,
                                    "Account created successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                currentScreen = AppScreen.Login
                            },

                            onError = { errorMessage ->

                                Toast.makeText(
                                    context,
                                    errorMessage,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    },

                    onLoginClick = {
                        currentScreen = AppScreen.Login
                    }
                )
            }

            // =================================================
            // FORGOT PASSWORD
            // =================================================

            AppScreen.ForgotPassword -> {

                ForgotPasswordScreen(
                    onSendResetLink = {
                        // ForgotPasswordScreen handles the Firebase reset email.
                        // Kept parameterless for screen compatibility.
                    },

                    onBackToLogin = {
                        currentScreen = AppScreen.Login
                    }
                )
            }

            // =================================================
            // STUDENT HOME
            // =================================================

            AppScreen.StudentHome -> {

                StudentHomeScreen(
                    onLogout = {
                        StudentActivityRepository.setInactive(
                            onSuccess = {
                                FirebaseAuth.getInstance().signOut()
                                currentScreen = AppScreen.Login
                            },
                            onError = {
                                // Logout must still complete even if attendance update fails.
                                FirebaseAuth.getInstance().signOut()
                                currentScreen = AppScreen.Login
                            }
                        )
                    },
                    onQuizzesClick = {
                        currentScreen = AppScreen.StudentQuizList
                    },
                    onResultsClick = {
                        currentScreen = AppScreen.ResultsHistory
                    },
                    onProfileClick = {
                        currentScreen = AppScreen.StudentProfile
                    },
                    onQuizSelected = { quizId ->
                        selectedQuizId = quizId
                        currentScreen = AppScreen.QuizDetails
                    }
                )
            }

            AppScreen.StudentQuizList -> {
                StudentQuizListScreen(
                    onBack = {
                        currentScreen = AppScreen.StudentHome
                    },
                    onQuizSelected = { quizId ->
                        selectedQuizId = quizId
                        currentScreen = AppScreen.QuizDetails
                    }
                )
            }

            // =================================================
            // RESULTS HISTORY
            // =================================================

            AppScreen.ResultsHistory -> {
                ResultsHistoryScreen(
                    onBack = {
                        currentScreen = AppScreen.StudentHome
                    },
                    onResultClick = { attemptId ->
                        selectedAttemptId = attemptId
                        currentScreen = AppScreen.Result
                    }
                )
            }

            // =================================================
            // QUIZ DETAILS
            // =================================================

            AppScreen.QuizDetails -> {

                QuizDetailsScreen(
                    quizId = selectedQuizId,
                    onBack = {
                        currentScreen = AppScreen.StudentQuizList
                    },
                    onStartQuiz = { quizId ->
                        selectedQuizId = quizId
                        currentScreen = AppScreen.StudentQuiz
                    }
                )
            }

            // =================================================
            // STUDENT QUIZ
            // =================================================

            AppScreen.StudentQuiz -> {

                StudentQuizScreen(
                    quizId = selectedQuizId,

                    onBack = {
                        currentScreen = AppScreen.QuizDetails
                    },

                    onSubmit = { quizId, answers, timeUp ->

                        AttemptRepository.submitAttempt(
                            quizId = quizId,
                            answers = answers,
                            timeUp = timeUp,

                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    if (timeUp) {
                                        "Time is up. Quiz submitted."
                                    } else {
                                        "Quiz submitted successfully."
                                    },
                                    Toast.LENGTH_LONG
                                ).show()

                                selectedAttemptId = it
                                currentScreen = AppScreen.Result
                            },

                            onError = { message ->
                                Toast.makeText(
                                    context,
                                    message,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                )
            }

            // =================================================
            // RESULT
            // =================================================

            AppScreen.Result -> {

                ResultScreen(
                    attemptId = selectedAttemptId,
                    onBackToQuizzes = {
                        currentScreen = AppScreen.StudentQuizList
                    },
                    onReviewAnswers = {
                        currentScreen = AppScreen.AnswerReview
                    }
                )
            }

            // =================================================
            // ANSWER REVIEW
            // =================================================

            AppScreen.AnswerReview -> {

                AnswerReviewScreen(
                    attemptId = selectedAttemptId,
                    onBack = {
                        currentScreen = AppScreen.Result
                    }
                )
            }

            // =================================================
            // STUDENT PROFILE
            // =================================================

            AppScreen.StudentProfile -> {

                StudentProfileScreen(
                    onBack = {
                        currentScreen = AppScreen.StudentHome
                    },
                    onSettingsClick = {
                        currentScreen = AppScreen.StudentSettings
                    },
                    onLogout = {
                        StudentActivityRepository.setInactive(
                            onSuccess = {
                                FirebaseAuth.getInstance().signOut()
                                currentScreen = AppScreen.Login
                            },
                            onError = {
                                // Logout must still complete even if attendance update fails.
                                FirebaseAuth.getInstance().signOut()
                                currentScreen = AppScreen.Login
                            }
                        )
                    }
                )
            }

            // =================================================
            // STUDENT SETTINGS
            // =================================================

            AppScreen.StudentSettings -> {

                StudentSettingsScreen(
                    onBack = {
                        currentScreen = AppScreen.StudentProfile
                    },
                    onLogout = {
                        StudentActivityRepository.setInactive(
                            onSuccess = {
                                FirebaseAuth.getInstance().signOut()
                                currentScreen = AppScreen.Login
                            },
                            onError = {
                                // Logout must still complete even if attendance update fails.
                                FirebaseAuth.getInstance().signOut()
                                currentScreen = AppScreen.Login
                            }
                        )
                    }
                )
            }

            // =================================================
            // TEACHER DASHBOARD
            // =================================================

            AppScreen.TeacherDashboard -> {

                TeacherDashboardScreen(
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        currentScreen = AppScreen.Login
                    },
                    onCreateQuiz = {
                        currentScreen = AppScreen.CreateQuiz
                    },
                    onManageQuizzes = {
                        currentScreen = AppScreen.ManageQuizzes
                    },
                    onManageQuestions = {
                        currentScreen = AppScreen.ManageQuestions
                    },
                    onViewResults = {
                        currentScreen = AppScreen.AdminResults
                    }
                )
            }

            // =================================================
            // ADMIN DASHBOARD
            // =================================================

            AppScreen.AdminDashboard -> {

                AdminDashboardScreen(
                    onLogout = {

                        FirebaseAuth.getInstance().signOut()
                        currentScreen = AppScreen.Login
                    },
                    onCreateQuiz = {

                        currentScreen = AppScreen.CreateQuiz
                    },
                    onManageQuizzes = {

                        currentScreen = AppScreen.ManageQuizzes
                    },
                    onManageQuestions = {

                        currentScreen = AppScreen.ManageQuestions
                    },
                    onManageSubjects = {

                        currentScreen = AppScreen.ManageSubjects
                    },
                    onManageTeachers = {

                        currentScreen = AppScreen.ManageTeachers
                    },
                    onViewStudents = {

                        currentScreen = AppScreen.StudentList
                    },
                    onViewResults = {

                        currentScreen = AppScreen.AdminResults
                    },
                    onAttendanceClick = {
                        currentScreen = AppScreen.Attendance
                    },
                    onSettingsClick = {
                        currentScreen = AppScreen.AdminSettings
                    }
                )
            }

            AppScreen.ManageSubjects -> {
                ManageSubjectsScreen(
                    onBack = {
                        currentScreen = AppScreen.AdminDashboard
                    }
                )
            }

            AppScreen.ManageTeachers -> {
                ManageTeachersScreen(
                    onBack = {
                        currentScreen = AppScreen.AdminDashboard
                    }
                )
            }
            // =================================================
            // CREATE QUIZ
            // =================================================

            AppScreen.CreateQuiz -> {

                CreateQuizScreen(

                    onBack = {

                        currentScreen = if (selectedRole == LoginRole.Teacher) AppScreen.TeacherDashboard else AppScreen.AdminDashboard
                    },
                    onQuizCreated = {

                        currentScreen = if (selectedRole == LoginRole.Teacher) AppScreen.TeacherDashboard else AppScreen.AdminDashboard
                    }
                )

            }
            // =================================================
            // MANAGE QUIZZES
            // =================================================

            AppScreen.ManageQuizzes -> {
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid

                ManageQuizzesScreen(
                    teacherId = if (selectedRole == LoginRole.Teacher) currentUid else null,

                    onBack = {
                        currentScreen = if (selectedRole == LoginRole.Teacher) AppScreen.TeacherDashboard else AppScreen.AdminDashboard
                    },

                    onEditQuiz = { quizId ->
                        selectedQuizId = quizId
                        currentScreen = AppScreen.EditQuiz
                    }
                )
            }

            AppScreen.EditQuiz -> {

                EditQuizScreen(
                    quizId = selectedQuizId,
                    onBack = {
                        currentScreen = AppScreen.ManageQuizzes
                    },
                    onQuizUpdated = {
                        currentScreen = AppScreen.ManageQuizzes
                    }
                )
            }

            AppScreen.ManageQuestions -> {
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid

                ManageQuestionsScreen(
                    teacherId = if (selectedRole == LoginRole.Teacher) currentUid else null,

                    onBack = {
                        currentScreen = if (selectedRole == LoginRole.Teacher) AppScreen.TeacherDashboard else AppScreen.AdminDashboard
                    },

                    onAddQuestion = { quizId ->

                        selectedQuizId = quizId
                        currentScreen = AppScreen.AddQuestion
                    },

                    onEditQuestion = { quizId, questionId ->

                        selectedQuizId = quizId
                        selectedQuestionId = questionId
                        currentScreen = AppScreen.EditQuestion
                    }
                )
            }

            // =================================================
            // ADD QUESTION
            // =================================================

            AppScreen.AddQuestion -> {

                AddQuestionScreen(

                    quizId = selectedQuizId,

                    onBack = {

                        currentScreen = AppScreen.ManageQuestions
                    },

                    onQuestionAdded = {

                        currentScreen = AppScreen.ManageQuestions
                    }
                )
            }

            // =================================================
            // EDIT QUESTION
            // =================================================

            AppScreen.EditQuestion -> {

                EditQuestionScreen(

                    quizId = selectedQuizId,

                    questionId = selectedQuestionId,

                    onBack = {

                        currentScreen = AppScreen.ManageQuestions
                    },

                    onQuestionUpdated = {

                        currentScreen = AppScreen.ManageQuestions
                    }
                )
            }

            // =================================================
            // ADMIN STUDENTS
            // =================================================

            AppScreen.AdminResults -> {
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid

                AdminResultsScreen(
                    teacherId = if (selectedRole == LoginRole.Teacher) currentUid else null,
                    onBack = {
                        currentScreen = if (selectedRole == LoginRole.Teacher) AppScreen.TeacherDashboard else AppScreen.AdminDashboard
                    },
                    onResultSelected = { attemptId ->
                        selectedAttemptId = attemptId
                        currentScreen = AppScreen.AdminResultDetails
                    }
                )
            }

            AppScreen.AdminResultDetails -> {

                AdminResultDetailsScreen(
                    attemptId = selectedAttemptId,
                    onBack = {
                        currentScreen = AppScreen.AdminResults
                    }
                )
            }

            AppScreen.AdminSettings -> {

                AdminSettingsScreen(
                    onBack = {
                        currentScreen = AppScreen.AdminDashboard
                    },
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        currentScreen = AppScreen.Login
                    }
                )
            }

            AppScreen.Attendance -> {

                AttendanceScreen(
                    onBack = {
                        currentScreen = AppScreen.AdminDashboard
                    }
                )
            }

            AppScreen.StudentList -> {

                StudentListScreen(
                    onBack = {

                        currentScreen = AppScreen.AdminDashboard
                    },
                    onStudentSelected = { studentId ->

                        selectedStudentId = studentId
                        currentScreen = AppScreen.StudentDetails
                    }
                )
            }

            AppScreen.StudentDetails -> {

                StudentDetailsScreen(
                    studentId = selectedStudentId,
                    onBack = {

                        currentScreen = AppScreen.StudentList
                    }
                )
            }
        }
    }
}


// =========================================================
// FIREBASE REGISTRATION
// =========================================================

private fun registerUser(
    name: String,
    email: String,
    password: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    val cleanName = name.trim()
    val cleanEmail = email.trim()

    // -----------------------------------------------------
    // CREATE FIREBASE AUTH ACCOUNT
    // -----------------------------------------------------

    auth.createUserWithEmailAndPassword(
        cleanEmail,
        password
    ).addOnCompleteListener { authTask ->

        if (!authTask.isSuccessful) {

            val message = when (authTask.exception) {

                is com.google.firebase.auth.FirebaseAuthUserCollisionException -> {
                    "An account with this email already exists."
                }

                is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> {
                    "Password is too weak. Please use a stronger password."
                }

                is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                    "Please enter a valid email address."
                }

                else -> {
                    authTask.exception?.localizedMessage
                        ?: "Account creation failed. Please try again."
                }
            }

            onError(message)

            return@addOnCompleteListener
        }

        // -------------------------------------------------
        // GET CREATED USER
        // -------------------------------------------------

        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {

            onError(
                "Account was created, but user information could not be loaded."
            )

            return@addOnCompleteListener
        }

        val userId = firebaseUser.uid

        // -------------------------------------------------
        // CREATE USER PROFILE
        // -------------------------------------------------

        val userProfile = hashMapOf(
            "userId" to userId,
            "name" to cleanName,
            "email" to cleanEmail,
            "role" to "student",
            "createdAt" to FieldValue.serverTimestamp()
        )

        // -------------------------------------------------
        // SAVE PROFILE TO FIRESTORE
        // -------------------------------------------------

        firestore
            .collection("users")
            .document(userId)
            .set(userProfile)
            .addOnSuccessListener {

                onSuccess()
            }
            .addOnFailureListener { exception ->

                // Do not leave the newly-created Firebase account signed in
                // when its Firestore profile could not be created.
                auth.signOut()

                onError(
                    exception.localizedMessage
                        ?: "Account created, but profile could not be saved."
                )
            }
    }
}

// =========================================================
// FIREBASE LOGIN
// =========================================================

private fun loginUser(
    email: String,
    password: String,
    selectedRole: LoginRole,
    onStudentLogin: () -> Unit,
    onTeacherLogin: () -> Unit,
    onAdminLogin: () -> Unit,
    onError: (String) -> Unit
) {

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    val cleanEmail = email.trim()

    // -----------------------------------------------------
    // SIGN IN WITH FIREBASE AUTHENTICATION
    // -----------------------------------------------------

    auth.signInWithEmailAndPassword(
        cleanEmail,
        password
    ).addOnCompleteListener { loginTask ->

        // -------------------------------------------------
        // LOGIN FAILED
        // -------------------------------------------------

        if (!loginTask.isSuccessful) {

            val message = when (loginTask.exception) {

                is com.google.firebase.auth.FirebaseAuthInvalidUserException -> {
                    "No account found with this email address."
                }

                is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                    "Incorrect email or password."
                }

                else -> {
                    loginTask.exception?.localizedMessage
                        ?: "Login failed. Please try again."
                }
            }

            onError(message)

            return@addOnCompleteListener
        }

        // -------------------------------------------------
        // GET LOGGED-IN USER
        // -------------------------------------------------

        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {

            onError(
                "Login succeeded, but user information could not be loaded."
            )

            return@addOnCompleteListener
        }

        val userId = firebaseUser.uid

        // -------------------------------------------------
        // GET USER PROFILE FROM FIRESTORE
        // -------------------------------------------------

        firestore
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    auth.signOut()

                    onError(
                        "Your user profile was not found. Please contact the administrator."
                    )

                    return@addOnSuccessListener
                }

                // -------------------------------------------------
                // CHECK ACCOUNT STATUS (ACTIVE VS INACTIVE)
                // -------------------------------------------------

                val status = document.getString("status")
                    ?.trim()
                    ?.lowercase()
                    ?: "active"

                if (status == "inactive") {

                    auth.signOut()

                    onError(
                        "Your account has been deactivated. Please contact the administrator."
                    )

                    return@addOnSuccessListener
                }

                // -------------------------------------------------
                // READ ACTUAL ROLE FROM FIRESTORE
                // -------------------------------------------------

                val actualRole =
                    document.getString("role")
                        ?.trim()
                        ?.lowercase()

                // -------------------------------------------------
                // CHECK ROLE
                // -------------------------------------------------

                when (actualRole) {

                    "student" -> {

                        if (selectedRole != LoginRole.Student) {

                            auth.signOut()

                            onError(
                                "This account is registered as a Student."
                            )

                            return@addOnSuccessListener
                        }

                        onStudentLogin()
                    }

                    "teacher" -> {

                        if (selectedRole != LoginRole.Teacher) {

                            auth.signOut()

                            onError(
                                "This account is registered as a Teacher."
                            )

                            return@addOnSuccessListener
                        }

                        onTeacherLogin()
                    }

                    "admin" -> {

                        if (selectedRole != LoginRole.Admin) {

                            auth.signOut()

                            onError(
                                "This account is registered as an Admin."
                            )

                            return@addOnSuccessListener
                        }

                        onAdminLogin()
                    }

                    else -> {

                        auth.signOut()

                        onError(
                            "Your account has an invalid role. Please contact the administrator."
                        )
                    }
                }
            }
            .addOnFailureListener { exception ->

                auth.signOut()

                onError(
                    exception.localizedMessage
                        ?: "Could not load your account information."
                )
            }
    }
}

// =========================================================
// SPLASH SCREEN
// =========================================================

@Composable
private fun SplashScreen() {

    val colorScheme = MaterialTheme.colorScheme

    val infiniteTransition = rememberInfiniteTransition(
        label = "splash_animation"
    )

    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1100,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    val logoAlpha by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1100,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_alpha"
    )

    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 900,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Clean app mark: intentionally built from Compose primitives so
            // the splash does not depend on a missing drawable resource.
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .background(
                        color = colorScheme.primary,
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "Q",
                    color = colorScheme.onPrimary,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .offset(x = 23.dp, y = (-23).dp)
                        .background(
                            color = colorScheme.onPrimary,
                            shape = RoundedCornerShape(50)
                        )
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "QuizMaster",
                color = colorScheme.onBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Test • Learn • Improve",
                color = colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
                textAlign = TextAlign.Center
            )
        }

        // Minimal progress cue. It communicates that the app is initializing
        // without adding a large progress bar or changing splash timing.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .alpha(
                            when (index) {
                                0 -> dotAlpha
                                1 -> 0.65f
                                else -> 0.35f
                            }
                        )
                        .background(
                            color = colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }
    }
}

// =========================================================
// LOGIN SCREEN (Role-Tailored Dedicated View)
// =========================================================

@Composable
private fun LoginScreen(
    selectedRole: LoginRole,
    onChangeRole: () -> Unit,
    onLogin: (String, String) -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var emailError by rememberSaveable { mutableStateOf(false) }
    var passwordError by rememberSaveable { mutableStateOf(false) }

    val roleAccentColor = when (selectedRole) {
        LoginRole.Student -> Color(0xFF2563EB)
        LoginRole.Teacher -> Color(0xFF10B981)
        LoginRole.Admin -> Color(0xFF6366F1)
    }

    val roleIcon = when (selectedRole) {
        LoginRole.Student -> Icons.Default.School
        LoginRole.Teacher -> Icons.AutoMirrored.Filled.MenuBook
        LoginRole.Admin -> Icons.Default.AdminPanelSettings
    }

    val roleTitle = when (selectedRole) {
        LoginRole.Student -> "Welcome back, Student"
        LoginRole.Teacher -> "Welcome back, Teacher"
        LoginRole.Admin -> "Administrator Access"
    }

    val roleSubtitle = when (selectedRole) {
        LoginRole.Student -> "Sign in to attempt quizzes and view results"
        LoginRole.Teacher -> "Sign in to manage your subject quizzes"
        LoginRole.Admin -> "Secure sign in for system administration"
    }

    val buttonLabel = when (selectedRole) {
        LoginRole.Student -> "Login as Student"
        LoginRole.Teacher -> "Teacher Login"
        LoginRole.Admin -> "Secure Admin Login"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {

        // Top Navigation: ← Change Role
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onChangeRole) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Change Role",
                    tint = roleAccentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Change Role",
                    color = roleAccentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Role Icon Badge
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(roleAccentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = roleIcon,
                contentDescription = null,
                tint = roleAccentColor,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = roleTitle,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = roleSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // EMAIL FIELD
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email address") },
            placeholder = {
                Text(
                    text = "Enter your email",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            isError = emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                cursorColor = roleAccentColor,
                focusedLabelColor = roleAccentColor,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = roleAccentColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error
            )
        )

        if (emailError) {
            Text(
                text = "Enter a valid email address",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(15.dp))

        // PASSWORD FIELD
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            placeholder = {
                Text(
                    text = "Enter your password",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            isError = passwordError,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "Hide" else "Show",
                        color = roleAccentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                cursorColor = roleAccentColor,
                focusedLabelColor = roleAccentColor,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = roleAccentColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error
            )
        )

        if (passwordError) {
            Text(
                text = "Password must contain at least 6 characters",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 5.dp)
            )
        }

        // FORGOT PASSWORD
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onForgotPassword) {
                Text(
                    text = "Forgot password?",
                    color = roleAccentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SUBMIT LOGIN BUTTON
        Button(
            onClick = {
                val validEmail = email.isNotBlank() &&
                        Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
                val validPassword = password.length >= 6

                emailError = !validEmail
                passwordError = !validPassword

                if (validEmail && validPassword) {
                    onLogin(email.trim(), password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = roleAccentColor,
                contentColor = Color.White
            )
        ) {
            Text(
                text = buttonLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // REGISTRATION FOOTER SECTION BY ROLE
        when (selectedRole) {
            LoginRole.Student -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Create Student Account",
                        color = roleAccentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onCreateAccount)
                    )
                }
            }

            LoginRole.Teacher -> {
                Text(
                    text = "No public teacher registration. Administrator provisions instructor accounts.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                )
            }

            LoginRole.Admin -> {
                Text(
                    text = "No public admin registration. Administrator access is strictly restricted.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}