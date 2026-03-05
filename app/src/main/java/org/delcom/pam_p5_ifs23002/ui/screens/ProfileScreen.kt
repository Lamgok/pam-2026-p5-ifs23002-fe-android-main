package org.delcom.pam_p5_ifs23002.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import org.delcom.pam_p5_ifs23002.R
import org.delcom.pam_p5_ifs23002.helper.ConstHelper
import org.delcom.pam_p5_ifs23002.helper.RouteHelper
import org.delcom.pam_p5_ifs23002.helper.SuspendHelper
import org.delcom.pam_p5_ifs23002.helper.ToolsHelper
import org.delcom.pam_p5_ifs23002.network.todos.data.ResponseUserData
import org.delcom.pam_p5_ifs23002.ui.components.BottomNavComponent
import org.delcom.pam_p5_ifs23002.ui.components.LoadingUI
import org.delcom.pam_p5_ifs23002.ui.components.TopAppBarComponent
import org.delcom.pam_p5_ifs23002.ui.components.TopAppBarMenuItem
import org.delcom.pam_p5_ifs23002.ui.theme.DelcomTheme
import org.delcom.pam_p5_ifs23002.ui.viewmodels.AuthLogoutUIState
import org.delcom.pam_p5_ifs23002.ui.viewmodels.AuthUIState
import org.delcom.pam_p5_ifs23002.ui.viewmodels.AuthViewModel
import org.delcom.pam_p5_ifs23002.ui.viewmodels.ProfileUIState
import org.delcom.pam_p5_ifs23002.ui.viewmodels.TodoActionUIState
import org.delcom.pam_p5_ifs23002.ui.viewmodels.TodoViewModel

@Composable
fun ProfileScreen(
    navController: NavHostController,
    snackbarHost: SnackbarHostState,
    authViewModel: AuthViewModel,
    todoViewModel: TodoViewModel
) {
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val uiStateTodo by todoViewModel.uiState.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf<ResponseUserData?>(null) }
    var authToken by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        if(uiStateAuth.auth !is AuthUIState.Success){
            RouteHelper.to(navController, ConstHelper.RouteNames.Home.path, true)
            return@LaunchedEffect
        }
        authToken = (uiStateAuth.auth as AuthUIState.Success).data.authToken
        todoViewModel.getProfile(authToken ?: "")
    }

    LaunchedEffect(uiStateTodo.profile) {
        if(uiStateTodo.profile !is ProfileUIState.Loading){
            isLoading = false
            if(uiStateTodo.profile is ProfileUIState.Success){
                profile = (uiStateTodo.profile as ProfileUIState.Success).data
            }
        }
    }

    LaunchedEffect(uiStateTodo.profileChange, uiStateTodo.profileChangePassword, uiStateTodo.profileChangePhoto) {
        val states = listOf(uiStateTodo.profileChange, uiStateTodo.profileChangePassword, uiStateTodo.profileChangePhoto)
        states.forEach { state ->
            if (state is TodoActionUIState.Success) {
                SuspendHelper.showSnackBar(snackbarHost, SuspendHelper.SnackBarType.SUCCESS, state.message)
                todoViewModel.getProfile(authToken ?: "")
            } else if (state is TodoActionUIState.Error) {
                SuspendHelper.showSnackBar(snackbarHost, SuspendHelper.SnackBarType.ERROR, state.message)
            }
        }
    }

    if(isLoading || profile == null){
        LoadingUI()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBarComponent(
            navController = navController,
            title = "Profile",
            showBackButton = false,
            customMenuItems = listOf(
                TopAppBarMenuItem("Logout", Icons.AutoMirrored.Filled.Logout, null, onClick = { authViewModel.logout(authToken ?: "") })
            )
        )

        Box(modifier = Modifier.weight(1f)) {
            ProfileUI(
                profile = profile!!,
                onSaveProfile = { n, u, a -> todoViewModel.putUserMe(authToken!!, n, u, a) },
                onSavePassword = { new, old -> todoViewModel.putUserMePassword(authToken!!, new, old) },
                onSavePhoto = { uri, context -> todoViewModel.putUserMePhoto(authToken!!, ToolsHelper.uriToMultipart(context, uri, "file")) }
            )
        }
        BottomNavComponent(navController = navController)
    }
}

@Composable
fun ProfileUI(
    profile: ResponseUserData,
    onSaveProfile: (String, String, String?) -> Unit,
    onSavePassword: (String, String) -> Unit,
    onSavePhoto: (Uri, Context) -> Unit
){
    val context = LocalContext.current
    var name by remember { mutableStateOf(profile.name) }
    var username by remember { mutableStateOf(profile.username) }
    var about by remember { mutableStateOf(profile.about ?: "") }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onSavePhoto(uri, context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ToolsHelper.getUserImage(profile.id),
            contentDescription = "Photo Profil",
            placeholder = painterResource(R.drawable.img_placeholder),
            error = painterResource(R.drawable.img_placeholder),
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        )
        Text("Sentuh foto untuk mengubah", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(24.dp))

        Text("Informasi Akun", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = about, onValueChange = { about = it }, label = { Text("Tentang") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        Button(onClick = { onSaveProfile(name, username, about) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Simpan Profil")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Ubah Kata Sandi", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = oldPassword, onValueChange = { oldPassword = it }, label = { Text("Kata Sandi Lama") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
        OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text("Kata Sandi Baru") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
        Button(onClick = { onSavePassword(newPassword, oldPassword) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
            Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Simpan Sandi")
        }
    }
}
