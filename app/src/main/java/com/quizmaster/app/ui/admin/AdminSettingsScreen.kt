package com.quizmaster.app.ui.admin

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quizmaster.app.data.preferences.AdminSettingsStorage
import com.quizmaster.app.data.preferences.UserPreferences
import kotlinx.coroutines.launch

@Composable
fun AdminSettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    val userPreferences = remember {
        UserPreferences(context.applicationContext)
    }

    val coroutineScope = rememberCoroutineScope()

    val darkModeEnabled by userPreferences.darkModeEnabled
        .collectAsState(initial = false)

    var adminName by remember { mutableStateOf("") }
    var adminEmail by remember { mutableStateOf("") }
    var adminRole by remember { mutableStateOf("admin") }

    var isLoadingProfile by remember { mutableStateOf(true) }
    var profileError by remember { mutableStateOf("") }
    var isSavingProfile by remember { mutableStateOf(false) }

    var defaultDuration by remember {
        mutableStateOf(
            AdminSettingsStorage.getDefaultDuration(context).toString()
        )
    }

    var defaultPassingScore by remember {
        mutableStateOf(
            AdminSettingsStorage.getDefaultPassingScore(context).toString()
        )
    }

    var defaultMarks by remember {
        mutableStateOf(
            AdminSettingsStorage.getDefaultMarks(context).toString()
        )
    }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    // =========================================================
    // LOAD ADMIN PROFILE
    // =========================================================

    LaunchedEffect(auth.currentUser?.uid) {

        val user = auth.currentUser

        if (user == null) {
            isLoadingProfile = false
            profileError =
                "Your session has expired. Please login again."
            return@LaunchedEffect
        }

        adminEmail = user.email ?: ""

        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    profileError =
                        "Administrator profile was not found."

                    isLoadingProfile = false
                    return@addOnSuccessListener
                }

                val role = document.getString("role")
                    ?.trim()
                    ?.lowercase()
                    ?: ""

                if (role != "admin") {
                    profileError =
                        "This account does not have administrator access."

                    isLoadingProfile = false
                    return@addOnSuccessListener
                }

                adminRole = role

                adminName = document.getString("name")
                    ?.trim()
                    ?.ifBlank { "Admin" }
                    ?: "Admin"

                isLoadingProfile = false
                profileError = ""
            }
            .addOnFailureListener { exception ->

                profileError =
                    exception.localizedMessage
                        ?: "Unable to load administrator profile."

                isLoadingProfile = false
            }
    }

    // =========================================================
    // MAIN SETTINGS CONTENT
    // =========================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {

        // =====================================================
        // HEADER
        // =====================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextButton(
                onClick = onBack
            ) {
                Text("Back")
            }

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text = "Admin Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // =====================================================
        // ACCOUNT
        // =====================================================

        SettingsSectionTitle("Account")

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        SettingsCard {

            if (isLoadingProfile) {

                Text(
                    text = "Loading profile...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )

            } else if (profileError.isNotBlank()) {

                Text(
                    text = profileError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )

            } else {

                OutlinedTextField(
                    value = adminName,
                    onValueChange = {
                        adminName = it
                        profileError = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Admin name")
                    },
                    singleLine = true,
                    enabled = !isSavingProfile
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value = adminEmail,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Admin email")
                    },
                    singleLine = true,
                    readOnly = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text =
                        "Role: ${adminRole.replaceFirstChar { it.uppercase() }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Button(
                    onClick = {

                        val user = auth.currentUser

                        if (user == null) {

                            profileError =
                                "Your session has expired. Please login again."

                            return@Button
                        }

                        val cleanName = adminName.trim()

                        if (cleanName.length < 2) {

                            profileError =
                                "Admin name must contain at least 2 characters."

                            return@Button
                        }

                        isSavingProfile = true
                        profileError = ""

                        firestore.collection("users")
                            .document(user.uid)
                            .update(
                                "name",
                                cleanName
                            )
                            .addOnSuccessListener {

                                adminName = cleanName
                                isSavingProfile = false

                                Toast.makeText(
                                    context,
                                    "Profile updated successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { exception ->

                                isSavingProfile = false

                                profileError =
                                    exception.localizedMessage
                                        ?: "Unable to update profile."
                            }
                    },
                    enabled = !isSavingProfile,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        if (isSavingProfile) {
                            "Saving..."
                        } else {
                            "Save Profile"
                        }
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // =====================================================
        // SECURITY
        // =====================================================

        SettingsSectionTitle("Security")

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        SettingsCard {

            SettingsActionRow(
                title = "Change password",
                description =
                    "Send a password reset link to your admin email.",
                actionText = "Change",
                onClick = {
                    showPasswordDialog = true
                }
            )

            HorizontalDivider()

            SettingsActionRow(
                title = "Logout",
                description =
                    "Sign out from the administrator account.",
                actionText = "Logout",
                actionIsDanger = true,
                onClick = {
                    showLogoutDialog = true
                }
            )
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // =====================================================
        // QUIZ SETTINGS
        // =====================================================

        SettingsSectionTitle("Quiz Settings")

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        SettingsCard {

            Text(
                text =
                    "These values are used as defaults for new quizzes and questions.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            OutlinedTextField(
                value = defaultDuration,
                onValueChange = {

                    if (
                        it.isEmpty() ||
                        (
                                it.all(Char::isDigit) &&
                                        it.length <= 3
                                )
                    ) {
                        defaultDuration = it
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Default quiz duration")
                },
                suffix = {
                    Text("min")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = defaultPassingScore,
                onValueChange = {

                    if (
                        it.isEmpty() ||
                        (
                                it.all(Char::isDigit) &&
                                        it.length <= 3
                                )
                    ) {
                        defaultPassingScore = it
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Default passing percentage")
                },
                suffix = {
                    Text("%")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = defaultMarks,
                onValueChange = {

                    if (
                        it.isEmpty() ||
                        (
                                it.all(Char::isDigit) &&
                                        it.length <= 3
                                )
                    ) {
                        defaultMarks = it
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Default marks")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Button(
                onClick = {

                    val duration =
                        defaultDuration.toIntOrNull()

                    val passing =
                        defaultPassingScore.toIntOrNull()

                    val marks =
                        defaultMarks.toIntOrNull()

                    when {

                        duration == null ||
                                duration !in 1..300 -> {

                            Toast.makeText(
                                context,
                                "Duration must be between 1 and 300 minutes.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        passing == null ||
                                passing !in 1..100 -> {

                            Toast.makeText(
                                context,
                                "Passing percentage must be between 1 and 100.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        marks == null ||
                                marks !in 1..100 -> {

                            Toast.makeText(
                                context,
                                "Marks must be between 1 and 100.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        else -> {

                            AdminSettingsStorage.saveDefaults(
                                context = context,
                                duration = duration,
                                passingScore = passing,
                                marks = marks
                            )

                            Toast.makeText(
                                context,
                                "Quiz settings saved.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Quiz Settings")
            }
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // =====================================================
        // APPEARANCE
        // =====================================================

        SettingsSectionTitle("Appearance")

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        SettingsCard {

            SettingsSwitchRow(
                title = "Dark Mode",
                description =
                    "Use dark appearance throughout the app.",
                checked = darkModeEnabled,
                onCheckedChange = { enabled ->

                    coroutineScope.launch {
                        userPreferences.setDarkModeEnabled(
                            enabled
                        )
                    }

                    Toast.makeText(
                        context,
                        if (enabled) {
                            "Dark Mode enabled"
                        } else {
                            "Dark Mode disabled"
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // =====================================================
        // APP
        // =====================================================

        SettingsSectionTitle("App")

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        SettingsCard {

            val versionName = remember {

                try {

                    context.packageManager
                        .getPackageInfo(
                            context.packageName,
                            0
                        )
                        .versionName
                        ?: "1.0"

                } catch (_: Exception) {

                    "1.0"
                }
            }

            Text(
                text = "QuizMaster",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = "Version $versionName",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Text(
                text =
                    "QuizMaster is a quiz management system for creating, managing and attempting quizzes.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            HorizontalDivider()

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            SettingsActionRow(
                title = "Download Latest Version",
                description =
                    "Open the QuizMaster download page to get the latest APK.",
                actionText = "Open",
                onClick = {

                    val downloadUrl =
                        "https://onlyindo52-debug.github.io/quizmaster-download/"

                    try {

                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(downloadUrl)
                        )

                        context.startActivity(intent)

                    } catch (
                        _: ActivityNotFoundException
                    ) {

                        Toast.makeText(
                            context,
                            "Unable to open the download page.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        // =====================================================
        // FOOTER
        // =====================================================

        Text(
            text = "QuizMaster",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )
    }

    // =========================================================
    // CHANGE PASSWORD DIALOG
    // =========================================================

    if (showPasswordDialog) {

        AlertDialog(

            onDismissRequest = {
                showPasswordDialog = false
            },

            title = {
                Text("Change password?")
            },

            text = {
                Text(
                    "A password reset link will be sent to " +
                            "${auth.currentUser?.email ?: "your admin email"}."
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        val email =
                            auth.currentUser?.email

                        if (email.isNullOrBlank()) {

                            Toast.makeText(
                                context,
                                "Admin email is not available.",
                                Toast.LENGTH_LONG
                            ).show()

                            showPasswordDialog = false

                            return@TextButton
                        }

                        auth.sendPasswordResetEmail(email)
                            .addOnSuccessListener {

                                Toast.makeText(
                                    context,
                                    "Password reset link sent.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            .addOnFailureListener { exception ->

                                Toast.makeText(
                                    context,
                                    exception.localizedMessage
                                        ?: "Unable to send password reset email.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                        showPasswordDialog = false
                    }
                ) {
                    Text("Send Link")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showPasswordDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }


    // =========================================================
    // LOGOUT DIALOG
    // =========================================================

    if (showLogoutDialog) {

        AlertDialog(

            onDismissRequest = {
                showLogoutDialog = false
            },

            title = {
                Text("Logout?")
            },

            text = {
                Text(
                    "Are you sure you want to logout?"
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        showLogoutDialog = false

                        onLogout()
                    }
                ) {

                    Text(
                        text = "Logout",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showLogoutDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}



// =============================================================
// SETTINGS SECTION TITLE
// =============================================================

@Composable
private fun SettingsSectionTitle(
    title: String
) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

// =============================================================
// SETTINGS CARD
// =============================================================

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            content()
        }
    }
}

// =============================================================
// SETTINGS ACTION ROW
// =============================================================

@Composable
private fun SettingsActionRow(
    title: String,
    description: String,
    actionText: String,
    actionIsDanger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = description,
                fontSize = 12.sp,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TextButton(
            onClick = onClick
        ) {

            Text(
                text = actionText,
                color =
                    if (actionIsDanger) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// =============================================================
// SETTINGS SWITCH ROW
// =============================================================

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Spacer(modifier = Modifier.height(0.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = description,
                fontSize = 12.sp,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
