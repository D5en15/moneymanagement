package com.example.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // cash, bank, card
    val balance: Double,
    val currency: String = "THB",
    val icon: String? = null,
    val color: Int? = null // Color ARGB int
)
