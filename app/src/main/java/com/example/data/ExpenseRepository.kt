package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ExpenseRepository(private val dao: ExpenseDao) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allGoals: Flow<List<GoalEntity>> = dao.getAllGoals()
    val familyConfigFlow: Flow<FamilyConfigEntity?> = dao.getFamilyConfigFlow()

    // New tables
    val allCategoryBudgets: Flow<List<CategoryBudgetEntity>> = dao.getAllCategoryBudgets()
    val allCreditCards: Flow<List<CreditCardEntity>> = dao.getAllCreditCards()
    val allBankLendings: Flow<List<BankLendingEntity>> = dao.getAllBankLendings()
    val allRecurringTransactions: Flow<List<RecurringTransactionEntity>> = dao.getAllRecurringTransactions()

    suspend fun insertTransaction(transaction: TransactionEntity) {
        dao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        dao.deleteTransaction(transaction)
    }

    suspend fun insertGoal(goal: GoalEntity) {
        dao.insertGoal(goal)
    }

    suspend fun updateGoalProgress(id: Int, currentAmount: Double) {
        dao.updateGoalProgress(id, currentAmount)
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        dao.deleteGoal(goal)
    }

    suspend fun getFamilyConfig(): FamilyConfigEntity? {
        return dao.getFamilyConfig()
    }

    suspend fun saveFamilyConfig(config: FamilyConfigEntity) {
        dao.insertFamilyConfig(config)
    }

    // Category Budgets
    suspend fun insertCategoryBudget(budget: CategoryBudgetEntity) {
        dao.insertCategoryBudget(budget)
    }

    suspend fun deleteCategoryBudget(budget: CategoryBudgetEntity) {
        dao.deleteCategoryBudget(budget)
    }

    // Credit Cards
    suspend fun insertCreditCard(card: CreditCardEntity) {
        dao.insertCreditCard(card)
    }

    suspend fun deleteCreditCard(card: CreditCardEntity) {
        dao.deleteCreditCard(card)
    }

    // Bank Lendings
    suspend fun insertBankLending(lending: BankLendingEntity) {
        dao.insertBankLending(lending)
    }

    suspend fun deleteBankLending(lending: BankLendingEntity) {
        dao.deleteBankLending(lending)
    }

    // Recurring Transactions
    suspend fun insertRecurringTransaction(recurring: RecurringTransactionEntity) {
        dao.insertRecurringTransaction(recurring)
    }

    suspend fun deleteRecurringTransaction(recurring: RecurringTransactionEntity) {
        dao.deleteRecurringTransaction(recurring)
    }

    suspend fun prepopulateIfEmpty() {
        // Prepopulate configuration if empty
        val existingConfig = dao.getFamilyConfig()
        if (existingConfig != null) {
            // Already initialized previously, do not prepopulate again (respect manual clear/delete operations)
            return
        }
        dao.insertFamilyConfig(FamilyConfigEntity())

        // Prepopulate transactions if empty
        val currentTransactions = dao.getAllTransactions().firstOrNull()
        if (currentTransactions.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L

            val initialTxs = listOf(
                TransactionEntity(
                    title = "Monthly Salary",
                    amount = 5000.00,
                    type = "INCOME",
                    category = "Salary",
                    date = now - (15 * dayMs)
                ),
                TransactionEntity(
                    title = "Partner Contribution",
                    amount = 1875.00,
                    type = "FAMILY_SHARING",
                    category = "Family Share",
                    date = now - (14 * dayMs)
                ),
                TransactionEntity(
                    title = "Amazon Shopping (Bundle)",
                    amount = 299.00,
                    type = "EXPENSE",
                    category = "Shopping",
                    date = now - (10 * dayMs),
                    bundleName = "Prime Day Bundle"
                ),
                TransactionEntity(
                    title = "Whole Foods Grocery",
                    amount = 154.50,
                    type = "EXPENSE",
                    category = "Food & Grocery",
                    date = now - (5 * dayMs)
                ),
                TransactionEntity(
                    title = "Vanguard Growth Index",
                    amount = 500.00,
                    type = "INVESTMENT",
                    category = "Investment",
                    date = now - (3 * dayMs)
                ),
                TransactionEntity(
                    title = "WIFI Monthly Bill",
                    amount = 399.00,
                    type = "EXPENSE",
                    category = "Bill & Subscription",
                    date = now - (2 * dayMs)
                ),
                TransactionEntity(
                    title = "Netflix Premium",
                    amount = 149.00,
                    type = "EXPENSE",
                    category = "Bill & Subscription",
                    date = now - dayMs
                ),
                TransactionEntity(
                    title = "Spotify Family",
                    amount = 49.00,
                    type = "EXPENSE",
                    category = "Bill & Subscription",
                    date = now
                ),
                TransactionEntity(
                    title = "Chase Visa Credit Card Expense",
                    amount = 89.90,
                    type = "EXPENSE",
                    category = "Credit Card Expense",
                    date = now - (4 * dayMs)
                ),
                TransactionEntity(
                    title = "Home Mortgage Payment",
                    amount = 1450.00,
                    type = "EXPENSE",
                    category = "Mortgages",
                    date = now - (8 * dayMs)
                )
            )

            for (tx in initialTxs) {
                dao.insertTransaction(tx)
            }
        }

        // Prepopulate savings goals if empty
        val currentGoals = dao.getAllGoals().firstOrNull()
        if (currentGoals.isNullOrEmpty()) {
            val initialGoals = listOf(
                GoalEntity(
                    title = "Apple iPhone 17 Pro",
                    targetAmount = 145000.0,
                    currentAmount = 75000.0,
                    recurringPeriod = "MONTHLY",
                    category = "Gadgets"
                ),
                GoalEntity(
                    title = "Japan Winter Vacation",
                    targetAmount = 300000.0,
                    currentAmount = 120000.0,
                    recurringPeriod = "MONTHLY",
                    category = "Travel"
                )
            )
            for (goal in initialGoals) {
                dao.insertGoal(goal)
            }
        }

        // Prepopulate Category Budgets if empty
        val currentBudgets = dao.getAllCategoryBudgets().firstOrNull()
        if (currentBudgets.isNullOrEmpty()) {
            val initialBudgets = listOf(
                CategoryBudgetEntity("Food & Grocery", 800.0),
                CategoryBudgetEntity("Shopping", 400.0),
                CategoryBudgetEntity("Bill & Subscription", 300.0),
                CategoryBudgetEntity("Mortgages", 2000.0),
                CategoryBudgetEntity("Credit Card Expense", 1000.0)
            )
            for (budget in initialBudgets) {
                dao.insertCategoryBudget(budget)
            }
        }

        // Prepopulate Credit Cards if empty
        val currentCards = dao.getAllCreditCards().firstOrNull()
        if (currentCards.isNullOrEmpty()) {
            val initialCards = listOf(
                CreditCardEntity(name = "Chase Sapphire Premium", cardLast4 = "4321", creditLimit = 5000.0, balance = 439.90, dueDate = "25th"),
                CreditCardEntity(name = "Apple Mastercard", cardLast4 = "8899", creditLimit = 3000.0, balance = 120.00, dueDate = "15th")
            )
            for (card in initialCards) {
                dao.insertCreditCard(card)
            }
        }

        // Prepopulate Bank Lendings if empty
        val currentLendings = dao.getAllBankLendings().firstOrNull()
        if (currentLendings.isNullOrEmpty()) {
            val initialLendings = listOf(
                BankLendingEntity(loanName = "First National Mortgage", totalAmount = 350000.0, remainingAmount = 280000.0, interestRate = 4.5, monthlyInstallment = 1450.0),
                BankLendingEntity(loanName = "Chase Premium Auto Loan", totalAmount = 25000.0, remainingAmount = 12000.0, interestRate = 3.2, monthlyInstallment = 380.0)
            )
            for (lending in initialLendings) {
                dao.insertBankLending(lending)
            }
        }

        // Prepopulate Recurring Transactions if empty
        val currentRecurrings = dao.getAllRecurringTransactions().firstOrNull()
        if (currentRecurrings.isNullOrEmpty()) {
            val initialRecurrings = listOf(
                RecurringTransactionEntity(title = "Netflix Premium Plan", amount = 15.99, type = "EXPENSE", category = "Bill & Subscription", frequency = "MONTHLY"),
                RecurringTransactionEntity(title = "Bi-weekly Job Salary", amount = 2500.00, type = "INCOME", category = "Salary", frequency = "WEEKLY"),
                RecurringTransactionEntity(title = "Gym Monthly Membership", amount = 45.00, type = "EXPENSE", category = "Education & Self-Care", frequency = "MONTHLY")
            )
            for (rec in initialRecurrings) {
                dao.insertRecurringTransaction(rec)
            }
        }
    }

    suspend fun clearAllData() {
        dao.deleteAllTransactions()
        dao.deleteAllGoals()
        dao.deleteAllCategoryBudgets()
        dao.deleteAllCreditCards()
        dao.deleteAllBankLendings()
        dao.deleteAllRecurringTransactions()
    }
}
