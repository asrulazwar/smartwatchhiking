package id.asrul.pendaki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.wear.ambient.AmbientLifecycleObserver
import dagger.hilt.android.AndroidEntryPoint
import id.asrul.pendaki.ui.nav.PendakiNavHost
import id.asrul.pendaki.ui.theme.PendakiTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** true saat layar dalam mode ambient (always-on). */
    var ambient by mutableStateOf(false)
        private set

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) { ambient = true }
        override fun onUpdateAmbient() = Unit
        override fun onExitAmbient() { ambient = false }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(AmbientLifecycleObserver(this, ambientCallback))
        setContent {
            PendakiTheme {
                PendakiNavHost(ambient = ambient)
            }
        }
    }
}
