package com.example.moco.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.moco.ui.navigation.Screen

@Composable
fun MocoBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Screen.Profile.route,
            onClick = { onNavigate(Screen.Profile.route) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
            label = { Text("Profil") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Messages.route,
            onClick = { onNavigate(Screen.Messages.route) },
            icon = { Icon(Icons.Default.Email, contentDescription = "Nachrichten") },
            label = { Text("Nachrichten") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.AddSpot.route,
            onClick = { onNavigate(Screen.AddSpot.route) },
            icon = { Icon(Icons.Default.Add, contentDescription = "Hinzufügen") },
            label = { Text("Hinzufügen") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Favorites.route,
            onClick = { onNavigate(Screen.Favorites.route) },
            icon = { Icon(Icons.Default.Favorite, contentDescription = "Favoriten") },
            label = { Text("Favoriten") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.MySpots.route,
            onClick = { onNavigate(Screen.MySpots.route) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Eigene") },
            label = { Text("Eigene") }
        )
    }
}