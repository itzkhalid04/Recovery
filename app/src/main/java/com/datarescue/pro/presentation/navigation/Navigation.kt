package com.datarescue.pro.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
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
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                sharedViewModel = sharedViewModel,
                onNavigateToResults = {
                    navController.navigate("results")
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