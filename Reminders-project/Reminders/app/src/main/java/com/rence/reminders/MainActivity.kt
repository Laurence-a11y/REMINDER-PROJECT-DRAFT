package com.rence.reminders

import android.app.Application
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rence.reminders.alarm.Notifier
import com.rence.reminders.data.Store
import com.rence.reminders.model.ThemeMode
import com.rence.reminders.ui.AppTheme
import com.rence.reminders.ui.CategoriesScreen
import com.rence.reminders.ui.DashboardScreen
import com.rence.reminders.ui.EditScreen
import com.rence.reminders.ui.HelpScreen
import com.rence.reminders.ui.SettingsScreen

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannel(this)
        Store.get(this).rescheduleAll()
    }
}

sealed interface Screen {
    data object Dashboard : Screen
    data class Edit(val id: Long?, val text: String?) : Screen
    data object Categories : Screen
    data object Help : Screen
    data object Settings : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = Store.get(this)

        setContent {
            val data by store.data.collectAsState()
            val dark = when (data.settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            AppTheme(dark) {
                var stack by remember {
                    mutableStateOf(listOf<Screen>(if (data.settings.onboarded) Screen.Dashboard else Screen.Help))
                }
                val push: (Screen) -> Unit = { stack = stack + it }
                val pop: () -> Unit = { if (stack.size > 1) stack = stack.dropLast(1) }
                BackHandler(enabled = stack.size > 1) { pop() }

                when (val s = stack.last()) {
                    Screen.Dashboard -> DashboardScreen(data, store, push)
                    is Screen.Edit -> EditScreen(data, store, s.id, s.text, pop)
                    Screen.Categories -> CategoriesScreen(data, store, pop)
                    Screen.Settings -> SettingsScreen(data, store, pop) { push(Screen.Help) }
                    Screen.Help -> {
                        val onboarding = !data.settings.onboarded
                        HelpScreen(
                            onboarding = onboarding,
                            onBack = pop,
                            onFinish = {
                                store.updateSettings { it.copy(onboarded = true) }
                                stack = listOf(Screen.Dashboard)
                            },
                        )
                    }
                }
            }
        }
    }
}
