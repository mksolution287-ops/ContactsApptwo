package com.contactsapptwomktech.ui

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
    object OverlayPermission : Screen("overlay_permission")
    object Onboarding  : Screen("onboarding")
    object CallHistory : Screen("contact_call_history/{contactId}") {
        fun createRoute(contactId: Long) = "contact_call_history/$contactId"
    }
}
