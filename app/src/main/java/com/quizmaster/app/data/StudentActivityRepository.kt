package com.quizmaster.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Handles the student's live activity state separately from quiz attempts/results.
 *
 * Firestore:
 * attendance/{studentUid}
 *
 * Fields written by this repository:
 * - userId
 * - studentName
 * - status: "active" | "attempting" | "inactive"
 * - currentQuizId
 * - currentQuizTitle
 * - lastSeen
 * - updatedAt
 *
 * The attendance document is intentionally separate from "attempts".
 * Existing scoring and result functionality therefore remains unchanged.
 */
object StudentActivityRepository {

    private const val ATTENDANCE_COLLECTION = "attendance"

    const val STATUS_ACTIVE = "active"
    const val STATUS_ATTEMPTING = "attempting"
    const val STATUS_INACTIVE = "inactive"

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    /**
     * Marks the currently logged-in student as active.
     *
     * The student's name is read from users/{uid} so the attendance document
     * remains useful to the admin screen without exposing the UID in the UI.
     */
    fun setActive(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = auth.currentUser

        if (user == null) {
            onError("Your session has expired. Please login again.")
            return
        }

        loadStudentName(
            uid = user.uid,
            fallbackName = user.displayName ?: user.email ?: "Student",
            onSuccess = { studentName ->
                writeActivity(
                    uid = user.uid,
                    studentName = studentName,
                    status = STATUS_ACTIVE,
                    currentQuizId = null,
                    currentQuizTitle = null,
                    onSuccess = onSuccess,
                    onError = onError
                )
            },
            onError = onError
        )
    }

    /**
     * Marks the student as currently attempting a quiz.
     */
    fun startQuiz(
        quizId: String,
        quizTitle: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = auth.currentUser

        if (user == null) {
            onError("Your session has expired. Please login again.")
            return
        }

        if (quizId.isBlank()) {
            onError("Invalid quiz.")
            return
        }

        val safeQuizTitle = quizTitle.trim().ifBlank { "Quiz" }

        loadStudentName(
            uid = user.uid,
            fallbackName = user.displayName ?: user.email ?: "Student",
            onSuccess = { studentName ->
                writeActivity(
                    uid = user.uid,
                    studentName = studentName,
                    status = STATUS_ATTEMPTING,
                    currentQuizId = quizId,
                    currentQuizTitle = safeQuizTitle,
                    onSuccess = onSuccess,
                    onError = onError
                )
            },
            onError = onError
        )
    }

    /**
     * Marks the student active again after a quiz is submitted.
     *
     * This does not modify the quiz attempt. AttemptRepository continues
     * to handle scoring and result creation.
     */
    fun finishQuiz(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        setActive(
            onSuccess = onSuccess,
            onError = onError
        )
    }

    /**
     * Updates lastSeen while keeping the student's current state.
     *
     * This should be called periodically while the student is using the app.
     * It is intentionally a small update and does not create quiz/result data.
     */
    fun heartbeat(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = auth.currentUser

        if (user == null) {
            onError("Your session has expired. Please login again.")
            return
        }

        db.collection(ATTENDANCE_COLLECTION)
            .document(user.uid)
            .update(
                mapOf(
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.localizedMessage
                        ?: "Failed to update activity."
                )
            }
    }

    /**
     * Marks the student inactive, normally during logout.
     *
     * The document is kept instead of deleted so the admin can still see
     * the student's last known activity.
     */
    fun setInactive(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = auth.currentUser

        if (user == null) {
            onSuccess()
            return
        }

        val attendanceRef = db
            .collection(ATTENDANCE_COLLECTION)
            .document(user.uid)

        attendanceRef
            .set(
                mapOf(
                    "userId" to user.uid,
                    "status" to STATUS_INACTIVE,
                    "currentQuizId" to null,
                    "currentQuizTitle" to null,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.localizedMessage
                        ?: "Failed to update logout activity."
                )
            }
    }

    /**
     * Reads the student's name from the existing user's collection.
     *
     * No new user fields are required.
     */
    private fun loadStudentName(
        uid: String,
        fallbackName: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val firestoreName = document.getString("name")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

                val firestoreEmail = document.getString("email")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

                val name = firestoreName
                    ?: firestoreEmail
                    ?: fallbackName.trim().ifBlank { "Student" }

                onSuccess(name)
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.localizedMessage
                        ?: "Failed to load student information."
                )
            }
    }

    /**
     * Creates/updates the student's attendance document.
     *
     * SetOptions.merge() is used so future fields added to the attendance
     * document are not accidentally removed by activity updates.
     */
    private fun writeActivity(
        uid: String,
        studentName: String,
        status: String,
        currentQuizId: String?,
        currentQuizTitle: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf<String, Any?>(
            "userId" to uid,
            "studentName" to studentName,
            "status" to status,
            "currentQuizId" to currentQuizId,
            "currentQuizTitle" to currentQuizTitle,
            "lastSeen" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection(ATTENDANCE_COLLECTION)
            .document(uid)
            .set(
                data,
                com.google.firebase.firestore.SetOptions.merge()
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.localizedMessage
                        ?: "Failed to update student activity."
                )
            }
    }
}
