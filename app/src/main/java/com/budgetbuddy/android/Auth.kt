package com.budgetbuddy.android.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

object SessionManager {
    var token: String? = null
        private set
    var userName: String? = null
        private set

    fun save(token: String, userName: String?) {
        this.token = token
        this.userName = userName
    }

    fun clear() {
        token = null
        userName = null
    }

    fun isLoggedIn(): Boolean = token != null
}

// Accepts an ApiService so tests can pass a fake. Defaults to the real client.
class AuthRepository(private val api: ApiService = RetrofitClient.api) {
    suspend fun login(email: String, password: String): User {
        val response = api.login(LoginRequest(email, password))
        SessionManager.save(response.token, response.user.name)
        return response.user
    }
}

class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun onEmailChange(value: String) { email = value; errorMessage = null }
    fun onPasswordChange(value: String) { password = value; errorMessage = null }

    fun login(onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please enter your email and password."
            return
        }
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                repository.login(email.trim(), password)
                isLoading = false
                onSuccess()
            } catch (e: retrofit2.HttpException) {
                isLoading = false
                errorMessage = if (e.code() == 401) {
                    "Incorrect email or password."
                } else {
                    "Something went wrong (${e.code()}). Please try again."
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Can't reach the server. Check your connection."
            }
        }
    }
}