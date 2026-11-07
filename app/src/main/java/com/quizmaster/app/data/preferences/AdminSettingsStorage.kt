package com.quizmaster.app.data.preferences

import android.content.Context

object AdminSettingsStorage {

    private const val PREFS_NAME = "quizmaster_admin_settings"

    private const val KEY_DEFAULT_DURATION = "default_quiz_duration"
    private const val KEY_DEFAULT_PASSING_SCORE = "default_passing_score"
    private const val KEY_DEFAULT_MARKS = "default_question_marks"

    const val DEFAULT_DURATION = 10
    const val DEFAULT_PASSING_SCORE = 40
    const val DEFAULT_MARKS = 1

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun getDefaultDuration(context: Context): Int {
        return preferences(context).getInt(
            KEY_DEFAULT_DURATION,
            DEFAULT_DURATION
        )
    }

    fun getDefaultPassingScore(context: Context): Int {
        return preferences(context).getInt(
            KEY_DEFAULT_PASSING_SCORE,
            DEFAULT_PASSING_SCORE
        )
    }

    fun getDefaultMarks(context: Context): Int {
        return preferences(context).getInt(
            KEY_DEFAULT_MARKS,
            DEFAULT_MARKS
        )
    }

    fun saveDefaults(
        context: Context,
        duration: Int,
        passingScore: Int,
        marks: Int
    ) {
        preferences(context)
            .edit()
            .putInt(KEY_DEFAULT_DURATION, duration)
            .putInt(KEY_DEFAULT_PASSING_SCORE, passingScore)
            .putInt(KEY_DEFAULT_MARKS, marks)
            .apply()
    }
}