package com.datarescue.pro.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.datarescue.pro.domain.model.ScanMode
import com.datarescue.pro.presentation.ui.screens.DeviceInfoScreen
import com.datarescue.pro.presentation.ui.screens.MainScreen
import com.datarescue.pro.presentation.ui.screens.ResultsScreen
import com.datarescue.pro.presentation.viewmodel.SharedDataViewModel

@Composable
fun DataRescueNavigation(
    navController: NavHostController = rememberNavController()
) {
    val sharedViewModel: SharedDataViewModel = hiltViewModel()
    
    NavHost(
        navController = navController,
        startDestination = "device_info"
    ) {
        composable("device_info") {
            DeviceInfoScreen(
                onScanModeSelected = { scanMode ->
                    sharedViewModel.setScanMode(scanMode)
                    navController.navigate("main")
                }
            )
        }
        
        composable("main") {
            MainScreen(
                sharedViewModel = sharedViewModel,
                onNavigateToResults = {
                    navController.navigate("results")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("results") {
            ResultsScreen(
                sharedViewModel = sharedViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}