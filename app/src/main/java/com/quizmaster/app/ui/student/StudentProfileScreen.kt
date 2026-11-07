package com.quizmaster.app.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun StudentProfileScreen(
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    BackHandler(onBack = onBack)

    var name by rememberSaveable {
        mutableStateOf("")
    }

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isEditing by rememberSaveable {
        mutableStateOf(false)
    }

    var editedName by rememberSaveable {
        mutableStateOf("")
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var message by rememberSaveable {
        mutableStateOf("")
    }

    var isError by rememberSaveable {
        mutableStateOf(false)
    }

    var reloadKey by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(reloadKey) {

        val user = auth.currentUser

        if (user == null) {
            message = "No user is currently signed in."
            isError = true
            isLoading = false
            return@LaunchedEffect
        }

        email = user.email ?: ""

        firestore
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    name =
                        document.getString("name")
                            ?: ""

                    editedName = name

                    email =
                        document.getString("email")
                            ?: user.email
                                    ?: ""

                } else {
                    message = "Profile information not found."
                    isError = true
                }

                isLoading = false
            }
            .addOnFailureListener { exception ->

                message =
                    exception.localizedMessage
                        ?: "Failed to load profile."

                isError = true
                isLoading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        ProfileHeader(
            onBack = onBack,
            onSettingsClick = onSettingsClick
        )

        when {

            isLoading -> {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            isError -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Unable to load profile",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = message.ifBlank {
                                "Something went wrong. Please try again."
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        Button(
                            onClick = {
                                isLoading = true
                                isError = false
                                message = ""
                                reloadKey++
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }

            else -> {

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 20.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement =
                        Arrangement.spacedBy(16.dp)
                ) {

                    item {

                        ProfileIdentityCard(
                            name = name,
                            email = email
                        )
                    }

                    item {

                        if (isEditing) {

                            EditProfileCard(
                                editedName = editedName,
                                onNameChange = {
                                    editedName = it
                                    message = ""
                                    isError = false
                                },
                                isSaving = isSaving,
                                onCancel = {
                                    editedName = name
                                    isEditing = false
                                    message = ""
                                },
                                onSave = {

                                    val cleanName =
                                        editedName.trim()

                                    if (cleanName.length < 2) {

                                        message =
                                            "Name must contain at least 2 characters."

                                        isError = true

                                    } else {

                                        val user =
                                            auth.currentUser

                                        if (user == null) {

                                            message =
                                                "User session not found."

                                            isError = true

                                        } else {

                                            isSaving = true
                                            message = ""
                                            isError = false

                                            firestore
                                                .collection("users")
                                                .document(user.uid)
                                                .update(
                                                    "name",
                                                    cleanName
                                                )
                                                .addOnSuccessListener {

                                                    name =
                                                        cleanName

                                                    editedName =
                                                        cleanName

                                                    isEditing =
                                                        false

                                                    isSaving =
                                                        false

                                                    message =
                                                        "Profile updated successfully."

                                                    isError =
                                                        false
                                                }
                                                .addOnFailureListener { exception ->

                                                    message =
                                                        exception.localizedMessage
                                                            ?: "Failed to update profile."

                                                    isError =
                                                        true

                                                    isSaving =
                                                        false
                                                }
                                        }
                                    }
                                }
                            )

                        } else {

                            ProfileMenuCard(
                                onEditProfile = {
                                    editedName = name
                                    isEditing = true
                                    message = ""
                                    isError = false
                                },
                                onSettingsClick = onSettingsClick
                            )
                        }
                    }

                    item {

                        if (message.isNotBlank()) {

                            Text(
                                text = message,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color =
                                    if (isError)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(
                                    horizontal = 4.dp
                                )
                            )
                        }
                    }

                    item {

                        AccountInfoCard(
                            email = email
                        )
                    }

                    item {

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        OutlinedButton(
                            onClick = {
                                if (!isSaving) {
                                    onLogout()
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Logout,
                                contentDescription = null,
                                modifier =
                                    Modifier.size(20.dp)
                            )

                            Spacer(
                                modifier = Modifier.size(8.dp)
                            )

                            Text(
                                text = "Logout",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(
                start = 8.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(
            onClick = onBack
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = "Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = "Manage your account",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = onSettingsClick
        ) {

            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ProfileIdentityCard(
    name: String,
    email: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text =
                        if (name.isNotBlank())
                            name.trim()
                                .first()
                                .uppercase()
                        else
                            "S",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(
                modifier = Modifier.size(16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text =
                        if (name.isBlank())
                            "Student"
                        else
                            name,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        if (email.isBlank())
                            "No email available"
                        else
                            email,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "STUDENT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50.dp)
                        )
                        .padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuCard(
    onEditProfile: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {

        ProfileMenuRow(
            icon = Icons.Default.Edit,
            title = "Edit Profile",
            subtitle = "Update your name",
            onClick = onEditProfile
        )

        androidx.compose.material3.HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        ProfileMenuRow(
            icon = Icons.Default.Settings,
            title = "Settings",
            subtitle = "App preferences and options",
            onClick = onSettingsClick
        )
    }
}

@Composable
private fun ProfileMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickableWithoutIndication(onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(
            modifier = Modifier.size(12.dp)
        )

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
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector =
                Icons.Default.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun EditProfileCard(
    editedName: String,
    onNameChange: (String) -> Unit,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = "Edit Profile",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            OutlinedTextField(
                value = editedName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Full name")
                },
                singleLine = true,
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {

                    Text(
                        text = "Cancel"
                    )
                }

                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {

                    if (isSaving) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )

                    } else {

                        Text(
                            text = "Save",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountInfoCard(
    email: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = "Account Information",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(
                    modifier = Modifier.size(10.dp)
                )

                Column {

                    Text(
                        text = "Email",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text =
                            if (email.isBlank())
                                "Not available"
                            else
                                email,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(
                    modifier = Modifier.size(10.dp)
                )

                Column {

                    Text(
                        text = "Account type",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = "Student",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/*
 * Small helper so menu rows don't need ripple-specific imports.
 */
private fun Modifier.clickableWithoutIndication(
    onClick: () -> Unit
): Modifier {
    return this.then(
        Modifier.clickable(
            onClick = onClick
        )
    )
}
