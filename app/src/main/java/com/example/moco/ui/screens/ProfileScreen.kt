package com.example.moco.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moco.viewmodel.ProfileViewModel

/**
 * ProfileScreen: Die View im MVVM-Muster.
 * Sie beobachtet den Zustand aus dem ProfileViewModel und leitet Nutzeraktionen weiter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel() // ViewModel wird automatisch bereitgestellt
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück zur Karte"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Profil bearbeiten",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Die View liest nur noch Daten aus dem ViewModel (MVVM-Konzept)
            OutlinedTextField(
                value = profileViewModel.realName,
                onValueChange = { profileViewModel.onNameChange(it) },
                label = { Text("Vollständiger Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = profileViewModel.userId,
                onValueChange = { profileViewModel.onUserIdChange(it) },
                label = { Text("Benutzer-ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = profileViewModel.licensePlate,
                onValueChange = { profileViewModel.onLicensePlateChange(it) },
                label = { Text("KFZ-Kennzeichen") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    // Die View delegiert die Logik an das ViewModel
                    profileViewModel.saveProfile()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Profil speichern")
            }
        }
    }
}
