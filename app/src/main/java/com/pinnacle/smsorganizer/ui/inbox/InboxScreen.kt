package com.pinnacle.smsorganizer.ui.inbox

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinnacle.smsorganizer.data.model.SmsCategory
import com.pinnacle.smsorganizer.ui.components.CategoryOverrideDialog
import com.pinnacle.smsorganizer.ui.components.DefaultSmsAppBanner
import com.pinnacle.smsorganizer.ui.components.EmptyStateView
import com.pinnacle.smsorganizer.ui.components.PermissionPrompt
import com.pinnacle.smsorganizer.ui.components.requestDefaultSmsRole
import com.pinnacle.smsorganizer.ui.otp.OtpCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onRequestSmsPermission: () -> Unit,
    onNavigateToConversation: (threadId: Long, address: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        if (state.isSearchActive) {
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = viewModel::onSearchQueryChanged,
                                placeholder = { Text("Search sender or message...") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 8.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(24.dp),
                                trailingIcon = {
                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                }
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SMS Organizer",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                if (state.isDemoMode) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Demo",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    },
                    actions = {
                        if (state.isSearchActive) {
                            IconButton(onClick = { viewModel.setSearchActive(false) }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Search")
                            }
                        } else {
                            IconButton(onClick = { viewModel.setSearchActive(true) }) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                            }

                            IconButton(onClick = viewModel::refresh) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                            }

                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (state.isDemoMode) "Disable Demo Mode" else "Enable Demo Mode") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.toggleDemoMode()
                                        }
                                    )
                                    if (!state.isDefaultSmsApp) {
                                        DropdownMenuItem(
                                            text = { Text("Set as Default SMS App") },
                                            onClick = {
                                                showMenu = false
                                                requestDefaultSmsRole(context)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Category Tabs Bar
                val tabs = listOf(
                    CategoryTabItem(null, "All", state.tabUnreadCounts[null] ?: 0),
                    CategoryTabItem(SmsCategory.PERSONAL, "Personal", state.tabUnreadCounts[SmsCategory.PERSONAL] ?: 0),
                    CategoryTabItem(SmsCategory.TRANSACTIONS, "Transactions", state.tabUnreadCounts[SmsCategory.TRANSACTIONS] ?: 0),
                    CategoryTabItem(SmsCategory.OTPS, "OTPs", state.tabUnreadCounts[SmsCategory.OTPS] ?: 0),
                    CategoryTabItem(SmsCategory.PROMOTIONS, "Promotions", state.tabUnreadCounts[SmsCategory.PROMOTIONS] ?: 0),
                    CategoryTabItem(SmsCategory.OTHER, "Other", state.tabUnreadCounts[SmsCategory.OTHER] ?: 0)
                )

                CategoryTabs(
                    selectedCategory = state.selectedCategory,
                    tabs = tabs,
                    onTabSelected = viewModel::selectCategory
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Default SMS App Banner
                DefaultSmsAppBanner(isDefaultSmsApp = state.isDefaultSmsApp)
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                !state.hasSmsPermission && !state.isDemoMode -> {
                    PermissionPrompt(
                        onRequestPermission = onRequestSmsPermission,
                        onEnableDemoMode = viewModel::enableDemoMode
                    )
                }

                state.selectedCategory == SmsCategory.OTPS -> {
                    // Dedicated OTP cards view
                    if (state.recentOtps.isEmpty()) {
                        EmptyStateView(
                            category = SmsCategory.OTPS,
                            isSearchActive = state.isSearchActive
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(state.recentOtps, key = { it.id }) { otpMessage ->
                                OtpCard(
                                    message = otpMessage,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                        }
                    }
                }

                state.conversations.isEmpty() -> {
                    EmptyStateView(
                        category = state.selectedCategory,
                        isSearchActive = state.isSearchActive
                    )
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.conversations, key = { it.threadId }) { conversation ->
                            ConversationItem(
                                conversation = conversation,
                                onClick = {
                                    onNavigateToConversation(conversation.threadId, conversation.address)
                                },
                                onToggleRead = {
                                    viewModel.markConversationRead(conversation.threadId, !conversation.isRead)
                                },
                                onTogglePin = {
                                    viewModel.togglePin(conversation.threadId, conversation.isPinned)
                                },
                                onChangeCategory = {
                                    viewModel.openCategoryOverride(conversation.address, conversation.category)
                                }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 74.dp)
                            )
                        }
                    }
                }
            }

            // Category Override Dialog
            state.senderForOverride?.let { (address, category) ->
                CategoryOverrideDialog(
                    senderAddress = address,
                    currentCategory = category,
                    onCategorySelected = viewModel::applyCategoryOverride,
                    onDismissRequest = viewModel::dismissCategoryOverride
                )
            }
        }
    }
}
