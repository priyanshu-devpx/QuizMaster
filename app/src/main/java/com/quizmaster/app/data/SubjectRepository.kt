package com.quizmaster.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.quizmaster.app.data.model.Subject

object SubjectRepository {

    private val db = FirebaseFirestore.getInstance()
    private val subjectsCollection = db.collection("subjects")

    fun getSubjects(
        onSuccess: (List<Subject>) -> Unit,
        onError: (String) -> Unit
    ) {
        subjectsCollection
            .orderBy("name", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        Subject(
                            subjectId = doc.getString("subjectId") ?: doc.id,
                            name = doc.getString("name") ?: "",
                            code = doc.getString("code") ?: "",
                            description = doc.getString("description") ?: "",
                            status = doc.getString("status") ?: "active",
                            createdAt = doc.get("createdAt"),
                            updatedAt = doc.get("updatedAt")
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                onSuccess(list)
            }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Failed to load subjects.")
            }
    }

    fun getActiveSubjects(
        onSuccess: (List<Subject>) -> Unit,
        onError: (String) -> Unit
    ) {
        subjectsCollection
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        Subject(
                            subjectId = doc.getString("subjectId") ?: doc.id,
                            name = doc.getString("name") ?: "",
                            code = doc.getString("code") ?: "",
                            description = doc.getString("description") ?: "",
                            status = doc.getString("status") ?: "active",
                            createdAt = doc.get("createdAt"),
                            updatedAt = doc.get("updatedAt")
                        )
                    } catch (_: Exception) {
                        null
                    }
                }.sortedBy { it.name }
                onSuccess(list)
            }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Failed to load active subjects.")
            }
    }

    fun getSubjectById(
        subjectId: String,
        onSuccess: (Subject?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (subjectId.isBlank()) {
            onSuccess(null)
            return
        }

        subjectsCollection
            .document(subjectId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onSuccess(null)
                } else {
                    val subject = Subject(
                        subjectId = doc.getString("subjectId") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        code = doc.getString("code") ?: "",
                        description = doc.getString("description") ?: "",
                        status = doc.getString("status") ?: "active",
                        createdAt = doc.get("createdAt"),
                        updatedAt = doc.get("updatedAt")
                    )
                    onSuccess(subject)
                }
            }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Failed to get subject details.")
            }
    }

    fun createSubject(
        name: String,
        code: String,
        description: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanName = name.trim()
        val cleanCode = code.trim().uppercase()
        val cleanDescription = description.trim()

        if (cleanName.length < 2) {
            onError("Subject name must be at least 2 characters.")
            return
        }

        val docRef = subjectsCollection.document()
        val subjectData = hashMapOf(
            "subjectId" to docRef.id,
            "name" to cleanName,
            "code" to cleanCode,
            "description" to cleanDescription,
            "status" to "active",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.set(subjectData)
            .addOnSuccessListener {
                onSuccess(docRef.id)
            }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Failed to create subject.")
            }
    }

    fun updateSubject(
        subjectId: String,
        name: String,
        code: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanName = name.trim()
        val cleanCode = code.trim().uppercase()
        val cleanDescription = description.trim()

        if (subjectId.isBlank()) {
            onError("Invalid subject ID.")
            return
        }

        if (cleanName.length < 2) {
            onError("Subject name must be at least 2 characters.")
            return
        }

        val updates = hashMapOf<String, Any>(
            "name" to cleanName,
            "code" to cleanCode,
            "description" to cleanDescription,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        subjectsCollection.document(subjectId)
            .update(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Failed to update subject.")
            }
    }

    fun setSubjectStatus(
        subjectId: String,
        status: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (subjectId.isBlank()) {
            onError("Invalid subject ID.")
            return
        }

        val updates = hashMapOf<String, Any>(
            "status" to status.lowercase(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        subjectsCollection.document(subjectId)
            .update(updates)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Failed to update subject status.")
            }
    }
}
