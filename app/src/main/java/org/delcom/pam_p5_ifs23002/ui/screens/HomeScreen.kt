package org.delcom.pam_p5_ifs23002.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.delcom.pam_p5_ifs23002.helper.ConstHelper
import org.delcom.pam_p5_ifs23002.helper.RouteHelper
import org.delcom.pam_p5_ifs23002.network.todos.data.ResponseTodoData
import org.delcom.pam_p5_ifs23002.ui.components.BottomNavComponent
import org.delcom.pam_p5_ifs23002.ui.components.LoadingUI
import org.delcom.pam_p5_ifs23002.ui.components.StatusCard
import org.delcom.pam_p5_ifs23002.ui.components.TopAppBarComponent
import org.delcom.pam_p5_ifs23002.ui.components.TopAppBarMenuItem
import org.delcom.pam_p5_ifs23002.ui.theme.DelcomTheme
import org.delcom.pam_p5_ifs23002.ui.viewmodels.AuthLogoutUIState
import org.delcom.pam_p5_ifs23002.ui.viewmodels.AuthUIState
import org.delcom.pam_p5_ifs23002.ui.viewmodels.AuthViewModel
import org.delcom.pam_p5_ifs23002.ui.viewmodels.TodoViewModel
import org.delcom.pam_p5_ifs23002.ui.viewmodels.TodosUIState

@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    todoViewModel: TodoViewModel
) {
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val uiStateTodo by todoViewModel.uiState.collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    var authToken by remember { mutableStateOf<String?>(null) }
    var todos by remember { mutableStateOf<List<ResponseTodoData>>(emptyList()) }

    // 1. Inisialisasi: Ambil token saat pertama kali masuk
    LaunchedEffect(Unit) {
        authViewModel.loadTokenFromPreferences()
    }

    // 2. Monitoring Auth State
    LaunchedEffect(uiStateAuth.auth) {
        when (val state = uiStateAuth.auth) {
            is AuthUIState.Success -> {
                // Jika token ditemukan, simpan dan ambil data todos
                if (authToken != state.data.authToken) {
                    authToken = state.data.authToken
                    todoViewModel.getAllTodos(state.data.authToken, refresh = true)
                }
            }
            is AuthUIState.Error -> {
                // Jika tidak ada token, arahkan ke login
                RouteHelper.to(navController, ConstHelper.RouteNames.AuthLogin.path, true)
            }
            else -> Unit
        }
    }

    // 3. Monitoring Todo State
    LaunchedEffect(uiStateTodo.todos) {
        if (uiStateTodo.todos is TodosUIState.Success) {
            todos = (uiStateTodo.todos as TodosUIState.Success).data
            isLoading = false
        } else if (uiStateTodo.todos is TodosUIState.Error) {
            isLoading = false
            // Opsional: Tampilkan snackbar jika gagal ambil data
        }
    }

    // 4. Monitoring Logout
    LaunchedEffect(uiStateAuth.authLogout) {
        if (uiStateAuth.authLogout is AuthLogoutUIState.Success) {
            RouteHelper.to(navController, ConstHelper.RouteNames.AuthLogin.path, true)
        }
    }

    if (isLoading || authToken == null) {
        LoadingUI()
    } else {
        val menuItems = listOf(
            TopAppBarMenuItem(
                text = "Profile",
                icon = Icons.Filled.Person,
                route = ConstHelper.RouteNames.Profile.path
            ),
            TopAppBarMenuItem(
                text = "Logout",
                icon = Icons.AutoMirrored.Filled.Logout,
                route = null,
                onClick = {
                    authViewModel.logout(authToken ?: "")
                }
            )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TopAppBarComponent(
                navController = navController,
                title = "Home",
                showBackButton = false,
                customMenuItems = menuItems
            )
            Box(modifier = Modifier.weight(1f)) {
                HomeUI(todos)
            }
            BottomNavComponent(navController = navController)
        }
    }
}

@Composable
fun HomeUI(todos: List<ResponseTodoData>) {
    val totalTodos = todos.size
    val doneTodos = todos.count { it.isDone }
    val pendingTodos = totalTodos - doneTodos

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = "📋 My Todos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatusCard(
                title = "Total",
                value = totalTodos.toString(),
                icon = Icons.AutoMirrored.Filled.List
            )
            StatusCard(
                title = "Selesai",
                value = doneTodos.toString(),
                icon = Icons.Default.CheckCircle
            )
            StatusCard(
                title = "Belum",
                value = pendingTodos.toString(),
                icon = Icons.Default.Schedule
            )
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun PreviewHomeUI() {
    DelcomTheme {
        HomeUI(emptyList())
    }
}