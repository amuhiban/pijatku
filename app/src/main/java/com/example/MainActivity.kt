package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.PijatKuViewModel
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.CustomerScreen
import com.example.ui.screens.TherapistScreen
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // State for manual theme overrules (Dark Mode vs Light Mode)
            var isDarkModeManual by remember { mutableStateOf(false) }
            // State for manual language overrules ("ID" vs "EN")
            var currentLanguage by remember { mutableStateOf("ID") }

            PijatKuTheme(darkTheme = isDarkModeManual) {
                val viewModel: PijatKuViewModel = viewModel()
                val currentRole by viewModel.currentRole.collectAsState()
                val activeOrder by viewModel.activeOrder.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Floating bottom bar selector to play Customer, Therapist, and Admin roles
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .shadow(8.dp, RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (currentLanguage == "ID") "PROYEK SIMULASI PIJATKU - GANTI ROLE MITRA" else "PIJATKU SIMULATION - SWITCH ROLES",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MintGreen
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Customer Role play (Ahmad)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.loginAs("cust_ahmad") }
                                            .background(if (currentRole == "CUSTOMER") MintGreen.copy(alpha = 0.15f) else Color.Transparent)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            tint = if (currentRole == "CUSTOMER") MintGreen else Color.Gray,
                                            contentDescription = "Customer"
                                        )
                                        Text(
                                            text = if (currentLanguage == "ID") "Pelanggan" else "Client",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentRole == "CUSTOMER") FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentRole == "CUSTOMER") MintGreen else Color.Gray
                                        )
                                    }

                                    // 2. Therapist Role play (Budi)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.loginAs("therapist_budi") }
                                            .background(if (currentRole == "THERAPIST") MintGreen.copy(alpha = 0.15f) else Color.Transparent)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MedicalServices,
                                            tint = if (currentRole == "THERAPIST") MintGreen else Color.Gray,
                                            contentDescription = "Therapist"
                                        )
                                        Text(
                                            text = if (currentLanguage == "ID") "Terapis (Budi)" else "Therapist (Budi)",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentRole == "THERAPIST") FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentRole == "THERAPIST") MintGreen else Color.Gray
                                        )
                                    }

                                    // 3. Admin Controller (Admin)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.loginAs("admin_utama") }
                                            .background(if (currentRole == "ADMIN") MintGreen.copy(alpha = 0.15f) else Color.Transparent)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            tint = if (currentRole == "ADMIN") MintGreen else Color.Gray,
                                            contentDescription = "Admin"
                                        )
                                        Text(
                                            text = "Admin",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentRole == "ADMIN") FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentRole == "ADMIN") MintGreen else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Display screens bases on active selection
                        when (currentRole) {
                            "CUSTOMER" -> CustomerScreen(viewModel = viewModel)
                            "THERAPIST" -> TherapistScreen(viewModel = viewModel)
                            "ADMIN" -> AdminScreen(viewModel = viewModel)
                        }

                        // Floating top Utility controls (Dark Mode & Language selector!)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .shadow(4.dp, RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(20.dp))
                                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dark Mode Switch
                            IconButton(
                                modifier = Modifier.size(28.dp),
                                onClick = { isDarkModeManual = !isDarkModeManual }
                            ) {
                                Icon(
                                    imageVector = if (isDarkModeManual) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    tint = IfThemeDark(isDarkModeManual),
                                    contentDescription = "Theme",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.height(16.dp).width(1.dp).background(Color.Gray))
                            Spacer(modifier = Modifier.width(6.dp))
                            // Language switcher
                            Text(
                                text = currentLanguage,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = NavyPrimary,
                                modifier = Modifier
                                    .clickable {
                                        currentLanguage = if (currentLanguage == "ID") "EN" else "ID"
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun IfThemeDark(isDark: Boolean): Color {
    return if (isDark) Color(0xFFF59E0B) else Color(0xFF1E293B)
}
