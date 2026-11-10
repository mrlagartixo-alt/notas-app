package com.notas.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notas.app.backup.BackupScheduler
import com.notas.app.ui.AnotaTheme
import com.notas.app.ui.EditorScreen
import com.notas.app.ui.ListScreen
import com.notas.app.ui.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    /** Texto recebido do menu "Compartilhar" de outro app */
    private val sharedText = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Garante que o backup diário está agendado (não mexe se já estiver)
        lifecycleScope.launch(Dispatchers.IO) { BackupScheduler.schedule(this@MainActivity, replace = false) }

        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            AnotaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNav(vm, sharedText)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(incoming: Intent?) {
        val i = incoming ?: return
        if (i.action == Intent.ACTION_SEND && i.type == "text/plain") {
            val text = i.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) sharedText.value = text
        }
    }
}

@Composable
private fun AppNav(vm: MainViewModel, sharedText: androidx.compose.runtime.MutableState<String?>) {
    val nav = rememberNavController()

    val shared = sharedText.value
    LaunchedEffect(shared) {
        if (shared != null) {
            val id = vm.createFromText(shared)
            sharedText.value = null
            nav.navigate("edit/$id")
        }
    }

    NavHost(navController = nav, startDestination = "list") {
        composable("list") {
            ListScreen(
                vm = vm,
                onOpenNote = { id -> nav.navigate("edit/$id") },
                onSettings = { nav.navigate("settings") }
            )
        }
        composable(
            route = "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            EditorScreen(
                noteId = entry.arguments?.getLong("id") ?: 0L,
                vm = vm,
                onBack = { nav.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
        }
    }
}
