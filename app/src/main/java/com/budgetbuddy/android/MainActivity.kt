package com.budgetbuddy.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetbuddy.android.data.*

private val BudgetPurple = Color(0xFF667EEA)
private val BudgetPurpleDark = Color(0xFF764BA2)
private val ScreenBg = Color(0xFFF8FAFC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    var isLoggedIn by remember { mutableStateOf(SessionManager.isLoggedIn()) }

    if (isLoggedIn) {
        HomeScreen(onSignOut = {
            SessionManager.clear()
            isLoggedIn = false
        })
    } else {
        LoginScreen(onLoggedIn = { isLoggedIn = true })
    }
}

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BudgetPurple, BudgetPurpleDark)))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Budget Buddy", color = BudgetPurple, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Welcome back!", color = Color(0xFF64748B), fontSize = 16.sp)
                Spacer(Modifier.height(28.dp))

                OutlinedTextField(
                    value = viewModel.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                val error = viewModel.errorMessage
                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(error, color = Color(0xFFD92D20), fontSize = 14.sp)
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.login(onSuccess = onLoggedIn) },
                    enabled = !viewModel.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = BudgetPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    onSignOut: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state = viewModel.uiState

    LaunchedEffect(state) {
        if (state is HomeUiState.SessionExpired) onSignOut()
    }

    when (state) {
        is HomeUiState.Loading -> CenterBox { CircularProgressIndicator(color = BudgetPurple) }
        is HomeUiState.SessionExpired -> CenterBox { CircularProgressIndicator(color = BudgetPurple) }
        is HomeUiState.Error -> CenterBox {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.message, color = Color(0xFF334155), fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.load() },
                    colors = ButtonDefaults.buttonColors(containerColor = BudgetPurple)
                ) { Text("Retry", color = Color.White) }
            }
        }
        is HomeUiState.Success -> {
            var showAdd by remember { mutableStateOf(false) }

            Scaffold(
                containerColor = ScreenBg,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAdd = true },
                        containerColor = BudgetPurple
                    ) {
                        Text("+", color = Color.White, fontSize = 26.sp)
                    }
                }
            ) { padding ->
                HomeContent(state.data, onSignOut, Modifier.padding(padding))
            }

            if (showAdd) {
                AddTransactionDialog(
                    categories = state.data.categories,
                    viewModel = viewModel,
                    onDismiss = { showAdd = false }
                )
            }
        }
    }
}

@Composable
fun CenterBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun HomeContent(data: HomeData, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    val totalSpent = data.categories.sumOf { it.spent.toMoney() }
    val totalBudgeted = data.categories.sumOf { it.limitAmount.toMoney() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Budget Buddy", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BudgetPurple)
                TextButton(onClick = onSignOut) { Text("Sign Out", color = BudgetPurple) }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Monthly Income", color = Color(0xFF64748B), fontSize = 14.sp)
                    Text(data.monthlyIncome.asMoney(), color = BudgetPurple, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Spent: ${totalSpent.asMoney()}", color = Color(0xFF334155), fontSize = 14.sp)
                        Text("Budgeted: ${totalBudgeted.asMoney()}", color = Color(0xFF334155), fontSize = 14.sp)
                    }
                }
            }
        }

        item { Text("Budgets", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) }

        if (data.categories.isEmpty()) {
            item { Text("No categories yet.", color = Color(0xFF64748B)) }
        } else {
            items(data.categories) { cat -> CategoryRow(cat) }
        }

        item { Text("Recent Transactions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)) }

        if (data.transactions.isEmpty()) {
            item { Text("No transactions yet.", color = Color(0xFF64748B)) }
        } else {
            items(data.transactions.take(15)) { txn -> TransactionRow(txn) }
        }
    }
}

@Composable
fun CategoryRow(cat: Category) {
    val spent = cat.spent.toMoney()
    val limit = cat.limitAmount.toMoney()
    val progress = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f

    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(cat.name, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                Text("${spent.asMoney()} / ${limit.asMoney()}", color = Color(0xFF64748B), fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (progress >= 1f) Color(0xFFD92D20) else BudgetPurple,
                trackColor = Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
fun TransactionRow(txn: Transaction) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(txn.description ?: "—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                Text(
                    "${txn.categoryName ?: "Uncategorized"} · ${txn.date?.take(10) ?: ""}",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }
            Text(txn.amount.toMoney().asMoney(), fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }
    }
}

@Composable
fun AddTransactionDialog(
    categories: List<Category>,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(categories.firstOrNull()) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column {
                Text("Category", fontSize = 13.sp, color = Color(0xFF64748B))
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selected?.name ?: "Select a category")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = { selected = cat; expanded = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val err = localError ?: viewModel.addError
                if (err != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(err, color = Color(0xFFD92D20), fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !viewModel.isAdding,
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    val cat = selected
                    when {
                        cat == null -> localError = "Pick a category."
                        amt == null || amt <= 0 -> localError = "Enter a valid amount."
                        description.isBlank() -> localError = "Enter a description."
                        else -> {
                            localError = null
                            viewModel.addTransaction(cat.id, amt, description.trim(), onSuccess = onDismiss)
                        }
                    }
                }
            ) { Text(if (viewModel.isAdding) "Adding…" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}