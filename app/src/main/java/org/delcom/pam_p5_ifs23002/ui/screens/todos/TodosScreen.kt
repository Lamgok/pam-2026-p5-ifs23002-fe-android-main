package org.delcom.pam_p5_ifs23002.ui.screens.todos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import org.delcom.pam_p5_ifs23002.R
import org.delcom.pam_p5_ifs23002.helper.ConstHelper
import org.delcom.pam_p5_ifs23002.helper.RouteHelper
import org.delcom.pam_p5_ifs23002.helper.ToolsHelper
import org.delcom.pam_p5_ifs23002.network.todos.data.ResponseTodoData
import org.delcom.pam_p5_ifs23002.ui.components.BottomNavComponent
import org.delcom.pam_p5_ifs23002.ui.components.LoadingUI
import org.delcom.pam_p5_ifs23002.ui.components.TopAppBarComponent
import org.delcom.pam_p5_ifs23002.ui.components.TopAppBarMenuItem
import org.delcom.pam_p5_ifs23002.ui.viewmodels.AuthLogoutUIState
import org.delcom.pam_p5_ifs23002.ui.viewmodels.AuthUIState
import org.delcom.pam_p5_ifs23002.ui.viewmodels.AuthViewModel
import org.delcom.pam_p5_ifs23002.ui.viewmodels.TodoViewModel
import org.delcom.pam_p5_ifs23002.ui.viewmodels.TodosUIState

@Composable
fun TodosScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    todoViewModel: TodoViewModel
) {
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val uiStateTodo by todoViewModel.uiState.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var filterDone by remember { mutableStateOf<Boolean?>(null) }
    var filterUrgency by remember { mutableStateOf<String?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    var todos by remember { mutableStateOf<List<ResponseTodoData>>(emptyList()) }
    var hasMore by remember { mutableStateOf(true) }
    var authToken by remember { mutableStateOf<String?>(null) }

    // Memastikan refresh data setiap kali masuk ke screen ini
    LaunchedEffect(Unit) {
        if (uiStateAuth.auth is AuthUIState.Success) {
            authToken = (uiStateAuth.auth as AuthUIState.Success).data.authToken
            todoViewModel.getAllTodos(authToken!!, refresh = true)
        } else {
            RouteHelper.to(navController, ConstHelper.RouteNames.AuthLogin.path, true)
        }
    }

    LaunchedEffect(uiStateTodo.todos) {
        when (val state = uiStateTodo.todos) {
            is TodosUIState.Success -> {
                todos = state.data
                hasMore = state.hasMore
                isLoading = false
            }
            is TodosUIState.Error -> {
                isLoading = false
            }
            is TodosUIState.Loading -> {
                // Jangan set isLoading = true di sini agar tidak flicker saat load more
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBarComponent(
            navController = navController,
            title = "Daftar Tugas",
            showBackButton = false,
            withSearch = true,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onSearchAction = { todoViewModel.getAllTodos(authToken ?: "", searchQuery.text, filterDone, filterUrgency, true) }
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Filter: ${if(filterDone == null) "Semua" else if(filterDone!!) "Selesai" else "Belum"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { showFilterMenu = true }) {
                Text("Ubah Filter", style = MaterialTheme.typography.labelLarge)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (todos.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(painterResource(R.drawable.img_placeholder), contentDescription = null, modifier = Modifier.size(100.dp), tint = Color.Gray.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("Belum ada tugas", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(todos) { todo ->
                        TodoItemUI(todo) { RouteHelper.to(navController, "todos/${todo.id}") }
                    }
                }
            }

            FloatingActionButton(
                onClick = { RouteHelper.to(navController, ConstHelper.RouteNames.TodosAdd.path) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
        BottomNavComponent(navController = navController)
    }

    if (showFilterMenu) {
        AlertDialog(
            onDismissRequest = { showFilterMenu = false },
            title = { Text("Filter Tugas") },
            text = {
                Column {
                    Text("Status", fontWeight = FontWeight.Bold)
                    Row {
                        FilterChip(selected = filterDone == null, onClick = { filterDone = null }, label = { Text("Semua") })
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = filterDone == true, onClick = { filterDone = true }, label = { Text("Selesai") })
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = filterDone == false, onClick = { filterDone = false }, label = { Text("Belum") })
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Urgensi", fontWeight = FontWeight.Bold)
                    Row {
                        FilterChip(selected = filterUrgency == null, onClick = { filterUrgency = null }, label = { Text("Semua") })
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = filterUrgency == "High", onClick = { filterUrgency = "High" }, label = { Text("High") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = { 
                    showFilterMenu = false
                    todoViewModel.getAllTodos(authToken ?: "", searchQuery.text, filterDone, filterUrgency, true)
                }) { Text("Terapkan") }
            }
        )
    }
}

@Composable
fun TodoItemUI(todo: ResponseTodoData, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ToolsHelper.getTodoImage(todo.id, todo.updatedAt),
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.img_placeholder),
                error = painterResource(R.drawable.img_placeholder)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(todo.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(todo.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Row {
                    Badge(containerColor = if(todo.isDone) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)) {
                        Text(if(todo.isDone) "Selesai" else "Proses", color = if(todo.isDone) Color(0xFF2E7D32) else Color(0xFFE65100), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Badge(containerColor = when(todo.urgency) { "High" -> Color(0xFFFFEBEE); "Medium" -> Color(0xFFE3F2FD); else -> Color(0xFFF5F5F5) }) {
                        Text(todo.urgency, color = when(todo.urgency) { "High" -> Color(0xFFC62828); "Medium" -> Color(0xFF1565C0); else -> Color.DarkGray }, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}