package com.example.moneymanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.moneymanager.data.local.dao.AccountDao
import com.example.moneymanager.data.local.dao.CategoryDao
import com.example.moneymanager.data.local.dao.TransactionDao
import com.example.moneymanager.data.local.entity.AccountEntity
import com.example.moneymanager.data.local.entity.CategoryEntity
import com.example.moneymanager.data.local.entity.TransactionEntity

@Database(
    entities = [AccountEntity::class, CategoryEntity::class, TransactionEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1) Create new table with RESTRICT FK for source and destination account.
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `transactions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` REAL NOT NULL,
                        `date` INTEGER NOT NULL,
                        `time` INTEGER NOT NULL,
                        `note` TEXT,
                        `type` TEXT NOT NULL,
                        `categoryId` INTEGER,
                        `accountId` INTEGER NOT NULL,
                        `toAccountId` INTEGER,
                        `photoPath` TEXT,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`toAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """)
                
                // 2) Copy data and normalize orphan toAccountId -> NULL.
                database.execSQL("""
                    INSERT INTO transactions_new (id, amount, date, time, note, type, categoryId, accountId, toAccountId, photoPath)
                    SELECT
                        id,
                        amount,
                        date,
                        time,
                        note,
                        type,
                        categoryId,
                        accountId,
                        CASE
                            WHEN toAccountId IS NULL THEN NULL
                            WHEN EXISTS(SELECT 1 FROM accounts WHERE id = toAccountId) THEN toAccountId
                            ELSE NULL
                        END AS toAccountId,
                        photoPath
                    FROM transactions
                """)

                // 3) Swap tables.
                database.execSQL("DROP TABLE transactions")
                database.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

                // 4) Recreate indices.
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_toAccountId` ON `transactions` (`toAccountId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
            }
        }
    }
}
