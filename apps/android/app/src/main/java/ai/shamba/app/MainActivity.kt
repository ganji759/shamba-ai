package ai.shamba.app

import ai.shamba.app.data.AppSettings
import ai.shamba.app.knowledge.Language
import ai.shamba.app.ui.AppViewModel
import ai.shamba.app.ui.ShambaRoot
import ai.shamba.app.ui.theme.ShambaTheme
import ai.shamba.app.util.withLanguage
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    /** Applies the app language. It can differ from the phone language. */
    override fun attachBaseContext(newBase: Context) {
        val language = AppSettings.peekLanguage(newBase)
        super.attachBaseContext(if (language != null) newBase.withLanguage(language) else newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShambaTheme {
                ShambaRoot(vm = vm, onLanguageChange = ::changeLanguage)
            }
        }
    }

    private fun changeLanguage(language: Language) {
        val before = AppSettings.peekLanguage(this)
        vm.setLanguage(language)
        // Re-create the activity so every string reloads in the new language. The ViewModel stays.
        if (before != language) recreate()
    }
}
