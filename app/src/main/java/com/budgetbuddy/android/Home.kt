package com.budgetbuddy.android.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun String?.toMoney(): Double = this?.toDoubleOrNull() ?: 0.0
fun Double.asMoney(): String = "$%,.2f".format(this)

// Today as the backend expects it, e.g. "2026-10-10T12:00:00.000Z"
private val dateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}
fun todayIsoDate(): String = dateOnly.format(Date()) + "T12:00:00.000Z"

// Week key like "2026-10-W2" (mirrors the web app's logic).
fun currentWeekKey(): String {
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val monthIndex = cal.get(Calendar.MONTH) // 0-based
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
    val first = Calendar.getInstance().apply { set(year, monthIndex, 1) }
    val firstDayOfWeek = first.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
    val week = (dayOfMonth + firstDayOfWeek - 1) / 7 + 1
    val mm = (monthIndex + 1).toString().padStart(2, '0')
    return "$year-$mm-W$week"
}

data class HomeData(
    val monthlyIncome: Double,
    val categories: List<Category>,
    val transactions: List<Transaction>
)

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val data: HomeData) : HomeUiState
    data class Error(val message: String) : HomeUiState
    object SessionExpired : HomeUiState
}

class HomeRepository(private val api: ApiService = RetrofitClient.api) {
    suspend fun loadHome(): HomeData {
        val profile = api.getProfile()
        val categories = api.getCategories()
        val transactions = api.getTransactions()
        return HomeData(
            monthlyIncome = profile.monthlyIncome.toMoney(),
            categories = categories,
            transactions = transactions
        )
    }

    suspend fun addTransaction(categoryId: Int, amount: Double, description: String) {
        api.createTransaction(
            CreateTransactionRequest(
                categoryId = categoryId,
                amount = amount,
                description = description,
                date = todayIsoDate(),
                weekKey = currentWeekKey()
            )
        )
    }
}

class HomeViewModel : ViewModel() {

    private val repository = HomeRepository()

    var uiState by mutableStateOf<HomeUiState>(HomeUiState.Loading)
        private set

    var isAdding by mutableStateOf(false)
        private set
    var addError by mutableStateOf<String?>(null)
        private set

    init { load() }

    fun load() {
        uiState = HomeUiState.Loading
        viewModelScope.launch {
            uiState = try {
                HomeUiState.Success(repository.loadHome())
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401 || e.code() == 403) HomeUiState.SessionExpired
                else HomeUiState.Error("Couldn't load your budget (error ${e.code()}).")
            } catch (e: Exception) {
                HomeUiState.Error("Can't reach the server. Check your connection.")
            }
        }
    }

    fun addTransaction(categoryId: Int, amount: Double, description: String, onSuccess: () -> Unit) {
        isAdding = true
        addError = null
        viewModelScope.launch {
            try {
                repository.addTransaction(categoryId, amount, description)
                isAdding = false
                onSuccess()
                load() // refresh so the new expense + updated budget appear
            } catch (e: retrofit2.HttpException) {
                isAdding = false
                addError = "Couldn't add the expense (error ${e.code()})."
            } catch (e: Exception) {
                isAdding = false
                addError = "Can't reach the server."
            }
        }
    }
}