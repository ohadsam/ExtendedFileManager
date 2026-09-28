package com.efm.filemanager

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.efm.filemanager.ui.EfmRoot
import dagger.hilt.android.AndroidEntryPoint

/**
 * Extends [AppCompatActivity], not the plain `ComponentActivity`, so that
 * Settings' per-app language switch (via `AppCompatDelegate`) reliably
 * recreates this activity with the new locale below API 33.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EfmRoot()
        }
    }
}
