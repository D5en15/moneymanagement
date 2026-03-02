package com.example.moneymanager.ui.screens.transactions

import com.example.moneymanager.data.local.entity.AccountEntity
import com.example.moneymanager.data.local.entity.CategoryEntity
import com.example.moneymanager.data.local.entity.TransactionEntity
import com.example.moneymanager.domain.repository.MoneyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class AddTransactionViewModelTest {

    @Mock
    private lateinit var repository: MoneyRepository

    private lateinit var viewModel: AddTransactionViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Mock Repository Data
        Mockito.`when`(repository.getAllAccounts()).thenReturn(flowOf(emptyList()))
        Mockito.`when`(repository.getAllCategories()).thenReturn(flowOf(emptyList()))

        viewModel = AddTransactionViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveTransaction updates account balance for Expense`() = runTest {
        // Arrange
        val accountId = 1
        val initialBalance = 1000.0
        val expenseAmount = 200.0
        val account = AccountEntity(id = accountId, name = "Cash", type = "cash", balance = initialBalance)

        Mockito.`when`(repository.getAccountById(accountId)).thenReturn(account)
        
        // Act
        viewModel.onTransactionTypeChanged("expense")
        viewModel.onAmountChanged(expenseAmount.toString())
        viewModel.onAccountSelected(accountId)
        viewModel.saveTransaction {}

        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        // Verify insertTransaction was called
        Mockito.verify(repository).insertTransaction(Mockito.any(TransactionEntity::class.java))
        
        // Verify updateAccount was called with correct balance
        val expectedBalance = initialBalance - expenseAmount
        Mockito.verify(repository).updateAccount(account.copy(balance = expectedBalance))
    }

    @Test
    fun `saveTransaction updates account balance for Income`() = runTest {
        // Arrange
        val accountId = 1
        val initialBalance = 1000.0
        val incomeAmount = 500.0
        val account = AccountEntity(id = accountId, name = "Bank", type = "bank", balance = initialBalance)

        Mockito.`when`(repository.getAccountById(accountId)).thenReturn(account)

        // Act
        viewModel.onTransactionTypeChanged("income")
        viewModel.onAmountChanged(incomeAmount.toString())
        viewModel.onAccountSelected(accountId)
        viewModel.saveTransaction {}

        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val expectedBalance = initialBalance + incomeAmount
        Mockito.verify(repository).updateAccount(account.copy(balance = expectedBalance))
    }
}
