package com.budgetbuddy.android.data

import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// ---- Auth models ----
data class HealthResponse(val status: String)
data class User(val id: Int, val email: String, val name: String?)
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val user: User)

// ---- Home models ----
data class ProfileResponse(
    val id: Int,
    val email: String,
    val name: String?,
    @SerializedName("monthly_income") val monthlyIncome: String?,
    val theme: String?
)

data class Category(
    val id: Int,
    val name: String,
    @SerializedName("limit_amount") val limitAmount: String?,
    val spent: String?
)

data class Transaction(
    val id: Int,
    val amount: String?,
    val description: String?,
    val date: String?,
    @SerializedName("category_name") val categoryName: String?
)

// ---- Create-transaction request ----
data class CreateTransactionRequest(
    @SerializedName("category_id") val categoryId: Int,
    val amount: Double,
    val description: String,
    val date: String,
    @SerializedName("week_key") val weekKey: String
)

// ---- Endpoints ----
interface ApiService {
    @GET("health")
    suspend fun health(): HealthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/user/profile")
    suspend fun getProfile(): ProfileResponse

    @GET("api/categories")
    suspend fun getCategories(): List<Category>

    @GET("api/transactions")
    suspend fun getTransactions(): List<Transaction>

    @POST("api/transactions")
    suspend fun createTransaction(@Body body: CreateTransactionRequest): Transaction
}

// ---- Retrofit client ----
object RetrofitClient {
    private const val BASE_URL = "https://backend-production-6e92.up.railway.app/"

    private val authInterceptor = Interceptor { chain ->
        val token = SessionManager.token
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .build()

    val api: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}