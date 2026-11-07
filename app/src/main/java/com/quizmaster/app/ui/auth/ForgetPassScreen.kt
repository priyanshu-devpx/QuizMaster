package com.quizmaster.app.ui.auth

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------
// FORGOT PASSWORD SCREEN
// ---------------------------------------------------------

@Composable
fun ForgotPasswordScreen(
    onSendResetLink: () -> Unit,
    onBackToLogin: () -> Unit
) {

    // -----------------------------------------------------
    // EMAIL STATE
    // -----------------------------------------------------

    var email by rememberSaveable {
        mutableStateOf("")
    }

    // -----------------------------------------------------
    // ERROR STATE
    // -----------------------------------------------------

    var emailError by rememberSaveable {
        mutableStateOf(false)
    }

    var isSending by rememberSaveable {
        mutableStateOf(false)
    }

    var successMessage by rememberSaveable {
        mutableStateOf("")
    }

    var firebaseError by rememberSaveable {
        mutableStateOf("")
    }

    val auth = remember { FirebaseAuth.getInstance() }

    BackHandler(enabled = !isSending) {
        onBackToLogin()
    }

    // -----------------------------------------------------
    // MAIN LAYOUT
    // -----------------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(
                horizontal = 24.dp,
                vertical = 28.dp
            ),
        horizontalAlignment = Alignment.Start
    ) {

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // -------------------------------------------------
        // RESET ICON
        // -------------------------------------------------

        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = Icons.Default.LockReset,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // -------------------------------------------------
        // TITLE
        // -------------------------------------------------

        Text(
            text = "Forgot password?",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // -------------------------------------------------
        // DESCRIPTION
        // -------------------------------------------------

        Text(
            text = "Enter the email address associated with your account and we'll help you reset your password.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        // -------------------------------------------------
        // EMAIL TITLE
        // -------------------------------------------------

        Text(
            text = "Email address",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // -------------------------------------------------
        // EMAIL FIELD
        // -------------------------------------------------

        OutlinedTextField(
            value = email,

            onValueChange = {
                email = it
                emailError = false
                firebaseError = ""
                successMessage =""
            },

            modifier = Modifier.fillMaxWidth(),

            label = {
                Text(
                    text = "Enter your email"
                )
            },

            placeholder = {
                Text(
                    text = "example@email.com"
                )
            },

            singleLine = true,

            isError = emailError,

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),

            shape = RoundedCornerShape(12.dp),

            colors = OutlinedTextFieldDefaults.colors(

                // -----------------------------------------
                // TYPED TEXT
                // -----------------------------------------

                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                errorTextColor = MaterialTheme.colorScheme.onSurface,

                // -----------------------------------------
                // PLACEHOLDER
                // -----------------------------------------

                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,

                // -----------------------------------------
                // CURSOR
                // -----------------------------------------

                cursorColor = MaterialTheme.colorScheme.primary,

                // -----------------------------------------
                // LABEL
                // -----------------------------------------

                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = MaterialTheme.colorScheme.error,

                // -----------------------------------------
                // BORDER
                // -----------------------------------------

                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )

        // -------------------------------------------------
        // EMAIL ERROR
        // -------------------------------------------------

        if (emailError) {

            Text(
                text = "Enter a valid email address",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 6.dp
                )
            )
        }

        if (firebaseError.isNotBlank()) {
            Text(
                text = firebaseError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 8.dp
                )
            )
        }

        if (successMessage.isNotBlank()) {
            Text(
                text = successMessage,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 8.dp
                )
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // -------------------------------------------------
        // SEND RESET LINK BUTTON
        // -------------------------------------------------

        Button(
            onClick = {

                val trimmedEmail = email.trim()

                val validEmail =
                    trimmedEmail.isNotBlank() &&
                            Patterns.EMAIL_ADDRESS
                                .matcher(trimmedEmail)
                                .matches()

                emailError = !validEmail
                firebaseError = ""
                successMessage = ""

                if (validEmail && !isSending) {
                    isSending = true

                    auth.sendPasswordResetEmail(trimmedEmail)
                        .addOnSuccessListener {
                            isSending = false
                            successMessage =
                                "Password reset email sent. Check your inbox and spam folder."
                            onSendResetLink()
                        }
                        .addOnFailureListener { exception ->
                            isSending = false
                            firebaseError = when {
                                exception.message?.contains(
                                    "no user record",
                                    ignoreCase = true
                                ) == true ->
                                    "No account was found with this email address."

                                exception.message?.contains(
                                    "badly formatted",
                                    ignoreCase = true
                                ) == true ->
                                    "Please enter a valid email address."

                                else ->
                                    exception.localizedMessage
                                        ?: "Unable to send reset email. Please try again."
                            }
                        }
                }
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            shape = RoundedCornerShape(12.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            enabled = !isSending
        ) {

            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Send Reset Link",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        // -------------------------------------------------
        // BACK TO LOGIN
        // -------------------------------------------------

        TextButton(
            onClick = onBackToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = "← Back to Login",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // -------------------------------------------------
        // PUSH FOOTER TO BOTTOM
        // -------------------------------------------------

        Spacer(
            modifier = Modifier.weight(1f)
        )

        // -------------------------------------------------
        // FOOTER INFORMATION
        // -------------------------------------------------

        Text(
            text = "You'll receive a password reset email if this address is registered.",

            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),

            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}
