package com.contactsapptwomktech.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.contactsapptwomktech.R
import com.contactsapptwomktech.data.model.Contact
import com.contactsapptwomktech.data.viewmodel.ContactsViewModel
import com.contactsapptwomktech.data.viewmodel.UiState
import com.contactsapptwomktech.ui.components.ContactAvatar
import com.contactsapptwomktech.ui.components.EmptyState
import com.contactsapptwomktech.ui.components.PermissionScreen
import com.contactsapptwomktech.utils.IntentUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    onContactClick: (Long) -> Unit,
    onFavoritesClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(android.Manifest.permission.READ_CONTACTS) { granted ->
        if (granted) viewModel.loadContacts()
    }

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) viewModel.loadContacts()
    }

    if (!permissionState.status.isGranted) {
        PermissionScreen(
            message = stringResource(R.string.contacts_permission_message),
            onGrantClicked = { permissionState.launchPermissionRequest() }
        )
        return
    }

    val state by viewModel.contactsState.collectAsState()
    val filteredContacts by viewModel.filteredContacts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.search_contacts_hint),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    containerColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                            LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        } else {
                            Text(
                                stringResource(R.string.contacts_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        if (isSearchActive) {
                            IconButton(onClick = {
                                isSearchActive = false
                                viewModel.setSearchQuery("")
                                focusManager.clearFocus()
                            }) {
                                Icon(Icons.Outlined.Close, stringResource(R.string.cd_search_clear))
                            }
                        } else {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Outlined.Search, stringResource(R.string.search_button_description))
                            }
                            // Favorites
                            IconButton(onClick = onFavoritesClick) {
                                Icon(
                                    imageVector = Icons.Outlined.StarBorder,
                                    contentDescription = stringResource(R.string.nav_favorites),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector        = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint               = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { IntentUtils.addContact(context) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Outlined.PersonAdd, stringResource(R.string.fab_add_contact_description))
            }
        }
    ) { paddingValues ->
        when (val s = state) {
            is UiState.Loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            is UiState.Error -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.error_loading_contacts),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is UiState.Success -> {
//                val grouped = filteredContacts
//                    .groupBy { it.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#" }
//                    .toSortedMap()
                val favorites = filteredContacts.filter { it.isFavorite }
                val grouped = buildMap {
                    if (favorites.isNotEmpty()) put("★", favorites)  // favorites section first
                    putAll(
                        filteredContacts
                            .filter { !it.isFavorite }
                            .groupBy { it.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#" }
                            .toSortedMap()
                    )
                }

                if (filteredContacts.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.PersonOutline,
                        title = stringResource(R.string.no_contacts_found),
                        subtitle = if (searchQuery.isBlank())
                            stringResource(R.string.no_contacts_message)
                        else stringResource(R.string.no_contacts_found),
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    Box(modifier = Modifier.padding(paddingValues)) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            // Contact count header
                            item {
                                Text(
                                    text = stringResource(R.string.contact_count, filteredContacts.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            }

                            grouped.forEach { (letter, contacts) ->
                                stickyHeader {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                                            .padding(horizontal = 20.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = letter,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                items(contacts, key = { it.id }) { contact ->
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn(tween(300)) + slideInVertically(tween(300))
                                    ) {
                                        ContactListItem(
                                            contact = contact,
                                            onClick = { onContactClick(contact.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactListItem(contact: Contact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(
            name = contact.displayName,
            photoUri = contact.photoUri,
            size = 48.dp,
            fontSize = MaterialTheme.typography.titleMedium.fontSize
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val sub = contact.company
                ?: contact.primaryPhone
                ?: contact.emails.firstOrNull()?.address
            if (sub != null) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
