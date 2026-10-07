package id.asrul.pendaki.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import id.asrul.pendaki.mobile.ui.screens.DaftarScreen
import id.asrul.pendaki.mobile.ui.screens.DetailScreen
import id.asrul.pendaki.mobile.ui.screens.EksporScreen
import id.asrul.pendaki.mobile.ui.theme.PendakiMobileTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PendakiMobileTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "daftar") {
                    composable("daftar") { DaftarScreen(onBuka = { id -> nav.navigate("detail/$id") }) }
                    composable("detail/{id}") { e ->
                        val id = e.arguments?.getString("id") ?: return@composable
                        DetailScreen(id = id, onEkspor = { nav.navigate("ekspor/$id") }, onKembali = { nav.popBackStack() })
                    }
                    composable("ekspor/{id}") { e ->
                        val id = e.arguments?.getString("id") ?: return@composable
                        EksporScreen(id = id, onKembali = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
