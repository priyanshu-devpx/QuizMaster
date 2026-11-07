package com.quizmaster.app.ui.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------
// REGISTER SCREEN
// ---------------------------------------------------------

@Composable
fun RegisterScreen(
    onRegister: (
        name: String,
        email: String,
        password: String
    ) -> Unit,
    onLoginClick: () -> Unit
) {

    // -----------------------------------------------------
    // FORM VALUES
    // -----------------------------------------------------

    var fullName by rememberSaveable {
        mutableStateOf("")
    }

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    var confirmPassword by rememberSaveable {
        mutableStateOf("")
    }

    // -----------------------------------------------------
    // PASSWORD VISIBILITY
    // -----------------------------------------------------

    var passwordVisible by rememberSaveable {
        mutableStateOf(false)
    }

    var confirmPasswordVisible by rememberSaveable {
        mutableStateOf(false)
    }

    // -----------------------------------------------------
    // TERMS
    // -----------------------------------------------------

    var termsAccepted by rememberSaveable {
        mutableStateOf(false)
    }

    // -----------------------------------------------------
    // ERROR STATES
    // -----------------------------------------------------

    var nameError by rememberSaveable {
        mutableStateOf(false)
    }

    var emailError by rememberSaveable {
        mutableStateOf(false)
    }

    var passwordError by rememberSaveable {
        mutableStateOf(false)
    }

    var confirmPasswordError by rememberSaveable {
        mutableStateOf(false)
    }

    var termsError by rememberSaveable {
        mutableStateOf(false)
    }

    // -----------------------------------------------------
    // MAIN LAYOUT
    // -----------------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 24.dp,
                vertical = 28.dp
            )
    ) {

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        // -------------------------------------------------
        // TITLE
        // -------------------------------------------------

        Text(
            text = "Create account",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Join QuizMaster and start testing your knowledge.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // -------------------------------------------------
        // FULL NAME
        // -------------------------------------------------

        OutlinedTextField(
            value = fullName,

            onValueChange = {
                fullName = it
                nameError = false
            },

            modifier = Modifier.fillMaxWidth(),

            label = {
                Text(
                    text = "Full name"
                )
            },

            singleLine = true,

            isError = nameError,

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text
            ),

            shape = RoundedCornerShape(12.dp),

            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                errorTextColor = MaterialTheme.colorScheme.onSurface,

                cursorColor = MaterialTheme.colorScheme.primary,

                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = MaterialTheme.colorScheme.error,

                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )

        // -------------------------------------------------
        // NAME ERROR
        // -------------------------------------------------

        if (nameError) {

            Text(
                text = "Please enter your full name",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 5.dp
                )
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // -------------------------------------------------
        // EMAIL
        // -------------------------------------------------

        OutlinedTextField(
            value = email,

            onValueChange = {
                email = it
                emailError = false
            },

            modifier = Modifier.fillMaxWidth(),

            label = {
                Text(
                    text = "Email address"
                )
            },

            singleLine = true,

            isError = emailError,

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),

            shape = RoundedCornerShape(12.dp),

            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                errorTextColor = MaterialTheme.colorScheme.onSurface,

                cursorColor = MaterialTheme.colorScheme.primary,

                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = MaterialTheme.colorScheme.error,

                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
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
                    top = 5.dp
                )
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // -------------------------------------------------
        // PASSWORD
        // -------------------------------------------------

        OutlinedTextField(
            value = password,

            onValueChange = {
                password = it
                passwordError = false
            },

            modifier = Modifier.fillMaxWidth(),

            label = {
                Text(
                    text = "Password"
                )
            },

            singleLine = true,

            isError = passwordError,

            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),

            trailingIcon = {

                TextButton(
                    onClick = {
                        passwordVisible = !passwordVisible
                    }
                ) {

                    Text(
                        text = if (passwordVisible) {
                            "Hide"
                        } else {
                            "Show"
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },

            shape = RoundedCornerShape(12.dp),

            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                errorTextColor = MaterialTheme.colorScheme.onSurface,

                cursorColor = MaterialTheme.colorScheme.primary,

                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = MaterialTheme.colorScheme.error,

                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )

        // -------------------------------------------------
        // PASSWORD ERROR
        // -------------------------------------------------

        if (passwordError) {

            Text(
                text = "Password must contain at least 6 characters",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 5.dp
                )
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // -------------------------------------------------
        // CONFIRM PASSWORD
        // -------------------------------------------------

        OutlinedTextField(
            value = confirmPassword,

            onValueChange = {
                confirmPassword = it
                confirmPasswordError = false
            },

            modifier = Modifier.fillMaxWidth(),

            label = {
                Text(
                    text = "Confirm password"
                )
            },

            singleLine = true,

            isError = confirmPasswordError,

            visualTransformation = if (confirmPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },

            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),

            trailingIcon = {

                TextButton(
                    onClick = {
                        confirmPasswordVisible =
                            !confirmPasswordVisible
                    }
                ) {

                    Text(
                        text = if (confirmPasswordVisible) {
                            "Hide"
                        } else {
                            "Show"
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },

            shape = RoundedCornerShape(12.dp),

            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                errorTextColor = MaterialTheme.colorScheme.onSurface,

                cursorColor = MaterialTheme.colorScheme.primary,

                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                errorLabelColor = MaterialTheme.colorScheme.error,

                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )

        // -------------------------------------------------
        // CONFIRM PASSWORD ERROR
        // -------------------------------------------------

        if (confirmPasswordError) {

            Text(
                text = "Passwords do not match",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(
                    start = 4.dp,
                    top = 5.dp
                )
            )
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        // -------------------------------------------------
        // TERMS & CONDITIONS
        // -------------------------------------------------

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Checkbox(
                checked = termsAccepted,

                onCheckedChange = {
                    termsAccepted = it
                    termsError = false
                }
            )

            Text(
                text = "I agree to the Terms & Conditions",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,

                modifier = Modifier.clickable {
                    termsAccepted = !termsAccepted
                    termsError = false
                }
            )
        }

        // -------------------------------------------------
        // TERMS ERROR
        // -------------------------------------------------

        if (termsError) {

            Text(
                text = "Please accept the Terms & Conditions",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(
                    start = 4.dp
                )
            )
        }

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        // -------------------------------------------------
        // CREATE ACCOUNT BUTTON
        // -------------------------------------------------

        Button(
            onClick = {

                val validName = fullName.trim().length >= 2

                val validEmail =
                    email.isNotBlank() &&
                            Patterns.EMAIL_ADDRESS
                                .matcher(email.trim())
                                .matches()

                val validPassword = password.length >= 6

                val validConfirmPassword =
                    confirmPassword.isNotEmpty() &&
                            confirmPassword == password

                val validTerms = termsAccepted

                nameError = !validName
                emailError = !validEmail
                passwordError = !validPassword
                confirmPasswordError = !validConfirmPassword
                termsError = !validTerms

                if (
                    validName &&
                    validEmail &&
                    validPassword &&
                    validConfirmPassword &&
                    validTerms
                ) {

                    onRegister(
                        fullName.trim(),
                        email.trim(),
                        password
                    )
                }
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),

            shape = RoundedCornerShape(12.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {

            Text(
                text = "Create Account",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        // -------------------------------------------------
        // LOGIN LINK
        // -------------------------------------------------

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Already have an account?",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            TextButton(
                onClick = onLoginClick
            ) {

                Text(
                    text = "Login",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )
    }
}
