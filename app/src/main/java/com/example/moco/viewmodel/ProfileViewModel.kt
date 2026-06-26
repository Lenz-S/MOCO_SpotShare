package com.example.moco.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

/**
 * ProfileViewModel: Verwaltet den Zustand und die Logik für den Profil-Screen.
 * Durch die Nutzung eines ViewModels bleibt der Zustand (z.B. Texteingaben) 
 * auch beim Drehen des Bildschirms erhalten.
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("moco_prefs", Context.MODE_PRIVATE)

    // UI-Zustand (State): Diese Variablen werden von der View (ProfileScreen) beobachtet.
    var realName by mutableStateOf(sharedPrefs.getString("real_name", "Benutzer") ?: "Benutzer")
        private set

    var userId by mutableStateOf(sharedPrefs.getString("user_id", "user_default") ?: "user_default")
        private set

    var licensePlate by mutableStateOf(sharedPrefs.getString("license_plate", "K-XY 123") ?: "K-XY 123")
        private set

    /**
     * Aktualisiert den Namen im internen Zustand.
     */
    fun onNameChange(newName: String) {
        realName = newName
    }

    /**
     * Aktualisiert die Benutzer-ID im internen Zustand.
     */
    fun onUserIdChange(newId: String) {
        userId = newId
    }

    /**
     * Aktualisiert das Kennzeichen im internen Zustand.
     */
    fun onLicensePlateChange(newPlate: String) {
        licensePlate = newPlate
    }

    /**
     * Speichert die aktuellen Werte dauerhaft in den SharedPreferences (Model-Ebene).
     */
    fun saveProfile() {
        sharedPrefs.edit().apply {
            putString("real_name", realName)
            putString("user_id", userId)
            putString("license_plate", licensePlate)
            apply()
        }
    }
}