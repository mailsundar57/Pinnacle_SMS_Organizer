package com.pinnacle.smsorganizer.ui.navigation

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
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
import com.pinnacle.smsorganizer.ui.inbox.InboxScreen
import com.pinnacle.smsorganizer.ui.inbox.InboxViewModel

@Composable
fun SmsOrganizerNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PinnacleSmsApp

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_SMS] == true
        if (readGranted) {
            Toast.makeText(context, "SMS permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "SMS permission denied. You can explore Demo Messages.", Toast.LENGTH_LONG).show()
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
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_SMS,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.READ_CONTACTS
                        )
                    )
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
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
