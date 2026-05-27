package com.example.moco.ui.navigation

/**
 * Definition aller Bildschirme (Screens) in der App.
 * Ermöglicht einheitliche Navigation im Team.
 */
sealed class Screen(val route: String) {
    object Map : Screen("map_screen")
    object Profile : Screen("profile_screen")
    object Messages : Screen("messages_screen")
    object AddSpot : Screen("add_spot_screen")
    object Favorites : Screen("favorites_screen")
    object MySpots : Screen("my_spots_screen")
    object Search : Screen("search_screen")
}
