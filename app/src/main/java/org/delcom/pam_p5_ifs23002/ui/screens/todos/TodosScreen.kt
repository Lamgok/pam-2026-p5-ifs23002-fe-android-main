package org.delcom.pam_p5_ifs23002.ui.screens.todos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    // Ambil data dari viewmodel
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val uiStateTodo by todoViewModel.uiState.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var filterDone by remember { mutableStateOf<Boolean?>(null) }
    var filterUrgency by remember { mutableStateOf<String?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    // Muat data
    var todos by remember { mutableStateOf<List<ResponseTodoData>>(emptyList()) }
    var hasMore by remember { mutableStateOf(true) }
    var authToken by remember { mutableStateOf<String?>(null) }

    fun fetchTodosData(refresh: Boolean = false) {
        if (refresh) isLoading = true
        authToken = (uiStateAuth.auth as? AuthUIState.Success)?.data?.authToken
        todoViewModel.getAllTodos(authToken ?: "", searchQuery.text, filterDone, filterUrgency, refresh)
    }

    // Picu pengambilan data todos
    LaunchedEffect(Unit) {
        if (uiStateAuth.auth !is AuthUIState.Success) {
            RouteHelper.to(navController, ConstHelper.RouteNames.Home.path, true)
            return@LaunchedEffect
        }
        fetchTodosData(refresh = true)
    }

    // Picu ketika terjadi perubahan data todos
    LaunchedEffect(uiStateTodo.todos) {
        if (uiStateTodo.todos !is TodosUIState.Loading) {
            isLoading = false
            if (uiStateTodo.todos is TodosUIState.Success) {
                val state = uiStateTodo.todos as TodosUIState.Success
                todos = state.data
                hasMore = state.hasMore
            }
        }
    }

    fun onLogout(token: String){
        isLoading = true
        authViewModel.logout(token)
    }

    LaunchedEffect(uiStateAuth.authLogout) {
        if (uiStateAuth.authLogout !is AuthLogoutUIState.Loading) {
            RouteHelper.to(navController, ConstHelper.RouteNames.AuthLogin.path, true)
        }
    }

    // Tampilkan halaman loading
    if (isLoading) {
        LoadingUI()
        return
    }

    // Menu Top App Bar
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
            onClick = { onLogout(authToken ?: "") }
        )
    )

    fun onOpen(todoId: String) {
        RouteHelper.to(navController, "todos/${todoId}")
    }

    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= todos.size - 1 && hasMore && uiStateTodo.todos !is TodosUIState.Loading
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            fetchTodosData(refresh = false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBarComponent(
            navController = navController,
            title = "Todos",
            showBackButton = false,
            customMenuItems = menuItems,
            withSearch = true,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onSearchAction = { fetchTodosData(refresh = true) }
        )

        // Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter: ${if(filterDone == null) "Semua" else if(filterDone!!) "Selesai" else "Belum"} | ${filterUrgency ?: "Semua Urgensi"}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
                DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                    Text("Status", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
                    DropdownMenuItem(text = { Text("Semua") }, onClick = { filterDone = null; showFilterMenu = false; fetchTodosData(refresh = true) })
                    DropdownMenuItem(text = { Text("Selesai") }, onClick = { filterDone = true; showFilterMenu = false; fetchTodosData(refresh = true) })
                    DropdownMenuItem(text = { Text("Belum Selesai") }, onClick = { filterDone = false; showFilterMenu = false; fetchTodosData(refresh = true) })
                    
                    Text("Urgensi", modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
                    DropdownMenuItem(text = { Text("Semua") }, onClick = { filterUrgency = null; showFilterMenu = false; fetchTodosData(refresh = true) })
                    DropdownMenuItem(text = { Text("Low") }, onClick = { filterUrgency = "Low"; showFilterMenu = false; fetchTodosData(refresh = true) })
                    DropdownMenuItem(text = { Text("Medium") }, onClick = { filterUrgency = "Medium"; showFilterMenu = false; fetchTodosData(refresh = true) })
                    DropdownMenuItem(text = { Text("High") }, onClick = { filterUrgency = "High"; showFilterMenu = false; fetchTodosData(refresh = true) })
                }
            }
        }

        // Content
        Box(
            modifier = Modifier
                .weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(todos) { todo ->
                    TodoItemUI(todo, ::onOpen)
                }
                if (hasMore) {
                    item {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                }
            }

            if (todos.isEmpty() && !isLoading) {
                Text(
                    text = "Tidak ada data!",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
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
        // Bottom Nav
        BottomNavComponent(navController = navController)
    }
}

@Composable
fun TodoItemUI(
    todo: ResponseTodoData,
    onOpen: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onOpen(todo.id) },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AsyncImage(
                model = ToolsHelper.getTodoImage(todo.id, todo.updatedAt),
                contentDescription = todo.title,
                placeholder = painterResource(R.drawable.img_placeholder),
                error = painterResource(R.drawable.img_placeholder),
                modifier = Modifier.size(70.dp).clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = todo.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = todo.description, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (todo.isDone) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = if (todo.isDone) "Selesai" else "Belum", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                when(todo.urgency) {
                                    "High" -> MaterialTheme.colorScheme.errorContainer
                                    "Medium" -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = todo.urgency, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
