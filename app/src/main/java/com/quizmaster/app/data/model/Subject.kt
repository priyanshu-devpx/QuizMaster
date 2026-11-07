package com.quizmaster.app.data.model

data class Subject(
    val subjectId: String = "",
    val name: String = "",
    val code: String = "",
    val description: String = "",
    val status: String = "active", // "active" | "inactive"
    val createdAt: Any? = null,
    val updatedAt: Any? = null
) {
    val isActive: Boolean
        get() = status.equals("active", ignoreCase = true)
}
