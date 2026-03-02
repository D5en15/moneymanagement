package com.example.moneymanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.moneymanager.R

sealed class MoneyManagerDestination(
    val route: String,
    val title: Int,
    val icon: ImageVector
) {
    data object Calendar : MoneyManagerDestination(
        route = "calendar_view",
        title = R.string.nav_calendar,
        icon = Icons.Default.CalendarMonth
    )

    data object Stats : MoneyManagerDestination(
        route = "stats",
        title = R.string.nav_stats,
        icon = Icons.Default.Assessment
    )

    data object Home : MoneyManagerDestination(
        route = "home",
        title = R.string.nav_home,
        icon = Icons.Default.Home
    )

    data object Accounts : MoneyManagerDestination(
        route = "accounts",
        title = R.string.nav_accounts,
        icon = Icons.Default.AccountBalance
    )

    data object Settings : MoneyManagerDestination(
        route = "settings",
        title = R.string.nav_settings,
        icon = Icons.Default.Settings
    )

    data object AddTransaction : MoneyManagerDestination(
        route = "add_transaction",
        title = R.string.nav_add_transaction,
        icon = Icons.Default.Add
    )

    data object ManageCategories : MoneyManagerDestination(
        route = "manage_categories",
        title = R.string.nav_manage_categories,
        icon = Icons.Default.Settings
    )

    data object ManageAccounts : MoneyManagerDestination(
        route = "manage_accounts",
        title = R.string.nav_manage_accounts,
        icon = Icons.Default.Settings
    )
}

val bottomNavItems = listOf(
    MoneyManagerDestination.Calendar,
    MoneyManagerDestination.Stats,
    MoneyManagerDestination.Home,
    MoneyManagerDestination.Accounts,
    MoneyManagerDestination.Settings
)
