package io.github.ar3ac.c4rds.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import io.github.ar3ac.c4rds.MyCardsApplication
import io.github.ar3ac.c4rds.ui.add.AddEditCardScreen
import io.github.ar3ac.c4rds.ui.add.AddEditViewModel
import io.github.ar3ac.c4rds.ui.add.AddEditViewModelFactory
import io.github.ar3ac.c4rds.ui.detail.CardDetailScreen
import io.github.ar3ac.c4rds.ui.detail.CardDetailViewModel
import io.github.ar3ac.c4rds.ui.detail.CardDetailViewModelFactory
import io.github.ar3ac.c4rds.ui.home.HomeScreen
import io.github.ar3ac.c4rds.ui.home.HomeViewModel
import io.github.ar3ac.c4rds.ui.home.HomeViewModelFactory
import io.github.ar3ac.c4rds.ui.scan.BarcodeScannerScreen
import io.github.ar3ac.c4rds.ui.settings.SettingsScreen
import io.github.ar3ac.c4rds.ui.settings.SettingsViewModel
import io.github.ar3ac.c4rds.ui.settings.SettingsViewModelFactory

@Composable
fun NavGraph(
    navController: NavHostController,
    application: MyCardsApplication,
    startDestination: String = Screen.Home.route
) {
    val repository = application.repository

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Home Screen
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(repository)
            )
            HomeScreen(
                viewModel = viewModel,
                onCardClick = { cardId ->
                    navController.navigate(Screen.Detail.createRoute(cardId))
                },
                onScanClick = {
                    navController.navigate(Screen.Scan.route)
                },
                onAddManualClick = {
                    navController.navigate(Screen.Add.createRoute())
                },
                onEditClick = { cardId ->
                    navController.navigate(Screen.Add.createRoute(cardId = cardId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // Camera Scanner Screen
        composable(Screen.Scan.route) {
            BarcodeScannerScreen(
                onBarcodeDetected = { value, format ->
                    val previousRoute = navController.previousBackStackEntry?.destination?.route
                    if (previousRoute?.startsWith("add") == true) {
                        navController.previousBackStackEntry?.savedStateHandle?.set("scanned_barcode", value)
                        navController.previousBackStackEntry?.savedStateHandle?.set("scanned_format", format)
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Add.createRoute(initialValue = value, initialFormat = format)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                },
                onManualInputClick = {
                    val previousRoute = navController.previousBackStackEntry?.destination?.route
                    if (previousRoute?.startsWith("add") == true) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.Add.createRoute()) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Add / Edit Card Screen
        composable(
            route = Screen.Add.route,
            arguments = listOf(
                navArgument("cardId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("initialValue") { type = NavType.StringType; defaultValue = "" },
                navArgument("initialFormat") { type = NavType.StringType; defaultValue = "" },
                navArgument("initialName") { type = NavType.StringType; defaultValue = "" },
                navArgument("initialOwner") { type = NavType.StringType; defaultValue = "" }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "mycards://add" },
                navDeepLink { uriPattern = "mycards://create?name={initialName}&barcode={initialValue}&format={initialFormat}&owner={initialOwner}" },
                navDeepLink { uriPattern = "c4rds://create?name={initialName}&barcode={initialValue}&format={initialFormat}&owner={initialOwner}" },
                navDeepLink { uriPattern = "https://c4rds.app/create?name={initialName}&barcode={initialValue}&format={initialFormat}&owner={initialOwner}" }
            )
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId") ?: -1L
            val rawValue = backStackEntry.arguments?.getString("initialValue") ?: ""
            val rawFormat = backStackEntry.arguments?.getString("initialFormat") ?: ""
            val rawName = backStackEntry.arguments?.getString("initialName") ?: ""
            val rawOwner = backStackEntry.arguments?.getString("initialOwner") ?: ""

            val initialValue = if (rawValue.isNotBlank()) UriUtil.decode(rawValue) else ""
            val initialFormat = if (rawFormat.isNotBlank()) UriUtil.decode(rawFormat) else ""
            val initialName = if (rawName.isNotBlank()) UriUtil.decode(rawName) else ""
            val initialOwner = if (rawOwner.isNotBlank()) UriUtil.decode(rawOwner) else ""

            val viewModel: AddEditViewModel = viewModel(
                factory = AddEditViewModelFactory(repository, cardId, initialValue, initialFormat, initialName, initialOwner)
            )

            val savedStateHandle = backStackEntry.savedStateHandle
            val scannedBarcode = savedStateHandle.get<String>("scanned_barcode")
            val scannedFormat = savedStateHandle.get<String>("scanned_format")

            androidx.compose.runtime.LaunchedEffect(scannedBarcode, scannedFormat) {
                if (scannedBarcode != null) {
                    viewModel.onBarcodeValueChanged(scannedBarcode)
                    savedStateHandle.remove<String>("scanned_barcode")
                }
                if (scannedFormat != null) {
                    viewModel.onFormatSelected(scannedFormat)
                    savedStateHandle.remove<String>("scanned_format")
                }
            }

            AddEditCardScreen(
                viewModel = viewModel,
                onSaveSuccess = { savedCardId ->
                    navController.navigate(Screen.Detail.createRoute(savedCardId)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBackClick = { navController.popBackStack() },
                onScanClick = { navController.navigate(Screen.Scan.route) }
            )
        }

        // Card Detail Screen (Supermarket Scanner View)
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("cardId") { type = NavType.LongType }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "mycards://card/{cardId}" }
            )
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId") ?: -1L
            val viewModel: CardDetailViewModel = viewModel(
                factory = CardDetailViewModelFactory(repository, cardId)
            )

            CardDetailScreen(
                viewModel = viewModel,
                onEditClick = { id ->
                    navController.navigate(Screen.Add.createRoute(cardId = id))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Settings & Backup Screen
        composable(
            route = Screen.Settings.route,
            arguments = listOf(
                navArgument("importUri") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(repository)
            )

            val importUriString = backStackEntry.arguments?.getString("importUri") ?: ""
            if (importUriString.isNotBlank()) {
                val decodedUri = UriUtil.decode(importUriString)
                val uri = android.net.Uri.parse(decodedUri)
                // Usiamo un flag o LaunchedEffect nel ViewModel/Screen, ma per semplicità passiamo l'URI al ViewModel se non è stato ancora processato
                androidx.compose.runtime.LaunchedEffect(uri) {
                    viewModel.prepareImport(navController.context, uri)
                }
            }

            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
