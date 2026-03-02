package com.example.moneymanager.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconUtils {
    // List of icons suitable for Accounts
    val accountIcons = listOf(
        "account_balance_wallet" to Icons.Default.AccountBalanceWallet, // Wallet
        "account_balance" to Icons.Default.AccountBalance, // Bank
        "credit_card" to Icons.Default.CreditCard, // Card
        "savings" to Icons.Default.Savings, // Piggy Bank
        "payments" to Icons.Default.Payments, // Cash/Notes
        "attach_money" to Icons.Default.AttachMoney, // Money Sign
        "monetization_on" to Icons.Default.MonetizationOn, // Coin
        "currency_exchange" to Icons.Default.CurrencyExchange, // Exchange
        "cases" to Icons.Default.Cases, // Briefcase
        "paid" to Icons.Default.Paid, // Paid
        "receipt_long" to Icons.Default.ReceiptLong, // Bill
        "pie_chart" to Icons.Default.PieChart, // Investment
        "trending_up" to Icons.Default.TrendingUp, // Growth
        "redeem" to Icons.Default.Redeem, // Gift/Bonus
        "lock" to Icons.Default.Lock // Safe
    )

    fun getIconByName(name: String?): ImageVector {
        return when (name?.lowercase()) {
            // ... Existing mapping ...
            "restaurant", "food" -> Icons.Default.Restaurant
            "directions_bus", "transport" -> Icons.Default.DirectionsBus
            "directions_car" -> Icons.Default.DirectionsCar
            "attach_money", "salary" -> Icons.Default.AttachMoney
            "account_balance", "bank" -> Icons.Default.AccountBalance
            "payments", "cash" -> Icons.Default.Payments
            "credit_card", "card" -> Icons.Default.CreditCard
            "shopping_cart", "shopping" -> Icons.Default.ShoppingCart
            "home", "housing" -> Icons.Default.Home
            "electric_bolt", "utility" -> Icons.Default.ElectricBolt
            "medical_services", "health" -> Icons.Default.MedicalServices
            "movie", "entertainment" -> Icons.Default.Movie
            "school", "education" -> Icons.Default.School
            "sync_alt", "transfer" -> Icons.Default.SyncAlt
            "receipt" -> Icons.Default.Receipt
            "laptop", "work" -> Icons.Default.Laptop
            "card_giftcard", "gift" -> Icons.Default.CardGiftcard
            "fastfood", "snacks" -> Icons.Default.Fastfood
            "person" -> Icons.Default.Person
            "build", "tools" -> Icons.Default.Build
            "storefront", "business" -> Icons.Default.Storefront
            "inventory", "supplies" -> Icons.Default.Inventory
            "more_horiz", "other" -> Icons.Default.MoreHoriz
            
            // New mappings for Accounts
            "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
            "savings" -> Icons.Default.Savings
            "monetization_on" -> Icons.Default.MonetizationOn
            "currency_exchange" -> Icons.Default.CurrencyExchange
            "cases" -> Icons.Default.Cases
            "paid" -> Icons.Default.Paid
            "receipt_long" -> Icons.Default.ReceiptLong
            "pie_chart" -> Icons.Default.PieChart
            "trending_up" -> Icons.Default.TrendingUp
            "redeem" -> Icons.Default.Redeem
            "lock" -> Icons.Default.Lock
            
            else -> Icons.Default.Category
        }
    }
}
