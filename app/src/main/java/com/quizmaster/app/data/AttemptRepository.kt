package com.quizmaster.app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.round

object AttemptRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun submitAttempt(
        quizId: String,
        answers: Map<String, String>,
        timeUp: Boolean,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
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

        db.collection("quizzes")
            .document(quizId)
            .get()
            .addOnSuccessListener { quizDocument ->
                if (!quizDocument.exists()) {
                    onError("Quiz not found.")
                    return@addOnSuccessListener
                }

                val status = quizDocument.getString("status") ?: ""
                if (status != "published") {
                    onError("This quiz is not available.")
                    return@addOnSuccessListener
                }

                val quizTitle = quizDocument.getString("title") ?: "Quiz"
                val passingScore = quizDocument.getLong("passingScore") ?: 40L
                val teacherId = quizDocument.getString("createdBy") ?: ""
                val subjectId = quizDocument.getString("subjectId") ?: ""

                // Load private questions containing correctAnswer for secure grading
                db.collection("questions")
                    .whereEqualTo("quizId", quizId)
                    .get()
                    .addOnSuccessListener { questionDocuments ->
                        if (questionDocuments.isEmpty) {
                            onError("No questions found for this quiz.")
                            return@addOnSuccessListener
                        }

                        var totalMarks = 0L
                        var score = 0L
                        var correctCount = 0L
                        var wrongCount = 0L
                        var unansweredCount = 0L

                        val reviewData = mutableListOf<Map<String, Any>>()

                        for (document in questionDocuments.documents) {
                            val questionId = document.getString("questionId") ?: document.id
                            val questionText = document.getString("question") ?: "Question"
                            val rawOptions = (document.get("options") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                            val correctAnswer = document.getString("correctAnswer")
                                ?.trim()
                                ?.uppercase()
                                ?: ""
                            val explanation = document.getString("explanation") ?: ""

                            val marks = document.getLong("marks") ?: 1L
                            totalMarks += marks

                            val studentAnswer = answers[questionId]
                                ?.trim()
                                ?.uppercase()
                                ?: ""

                            val isCorrect: Boolean
                            when {
                                studentAnswer.isBlank() -> {
                                    unansweredCount++
                                    isCorrect = false
                                }
                                studentAnswer == correctAnswer -> {
                                    score += marks
                                    correctCount++
                                    isCorrect = true
                                }
                                else -> {
                                    wrongCount++
                                    isCorrect = false
                                }
                            }

                            reviewData.add(
                                hashMapOf(
                                    "questionId" to questionId,
                                    "question" to questionText,
                                    "options" to rawOptions,
                                    "selectedAnswer" to studentAnswer,
                                    "correctAnswer" to correctAnswer,
                                    "explanation" to explanation,
                                    "isCorrect" to isCorrect,
                                    "marks" to marks
                                )
                            )
                        }

                        val percentage = if (totalMarks > 0L) {
                            round((score.toDouble() / totalMarks.toDouble()) * 10000.0) / 100.0
                        } else {
                            0.0
                        }

                        val passed = percentage >= passingScore.toDouble()

                        val attemptRef = db.collection("attempts").document()

                        val attemptData = hashMapOf<String, Any>(
                            "attemptId" to attemptRef.id,
                            "quizId" to quizId,
                            "quizTitle" to quizTitle,
                            "teacherId" to teacherId,
                            "subjectId" to subjectId,
                            "userId" to user.uid,
                            "answers" to answers,
                            "reviewData" to reviewData,
                            "questionCount" to questionDocuments.size(),
                            "answeredCount" to answers.size,
                            "correctCount" to correctCount,
                            "wrongCount" to wrongCount,
                            "unansweredCount" to unansweredCount,
                            "score" to score,
                            "totalMarks" to totalMarks,
                            "percentage" to percentage,
                            "passingScore" to passingScore,
                            "passed" to passed,
                            "timeUp" to timeUp,
                            "status" to "scored",
                            "submittedAt" to FieldValue.serverTimestamp(),
                            "scoredAt" to FieldValue.serverTimestamp()
                        )

                        attemptRef.set(attemptData)
                            .addOnSuccessListener {
                                onSuccess(attemptRef.id)
                            }
                            .addOnFailureListener { exception ->
                                onError(
                                    exception.localizedMessage
                                        ?: "Failed to save quiz result."
                                )
                            }
                    }
                    .addOnFailureListener { exception ->
                        onError(
                            exception.localizedMessage
                                ?: "Failed to load quiz questions."
                        )
                    }
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.localizedMessage
                        ?: "Failed to load quiz information."
                )
            }
    }
}
