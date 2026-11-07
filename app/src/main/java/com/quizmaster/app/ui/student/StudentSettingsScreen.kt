package com.quizmaster.app.ui.student

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.quizmaster.app.data.preferences.UserPreferences
import kotlinx.coroutines.launch

@Composable
fun StudentSettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onProfileClick: () -> Unit = {}
) {
    // System back from Settings returns to the Profile screen.
    BackHandler(onBack = onBack)

    val context = LocalContext.current

    val userPreferences = remember {
        UserPreferences(context.applicationContext)
    }

    val coroutineScope = rememberCoroutineScope()

    var notificationsEnabled by remember {
        mutableStateOf(true)
    }

    var darkModeEnabled by remember {
        mutableStateOf(false)
    }

    var isSendingPasswordReset by remember {
        mutableStateOf(false)
    }

    var showLogoutDialog by rememberSaveable {
        mutableStateOf(false)
    }

    // ---------------------------------------------------------
    // LOAD SAVED SETTINGS
    // ---------------------------------------------------------

    LaunchedEffect(Unit) {

        launch {
            userPreferences.notificationsEnabled.collect { enabled ->
                notificationsEnabled = enabled
            }
        }

        launch {
            userPreferences.darkModeEnabled.collect { enabled ->
                darkModeEnabled = enabled
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
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

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // =====================================================
        // PREFERENCES
        // =====================================================

        Text(
            text = "Preferences",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        SettingsCard {

            SettingsSwitchRow(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                description = "Receive quiz and result notifications",
                checked = notificationsEnabled,
                onCheckedChange = { enabled ->

                    notificationsEnabled = enabled

                    coroutineScope.launch {
                        userPreferences.setNotificationsEnabled(
                            enabled
                        )
                    }
                }
            )

            HorizontalDivider()

            SettingsSwitchRow(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                description = "Use dark appearance throughout the app",
                checked = darkModeEnabled,
                onCheckedChange = { enabled ->

                    darkModeEnabled = enabled

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
        // ACCOUNT
        // =====================================================

        Text(
            text = "Account",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        SettingsCard {

            SettingsActionRow(
                icon = Icons.Default.Lock,
                title = "Change Password",
                description =
                    if (isSendingPasswordReset) {
                        "Sending reset email..."
                    } else {
                        "Send a password reset email"
                    },
                onClick = {

                    if (!isSendingPasswordReset) {

                        val user = FirebaseAuth
                            .getInstance()
                            .currentUser

                        val email = user?.email

                        if (email.isNullOrBlank()) {

                            Toast.makeText(
                                context,
                                "Email address not available",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {

                            isSendingPasswordReset = true

                            FirebaseAuth
                                .getInstance()
                                .sendPasswordResetEmail(email)
                                .addOnSuccessListener {

                                    isSendingPasswordReset = false

                                    Toast.makeText(
                                        context,
                                        "Password reset email sent to $email",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                .addOnFailureListener { error ->

                                    isSendingPasswordReset = false

                                    Toast.makeText(
                                        context,
                                        error.localizedMessage
                                            ?: "Unable to send reset email",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    }
                }
            )

            HorizontalDivider()

            SettingsActionRow(
                icon = Icons.Default.Person,
                title = "Profile",
                description = "Manage your personal information",
                onClick = onProfileClick
            )
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // =====================================================
        // APP
        // =====================================================

        Text(
            text = "App",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        SettingsCard {

            SettingsActionRow(
                icon = Icons.Default.Download,
                title = "Download Latest Version",
                description =
                    "Open the QuizMaster download page to get the latest APK.",
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
            modifier = Modifier.height(28.dp)
        )

        // =====================================================
        // ABOUT
        // =====================================================

        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        SettingsCard {

            SettingsActionRow(
                icon = Icons.Default.Info,
                title = "About QuizMaster",
                description = "Quiz management and learning platform",
                onClick = {

                    Toast.makeText(
                        context,
                        "QuizMaster\nVersion 1.0",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // =====================================================
        // LOGOUT
        // =====================================================

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.errorContainer
            ),
            shape = MaterialTheme.shapes.large
        ) {

            TextButton(
                onClick = {
                    if (!isSendingPasswordReset) {
                        showLogoutDialog = true
                    }
                },
                enabled = !isSendingPasswordReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {

                Text(
                    text = "Logout",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        Text(
            text = "QuizMaster • Version 1.0",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(24.dp)
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
                Text(
                    text = "Logout?",
                    fontWeight = FontWeight.Bold
                )
            },

            text = {
                Text(
                    text = "Are you sure you want to log out of QuizMaster?"
                )
            },

            confirmButton = {

                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Logout")
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


// ==========================================================
// SETTINGS CARD
// ==========================================================

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}


// ==========================================================
// SWITCH ROW
// ==========================================================

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}


// ==========================================================
// ACTION ROW
// ==========================================================

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 16.dp,
                vertical = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}