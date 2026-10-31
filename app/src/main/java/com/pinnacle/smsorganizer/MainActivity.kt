package com.pinnacle.smsorganizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.pinnacle.smsorganizer.ui.navigation.SmsOrganizerNavGraph
import com.pinnacle.smsorganizer.ui.theme.PinnacleSMSOrganizerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        (application as? PinnacleSmsApp)?.repository?.let { repository ->
            lifecycleScope.launch { repository.refresh() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PinnacleSMSOrganizerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    SmsOrganizerNavGraph(navController = navController)
                }
            }
        }
    }
}
