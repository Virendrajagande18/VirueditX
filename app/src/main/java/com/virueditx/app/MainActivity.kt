package com.virueditx.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.virueditx.app.ui.ThemeMode
import com.virueditx.app.ui.VirueditXApp
import com.virueditx.app.ui.VirueditXTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // PDFBox needs its resources (fonts, ICC profiles) initialised once per process.
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        setContent {
            var themeMode by remember {
                mutableStateOf(
                    when (prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)) {
                        ThemeMode.LIGHT.name -> ThemeMode.LIGHT
                        ThemeMode.DARK.name -> ThemeMode.DARK
                        else -> ThemeMode.SYSTEM
                    }
                )
            }
            VirueditXTheme(mode = themeMode) {
                VirueditXApp(
                    themeMode = themeMode,
                    onThemeModeChange = { newMode ->
                        themeMode = newMode
                        prefs.edit().putString(KEY_THEME, newMode.name).apply()
                    }
                )
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "virueditx_settings"
        private const val KEY_THEME = "theme_mode"
    }
}
