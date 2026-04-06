package com.contactsapp.ui

sealed class Screen(val route: String) {
    object Contacts : Screen("contacts")
    object Recents : Screen("recents")
    object Favorites : Screen("favorites")
    object Settings      : Screen("settings")
    object Dialpad:Screen("dialpad")
    object ContactDetail : Screen("contact/{contactId}") {
        fun createRoute(contactId: Long) = "contact/$contactId"
    }

    // ── Onboarding flow ────────────────────────────────────────────────────
    object Splash      : Screen("splash")
    object Language    : Screen("language")
    object Permission  : Screen("permission")
    object Onboarding  : Screen("onboarding")
}
