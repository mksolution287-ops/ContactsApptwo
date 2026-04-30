package com.callerinfocom.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.callerinfocom.R
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.callerinfocom.data.viewmodel.ContactsViewModel
import com.callerinfocom.data.viewmodel.OnboardingViewModel
import com.callerinfocom.data.viewmodel.SettingsViewModel
import com.callerinfocom.ui.Screen
import com.callerinfocom.ui.components.BannerAd
import com.callerinfocom.utils.AdLoadingOverlay
import com.callerinfocom.utils.AdManager
import com.callerinfocom.utils.AnalyticsHelper.logScreenView
import com.callerinfocom.utils.LocaleHelper
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

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

    Box(
        modifier = Modifier.wrapContentHeight()
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment     = Alignment.CenterVertically
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
    val activity = LocalContext.current as? Activity
    val isAdLoading by AdManager.isAdLoading.collectAsState()

    // Log screen view whenever current route changes
    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            logScreenView(route)
        }
    }

    val startDestination = when {
        selectedLanguage != null -> Screen.Recents.route
        else                     -> Screen.Splash.route
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
            if (showBottomBar) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                ) {
                    // Bottom Navigation
                    AnimatedVisibility(
                        visible = showBottomBar,
                        enter = slideInVertically(tween(200)) { it } + fadeIn(tween(200)),
                        exit = slideOutVertically(tween(150)) { it } + fadeOut(tween(150))
                    ) {
                        CustomBottomBar(
                            items = items,
                            currentRoute = currentRoute,
                            onItemClick = { route ->
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                    BannerAd()
                }
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
                            navController.navigate(Screen.Permission.route) {
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
//                            navController.navigate(Screen.Permission.route) {
//                                popUpTo(Screen.Language.route) { inclusive = true }
//                            }
                            navController.navigate(Screen.Recents.route) {
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
                            navController.navigate(Screen.Language.route) {
                                popUpTo(Screen.Permission.route) { inclusive = true }
                            }
                        },
                        onSkip = {
                            navController.navigate(Screen.Language.route) {
                                popUpTo(Screen.Permission.route) { inclusive = true }
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
//                            navController.navigate(Screen.Settings.route)
                            AdManager.immediateInterstitialAd(activity) {
                                navController.navigate(Screen.Settings.route)
                            }
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
                            AdManager.immediateInterstitialAd(activity) {
                                navController.navigate(Screen.Settings.route)
                            }
                        },
                        onOpenContactHistory = { phoneNumber ->
                            navController.navigate(Screen.CallHistory.createRouteByNumber(phoneNumber))
                        }
                    )
                }

                composable(Screen.Dialpad.route) {
                    DialpadScreen(
                        onSettingsClick = {
                            AdManager.immediateInterstitialAd(activity) {
                                navController.navigate(Screen.Settings.route)
                            }
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
                            AdManager.immediateInterstitialAd(activity) {
                                navController.navigate(Screen.Settings.route)
                            }
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
                        onBack    = { navController.popBackStack() },
                        onCallHistory = { id ->
                            navController.navigate(Screen.CallHistory.createRoute(id))
                        }
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

//                composable(
//                    route = Screen.CallHistory.route,
//                    arguments = listOf(
//                        navArgument("contactId") {
//                            type = NavType.LongType
//                        }
//                    )
//                ) { backStackEntry ->
//                    val contactId = backStackEntry.arguments?.getLong("contactId") ?: return@composable
//
//                    ContactCallHistoryScreen(
//                        contactId = contactId,
//                        onBack = { navController.popBackStack() }
//                    )
//                }
                // Replace the old CallHistory composable with this:

                composable(
                    route = Screen.CallHistory.route,
                    arguments = listOf(
                        navArgument("identifier") {
                            type = NavType.StringType
                        },
                        navArgument("isNumber") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { backStackEntry ->
                    val identifier = backStackEntry.arguments?.getString("identifier") ?: ""
                    val isNumber = backStackEntry.arguments?.getBoolean("isNumber") ?: false

                    val contactId: Long = if (isNumber) {
                        // Find contact by phone number
                        rootViewModel.getContactIdByNumber(identifier) ?: 0L
                    } else {
                        identifier.toLongOrNull() ?: 0L
                    }

                    ContactCallHistoryScreen(
                        contactId = contactId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            AdLoadingOverlay(isVisible = isAdLoading)
        }
    }
}

// Helper function to log screen views
private fun logScreenView(route: String) {
    val screenName = when (route) {
        Screen.Contacts.route -> "ContactsScreen"
        Screen.Recents.route -> "RecentsScreen"
        Screen.Dialpad.route -> "DialpadScreen"
        Screen.Permission.route -> "PermissionScreen"
        Screen.OverlayPermission.route -> "OverlayPermissionScreen"
        Screen.Onboarding.route -> "OnboardingScreen"
        Screen.Language.route -> "LanguageSelectScreen"
        Screen.Splash.route -> "SplashScreen"
        Screen.Favorites.route -> "FavoritesScreen"
        Screen.Settings.route -> "SettingsScreen"
        else -> route
    }

    Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
        param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        param(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
    }
}






//package com.contactsapptwomktech.ui.screens
//
//import android.app.Activity
//import android.graphics.Color
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.core.tween
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.animation.slideInVertically
//import androidx.compose.animation.slideOutVertically
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.WindowInsets
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.navigationBarsPadding
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Dialpad
//import androidx.compose.material.icons.filled.History
//import androidx.compose.material.icons.filled.People
//import androidx.compose.material.icons.outlined.Dialpad
//import androidx.compose.material.icons.outlined.History
//import androidx.compose.material.icons.outlined.PeopleOutline
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.NavigationBar
//import androidx.compose.material3.NavigationBarDefaults
//import androidx.compose.material3.NavigationBarItem
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavGraph.Companion.findStartDestination
//import androidx.navigation.NavType
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.currentBackStackEntryAsState
//import androidx.navigation.compose.rememberNavController
//import androidx.navigation.navArgument
//import com.contactsapptwomktech.R
//import com.contactsapptwomktech.data.viewmodel.ContactsViewModel
//import com.contactsapptwomktech.data.viewmodel.OnboardingViewModel
//import com.contactsapptwomktech.data.viewmodel.SettingsViewModel
//import com.contactsapptwomktech.ui.Screen
//import com.contactsapptwomktech.ui.components.BannerAd
//import com.contactsapptwomktech.utils.AdLoadingOverlay
//import com.contactsapptwomktech.utils.AdManager
//import com.contactsapptwomktech.utils.AnalyticsHelper.logScreenView
//import com.contactsapptwomktech.utils.LocaleHelper
//import com.google.firebase.Firebase
//import com.google.firebase.analytics.FirebaseAnalytics
//import com.google.firebase.analytics.analytics
//import com.google.firebase.analytics.logEvent
//
//data class BottomNavItem(
//    val route         : String,
//    val label         : String,
//    val selectedIcon  : ImageVector,
//    val unselectedIcon: ImageVector
//)
//
//private val bottomBarRoutes = setOf(
//    Screen.Contacts.route,
//    Screen.Recents.route,
//    Screen.Dialpad.route
//)
//
//// ---------------------------------------------------------------------------
//// Root composable
//// ---------------------------------------------------------------------------
//
//@Composable
//fun MainScreen(
//    rootViewModel      : ContactsViewModel,
//    settingsViewModel  : SettingsViewModel,
//    onboardingViewModel: OnboardingViewModel = viewModel()
//) {
//    val navController = rememberNavController()
//    val currentEntry  by navController.currentBackStackEntryAsState()
//    val currentRoute  = currentEntry?.destination?.route
//
//    val onboardingDone   by onboardingViewModel.onboardingDone.collectAsState()
//    val selectedLanguage by onboardingViewModel.selectedLanguage.collectAsState()
//    val context  = LocalContext.current
//    val activity = LocalContext.current as? Activity
//    val isAdLoading by AdManager.isAdLoading.collectAsState()
//
//    LaunchedEffect(currentRoute) {
//        currentRoute?.let { route -> logScreenView(route) }
//    }
//
//    val startDestination = when {
//        selectedLanguage != null -> Screen.Recents.route
//        else                     -> Screen.Splash.route
//    }
//
//    val items = listOf(
//        BottomNavItem(
//            route          = Screen.Contacts.route,
//            label          = stringResource(R.string.nav_contacts),
//            selectedIcon   = Icons.Filled.People,
//            unselectedIcon = Icons.Outlined.PeopleOutline
//        ),
//        BottomNavItem(
//            route          = Screen.Recents.route,
//            label          = stringResource(R.string.nav_recents),
//            selectedIcon   = Icons.Filled.History,
//            unselectedIcon = Icons.Outlined.History
//        ),
//        BottomNavItem(
//            route          = Screen.Dialpad.route,
//            label          = stringResource(R.string.nav_dialpad),
//            selectedIcon   = Icons.Filled.Dialpad,
//            unselectedIcon = Icons.Outlined.Dialpad
//        )
//    )
//
//    val showBottomBar = currentRoute in bottomBarRoutes
//
//    Scaffold(
//        contentWindowInsets = WindowInsets(0,0,0,0),
//        bottomBar = {
//            if (showBottomBar) {
//                Column(modifier = Modifier
//                    .navigationBarsPadding()
//                ){
//                    AnimatedVisibility(
//                        visible = showBottomBar,
//                        enter   = slideInVertically(tween(200)) { it } + fadeIn(tween(200)),
//                        exit    = slideOutVertically(tween(150)) { it } + fadeOut(tween(150))
//                    ) {
//                        // ── M3 NavigationBar ──────────────────────────────
//                        NavigationBar(
//                            containerColor = MaterialTheme.colorScheme.surface,
//                            windowInsets = NavigationBarDefaults.windowInsets,
//                            tonalElevation = 0.dp,
//                        ) {
//                            items.forEach { item ->
//                                val selected = currentRoute == item.route
//                                NavigationBarItem(
//                                    selected = selected,
//                                    onClick  = {
//                                        if (currentRoute != item.route) {
//                                            navController.navigate(item.route) {
//                                                popUpTo(navController.graph.findStartDestination().id) {
//                                                    saveState = true
//                                                }
//                                                launchSingleTop = true
//                                                restoreState    = true
//                                            }
//                                        }
//                                    },
//                                    icon = {
//                                        Icon(
//                                            imageVector        = if (selected) item.selectedIcon
//                                            else item.unselectedIcon,
//                                            contentDescription = item.label
//                                        )
//                                    },
//                                    label = { Text(item.label) }
//                                )
//                            }
//                        }
//                    }
//                    BannerAd()
//                }
//            }
//        }
//    ) { innerPadding ->
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//        ) {
//            NavHost(
//                navController      = navController,
//                startDestination   = startDestination,
//                enterTransition    = { fadeIn(tween(180)) },
//                exitTransition     = { fadeOut(tween(120)) },
//                popEnterTransition = { fadeIn(tween(180)) },
//                popExitTransition  = { fadeOut(tween(120)) }
//            ) {
//                composable(Screen.Splash.route) {
//                    SplashScreen(
//                        onFinished = {
//                            navController.navigate(Screen.Permission.route) {
//                                popUpTo(Screen.Splash.route) { inclusive = true }
//                            }
//                        }
//                    )
//                }
//
//                composable(Screen.Language.route) {
//                    LanguageSelectScreen(
//                        initialLanguage  = selectedLanguage,
//                        onLanguageChosen = { language ->
//                            LocaleHelper.saveLanguage(context, language.code)
//                            onboardingViewModel.saveLanguage(language)
//                            navController.navigate(Screen.Recents.route) {
//                                popUpTo(Screen.Language.route) { inclusive = true }
//                            }
//                            (context as Activity).recreate()
//                        }
//                    )
//                }
//
//                composable(Screen.Permission.route) {
//                    PermissionScreen(
//                        onPermissionsResult = { _ ->
//                            onboardingViewModel.markPermissionsAsked()
//                            navController.navigate(Screen.Language.route) {
//                                popUpTo(Screen.Permission.route) { inclusive = true }
//                            }
//                        },
//                        onSkip = {
//                            navController.navigate(Screen.Language.route) {
//                                popUpTo(Screen.Permission.route) { inclusive = true }
//                            }
//                        }
//                    )
//                }
//
//                composable(Screen.Contacts.route) {
//                    ContactsScreen(
//                        viewModel        = rootViewModel,
//                        onContactClick   = { id -> navController.navigate(Screen.ContactDetail.createRoute(id)) },
//                        onFavoritesClick = { navController.navigate(Screen.Favorites.route) },
//                        onSettingsClick  = {
//                            AdManager.immediateInterstitialAd(activity) {
//                                navController.navigate(Screen.Settings.route)
//                            }
//                        }
//                    )
//                }
//
//                composable(Screen.Recents.route) {
//                    RecentsScreen(
//                        viewModel            = rootViewModel,
//                        onFavoritesClick     = { navController.navigate(Screen.Favorites.route) },
//                        onSettingsClick      = {
//                            AdManager.immediateInterstitialAd(activity) {
//                                navController.navigate(Screen.Settings.route)
//                            }
//                        },
//                        onOpenContactHistory = { phoneNumber ->
//                            navController.navigate(Screen.CallHistory.createRouteByNumber(phoneNumber))
//                        }
//                    )
//                }
//
//                composable(Screen.Dialpad.route) {
//                    DialpadScreen(
//                        onSettingsClick = {
//                            AdManager.immediateInterstitialAd(activity) {
//                                navController.navigate(Screen.Settings.route)
//                            }
//                        }
//                    )
//                }
//
//                composable(
//                    route             = Screen.Favorites.route,
//                    enterTransition   = { slideInVertically(tween(280)) { it / 12 } + fadeIn(tween(280)) },
//                    popExitTransition = { slideOutVertically(tween(220)) { it / 12 } + fadeOut(tween(220)) }
//                ) {
//                    FavoritesScreen(
//                        viewModel       = rootViewModel,
//                        onContactClick  = { id -> navController.navigate(Screen.ContactDetail.createRoute(id)) },
//                        onSettingsClick = {
//                            AdManager.immediateInterstitialAd(activity) {
//                                navController.navigate(Screen.Settings.route)
//                            }
//                        }
//                    )
//                }
//
//                composable(
//                    route             = Screen.ContactDetail.route,
//                    enterTransition   = { slideInVertically(tween(280)) { it / 12 } + fadeIn(tween(280)) },
//                    popExitTransition = { slideOutVertically(tween(220)) { it / 12 } + fadeOut(tween(220)) }
//                ) { backStackEntry ->
//                    val contactId = backStackEntry.arguments
//                        ?.getString("contactId")?.toLongOrNull() ?: return@composable
//                    ContactDetailScreen(
//                        contactId    = contactId,
//                        viewModel    = rootViewModel,
//                        onBack       = { navController.popBackStack() },
//                        onCallHistory = { id -> navController.navigate(Screen.CallHistory.createRoute(id)) }
//                    )
//                }
//
//                composable(
//                    route             = Screen.Settings.route,
//                    enterTransition   = { slideInVertically(tween(280)) { it / 12 } + fadeIn(tween(280)) },
//                    popExitTransition = { slideOutVertically(tween(220)) { it / 12 } + fadeOut(tween(220)) }
//                ) {
//                    SettingsScreen(
//                        viewModel           = settingsViewModel,
//                        onboardingViewModel = onboardingViewModel,
//                        onBack              = { navController.popBackStack() }
//                    )
//                }
//
//                composable(
//                    route     = Screen.CallHistory.route,
//                    arguments = listOf(
//                        navArgument("identifier") { type = NavType.StringType },
//                        navArgument("isNumber")   { type = NavType.BoolType; defaultValue = false }
//                    )
//                ) { backStackEntry ->
//                    val identifier = backStackEntry.arguments?.getString("identifier") ?: ""
//                    val isNumber   = backStackEntry.arguments?.getBoolean("isNumber") ?: false
//                    val contactId: Long = if (isNumber)
//                        rootViewModel.getContactIdByNumber(identifier) ?: 0L
//                    else
//                        identifier.toLongOrNull() ?: 0L
//
//                    ContactCallHistoryScreen(
//                        contactId = contactId,
//                        onBack    = { navController.popBackStack() }
//                    )
//                }
//            }
//
//            AdLoadingOverlay(isVisible = isAdLoading)
//        }
//    }
//}
//
//// Helper function to log screen views
//private fun logScreenView(route: String) {
//    val screenName = when (route) {
//        Screen.Contacts.route        -> "ContactsScreen"
//        Screen.Recents.route         -> "RecentsScreen"
//        Screen.Dialpad.route         -> "DialpadScreen"
//        Screen.Permission.route      -> "PermissionScreen"
//        Screen.OverlayPermission.route -> "OverlayPermissionScreen"
//        Screen.Onboarding.route      -> "OnboardingScreen"
//        Screen.Language.route        -> "LanguageSelectScreen"
//        Screen.Splash.route          -> "SplashScreen"
//        Screen.Favorites.route       -> "FavoritesScreen"
//        Screen.Settings.route        -> "SettingsScreen"
//        else                         -> route
//    }
//    Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
//        param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
//        param(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
//    }
//}