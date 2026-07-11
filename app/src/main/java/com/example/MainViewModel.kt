package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ExpenseRepository
import com.example.data.FamilyConfigEntity
import com.example.data.GeminiScannerService
import com.example.data.GoalEntity
import com.example.data.ParsedReceipt
import com.example.data.TransactionEntity
import com.example.data.CategoryBudgetEntity
import com.example.data.CreditCardEntity
import com.example.data.BankLendingEntity
import com.example.data.RecurringTransactionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // Theme state
    private val _darkModeEnabled = MutableStateFlow(true)
    val darkModeEnabled = _darkModeEnabled.asStateFlow()

    // Voice Input Bottom Sheet state
    private val _showVoiceInputSheet = MutableStateFlow(false)
    val showVoiceInputSheet = _showVoiceInputSheet.asStateFlow()

    fun setVoiceInputSheetVisible(visible: Boolean) {
        _showVoiceInputSheet.value = visible
    }

    // Sign in and Biometrics state
    private val _userSignedIn = MutableStateFlow(false)
    val userSignedIn = _userSignedIn.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName = _userName.asStateFlow()

    private val _biometricsPassed = MutableStateFlow(false)
    val biometricsPassed = _biometricsPassed.asStateFlow()

    private val _showBiometricPrompt = MutableStateFlow(false)
    val showBiometricPrompt = _showBiometricPrompt.asStateFlow()

    // Loading states
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _isDocumentScanning = MutableStateFlow(false)
    val isDocumentScanning = _isDocumentScanning.asStateFlow()

    // Active in-app notification state for recurring goals push reminders
    private val _activeNotification = MutableStateFlow<String?>(null)
    val activeNotification = _activeNotification.asStateFlow()

    // Partner Sync and Family Plan states
    private val _partnerSynced = MutableStateFlow(false)
    val partnerSynced = _partnerSynced.asStateFlow()

    private val _partnerName = MutableStateFlow("")
    val partnerName = _partnerName.asStateFlow()

    private val _familySyncStatus = MutableStateFlow("UNLINKED") // "UNLINKED", "SYNCING", "LINKED"
    val familySyncStatus = _familySyncStatus.asStateFlow()

    private val _familySharingCode = MutableStateFlow("EX-8902-SYNC")
    val familySharingCode = _familySharingCode.asStateFlow()

    // Undo stack state
    private val undoStack = java.util.ArrayList<suspend () -> Unit>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    fun pushUndoAction(message: String, action: suspend () -> Unit) {
        undoStack.add(action)
        _canUndo.value = true
        simulateNotification("$message (Tap Undo to revert)")
    }

    fun triggerUndo() {
        viewModelScope.launch {
            if (undoStack.isNotEmpty()) {
                val lastAction = undoStack.removeAt(undoStack.size - 1)
                lastAction()
                _canUndo.value = undoStack.isNotEmpty()
                simulateNotification("Action undone successfully!")
            } else {
                simulateNotification("Nothing to undo!")
            }
        }
    }

    // Database Flows
    val transactions: StateFlow<List<TransactionEntity>> = combine(
        repository.allTransactions,
        _userName,
        _userSignedIn
    ) { all, user, signedIn ->
        if (signedIn && user.isNotBlank()) {
            all.filter { it.userName.isNullOrBlank() || it.userName.equals(user, ignoreCase = true) }
        } else {
            all
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val goals: StateFlow<List<GoalEntity>> = repository.allGoals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val familyConfig: StateFlow<FamilyConfigEntity?> = repository.familyConfigFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val categoryBudgets: StateFlow<List<CategoryBudgetEntity>> = repository.allCategoryBudgets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val creditCards: StateFlow<List<CreditCardEntity>> = repository.allCreditCards
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bankLendings: StateFlow<List<BankLendingEntity>> = repository.allBankLendings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recurringTransactions: StateFlow<List<RecurringTransactionEntity>> = repository.allRecurringTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Pre-populate data offline if database is blank
        viewModelScope.launch {
            try {
                repository.prepopulateIfEmpty()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error prepopulating DB: ${e.message}")
            }
        }
        // Load persistent profile state
        viewModelScope.launch {
            repository.familyConfigFlow.collect { config ->
                if (config != null) {
                    _darkModeEnabled.value = config.darkModeEnabled
                    if (config.currentUserName.isNotBlank() && _userName.value.isBlank()) {
                        _userName.value = config.currentUserName
                    }
                    if (config.userSignedIn && !_userSignedIn.value) {
                        _userSignedIn.value = true
                        if (config.biometricsPassed) {
                            _biometricsPassed.value = true
                        }
                    }
                    if (config.partnerName.isNotBlank() && _partnerName.value.isBlank()) {
                        _partnerName.value = config.partnerName
                        _partnerSynced.value = true
                        _familySyncStatus.value = "LINKED"
                    }
                }
            }
        }
    }

    // Actions
    fun toggleDarkMode() {
        val nextValue = !_darkModeEnabled.value
        _darkModeEnabled.value = nextValue
        viewModelScope.launch {
            val current = repository.getFamilyConfig() ?: FamilyConfigEntity()
            repository.saveFamilyConfig(current.copy(darkModeEnabled = nextValue))
        }
    }

    fun signIn(username: String) {
        signInWithCredentials(username, "1234") { _, _ -> }
    }

    fun signInWithCredentials(username: String, pin: String, onResult: (Boolean, String) -> Unit) {
        if (username.isBlank()) {
            onResult(false, "Username cannot be blank")
            return
        }
        viewModelScope.launch {
            val current = repository.getFamilyConfig() ?: FamilyConfigEntity()
            if (current.storedPasscode.isNotBlank() && current.storedPasscode != pin) {
                onResult(false, "Incorrect PIN. Please try again.")
            } else {
                _userName.value = username
                _userSignedIn.value = true
                _showBiometricPrompt.value = true
                
                repository.saveFamilyConfig(
                    current.copy(
                        currentUserName = username,
                        storedPasscode = pin.ifBlank { "1234" },
                        userSignedIn = true,
                        biometricsPassed = false,
                        passkeyRegistered = true,
                        fingerAuthRegistered = true
                    )
                )
                onResult(true, "Authentication details recorded in database.")
            }
        }
    }

    fun registerCredentials(passkey: Boolean, finger: Boolean) {
        viewModelScope.launch {
            val current = repository.getFamilyConfig() ?: FamilyConfigEntity()
            repository.saveFamilyConfig(
                current.copy(
                    passkeyRegistered = passkey,
                    fingerAuthRegistered = finger
                )
            )
            simulateNotification("Passkey & Finger Auth settings updated in local DB.")
        }
    }

    fun registerPIN(pin: String) {
        viewModelScope.launch {
            val current = repository.getFamilyConfig() ?: FamilyConfigEntity()
            repository.saveFamilyConfig(
                current.copy(
                    storedPasscode = pin
                )
            )
            simulateNotification("Account passcode PIN updated in local DB.")
        }
    }

    fun authenticateBiometrics(success: Boolean) {
        if (success) {
            _biometricsPassed.value = true
            _showBiometricPrompt.value = false
            simulateNotification("Securely authenticated using Biometrics.")
            
            viewModelScope.launch {
                val current = repository.getFamilyConfig() ?: FamilyConfigEntity()
                repository.saveFamilyConfig(
                    current.copy(
                        biometricsPassed = true
                    )
                )
            }
        }
    }

    fun lockApp() {
        _biometricsPassed.value = false
        viewModelScope.launch {
            val current = repository.getFamilyConfig() ?: FamilyConfigEntity()
            repository.saveFamilyConfig(
                current.copy(
                    biometricsPassed = false
                )
            )
        }
        simulateNotification("App locked successfully.")
    }

    fun logout() {
        _userSignedIn.value = false
        _biometricsPassed.value = false
        _showBiometricPrompt.value = false
        _userName.value = ""
        
        viewModelScope.launch {
            val current = repository.getFamilyConfig() ?: FamilyConfigEntity()
            repository.saveFamilyConfig(
                current.copy(
                    userSignedIn = false,
                    biometricsPassed = false
                )
            )
        }
    }

    fun triggerBiometricPrompt() {
        _showBiometricPrompt.value = true
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: String, // INCOME, EXPENSE, INVESTMENT, FAMILY_SHARING
        category: String,
        bundleName: String? = null,
        date: Long? = null
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                title = title,
                amount = amount,
                type = type,
                category = category,
                bundleName = if (bundleName?.trim().isNullOrEmpty()) null else bundleName?.trim(),
                userName = _userName.value,
                date = date ?: System.currentTimeMillis()
            )
            repository.insertTransaction(tx)
            simulateNotification("Recorded $type: ${tx.title} ($$amount)")
        }
    }

    fun generateSharingCode() {
        val randomNum = (1000..9999).random()
        _familySharingCode.value = "EX-${randomNum}-SYNC"
    }

    fun syncPartner(partnerCode: String, name: String) {
        viewModelScope.launch {
            _familySyncStatus.value = "SYNCING"
            simulateNotification("Initiating handshake with partner client $partnerCode...")
            kotlinx.coroutines.delay(1800)
            _familySyncStatus.value = "LINKED"
            _partnerSynced.value = true
            val pName = name.trim().ifBlank { "Partner Arjun" }
            _partnerName.value = pName
            simulateNotification("Linked successfully with $pName! Family plan synced.")
            
            // Save to DB
            val current = repository.getFamilyConfig() ?: FamilyConfigEntity()
            repository.saveFamilyConfig(
                current.copy(
                    partnerName = pName
                )
            )
            
            // Insert some partner specific transactions for family sync simulation
            val partnerTx = TransactionEntity(
                title = "Partner: Apple Music Family",
                amount = 29.90,
                type = "EXPENSE",
                category = "Bill & Subscription",
                date = System.currentTimeMillis() - (4 * 60 * 60 * 1000L),
                userName = pName
            )
            repository.insertTransaction(partnerTx)

            val partnerTx2 = TransactionEntity(
                title = "Partner: Pinduoduo Purchase",
                amount = 88.00,
                type = "EXPENSE",
                category = "Shopping",
                date = System.currentTimeMillis() - (2 * 60 * 60 * 1000L),
                userName = pName
            )
            repository.insertTransaction(partnerTx2)
        }
    }

    fun disconnectPartner() {
        viewModelScope.launch {
            val name = _partnerName.value
            _partnerSynced.value = false
            _partnerName.value = ""
            _familySyncStatus.value = "UNLINKED"
            simulateNotification("Disconnected from $name.")
            
            // Save to DB
            val current = repository.getFamilyConfig() ?: FamilyConfigEntity()
            repository.saveFamilyConfig(
                current.copy(
                    partnerName = ""
                )
            )
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            pushUndoAction("Deleted record: ${tx.title}") {
                repository.insertTransaction(tx)
            }
        }
    }

    fun updateTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.insertTransaction(tx)
            simulateNotification("Updated record: ${tx.title} ($${String.format("%.2f", tx.amount)})")
        }
    }

    fun addCategoryBudget(category: String, limitAmount: Double) {
        viewModelScope.launch {
            repository.insertCategoryBudget(CategoryBudgetEntity(category, limitAmount))
            simulateNotification("Budget limit set for $category: $$limitAmount")
        }
    }

    fun deleteCategoryBudget(budget: CategoryBudgetEntity) {
        viewModelScope.launch {
            repository.deleteCategoryBudget(budget)
            pushUndoAction("Deleted budget limit for ${budget.category}") {
                repository.insertCategoryBudget(budget)
            }
        }
    }

    fun addCreditCard(name: String, cardLast4: String, creditLimit: Double, balance: Double) {
        viewModelScope.launch {
            repository.insertCreditCard(CreditCardEntity(name = name, cardLast4 = cardLast4, creditLimit = creditLimit, balance = balance))
            simulateNotification("Credit card added: $name")
        }
    }

    fun deleteCreditCard(card: CreditCardEntity) {
        viewModelScope.launch {
            repository.deleteCreditCard(card)
            pushUndoAction("Removed credit card: ${card.name}") {
                repository.insertCreditCard(card)
            }
        }
    }

    fun chargeCreditCard(card: CreditCardEntity, amount: Double, title: String, category: String) {
        viewModelScope.launch {
            val updatedCard = card.copy(balance = card.balance + amount)
            repository.insertCreditCard(updatedCard)
            repository.insertTransaction(
                TransactionEntity(
                    title = "$title (Charged to ${card.name})",
                    amount = amount,
                    type = "EXPENSE",
                    category = category
                )
            )
            simulateNotification("Charged $$amount to ${card.name} for \"$title\"")
        }
    }

    fun payCreditCard(card: CreditCardEntity, amount: Double) {
        viewModelScope.launch {
            val updatedCard = card.copy(balance = (card.balance - amount).coerceAtLeast(0.0))
            repository.insertCreditCard(updatedCard)
            repository.insertTransaction(
                TransactionEntity(
                    title = "Credit Card Payment: ${card.name}",
                    amount = amount,
                    type = "EXPENSE",
                    category = "Credit Card Payment"
                )
            )
            simulateNotification("Paid $$amount towards ${card.name}")
        }
    }

    fun addBankLending(loanName: String, totalAmount: Double, remainingAmount: Double, interestRate: Double, monthlyInstallment: Double) {
        viewModelScope.launch {
            repository.insertBankLending(
                BankLendingEntity(
                    loanName = loanName,
                    totalAmount = totalAmount,
                    remainingAmount = remainingAmount,
                    interestRate = interestRate,
                    monthlyInstallment = monthlyInstallment
                )
            )
            simulateNotification("Bank lending/loan added: $loanName")
        }
    }

    fun deleteBankLending(lending: BankLendingEntity) {
        viewModelScope.launch {
            repository.deleteBankLending(lending)
            pushUndoAction("Removed bank lending: ${lending.loanName}") {
                repository.insertBankLending(lending)
            }
        }
    }

    fun payBankLending(lending: BankLendingEntity, amount: Double) {
        viewModelScope.launch {
            val updatedLending = lending.copy(remainingAmount = (lending.remainingAmount - amount).coerceAtLeast(0.0))
            repository.insertBankLending(updatedLending)
            repository.insertTransaction(
                TransactionEntity(
                    title = "Repayment: ${lending.loanName}",
                    amount = amount,
                    type = "EXPENSE",
                    category = "Bank Repayment"
                )
            )
            simulateNotification("Repaid $$amount to ${lending.loanName}")
        }
    }

    fun addRecurringTransaction(title: String, amount: Double, type: String, category: String, frequency: String) {
        viewModelScope.launch {
            repository.insertRecurringTransaction(
                RecurringTransactionEntity(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    frequency = frequency
                )
            )
            simulateNotification("New recurring transaction set: $title ($frequency)")
        }
    }

    fun deleteRecurringTransaction(rec: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(rec)
            pushUndoAction("Removed recurring transaction: ${rec.title}") {
                repository.insertRecurringTransaction(rec)
            }
        }
    }

    fun processRecurringTransaction(rec: RecurringTransactionEntity) {
        viewModelScope.launch {
            // Apply the transaction immediately
            repository.insertTransaction(
                TransactionEntity(
                    title = rec.title,
                    amount = rec.amount,
                    type = rec.type,
                    category = rec.category,
                    date = System.currentTimeMillis()
                )
            )
            // Update nextDueDate
            val intervalMs = when (rec.frequency) {
                "DAILY" -> 24 * 60 * 60 * 1000L
                "WEEKLY" -> 7 * 24 * 60 * 60 * 1000L
                else -> 30 * 24 * 60 * 60 * 1000L
            }
            val updatedRec = rec.copy(nextDueDate = System.currentTimeMillis() + intervalMs)
            repository.insertRecurringTransaction(updatedRec)
            simulateNotification("Successfully processed recurring transaction: ${rec.title} of $${rec.amount}")
        }
    }

    fun addGoal(title: String, targetAmount: Double, recurringPeriod: String, category: String) {
        viewModelScope.launch {
            val goal = GoalEntity(
                title = title,
                targetAmount = targetAmount,
                currentAmount = 0.0,
                recurringPeriod = recurringPeriod,
                category = category
            )
            repository.insertGoal(goal)
            simulateNotification("New savings goal created: ${goal.title}")

            // Simulate future push notifications after setting a recurring goal
            viewModelScope.launch {
                kotlinx.coroutines.delay(4000)
                simulateNotification("Goal Reminder: Time to contribute your recurring $recurringPeriod savings to \"${goal.title}\"!")
            }
        }
    }

    fun contributeToGoal(goal: GoalEntity, amount: Double) {
        viewModelScope.launch {
            val updatedAmount = (goal.currentAmount + amount).coerceAtMost(goal.targetAmount)
            repository.updateGoalProgress(goal.id, updatedAmount)

            // Also log it as a transaction of type "INVESTMENT" (since it goes to savings goals)
            repository.insertTransaction(
                TransactionEntity(
                    title = "Goal Save: ${goal.title}",
                    amount = amount,
                    type = "INVESTMENT",
                    category = "Savings Goal"
                )
            )

            simulateNotification("Contributed $$amount to \"${goal.title}\"!")
            if (updatedAmount >= goal.targetAmount) {
                simulateNotification("Congratulations! You've reached your savings goal \"${goal.title}\"!")
            }
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            pushUndoAction("Savings goal removed: ${goal.title}") {
                repository.insertGoal(goal)
            }
        }
    }

    fun updateFamilyIncome(own: Double, partner: Double) {
        viewModelScope.launch {
            val total = own + partner
            val ratio = if (total > 0) (own / total).toFloat() else 0.5f

            val current = repository.getFamilyConfig() ?: FamilyConfigEntity()
            val updated = current.copy(
                ownIncome = own,
                partnerIncome = partner,
                proportionateRatio = ratio
            )
            repository.saveFamilyConfig(updated)
            simulateNotification("Calculated Family Income Proportion: ${(ratio * 100).toInt()}% Own / ${((1f - ratio) * 100).toInt()}% Partner")
        }
    }

    fun updateFamilyContributionType(type: String) {
        viewModelScope.launch {
            val current = repository.getFamilyConfig() ?: FamilyConfigEntity()
            val updated = current.copy(contributionType = type)
            repository.saveFamilyConfig(updated)
            simulateNotification("Contribution method updated to: ${type.replace("_", " ")}")
        }
    }

    fun updateDailyAlertSettings(context: Context, enabled: Boolean, timeString: String) {
        viewModelScope.launch {
            val current = repository.getFamilyConfig() ?: FamilyConfigEntity()
            val updated = current.copy(
                dailyAlertEnabled = enabled,
                dailyAlertTime = timeString
            )
            repository.saveFamilyConfig(updated)
            if (enabled) {
                DailyReminderReceiver.schedule(context, timeString)
                simulateNotification("Daily recording reminder active for $timeString!")
            } else {
                DailyReminderReceiver.cancel(context)
                simulateNotification("Daily recording reminder disabled")
            }
        }
    }

    fun contributeToEmergencyFund(amount: Double) {
        viewModelScope.launch {
            val current = repository.getFamilyConfig() ?: FamilyConfigEntity()
            val updatedSaved = (current.emergencyFundSaved + amount).coerceAtLeast(0.0)
            val updated = current.copy(emergencyFundSaved = updatedSaved)
            repository.saveFamilyConfig(updated)

            // Log the contribution transaction
            if (amount > 0) {
                repository.insertTransaction(
                    TransactionEntity(
                        title = "Emergency Fund Contribution",
                        amount = amount,
                        type = "INVESTMENT",
                        category = "Emergency Fund"
                    )
                )
                simulateNotification("Deposited $$amount into Emergency Fund!")
            } else if (amount < 0) {
                repository.insertTransaction(
                    TransactionEntity(
                        title = "Emergency Fund Withdrawal",
                        amount = -amount,
                        type = "INCOME",
                        category = "Emergency Fund"
                    )
                )
                simulateNotification("Withdrew $${-amount} from Emergency Fund.")
            }
        }
    }

    fun updateEmergencyFundTarget(target: Double) {
        viewModelScope.launch {
            val current = repository.getFamilyConfig() ?: FamilyConfigEntity()
            val updated = current.copy(emergencyFundTarget = target)
            repository.saveFamilyConfig(updated)
            simulateNotification("Emergency fund target set to $$target")
        }
    }

    fun scanReceiptImage(context: Context, uri: Uri, onResult: (ParsedReceipt) -> Unit) {
        viewModelScope.launch {
            _isScanning.value = true
            val bitmap = GeminiScannerService.decodeUriToBitmap(context, uri)
            if (bitmap == null) {
                _isScanning.value = false
                simulateNotification("Error reading image file.")
                return@launch
            }

            // Call the scan service
            val result = GeminiScannerService.scanReceipt(bitmap)
            _isScanning.value = false

            if (result.errorMessage != null) {
                simulateNotification(result.errorMessage)
            } else {
                simulateNotification("Receipt scanned successfully!")
            }
            onResult(result)
        }
    }

    fun scanDocumentFile(context: Context, uri: Uri, fileType: String, onResult: (com.example.data.ParsedDocumentResult) -> Unit) {
        viewModelScope.launch {
            _isDocumentScanning.value = true
            try {
                val result = GeminiScannerService.scanDocument(context, uri, fileType)
                _isDocumentScanning.value = false
                if (result.errorMessage != null) {
                    simulateNotification(result.errorMessage)
                } else {
                    simulateNotification("Document scanned! Found ${result.transactions.size} records.")
                }
                onResult(result)
            } catch (e: Exception) {
                _isDocumentScanning.value = false
                simulateNotification("Scan failed: ${e.localizedMessage}")
                onResult(com.example.data.ParsedDocumentResult(emptyList(), false, e.localizedMessage))
            }
        }
    }

    fun clearAllSampleData() {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                simulateNotification("All sample records, cards, loans, budgets & savings goals erased completely.")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error clearing data: ${e.message}")
            }
        }
    }

    fun simulateNotification(message: String) {
        _activeNotification.value = message
        // Automatically dismiss in-app notification banner after 4.5 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(4500)
            if (_activeNotification.value == message) {
                _activeNotification.value = null
            }
        }
    }

    fun clearNotification() {
        _activeNotification.value = null
    }

    // Well-Structured CSV format generator with Metadata Header Table Format
    fun generateCsvReport(): String {
        val txList = transactions.value
        val sb = StringBuilder()
        
        sb.append("# =========================================================\n")
        sb.append("# DIGITAL TRANSACTION AUDIT EXPORT TABLE\n")
        sb.append("# Generated At: ,${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
        
        val totalIncome = txList.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = txList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalInvestment = txList.filter { it.type == "INVESTMENT" }.sumOf { it.amount }
        val netWorth = totalIncome - totalExpense + totalInvestment
        
        sb.append("# Net Worth Summary: ,$${String.format("%.2f", netWorth)}\n")
        sb.append("# Total Income Recs: ,$${String.format("%.2f", totalIncome)}\n")
        sb.append("# Total Expenses Recs: ,$${String.format("%.2f", totalExpense)}\n")
        sb.append("# Total Investment Recs: ,$${String.format("%.2f", totalInvestment)}\n")
        sb.append("# Total Itemized Records: ,${txList.size}\n")
        sb.append("# =========================================================\n\n")
        
        sb.append("ID,Date,Title,Amount,Type,Category,Group Bundle\n")
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

        txList.forEachIndexed { index, tx ->
            val dateStr = sdf.format(java.util.Date(tx.date))
            val bundle = tx.bundleName ?: "None"
            val cleanTitle = tx.title.replace("\"", "\"\"")
            val cleanBundle = bundle.replace("\"", "\"\"")
            sb.append("${index + 1},$dateStr,\"$cleanTitle\",${tx.amount},${tx.type},\"${tx.category}\",\"$cleanBundle\"\n")
        }
        return sb.toString()
    }

    // Beautifully Designed Details HTML Report Generator
    fun generateHtmlReport(): String {
        val txList = transactions.value
        val sb = java.lang.StringBuilder()
        
        val sdfDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val dateGenerated = sdfDate.format(java.util.Date())
        
        val totalIncome = txList.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = txList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalInvestment = txList.filter { it.type == "INVESTMENT" }.sumOf { it.amount }
        val netWorth = totalIncome - totalExpense + totalInvestment
        
        sb.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Sophisticated Expense Audit Report</title>
                <style>
                    :root {
                        --bg-color: #0B0A11;
                        --surface-color: #12111A;
                        --surface-elevated: #1A1924;
                        --primary: #8A70FF;
                        --accent-green: #00B894;
                        --accent-coral: #FD9644;
                        --accent-blue: #0984E3;
                        --text-main: #FFFFFF;
                        --text-muted: #A0A0AB;
                        --border: #23222D;
                    }
                    body {
                        background-color: var(--bg-color);
                        color: var(--text-main);
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        margin: 0;
                        padding: 32px 24px;
                    }
                    .container {
                        max-width: 900px;
                        margin: 0 auto;
                    }
                    header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        border-bottom: 1px solid var(--border);
                        padding-bottom: 24px;
                        margin-bottom: 32px;
                    }
                    .logo-title h1 {
                        font-size: 26px;
                        font-weight: 900;
                        margin: 0;
                        background: linear-gradient(135deg, var(--primary), var(--accent-green));
                        -webkit-background-clip: text;
                        -webkit-text-fill-color: transparent;
                        letter-spacing: -0.5px;
                    }
                    .logo-title p {
                        margin: 4px 0 0 0;
                        font-size: 13px;
                        color: var(--text-muted);
                    }
                    .report-meta {
                        font-size: 12px;
                        color: var(--text-muted);
                        text-align: right;
                        line-height: 1.6;
                    }
                    .report-meta strong {
                        color: var(--text-main);
                    }
                    .grid-summary {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
                        gap: 20px;
                        margin-bottom: 32px;
                    }
                    .card {
                        background-color: var(--surface-color);
                        border: 1px solid var(--border);
                        border-radius: 18px;
                        padding: 20px;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.3);
                        transition: transform 0.2s;
                    }
                    .card:hover {
                        transform: translateY(-2px);
                    }
                    .card-title {
                        font-size: 11px;
                        text-transform: uppercase;
                        font-weight: 700;
                        color: var(--text-muted);
                        letter-spacing: 1.2px;
                        margin-bottom: 6px;
                    }
                    .card-value {
                        font-size: 22px;
                        font-weight: 900;
                    }
                    .card-value.primary { color: var(--primary); }
                    .card-value.green { color: var(--accent-green); }
                    .card-value.coral { color: var(--accent-coral); }
                    .card-value.blue { color: var(--accent-blue); }

                    .section-title {
                        font-size: 16px;
                        font-weight: 800;
                        margin: 0 0 16px 0;
                        letter-spacing: -0.2px;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                    }
                    .section-title::before {
                        content: '';
                        display: inline-block;
                        width: 4px;
                        height: 16px;
                        background-color: var(--primary);
                        border-radius: 2px;
                    }

                    .chart-section {
                        margin-bottom: 32px;
                    }
                    .chart-container {
                        display: flex;
                        justify-content: space-around;
                        align-items: flex-end;
                        height: 180px;
                        padding: 24px;
                        background-color: var(--surface-color);
                        border: 1px solid var(--border);
                        border-radius: 18px;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.3);
                    }
                    .bar-col {
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        width: 50px;
                    }
                    .bar-wrapper {
                        display: flex;
                        align-items: flex-end;
                        height: 120px;
                        width: 100%;
                        justify-content: center;
                    }
                    .bar {
                        width: 28px;
                        background: linear-gradient(180deg, var(--primary), rgba(138, 112, 255, 0.4));
                        border-radius: 8px 8px 0 0;
                        box-shadow: 0 0 12px rgba(138, 112, 255, 0.2);
                    }
                    .bar-label {
                        font-size: 11px;
                        font-weight: 700;
                        color: var(--text-muted);
                        margin-top: 10px;
                    }
                    .bar-value {
                        font-size: 9px;
                        font-weight: 600;
                        color: var(--text-muted);
                        margin-bottom: 4px;
                    }

                    .table-section {
                        margin-bottom: 32px;
                    }
                    .table-wrapper {
                        border: 1px solid var(--border);
                        border-radius: 18px;
                        overflow: hidden;
                        box-shadow: 0 4px 20px rgba(0,0,0,0.3);
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        background-color: var(--surface-color);
                    }
                    th, td {
                        padding: 14px 20px;
                        text-align: left;
                        border-bottom: 1px solid var(--border);
                    }
                    th {
                        background-color: var(--surface-elevated);
                        color: var(--text-muted);
                        font-size: 11px;
                        text-transform: uppercase;
                        font-weight: 800;
                        letter-spacing: 0.8px;
                    }
                    tr:last-child td {
                        border-bottom: none;
                    }
                    .badge {
                        display: inline-block;
                        padding: 4px 10px;
                        border-radius: 12px;
                        font-size: 10px;
                        font-weight: 700;
                        text-transform: uppercase;
                    }
                    .badge.income { background-color: rgba(0, 184, 148, 0.12); color: var(--accent-green); }
                    .badge.expense { background-color: rgba(253, 150, 68, 0.12); color: var(--accent-coral); }
                    .badge.investment { background-color: rgba(9, 132, 227, 0.12); color: var(--accent-blue); }

                    .type-indicator {
                        font-weight: 800;
                    }
                    .type-indicator.plus { color: var(--accent-green); }
                    .type-indicator.minus { color: var(--accent-coral); }

                    .bundle-tag {
                        font-size: 11px;
                        color: var(--text-muted);
                        background-color: var(--surface-elevated);
                        padding: 2px 6px;
                        border-radius: 4px;
                        border: 1px solid var(--border);
                    }

                    footer {
                        text-align: center;
                        font-size: 11px;
                        color: var(--text-muted);
                        border-top: 1px solid var(--border);
                        padding-top: 20px;
                        margin-top: 48px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <header>
                        <div class="logo-title">
                            <h1>Sophisticated Audit</h1>
                            <p>Digital Financial Analysis & Transaction Logs</p>
                        </div>
                        <div class="report-meta">
                            <div>Generated: <strong>$dateGenerated</strong></div>
                            <div>Format: <strong>HTML5 / CSS3 Responsive</strong></div>
                        </div>
                    </header>

                    <div class="grid-summary">
                        <div class="card">
                            <div class="card-title">Net Worth</div>
                            <div class="card-value primary">$${String.format("%,.2f", netWorth)}</div>
                        </div>
                        <div class="card">
                            <div class="card-title">Total Income</div>
                            <div class="card-value green">$${String.format("%,.2f", totalIncome)}</div>
                        </div>
                        <div class="card">
                            <div class="card-title">Total Expenses</div>
                            <div class="card-value coral">$${String.format("%,.2f", totalExpense)}</div>
                        </div>
                        <div class="card">
                            <div class="card-title">Total Investments</div>
                            <div class="card-value blue">$${String.format("%,.2f", totalInvestment)}</div>
                        </div>
                    </div>

                    <div class="chart-section">
                        <h2 class="section-title">Standardized Monthly Spending Trends</h2>
                        <div class="chart-container">
        """.trimIndent())
        
        val monthsList = listOf("Dec", "Jan", "Feb", "Mar", "Apr", "May")
        val expenseVals = listOf(14000f, 17500f, 21000f, 8000f, 15000f, 19000f)
        val maxVal = 25000f
        
        monthsList.forEachIndexed { idx, m ->
            val valFloat = expenseVals[idx]
            val pct = (valFloat / maxVal * 100).toInt()
            sb.append("""
                            <div class="bar-col">
                                <span class="bar-value">$${String.format("%.0f", valFloat)}</span>
                                <div class="bar-wrapper">
                                    <div class="bar" style="height: ${pct}%;"></div>
                                </div>
                                <span class="bar-label">$m</span>
                            </div>
            """.trimIndent())
        }
        
        sb.append("""
                        </div>
                    </div>

                    <div class="table-section">
                        <h2 class="section-title">Itemized Audit Ledger</h2>
                        <div class="table-wrapper">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Date</th>
                                        <th>Title</th>
                                        <th>Category</th>
                                        <th>Bundle Group</th>
                                        <th>Type</th>
                                        <th style="text-align: right;">Amount</th>
                                    </tr>
                                </thead>
                                <tbody>
        """.trimIndent())
        
        val sdfDateTx = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        txList.forEach { tx ->
            val dtStr = sdfDateTx.format(java.util.Date(tx.date))
            val bName = tx.bundleName ?: "None"
            val badgeClass = when(tx.type) {
                "INCOME" -> "income"
                "EXPENSE" -> "expense"
                else -> "investment"
            }
            val indicatorClass = if (tx.type == "INCOME") "plus" else "minus"
            val indicatorPrefix = if (tx.type == "INCOME") "+" else "-"
            
            sb.append("""
                                    <tr>
                                        <td style="font-size: 13px; color: var(--text-muted);">$dtStr</td>
                                        <td style="font-weight: 700; font-size: 14px;">${tx.title}</td>
                                        <td><span class="badge $badgeClass">${tx.category}</span></td>
                                        <td><span class="bundle-tag">$bName</span></td>
                                        <td style="font-size: 12px; font-weight: 600; color: var(--text-muted);">${tx.type}</td>
                                        <td style="text-align: right; font-weight: 800;" class="type-indicator $indicatorClass">$indicatorPrefix$${String.format("%,.2f", tx.amount)}</td>
                                    </tr>
            """.trimIndent())
        }
        
        sb.append("""
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <footer>
                        <p>Sophisticated Expense Audit Report &bull; Rendered locally via AI engine &bull; Confidential &bull; Page 1 of 1</p>
                    </footer>
                </div>
            </body>
            </html>
        """.trimIndent())
        
        return sb.toString()
    }
}

// ViewModelFactory
class MainViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class ParsedVoiceTransaction(
    val title: String,
    val amount: Double,
    val type: String, // INCOME, EXPENSE, INVESTMENT
    val category: String
)

fun parseNaturalLanguageExpense(text: String): ParsedVoiceTransaction {
    val clean = text.lowercase().trim()
    
    // 1. Parse numeric amount
    // Regular expression to look for numbers representing currency, e.g., "50", "12.50"
    val matchResult = Regex("\\b(\\d+(?:\\.\\d{1,2})?)\\b").find(clean)
    val amount = matchResult?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

    // 2. Determine type
    val type = when {
        clean.contains("income") || clean.contains("salary") || clean.contains("earned") || clean.contains("received") || clean.contains("deposited") || clean.contains("dividend") -> "INCOME"
        clean.contains("invest") || clean.contains("stock") || clean.contains("crypto") || clean.contains("bitcoin") || clean.contains("shares") || clean.contains("tesla") -> "INVESTMENT"
        else -> "EXPENSE"
    }

    // 3. Determine category and build descriptive title
    var category = "Miscellaneous"
    var title = "Voice Transaction"

    // Match categories based on typical natural language terms
    when {
        clean.contains("grocery") || clean.contains("groceries") || clean.contains("market") || clean.contains("food") || clean.contains("supermarket") || clean.contains("milk") || clean.contains("bread") -> {
            category = "Food & Grocery"
            title = "Groceries"
        }
        clean.contains("dinner") || clean.contains("lunch") || clean.contains("restaurant") || clean.contains("cafe") || clean.contains("coffee") || clean.contains("starbucks") || clean.contains("dine out") || clean.contains("pizza") || clean.contains("burger") || clean.contains("café") -> {
            category = "Dine Out & Café"
            title = "Dine Out"
        }
        clean.contains("netflix") || clean.contains("spotify") || clean.contains("disney") || clean.contains("youtube") || clean.contains("sub") || clean.contains("subscription") || clean.contains("membership") -> {
            category = "Bill & Subscription"
            title = "Subscription Service"
        }
        clean.contains("rent") || clean.contains("electric") || clean.contains("electricity") || clean.contains("water") || clean.contains("gas") || clean.contains("utilities") || clean.contains("wifi") || clean.contains("internet") -> {
            category = "Utilities & Rent"
            title = "Utilities"
        }
        clean.contains("shop") || clean.contains("shopping") || clean.contains("clothes") || clean.contains("shoes") || clean.contains("amazon") || clean.contains("ebay") || clean.contains("target") || clean.contains("nike") || clean.contains("mall") -> {
            category = "Shopping"
            title = "Shopping Purchase"
        }
        clean.contains("invest") || clean.contains("stock") || clean.contains("crypto") || clean.contains("bitcoin") || clean.contains("ethereum") || clean.contains("shares") || clean.contains("tesla") || clean.contains("etf") -> {
            category = "Investment"
            title = "Investment Asset"
        }
        clean.contains("travel") || clean.contains("flight") || clean.contains("uber") || clean.contains("taxi") || clean.contains("hotel") || clean.contains("gas station") || clean.contains("petrol") || clean.contains("fuel") || clean.contains("cab") -> {
            category = "Travelling"
            title = "Travel & Transit"
        }
        clean.contains("hospital") || clean.contains("doctor") || clean.contains("medical") || clean.contains("pharmacy") || clean.contains("medicine") || clean.contains("dental") || clean.contains("health") -> {
            category = "Health & Medical"
            title = "Medical Expense"
        }
        clean.contains("game") || clean.contains("gaming") || clean.contains("playstation") || clean.contains("xbox") || clean.contains("movie") || clean.contains("cinema") || clean.contains("theatre") || clean.contains("concert") || clean.contains("fun") -> {
            category = "Entertainment & Gaming"
            title = "Entertainment"
        }
        clean.contains("course") || clean.contains("education") || clean.contains("book") || clean.contains("books") || clean.contains("class") || clean.contains("tuition") || clean.contains("training") || clean.contains("school") -> {
            category = "Education & Self-Care"
            title = "Education"
        }
        clean.contains("mortgage") || clean.contains("loan") || clean.contains("debt") || clean.contains("lending") || clean.contains("repay") || clean.contains("installment") -> {
            category = "Mortgages"
            title = "Loan Repayment"
        }
    }

    // Attempt to extract title after prepositions like "on", "at", "for", "from"
    val prepositionMatch = Regex("\\b(?:on|at|for|from)\\s+([a-zA-Z0-9\\s&]+)$").find(clean)
    if (prepositionMatch != null) {
        val extractedName = prepositionMatch.groupValues[1].trim()
        if (extractedName.isNotBlank() && extractedName.length <= 30) {
            title = extractedName.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        }
    } else {
        val words = clean.split(" ")
        if (words.size > 3) {
            val lastWords = words.takeLast(2).joinToString(" ")
            if (!lastWords.contains("dollar") && !lastWords.contains("buck") && lastWords.length < 20) {
                title = lastWords.replaceFirstChar { it.uppercase() }
            }
        }
    }

    return ParsedVoiceTransaction(title, amount, type, category)
}
