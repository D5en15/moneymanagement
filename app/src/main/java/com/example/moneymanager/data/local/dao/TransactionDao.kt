package com.example.moneymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.moneymanager.data.local.dto.CategorySum
import com.example.moneymanager.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, time DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    fun getTotalAmountByType(type: String, startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    fun getTransactionCount(startDate: Long, endDate: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :id OR toAccountId = :id")
    suspend fun getTransactionCountByAccount(id: Int): Int

    @Query("SELECT categoryId, SUM(amount) as total FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate GROUP BY categoryId ORDER BY total DESC")
    fun getCategorySumsByType(type: String, startDate: Long, endDate: Long): Flow<List<CategorySum>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: Int, startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(CASE WHEN type = 'income' THEN amount WHEN type = 'expense' THEN -amount ELSE 0 END) FROM transactions WHERE date >= :startDate")
    fun getNetChangeSince(startDate: Long): Flow<Double?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
