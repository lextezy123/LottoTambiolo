package com.lextezy.tambiolo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lextezy.tambiolo.data.Language
import com.lextezy.tambiolo.data.ThemeMode
import com.lextezy.tambiolo.ui.TambioloAppUi
import com.lextezy.tambiolo.ui.i18n.LocalStrings
import com.lextezy.tambiolo.ui.i18n.stringsFor
import com.lextezy.tambiolo.ui.theme.TambioloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TambioloApplication
        setContent {
            val mode by app.prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val lang by app.prefs.language.collectAsState(initial = Language.fromPhone())
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            CompositionLocalProvider(LocalStrings provides stringsFor(lang)) {
                TambioloTheme(dark = dark) {
                    TambioloAppUi(app)
                }
            }
        }
    }
}
