package com.budgetbuddy.android

import com.budgetbuddy.android.data.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRepositoryTest {

    // A fake backend that returns canned data — no real network involved.
    private class FakeApi(
        private val profile: ProfileResponse,
        private val categories: List<Category>,
        private val transactions: List<Transaction>
    ) : ApiService {
        override suspend fun health() = HealthResponse("ok")
        override suspend fun login(request: LoginRequest) =
            LoginResponse("fake-token", User(1, "a@b.com", "Test"))
        override suspend fun getProfile() = profile
        override suspend fun getCategories() = categories
        override suspend fun getTransactions() = transactions
        override suspend fun createTransaction(body: CreateTransactionRequest) =
            Transaction(99, body.amount.toString(), body.description, body.date, "Test")
    }

    @Test
    fun loadHome_mapsIncomeAndLists() = runTest {
        val fake = FakeApi(
            profile = ProfileResponse(1, "a@b.com", "Test", "3000.00", "purple"),
            categories = listOf(Category(1, "Groceries", "400.00", "50.00")),
            transactions = listOf(
                Transaction(1, "50.00", "Milk", "2026-10-10T12:00:00Z", "Groceries")
            )
        )
        val repo = HomeRepository(fake)

        val data = repo.loadHome()

        assertEquals(3000.0, data.monthlyIncome, 0.001)
        assertEquals(1, data.categories.size)
        assertEquals("Groceries", data.categories[0].name)
        assertEquals(1, data.transactions.size)
        assertEquals("Milk", data.transactions[0].description)
    }
}