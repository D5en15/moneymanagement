package com.example.moneymanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // income, expense
    val icon: String? = null,
    val color: Int? = null,
    val parentId: Int? = null // For sub-categories
)
