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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.derivedStateOf

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

    // [BARU] State untuk Filter Urgensi dan Scroll
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    var selectedUrgency by remember { mutableStateOf<Int?>(null) } // State Urgensi
    val listState = rememberLazyListState()

    var todos by remember { mutableStateOf<List<ResponseTodoData>>(emptyList()) }
    var authToken by remember { mutableStateOf<String?>(null) }

    fun fetchTodosData() {
        isLoading = true
        authToken = (uiStateAuth.auth as AuthUIState.Success).data.authToken
        // Kirim selectedUrgency ke ViewModel
        todoViewModel.resetAndGetAllTodos(authToken ?: "", searchQuery.text, selectedFilter, selectedUrgency)
    }

    LaunchedEffect(Unit) {
        if (uiStateAuth.auth !is AuthUIState.Success) {
            RouteHelper.to(navController, ConstHelper.RouteNames.Home.path, true)
            return@LaunchedEffect
        }
        fetchTodosData()
    }

    // Logika Pagination (Infinite Scroll)
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= (totalItems - 2) && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && uiStateTodo.todos !is TodosUIState.Loading) {
            todoViewModel.getAllTodos(authToken ?: "", searchQuery.text, selectedFilter, selectedUrgency)
        }
    }

    LaunchedEffect(uiStateTodo.todos) {
        if (uiStateTodo.todos !is TodosUIState.Loading) {
            isLoading = false
            todos = if (uiStateTodo.todos is TodosUIState.Success) {
                (uiStateTodo.todos as TodosUIState.Success).data
            } else {
                emptyList()
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

    if (isLoading && todos.isEmpty()) {
        LoadingUI()
        return
    }

    val menuItems = listOf(
        TopAppBarMenuItem(text = "Profile", icon = Icons.Filled.Person, route = ConstHelper.RouteNames.Profile.path),
        TopAppBarMenuItem(text = "Logout", icon = Icons.AutoMirrored.Filled.Logout, route = null, onClick = { onLogout(authToken ?: "") })
    )

    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        TopAppBarComponent(
            navController = navController,
            title = "Todos",
            showBackButton = false,
            customMenuItems = menuItems,
            withSearch = true,
            searchQuery = searchQuery,
            onSearchQueryChange = { query -> searchQuery = query },
            onSearchAction = { fetchTodosData() }
        )

        // Barisan Filter Chips (Status & Urgensi)
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            // Row 1: Filter Status (Sesuai kode lama Anda)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(null to "Semua", "complete" to "Selesai", "active" to "Belum")
                filters.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key; fetchTodosData() },
                        label = { Text(label) }
                    )
                }
            }

            // Row 2: Filter Urgensi (Fitur Baru)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val urgencies = listOf(null to "Semua", 1 to "Low", 2 to "Medium", 3 to "High")
                urgencies.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedUrgency == key,
                        onClick = { selectedUrgency = key; fetchTodosData() },
                        label = { Text(label) }
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            TodosUI(todos = todos, onOpen = { id -> RouteHelper.to(navController, "todos/$id") }, listState = listState)

            Box(modifier = Modifier.fillMaxSize()) {
                FloatingActionButton(
                    onClick = { RouteHelper.to(navController, ConstHelper.RouteNames.TodosAdd.path) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Todo")
                }
            }
        }
        BottomNavComponent(navController = navController)
    }
}

@Composable
fun TodosUI(
    todos: List<ResponseTodoData>,
    onOpen: (String) -> Unit,
    listState: LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(todos) { todo ->
            TodoItemUI(todo, onOpen)
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    if (todos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Tidak ada data!", style = MaterialTheme.typography.bodyMedium)
        }
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
            .padding(8.dp)
            .clickable { onOpen(todo.id) },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically // Sejajarkan isi Row
        ) {

            AsyncImage(
                model = ToolsHelper.getTodoImage(todo.id, todo.updatedAt),
                contentDescription = todo.title,
                placeholder = painterResource(R.drawable.img_placeholder),
                error = painterResource(R.drawable.img_placeholder),
                modifier = Modifier
                    .size(70.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // ROW UNTUK JUDUL DAN LABEL URGENSI
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Judul Todo
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f), // Judul mengalah
                        maxLines = 1, // Judul hanya 1 baris
                        overflow = TextOverflow.Ellipsis // Judul dipotong jika kepanjangan
                    )

                    Spacer(modifier = Modifier.width(8.dp)) // Jarak

                    // Label Urgensi Anti-Vertikal
                    Text(
                        text = when(todo.urgency) { 3 -> "High"; 2 -> "Med"; else -> "Low" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1, // KUNCI 1
                        softWrap = false // KUNCI 2: Dilarang melipat ke bawah
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = todo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Tetap gunakan warna asli Anda (tertiaryContainer / secondaryContainer)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (todo.isDone)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                MaterialTheme.colorScheme.tertiaryContainer
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (todo.isDone) "Selesai" else "Belum Selesai",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (todo.isDone)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun PreviewTodosUI() {
//    DelcomTheme {
//        TodosUI(
//            todos = DummyData.getTodosData(),
//            onOpen = {},
//            listState = rememberLazyListState() // Tambahkan ini agar tidak error jika di-uncomment
//        )
//    }
}