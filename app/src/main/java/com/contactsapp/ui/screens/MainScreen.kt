package com.contactsapp.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.contactsapp.R
import com.contactsapp.data.viewmodel.ContactsViewModel
import com.contactsapp.data.viewmodel.OnboardingViewModel
import com.contactsapp.data.viewmodel.SettingsViewModel
import com.contactsapp.ui.Screen
import com.contactsapp.utils.LocaleHelper

data class BottomNavItem(
    val route         : String,
    val label         : String,
    val selectedIcon  : ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomBarRoutes = setOf(
    Screen.Contacts.route,
    Screen.Recents.route,
    Screen.Dialpad.route
)

// ---------------------------------------------------------------------------
// Bottom bar
// ---------------------------------------------------------------------------

@Composable
fun CustomBottomBar(
    items        : List<BottomNavItem>,
    currentRoute : String?,
    onItemClick  : (String) -> Unit
) {
    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    val barHeight    = screenHeight * 0.08f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment     = Alignment.Top
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier
                    .weight(1f)
                    .clickable { onItemClick(item.route) }
                    .padding(bottom = 10.dp)
            ) {
                Icon(
                    imageVector        = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    tint               = if (selected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Root composable
// ---------------------------------------------------------------------------

@Composable
fun MainScreen(
    rootViewModel     : ContactsViewModel,
    settingsViewModel : SettingsViewModel,
    onboardingViewModel: OnboardingViewModel = viewModel()
) {
    val navController = rememberNavController()
    val currentEntry  by navController.currentBackStackEntryAsState()
    val currentRoute  = currentEntry?.destination?.route

    // Observe onboarding state to decide the start destination
    val onboardingDone   by onboardingViewModel.onboardingDone.collectAsState()
    val selectedLanguage by onboardingViewModel.selectedLanguage.collectAsState()

    val context = LocalContext.current
    // ── Determine start destination ────────────────────────────────────────
    // • Language already chosen + onboarding done  → go straight to Contacts
    // • Language already chosen + onboarding NOT done → skip Splash, go to Onboarding
    // • Nothing chosen yet → show Splash → Language
    val startDestination = when {
        onboardingDone               -> Screen.Recents.route
        selectedLanguage != null     -> Screen.Permission.route   // language set, finish onboarding
        else                         -> Screen.Splash.route
    }

    val items = listOf(
        BottomNavItem(
            route          = Screen.Contacts.route,
            label          = stringResource(R.string.nav_contacts),
            selectedIcon   = Icons.Filled.People,
            unselectedIcon = Icons.Outlined.PeopleOutline
        ),
        BottomNavItem(
            route          = Screen.Recents.route,
            label          = stringResource(R.string.nav_recents),
            selectedIcon   = Icons.Filled.History,
            unselectedIcon = Icons.Outlined.History
        ),
        BottomNavItem(
            route          = Screen.Dialpad.route,
            label          = stringResource(R.string.nav_dialpad),
            selectedIcon   = Icons.Filled.Dialpad,
            unselectedIcon = Icons.Outlined.Dialpad
        )
    )

    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter   = slideInVertically(tween(200)) { it } + fadeIn(tween(200)),
                exit    = slideOutVertically(tween(150)) { it } + fadeOut(tween(150))
            ) {
                CustomBottomBar(
                    items        = items,
                    currentRoute = currentRoute,
                    onItemClick  = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            NavHost(
                navController      = navController,
                startDestination   = startDestination,
                enterTransition    = { fadeIn(tween(180)) },
                exitTransition     = { fadeOut(tween(120)) },
                popEnterTransition = { fadeIn(tween(180)) },
                popExitTransition  = { fadeOut(tween(120)) }
            ) {

                // ── Onboarding flow ────────────────────────────────────────

                composable(Screen.Splash.route) {
                    SplashScreen(
                        onFinished = {
                            navController.navigate(Screen.Language.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Language.route) {
                    LanguageSelectScreen(
                        initialLanguage  = selectedLanguage,
                        onLanguageChosen = { language ->
                            LocaleHelper.saveLanguage(context, language.code)
                            onboardingViewModel.saveLanguage(language)
                            // Language chosen — skip splash on next launch, go to permissions
                            navController.navigate(Screen.Permission.route) {
                                popUpTo(Screen.Language.route) { inclusive = true }
                            }
                            (context as Activity).recreate()
                        }
                    )
                }

                composable(Screen.Permission.route) {
                    PermissionScreen(
                        onPermissionsResult = { _ ->
                            // Whether granted or denied, continue to onboarding
                            onboardingViewModel.markPermissionsAsked()
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo(Screen.Permission.route) { inclusive = true }
                            }
                        },
                        onSkip = {
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo(Screen.Permission.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onFinished = {
                            onboardingViewModel.markOnboardingDone()
                            navController.navigate(Screen.Contacts.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }

                // ── Main app ───────────────────────────────────────────────

                composable(Screen.Contacts.route) {
                    ContactsScreen(
                        viewModel        = rootViewModel,
                        onContactClick   = { id ->
                            navController.navigate(Screen.ContactDetail.createRoute(id))
                        },
                        onFavoritesClick = {
                            navController.navigate(Screen.Favorites.route)
                        },
                        onSettingsClick  = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }

                composable(Screen.Recents.route) {
                    RecentsScreen(
                        viewModel        = rootViewModel,
                        onFavoritesClick = {
                            navController.navigate(Screen.Favorites.route)
                        },
                        onSettingsClick  = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }

                composable(Screen.Dialpad.route) {
                    DialpadScreen(
                        onSettingsClick = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }

                composable(
                    route             = Screen.Favorites.route,
                    enterTransition   = {
                        slideInVertically(tween(280)) { it / 12 } + fadeIn(tween(280))
                    },
                    popExitTransition = {
                        slideOutVertically(tween(220)) { it / 12 } + fadeOut(tween(220))
                    }
                ) {
                    FavoritesScreen(
                        viewModel       = rootViewModel,
                        onContactClick  = { id ->
                            navController.navigate(Screen.ContactDetail.createRoute(id))
                        },
                        onSettingsClick = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }

                composable(
                    route             = Screen.ContactDetail.route,
                    enterTransition   = {
                        slideInVertically(tween(280)) { it / 12 } + fadeIn(tween(280))
                    },
                    popExitTransition = {
                        slideOutVertically(tween(220)) { it / 12 } + fadeOut(tween(220))
                    }
                ) { backStackEntry ->
                    val contactId = backStackEntry.arguments
                        ?.getString("contactId")?.toLongOrNull()
                        ?: return@composable
                    ContactDetailScreen(
                        contactId = contactId,
                        viewModel = rootViewModel,
                        onBack    = { navController.popBackStack() }
                    )
                }

                composable(
                    route             = Screen.Settings.route,
                    enterTransition   = {
                        slideInVertically(tween(280)) { it / 12 } + fadeIn(tween(280))
                    },
                    popExitTransition = {
                        slideOutVertically(tween(220)) { it / 12 } + fadeOut(tween(220))
                    }
                ) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onboardingViewModel = onboardingViewModel,
                        onBack    = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}