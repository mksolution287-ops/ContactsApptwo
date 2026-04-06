package com.contactsapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.contactsapp.data.viewmodel.SettingsViewModel
import com.contactsapp.data.viewmodel.ContactsViewModel
import com.contactsapp.ui.screens.MainScreen
import com.contactsapp.ui.theme.ContactsAppTheme
import com.contactsapp.utils.BaseContextWrapper

class MainActivity : ComponentActivity() {

    private val contactsViewModel: ContactsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            BaseContextWrapper.wrap(newBase)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val themeSettings by settingsViewModel.themeSettings.collectAsState()

            ContactsAppTheme(settings = themeSettings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        rootViewModel    = contactsViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}