package com.example.moco

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.moco.ui.components.MocoBottomBar
import com.example.moco.ui.navigation.Screen
import com.example.moco.ui.screens.*
import com.example.moco.ui.theme.MOCOTheme
import com.example.moco.viewmodel.ParkingViewModel

/**
 * MainActivity: Der Haupteinstiegspunkt der App.
 * Hier wird die Navigationsstruktur und das Grundgerüst (Scaffold) definiert.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Aktiviert modernes Edge-to-Edge Design
        enableEdgeToEdge()
        
        val mapboxToken = getString(R.string.mapbox_access_token)
        com.mapbox.common.MapboxOptions.accessToken = mapboxToken

        setContent {
            MOCOTheme {
                MocoAppMain()
            }
        }
    }
}

@Composable
fun MocoAppMain() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val parkingViewModel: ParkingViewModel = viewModel()
    val mapboxToken = stringResource(R.string.mapbox_access_token)

    LaunchedEffect(mapboxToken) {
        parkingViewModel.initSearchEngine(mapboxToken)
    }

    // Scaffold verwaltet die Top-Level-UI-Elemente wie die BottomBar
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            MocoBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Verhindert mehrfaches Aufstapeln desselben Ziels
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
                MapScreen(viewModel = parkingViewModel) 
            }
            
            // Für alle anderen Screens wird eine Navigationsfunktion zum Zurückspringen auf die Karte mitgegeben
            val backToMap = {
                navController.navigate(Screen.Map.route) {
                    popUpTo(Screen.Map.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
            
            composable(Screen.Profile.route) { ProfileScreen(onBackClick = backToMap) }
            composable(Screen.Messages.route) { MessagesScreen(onBackClick = backToMap) }
            composable(Screen.AddSpot.route) { 
                AddSpotScreen(
                    onNavigateBack = backToMap,
                    onSaveSpot = { title, desc, addr, price ->
                        parkingViewModel.addSpotWithAddress(title, desc, addr, price)
                        backToMap()
                    }
                )
            }
            composable(Screen.Favorites.route) { FavoritesScreen(onBackClick = backToMap) }
            composable(Screen.MySpots.route) { 
                MySpotsScreen(
                    viewModel = parkingViewModel,
                    onBackClick = backToMap
                ) 
            }
            composable(Screen.Search.route) { SearchScreen(onBackClick = backToMap) }
        }
    }
}
