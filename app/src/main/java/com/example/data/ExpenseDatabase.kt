package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String, // "INCOME", "EXPENSE", "INVESTMENT", "FAMILY_SHARING"
    val category: String,
    val date: Long = System.currentTimeMillis(),
    val bundleName: String? = null, // For dynamic group itemizing bundles
    val userName: String? = null // Associate to specific logged-in user account
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val recurringPeriod: String, // "WEEKLY", "MONTHLY"
    val category: String,
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "family_config")
data class FamilyConfigEntity(
    @PrimaryKey val id: Int = 1, // Single row configuration
    val ownIncome: Double = 5000.0,
    val partnerIncome: Double = 3000.0,
    val proportionateRatio: Float = 0.625f, // 62.5% based on 5000 / 8000
    val contributionType: String = "MANUAL_DEBIT", // "AUTO_DEDUCT", "MANUAL_DEBIT"
    val emergencyFundTarget: Double = 10000.0,
    val emergencyFundSaved: Double = 1500.0,
    val currentUserName: String = "",
    val partnerName: String = "",
    val userSignedIn: Boolean = false,
    val biometricsPassed: Boolean = false
)

@Entity(tableName = "category_budgets")
data class CategoryBudgetEntity(
    @PrimaryKey val category: String,
    val limitAmount: Double
)

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val cardLast4: String,
    val creditLimit: Double,
    val balance: Double = 0.0,
    val dueDate: String = "25th"
)

@Entity(tableName = "bank_lendings")
data class BankLendingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val loanName: String,
    val totalAmount: Double,
    val remainingAmount: Double,
    val interestRate: Double,
    val monthlyInstallment: Double
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String, // "EXPENSE", "INCOME"
    val category: String,
    val frequency: String, // "DAILY", "WEEKLY", "MONTHLY"
    val nextDueDate: Long = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L)
)

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM goals ORDER BY dateCreated DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Query("UPDATE goals SET currentAmount = :currentAmount WHERE id = :id")
    suspend fun updateGoalProgress(id: Int, currentAmount: Double)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("SELECT * FROM family_config WHERE id = 1 LIMIT 1")
    fun getFamilyConfigFlow(): Flow<FamilyConfigEntity?>

    @Query("SELECT * FROM family_config WHERE id = 1 LIMIT 1")
    suspend fun getFamilyConfig(): FamilyConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyConfig(config: FamilyConfigEntity)

    // Category Budgets
    @Query("SELECT * FROM category_budgets ORDER BY category ASC")
    fun getAllCategoryBudgets(): Flow<List<CategoryBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryBudget(budget: CategoryBudgetEntity)

    @Delete
    suspend fun deleteCategoryBudget(budget: CategoryBudgetEntity)

    // Credit Cards
    @Query("SELECT * FROM credit_cards ORDER BY id ASC")
    fun getAllCreditCards(): Flow<List<CreditCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCard(card: CreditCardEntity)

    @Delete
    suspend fun deleteCreditCard(card: CreditCardEntity)

    // Bank Lendings
    @Query("SELECT * FROM bank_lendings ORDER BY id ASC")
    fun getAllBankLendings(): Flow<List<BankLendingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankLending(lending: BankLendingEntity)

    @Delete
    suspend fun deleteBankLending(lending: BankLendingEntity)

    // Recurring Transactions
    @Query("SELECT * FROM recurring_transactions ORDER BY nextDueDate ASC")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurring: RecurringTransactionEntity)

    @Delete
    suspend fun deleteRecurringTransaction(recurring: RecurringTransactionEntity)
}

@Database(entities = [
    TransactionEntity::class, 
    GoalEntity::class, 
    FamilyConfigEntity::class, 
    CategoryBudgetEntity::class, 
    CreditCardEntity::class, 
    BankLendingEntity::class, 
    RecurringTransactionEntity::class
], version = 4, exportSchema = false)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
}
