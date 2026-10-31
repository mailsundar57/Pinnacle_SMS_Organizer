package com.pinnacle.smsorganizer.ui.navigation

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pinnacle.smsorganizer.PinnacleSmsApp
import com.pinnacle.smsorganizer.ui.conversation.ConversationScreen
import com.pinnacle.smsorganizer.ui.conversation.ConversationViewModel
import com.pinnacle.smsorganizer.ui.compose.ComposeSmsActivity
import com.pinnacle.smsorganizer.ui.inbox.InboxScreen
import com.pinnacle.smsorganizer.ui.inbox.InboxViewModel
import kotlinx.coroutines.launch

@Composable
fun SmsOrganizerNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PinnacleSmsApp
    val coroutineScope = rememberCoroutineScope()

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Notifications are optional; incoming SMS remains stored if denied. */ }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_SMS] == true
        val allSmsPermissionsGranted = readGranted &&
            permissions[Manifest.permission.RECEIVE_SMS] == true &&
            permissions[Manifest.permission.SEND_SMS] == true
        coroutineScope.launch { app.repository.refresh() }
        if (readGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        Toast.makeText(
            context,
            when {
                allSmsPermissionsGranted -> "SMS access enabled"
                readGranted -> "Some SMS permissions were denied; send or receive may be unavailable."
                else -> "SMS access denied. Demo mode remains available."
            },
            if (allSmsPermissionsGranted) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
        ).show()
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val isDefault = android.provider.Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        if (isDefault) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.SEND_SMS
                )
            )
        } else {
            Toast.makeText(context, "Default SMS role was not granted.", Toast.LENGTH_LONG).show()
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
                onRequestSmsPermission = {
                    if (android.provider.Telephony.Sms.getDefaultSmsPackage(context) == context.packageName) {
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.SEND_SMS
                        ))
                    } else {
                        val roleIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val manager = context.getSystemService(RoleManager::class.java)
                            if (manager?.isRoleAvailable(RoleManager.ROLE_SMS) == true)
                                manager.createRequestRoleIntent(RoleManager.ROLE_SMS) else null
                        } else null
                        roleLauncher.launch(roleIntent ?: Intent(
                            android.provider.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT
                        ).putExtra(android.provider.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName))
                    }
                },
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
                onCompose = { address ->
                    context.startActivity(Intent(context, ComposeSmsActivity::class.java).apply {
                        data = Uri.fromParts("smsto", address, null)
                    })
                }
            )
        }
    }
}
