package com.example.moco

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.moco.data.FirebaseHelper
import com.example.moco.data.MocoMessagingService
import com.example.moco.ui.components.MocoBottomBar
import com.example.moco.ui.navigation.Screen
import com.example.moco.ui.screens.*
import com.example.moco.ui.theme.MOCOTheme
import com.google.firebase.messaging.messaging
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.Firebase
import com.google.firebase.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * MainActivity: Der Haupteinstiegspunkt der App.
 * Hier wird die Navigationsstruktur und das Grundgerüst (Scaffold) definiert.
 */
class MainActivity : ComponentActivity() {

    // Launcher für die Benachrichtigungs-Berechtigung (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Berechtigung erteilt - Push kann genutzt werden
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Aktiviert modernes Edge-to-Edge Design
        enableEdgeToEdge()

        // Berechtigung ab Android 13 anfragen
        askNotificationPermission()

        setContent {
            MOCOTheme {
                MocoAppMain()
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun MocoAppMain() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firebaseHelper = remember { FirebaseHelper() }
    val sharedPrefs = remember { context.getSharedPreferences("moco_prefs", Context.MODE_PRIVATE) }
    val userId = sharedPrefs.getString("user_id", null)

    // NEBENLÄUFIGKEIT: Globaler Listener für neue Benachrichtigungen
    // Dieser Block beobachtet Firestore auf Benachrichtigungs-Anfragen und zeigt diese als lokale Notification an.
    LaunchedEffect(userId) {
        if (userId != null) {
            firebaseHelper.observeNotifications(userId).collectLatest { notifications ->
                notifications.forEach { data ->
                    val id = data["id"] as? String ?: ""
                    val title = data["title"] as? String ?: "MOCO Update"
                    val message = data["message"] as? String ?: ""

                    // Zeigt die Benachrichtigung auf dem Handy an
                    MocoMessagingService.showLocalNotification(context, title, message)

                    // Markiert sie in DB als gelesen
                    firebaseHelper.markNotificationAsRead(id)
                }
            }
        }
    }

    // NEBENLÄUFIGKEIT: Registriert das FCM-Token für den Nutzer bei App-Start
    LaunchedEffect(userId) {
        if (userId != null) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                scope.launch {
                    firebaseHelper.updatePushToken(userId, token)
                }
            }
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Scaffold verwaltet die Top-Level-UI-Elemente wie die BottomBar
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            MocoBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Map.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        // Der NavHost steuert, welcher Screen gerade angezeigt wird
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Map.route) {
                MapScreen(
                    onSearchClick = { navController.navigate(Screen.Search.route) }
                )
            }

            // Hilfsfunktion zum Zurückkehren auf die Karte
            val backToMap = {
                navController.popBackStack(Screen.Map.route, inclusive = false)
            }

            composable(Screen.Profile.route) { ProfileScreen(onBackClick = { backToMap() }) }
            composable(Screen.Messages.route) { MessagesScreen(onBackClick = { backToMap() }) }
            composable(Screen.AddSpot.route) { AddSpotScreen(onBackClick = { backToMap() }) }
            composable(Screen.Favorites.route) { FavoritesScreen(onBackClick = { backToMap() }) }
            composable(Screen.MySpots.route) { MySpotsScreen(onBackClick = { backToMap() }) }

            composable(Screen.Search.route) {
                SearchScreen(
                    onBackClick = { backToMap() }
                )
            }
        }
    }
}