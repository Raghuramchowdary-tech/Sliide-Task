package com.sliide.app.features.users.presentation.userlist.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.sliide.app.core.common.TimeProvider
import com.sliide.app.core.ui.components.ErrorScreen
import com.sliide.app.core.ui.components.ShimmerUserList
import com.sliide.app.features.users.domain.model.User
import com.sliide.app.features.users.presentation.adduser.AddUserDialog
import com.sliide.app.features.users.presentation.userlist.UserListEffect
import com.sliide.app.features.users.presentation.userlist.UserListIntent
import com.sliide.app.features.users.presentation.userlist.UserListState
import com.sliide.app.features.users.presentation.userlist.UserListViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    viewModel: UserListViewModel = koinViewModel(),
    timeProvider: TimeProvider = koinInject(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is UserListEffect.ShowUndoSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "${effect.deletedUser.name} deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(UserListIntent.UndoDelete)
                    }
                }
                is UserListEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is UserListEffect.ScrollToUser -> {
                    listState.animateScrollToItem(0)
                }
                is UserListEffect.ScrollToTop -> {
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

    // Scroll to top when loading completes
    LaunchedEffect(Unit) {
        snapshotFlow { state.isLoading }
            .distinctUntilChanged()
            .filter { !it }
            .collect { listState.scrollToItem(0) }
    }

    // Detect scroll near bottom to trigger load more
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem to totalItems
        }.distinctUntilChanged().collect { (lastVisible, total) ->
            if (total > 0 && lastVisible >= total - 3) {
                viewModel.onIntent(UserListIntent.LoadMore)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Users") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                actions = {
                    IconButton(onClick = { viewModel.onIntent(UserListIntent.RefreshUsers) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add User")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val widthSizeClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
        when (widthSizeClass) {
            WindowWidthSizeClass.MEDIUM, WindowWidthSizeClass.EXPANDED ->
                TwoColumnLayout(state = state, listState = listState, timeProvider = timeProvider, onIntent = viewModel::onIntent, modifier = Modifier.padding(padding))
            else ->
                SingleColumnLayout(state = state, listState = listState, timeProvider = timeProvider, onIntent = viewModel::onIntent, modifier = Modifier.padding(padding))
        }
    }

    if (showAddDialog) {
        AddUserDialog(
            onDismiss = { showAddDialog = false },
            onUserAdded = {
                showAddDialog = false
                viewModel.onIntent(UserListIntent.RefreshUsers)
                viewModel.onIntent(UserListIntent.ScrollToTop)
            },
            onError = { /* errors handled inside dialog */ },
        )
    }

    state.deleteConfirmUser?.let { user ->
        DeleteConfirmDialog(
            userName = user.name,
            onConfirm = { viewModel.onIntent(UserListIntent.ConfirmDelete) },
            onDismiss = { viewModel.onIntent(UserListIntent.DismissDeleteConfirm) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleColumnLayout(state: UserListState, listState: LazyListState, timeProvider: TimeProvider, onIntent: (UserListIntent) -> Unit, modifier: Modifier = Modifier) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onIntent(UserListIntent.RefreshUsers) },
        modifier = modifier.fillMaxSize(),
    ) {
        UserListContent(state = state, listState = listState, timeProvider = timeProvider, selectedUser = null, onUserClick = {}, onUserLongClick = { onIntent(UserListIntent.RequestDelete(it)) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwoColumnLayout(state: UserListState, listState: LazyListState, timeProvider: TimeProvider, onIntent: (UserListIntent) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(UserListIntent.RefreshUsers) },
            modifier = Modifier.weight(0.4f),
        ) {
            UserListContent(state = state, listState = listState, timeProvider = timeProvider, selectedUser = state.selectedUser, onUserClick = { onIntent(UserListIntent.SelectUser(it)) }, onUserLongClick = { onIntent(UserListIntent.RequestDelete(it)) }, bottomPadding = 8.dp)
        }
        VerticalDivider()
        Box(modifier = Modifier.weight(0.6f)) {
            UserDetailPane(user = state.selectedUser, timeProvider = timeProvider)
        }
    }
}

@Composable
private fun UserListContent(state: UserListState, listState: LazyListState, timeProvider: TimeProvider, selectedUser: User?, onUserClick: (User) -> Unit, onUserLongClick: (User) -> Unit, bottomPadding: Dp = 88.dp) {
    when {
        state.isLoading && state.users.isEmpty() -> ShimmerUserList()
        state.error != null && state.users.isEmpty() -> {
            ErrorScreen(
                error = state.error,
                onRetry = { /* retry handled via intent */ },
            )
        }
        else -> {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding)) {
                items(items = state.users, key = { it.id }) { user ->
                    UserCard(
                        user = user,
                        timeProvider = timeProvider,
                        onClick = { onUserClick(user) },
                        onLongClick = { onUserLongClick(user) },
                        isSelected = user.id == selectedUser?.id,
                        modifier = Modifier.animateItem(),
                    )
                }
                if (state.isLoadingMore && state.hasMorePages) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
