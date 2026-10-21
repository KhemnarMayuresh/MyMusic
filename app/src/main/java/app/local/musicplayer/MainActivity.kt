package app.local.musicplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import app.local.musicplayer.ui.MusicPlayerApp
import app.local.musicplayer.ui.AppIntro

class MainActivity: ComponentActivity() {
    private val model: PlayerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),navigationBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent { AppIntro {MusicPlayerApp(model)} }
        handle(intent)
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handle(intent) }
    private fun handle(intent: Intent?) {
        if(intent?.getBooleanExtra("open_queue",false)==true) {model.navigation.value="queue";intent.removeExtra("open_queue")}
        if(intent?.action==Intent.ACTION_VIEW) intent.data?.let { uri ->
            try { contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch(_: SecurityException) { }
            model.openExternal(uri)
        }
    }
    override fun onResume() { super.onResume(); model.rescan() }
}
