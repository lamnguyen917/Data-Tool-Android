package com.elp.datatool.presentation.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.elp.datatool.R
import com.elp.datatool.presentation.AppViewModelProvider
import com.elp.datatool.presentation.collect.CollectRoute
import com.elp.datatool.presentation.collect.CollectViewModel
import com.elp.datatool.presentation.collect.SearchProductsRoute
import com.elp.datatool.presentation.results.ResultDetailRoute
import com.elp.datatool.presentation.results.ResultDetailViewModel
import com.elp.datatool.presentation.results.ResultsRoute
import com.elp.datatool.presentation.settings.SettingsRoute
import com.elp.datatool.ui.icons.DtIcons

object Routes {
    const val COLLECT = "collect"
    const val SEARCH = "collect/search"
    const val RESULTS = "results"
    const val DETAIL = "results/detail/{${ResultDetailViewModel.KEY_ARG}}"
    const val SETTINGS = "settings"

    fun detail(key: String) = "results/detail/${Uri.encode(key)}"
}

enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    COLLECT(Routes.COLLECT, R.string.nav_collect, DtIcons.Inbox),
    RESULTS(Routes.RESULTS, R.string.nav_results, DtIcons.LayoutList),
    SETTINGS(Routes.SETTINGS, R.string.nav_settings, DtIcons.Settings),
}

@Composable
fun DataToolNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTopLevel = TopLevelDestination.entries.firstOrNull { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            // Secondary screens (search, details) hide the bottom navigation.
            if (currentTopLevel != null) {
                DtBottomNavigation(
                    selected = currentTopLevel,
                    onSelect = { navController.navigateToTopLevel(it.route) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.COLLECT,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            composable(Routes.COLLECT) {
                val viewModel: CollectViewModel = viewModel(factory = AppViewModelProvider.Factory)
                CollectRoute(
                    viewModel = viewModel,
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onViewResults = { navController.navigateToTopLevel(Routes.RESULTS) },
                )
            }
            composable(Routes.SEARCH) { entry ->
                // Share the Collect ViewModel so selected products land in the same input.
                val collectEntry = remember(entry) { navController.getBackStackEntry(Routes.COLLECT) }
                val viewModel: CollectViewModel = viewModel(collectEntry, factory = AppViewModelProvider.Factory)
                SearchProductsRoute(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable(Routes.RESULTS) {
                ResultsRoute(
                    viewModel = viewModel(factory = AppViewModelProvider.Factory),
                    onOpenDetail = { key -> navController.navigate(Routes.detail(key)) },
                    onGoToCollect = { navController.navigateToTopLevel(Routes.COLLECT) },
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument(ResultDetailViewModel.KEY_ARG) { type = NavType.StringType }),
            ) {
                ResultDetailRoute(
                    viewModel = viewModel(factory = AppViewModelProvider.Factory),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsRoute(viewModel = viewModel(factory = AppViewModelProvider.Factory))
            }
        }
    }
}

@Composable
private fun DtBottomNavigation(
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            TopLevelDestination.entries.forEach { destination ->
                NavigationBarItem(
                    selected = destination == selected,
                    onClick = { onSelect(destination) },
                    icon = { Icon(destination.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    label = { Text(stringResource(destination.labelRes)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
