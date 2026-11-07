const { setGlobalOptions } = require("firebase-functions");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getAuth } = require("firebase-admin/auth");
const logger = require("firebase-functions/logger");

initializeApp();

const db = getFirestore();
const authAdmin = getAuth();

// Limit concurrent containers for cost control.
setGlobalOptions({
    maxInstances: 10,
});

/**
 * Secure quiz scoring function.
 *
 * Trigger:
 * attempts/{attemptId}
 */
exports.scoreQuizAttempt = onDocumentCreated(
    "attempts/{attemptId}",
    async (event) => {
        const snapshot = event.data;

        if (!snapshot) {
            logger.error("Attempt document is missing.");
            return;
        }

        const attemptRef = snapshot.ref;
        const attemptData = snapshot.data();

        const attemptId = event.params.attemptId;

        logger.info("Scoring attempt", {
            attemptId: attemptId,
        });

        if (!attemptData) {
            await attemptRef.set(
                {
                    status: "scoring_failed",
                    errorMessage: "Attempt data is missing.",
                },
                { merge: true }
            );

            return;
        }

        const quizId = attemptData.quizId;
        const userId = attemptData.userId;
        const answers = attemptData.answers || {};
        const timeUp = attemptData.timeUp === true;

        if (!quizId || !userId) {
            logger.error("Invalid attempt data.", {
                attemptId: attemptId,
                quizId: quizId,
                userId: userId,
            });

            await attemptRef.set(
                {
                    status: "scoring_failed",
                    errorMessage: "Invalid quiz or user information.",
                },
                { merge: true }
            );

            return;
        }

        try {
            const userSnapshot = await db
                .collection("users")
                .doc(userId)
                .get();

            if (!userSnapshot.exists) {
                throw new Error("User profile does not exist.");
            }

            const userData = userSnapshot.data();

            if (userData.role !== "student") {
                throw new Error("Only student attempts can be scored.");
            }

            const quizSnapshot = await db
                .collection("quizzes")
                .doc(quizId)
                .get();

            if (!quizSnapshot.exists) {
                throw new Error("Quiz does not exist.");
            }

            const quizData = quizSnapshot.data();

            if (quizData.status !== "published") {
                throw new Error("This quiz is not published.");
            }

            const quizTitle = quizData.title || "Quiz";
            const passingScore = Number(quizData.passingScore || 0);

            const questionsSnapshot = await db
                .collection("questions")
                .where("quizId", "==", quizId)
                .get();

            if (questionsSnapshot.empty) {
                throw new Error("This quiz has no questions.");
            }

            let totalMarks = 0;
            let score = 0;

            let correctCount = 0;
            let wrongCount = 0;
            let unansweredCount = 0;

            questionsSnapshot.forEach((questionDocument) => {
                const question = questionDocument.data();

                const questionId =
                    question.questionId || questionDocument.id;

                const marks = Number(question.marks || 1);
                const correctAnswer = question.correctAnswer;

                totalMarks += marks;

                const studentAnswer = answers[questionId];

                if (
                    studentAnswer === undefined ||
                    studentAnswer === null ||
                    String(studentAnswer).trim() === ""
                ) {
                    unansweredCount++;
                    return;
                }

                if (
                    String(studentAnswer).trim() ===
                    String(correctAnswer).trim()
                ) {
                    score += marks;
                    correctCount++;
                } else {
                    wrongCount++;
                }
            });

            const percentage =
                totalMarks > 0
                    ? Number(((score / totalMarks) * 100).toFixed(2))
                    : 0;

            const passed = percentage >= passingScore;

            await attemptRef.set(
                {
                    status: "scored",

                    quizId: quizId,
                    quizTitle: quizTitle,
                    userId: userId,

                    score: score,
                    totalMarks: totalMarks,
                    percentage: percentage,

                    correctCount: correctCount,
                    wrongCount: wrongCount,
                    unansweredCount: unansweredCount,

                    passingScore: passingScore,
                    passed: passed,

                    timeUp: timeUp,

                    scoredAt: new Date(),
                },
                { merge: true }
            );

            logger.info("Quiz attempt scored successfully.", {
                attemptId: attemptId,
                quizId: quizId,
                userId: userId,
                score: score,
                totalMarks: totalMarks,
                percentage: percentage,
                passed: passed,
            });
        } catch (error) {
            logger.error("Quiz scoring failed.", {
                attemptId: attemptId,
                error: error.message,
            });

            await attemptRef.set(
                {
                    status: "scoring_failed",
                    errorMessage:
                        error.message || "Unable to calculate result.",
                },
                { merge: true }
            );
        }
    }
);

/**
 * Callable function for Admin to create teacher or student accounts
 */
exports.createManagedUser = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Authentication required.");
    }

    const callerUid = request.auth.uid;
    const callerDoc = await db.collection("users").doc(callerUid).get();

    if (!callerDoc.exists || callerDoc.data().role !== "admin") {
        throw new HttpsError("permission-denied", "Only administrators can perform this action.");
    }

    const { name, email, password, role, assignedSubjectId } = request.data;

    if (!name || !email || !password || !role) {
        throw new HttpsError("invalid-argument", "Name, email, password, and role are required.");
    }

    if (role !== "teacher" && role !== "student") {
        throw new HttpsError("invalid-argument", "Role must be 'teacher' or 'student'.");
    }

    if (role === "teacher" && !assignedSubjectId) {
        throw new HttpsError("invalid-argument", "Teachers must be assigned exactly one subject.");
    }

    try {
        const userRecord = await authAdmin.createUser({
            email: email.trim(),
            password: password,
            displayName: name.trim(),
        });

        const userData = {
            userId: userRecord.uid,
            name: name.trim(),
            email: email.trim(),
            role: role,
            status: "active",
            createdAt: new Date(),
            updatedAt: new Date(),
        };

        if (role === "teacher") {
            userData.assignedSubjectId = assignedSubjectId;
        }

        await db.collection("users").doc(userRecord.uid).set(userData);

        logger.info("Managed user created successfully", {
            createdUid: userRecord.uid,
            role: role,
            by: callerUid,
        });

        return { success: true, uid: userRecord.uid };
    } catch (error) {
        logger.error("Failed to create managed user", { error: error.message });
        throw new HttpsError("internal", error.message || "Failed to create user account.");
    }
});

/**
 * Callable function for Admin to toggle user account status (active/inactive)
 */
exports.setUserStatus = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Authentication required.");
    }

    const callerUid = request.auth.uid;
    const callerDoc = await db.collection("users").doc(callerUid).get();

    if (!callerDoc.exists || callerDoc.data().role !== "admin") {
        throw new HttpsError("permission-denied", "Only administrators can perform this action.");
    }

    const { targetUid, status } = request.data;

    if (!targetUid || (status !== "active" && status !== "inactive")) {
        throw new HttpsError("invalid-argument", "Target UID and valid status ('active'/'inactive') required.");
    }

    try {
        await authAdmin.updateUser(targetUid, {
            disabled: status === "inactive",
        });

        await db.collection("users").doc(targetUid).update({
            status: status,
            updatedAt: new Date(),
        });

        logger.info("User status updated", {
            targetUid: targetUid,
            status: status,
            by: callerUid,
        });

        return { success: true };
    } catch (error) {
        logger.error("Failed to set user status", { error: error.message });
        throw new HttpsError("internal", error.message || "Failed to update account status.");
    }
});
