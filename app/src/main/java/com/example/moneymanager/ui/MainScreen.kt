package com.example.moneymanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.moneymanager.ui.navigation.MoneyManagerDestination
import com.example.moneymanager.ui.navigation.bottomNavItems
import com.example.moneymanager.ui.screens.accounts.AccountsScreen
import com.example.moneymanager.ui.screens.settings.ManageCategoriesScreen
import com.example.moneymanager.ui.screens.settings.SettingsScreen
import com.example.moneymanager.ui.screens.stats.StatsScreen
import com.example.moneymanager.ui.screens.transactions.AddTransactionScreen
import com.example.moneymanager.ui.screens.transactions.TransactionsScreen
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import com.example.moneymanager.ui.screens.goals.GoalsScreen
import com.example.moneymanager.ui.screens.calendar.CalendarScreen
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material3.BottomAppBar
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.fillMaxSize
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymanager.ui.screens.calendar.CalendarViewModel

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val calendarViewModel: CalendarViewModel = hiltViewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun navigateToRoot(route: String, resetCalendarTransientState: Boolean = false) {
        if (resetCalendarTransientState) {
            calendarViewModel.resetTransientUi()
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = false }
            launchSingleTop = true
            restoreState = false
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), 
                tonalElevation = 0.dp,
                modifier = Modifier.height(80.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    bottomNavItems.forEachIndexed { index, screen ->
                        val isSelected = currentDestination?.route?.startsWith(screen.route) == true
                        val isHome = index == 2

                        if (isHome) {
                            androidx.compose.material3.Surface(
                                onClick = {
                                    navigateToRoot(
                                        route = screen.route,
                                        resetCalendarTransientState = screen.route == MoneyManagerDestination.Calendar.route
                                    )
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(60.dp),
                                shadowElevation = 8.dp
                            ) {
                                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = stringResource(screen.contentDescription),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        } else {
                            CustomNavBarItem(
                                screen = screen,
                                isSelected = isSelected,
                                onClick = {
                                    navigateToRoot(
                                        route = screen.route,
                                        resetCalendarTransientState = screen.route == MoneyManagerDestination.Calendar.route
                                    )
                                }
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
                .background(MaterialTheme.colorScheme.background)
        ) {
             Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 1000f
                        )
                    )
             )

            NavHost(
                navController = navController,
                startDestination = MoneyManagerDestination.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(MoneyManagerDestination.Home.route) {
                    TransactionsScreen(
                        onAddTransactionClick = { type -> navController.navigate(MoneyManagerDestination.AddTransaction.route + "?type=$type") },
                        onTransactionClick = { id -> navController.navigate("edit_transaction/$id") }
                    )
                }
                composable(MoneyManagerDestination.Calendar.route) {
                    CalendarScreen(
                        viewModel = calendarViewModel,
                        onDayLongPress = { date ->
                            val dateTimestamp = date.time
                            navController.navigate(MoneyManagerDestination.AddTransaction.route + "?date=$dateTimestamp")
                        }
                    )
                }
                composable(MoneyManagerDestination.Stats.route) {
                    StatsScreen(
                        onNavigateToAddTransaction = { navController.navigate(MoneyManagerDestination.AddTransaction.route) }
                    )
                }
                composable(MoneyManagerDestination.Accounts.route) {
                    // Accounts screen now handles its own dialogs
                    AccountsScreen()
                }
                composable(MoneyManagerDestination.Settings.route) {
                    SettingsScreen(
                        onManageCategoriesClick = { navController.navigate(MoneyManagerDestination.ManageCategories.route) }
                        // onManageAccountsClick removed
                    )
                }
                composable(MoneyManagerDestination.ManageCategories.route) {
                    ManageCategoriesScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                // ManageAccounts route removed
                
                composable(
                    route = MoneyManagerDestination.AddTransaction.route + "?type={type}&date={date}",
                    arguments = listOf(
                        navArgument("type") {
                            type = NavType.StringType
                            nullable = true
                        },
                        navArgument("date") {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
                    )
                ) {
                    AddTransactionScreen(
                        onBackClick = { navController.popBackStack() },
                        onSaveClick = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "edit_transaction/{transactionId}",
                    arguments = listOf(navArgument("transactionId") {
                        type = NavType.IntType
                    })
                ) {
                    AddTransactionScreen(
                        onBackClick = { navController.popBackStack() },
                        onSaveClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomNavBarItem(
    screen: MoneyManagerDestination,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(64.dp)
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = stringResource(screen.contentDescription),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(screen.title),
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
