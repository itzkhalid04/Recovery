package com.datarescue.pro.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.datarescue.pro.domain.model.RecoverableFile
import com.datarescue.pro.presentation.ui.screens.MainScreen
import com.datarescue.pro.presentation.ui.screens.ResultsScreen

@Composable
fun DataRescueNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                onNavigateToResults = { files ->
                    // In a real app, you'd pass this data properly
                    navController.navigate("results")
                }
            )
        }
        
        composable("results") {
            // In a real app, you'd get the files from navigation arguments or shared state
            ResultsScreen(
                recoveredFiles = emptyList(), // This would come from navigation args
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}