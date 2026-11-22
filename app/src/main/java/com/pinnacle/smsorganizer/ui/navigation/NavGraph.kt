package com.pinnacle.smsorganizer.ui.navigation

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pinnacle.smsorganizer.PinnacleSmsApp
import com.pinnacle.smsorganizer.ui.components.openAppSettings
import com.pinnacle.smsorganizer.ui.components.openDefaultAppsSettings
import com.pinnacle.smsorganizer.ui.compose.ComposeSmsActivity
import com.pinnacle.smsorganizer.ui.conversation.ConversationScreen
import com.pinnacle.smsorganizer.ui.conversation.ConversationViewModel
import com.pinnacle.smsorganizer.ui.inbox.InboxScreen
import com.pinnacle.smsorganizer.ui.inbox.InboxViewModel
import kotlinx.coroutines.launch

private enum class PermissionHelpReason {
    ROLE_BLOCKED,
    READ_ONLY_DENIED
}

private val FULL_SMS_PERMISSIONS = arrayOf(
    Manifest.permission.READ_SMS,
    Manifest.permission.RECEIVE_SMS,
    Manifest.permission.SEND_SMS
)

private val READ_ONLY_SMS_PERMISSIONS = arrayOf(
    Manifest.permission.READ_SMS,
    Manifest.permission.RECEIVE_SMS
)

@Composable
fun SmsOrganizerNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PinnacleSmsApp
    val coroutineScope = rememberCoroutineScope()
    var permissionHelpReason by remember { mutableStateOf<PermissionHelpReason?>(null) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // Notifications are optional; incoming SMS remains stored if denied.
    }

    fun requestNotificationsIfNeeded() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val fullPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        coroutineScope.launch { app.repository.refresh() }
        val readGranted = isPermissionGranted(context, Manifest.permission.READ_SMS)
        val allGranted = missingPermissions(context, FULL_SMS_PERMISSIONS).isEmpty()
        if (readGranted) {
            requestNotificationsIfNeeded()
        }
        when {
            allGranted -> {
                Toast.makeText(context, "SMS access enabled", Toast.LENGTH_SHORT).show()
            }
            readGranted -> {
                Toast.makeText(
                    context,
                    "Read access enabled; some SMS permissions were denied.",
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                permissionHelpReason = PermissionHelpReason.READ_ONLY_DENIED
            }
        }
    }

    val readOnlyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        coroutineScope.launch { app.repository.refresh() }
        if (isPermissionGranted(context, Manifest.permission.READ_SMS)) {
            requestNotificationsIfNeeded()
            Toast.makeText(context, "Read-only SMS access enabled", Toast.LENGTH_SHORT).show()
        } else {
            permissionHelpReason = PermissionHelpReason.READ_ONLY_DENIED
        }
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        coroutineScope.launch { app.repository.refresh() }
        if (isDefaultSmsApp(context)) {
            val missing = missingPermissions(context, FULL_SMS_PERMISSIONS)
            if (missing.isEmpty()) {
                requestNotificationsIfNeeded()
                Toast.makeText(context, "Pinnacle is now your default SMS app", Toast.LENGTH_SHORT).show()
            } else {
                fullPermissionLauncher.launch(missing)
            }
        } else {
            permissionHelpReason = PermissionHelpReason.ROLE_BLOCKED
        }
    }

    val onRequestDefaultSms: () -> Unit = {
        if (isDefaultSmsApp(context)) {
            val missing = missingPermissions(context, FULL_SMS_PERMISSIONS)
            if (missing.isEmpty()) {
                coroutineScope.launch { app.repository.refresh() }
                requestNotificationsIfNeeded()
                Toast.makeText(context, "All SMS permissions are already granted", Toast.LENGTH_SHORT).show()
            } else {
                fullPermissionLauncher.launch(missing)
            }
        } else {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS))
            } else {
                permissionHelpReason = PermissionHelpReason.ROLE_BLOCKED
            }
        }
    }

    val onRequestReadOnlyPermission: () -> Unit = {
        val missing = missingPermissions(context, READ_ONLY_SMS_PERMISSIONS)
        if (missing.isEmpty()) {
            coroutineScope.launch { app.repository.refresh() }
            Toast.makeText(context, "Read-only SMS access enabled", Toast.LENGTH_SHORT).show()
        } else {
            readOnlyPermissionLauncher.launch(missing)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Inbox.route,
        modifier = modifier
    ) {
        composable(Screen.Inbox.route) {
            val inboxViewModel: InboxViewModel = viewModel(
                factory = InboxViewModel.Factory(app.repository)
            )

            InboxScreen(
                viewModel = inboxViewModel,
                onRequestDefaultSms = onRequestDefaultSms,
                onRequestReadOnlyPermission = onRequestReadOnlyPermission,
                onNavigateToConversation = { threadId, address ->
                    navController.navigate(Screen.Conversation.createRoute(threadId, address))
                }
            )
        }

        composable(
            route = Screen.Conversation.route,
            arguments = listOf(
                navArgument("threadId") { type = NavType.LongType },
                navArgument("address") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getLong("threadId") ?: 0L
            val rawAddress = backStackEntry.arguments?.getString("address") ?: ""
            val address = Uri.decode(rawAddress)

            val conversationViewModel: ConversationViewModel = viewModel(
                factory = ConversationViewModel.Factory(threadId, address, app.repository)
            )

            ConversationScreen(
                viewModel = conversationViewModel,
                onNavigateBack = { navController.popBackStack() },
                onCompose = { targetAddress ->
                    context.startActivity(
                        Intent(context, ComposeSmsActivity::class.java).apply {
                            data = Uri.fromParts("smsto", targetAddress, null)
                        }
                    )
                }
            )
        }
    }

    permissionHelpReason?.let { reason ->
        val isAndroid15Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
        val title = when (reason) {
            PermissionHelpReason.ROLE_BLOCKED -> "Default SMS app was not enabled"
            PermissionHelpReason.READ_ONLY_DENIED -> "SMS permission was not granted"
        }
        val body = when {
            reason == PermissionHelpReason.ROLE_BLOCKED && isAndroid15Plus ->
                "On Android 15 and 16, apps installed directly from an APK are restricted from holding the SMS role until unlocked:\n\n" +
                    "1. Tap Open App info below → Permissions → SMS (tap it once if greyed out to trigger Android's Restricted setting notice).\n" +
                    "2. Go back one screen to App info, tap ⋮ (top right) → Allow restricted settings, and unlock with your screen lock.\n" +
                    "3. Return here and tap Set as Default SMS App, or select Pinnacle under Default apps → SMS app.\n\n" +
                    "Apps installed via Google Play do not require this manual unlock."
            reason == PermissionHelpReason.ROLE_BLOCKED ->
                "Open Default apps → SMS app to choose Pinnacle, or check if a work profile or device policy restricts changing the default SMS app."
            isAndroid15Plus ->
                "Android denied SMS read access. On Android 15 and 16, sideloaded APKs block SMS permissions until unlocked:\n\n" +
                    "1. Tap Open App info → Permissions → SMS.\n" +
                    "2. If SMS is greyed out, tap it once, return to App info, tap ⋮ → Allow restricted settings, then go back to Permissions → SMS → Allow.\n\n" +
                    "You can also grant full access by setting Pinnacle as your default SMS app."
            else ->
                "SMS permission was denied. Open App info → Permissions → SMS and select Allow, or set Pinnacle as your default SMS app."
        }

        AlertDialog(
            onDismissRequest = { permissionHelpReason = null },
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(
                    onClick = {
                        permissionHelpReason = null
                        openAppSettings(context)
                    }
                ) {
                    Text("Open App info")
                }
            },
            dismissButton = {
                if (reason == PermissionHelpReason.ROLE_BLOCKED) {
                    TextButton(
                        onClick = {
                            permissionHelpReason = null
                            openDefaultAppsSettings(context)
                        }
                    ) {
                        Text("Default apps")
                    }
                } else {
                    TextButton(onClick = { permissionHelpReason = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

private fun isDefaultSmsApp(context: Context): Boolean {
    val roleManager = context.getSystemService(RoleManager::class.java)
    if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
        return roleManager.isRoleHeld(RoleManager.ROLE_SMS)
    }
    return Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
}

private fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun missingPermissions(context: Context, permissions: Array<String>): Array<String> {
    return permissions.filterNot { isPermissionGranted(context, it) }.toTypedArray()
}
