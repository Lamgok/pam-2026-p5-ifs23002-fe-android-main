package org.delcom.pam_p5_ifs23002.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
        if(uiStateAuth.auth is AuthUIState.Success){
            authToken = (uiStateAuth.auth as AuthUIState.Success).data.authToken
            todoViewModel.getProfile(authToken!!)
        } else {
            RouteHelper.to(navController, ConstHelper.RouteNames.AuthLogin.path, true)
        }
    }

    LaunchedEffect(uiStateTodo.profile) {
        if(uiStateTodo.profile is ProfileUIState.Success){
            profile = (uiStateTodo.profile as ProfileUIState.Success).data
            isLoading = false
        }
    }

    // Penanganan SnackBar yang terpisah agar tidak bentrok
    LaunchedEffect(uiStateTodo.profileChange) {
        if (uiStateTodo.profileChange is TodoActionUIState.Success) {
            SuspendHelper.showSnackBar(snackbarHost, SuspendHelper.SnackBarType.SUCCESS, "Profil berhasil diperbarui")
            todoViewModel.getProfile(authToken ?: "")
        }
    }

    LaunchedEffect(uiStateTodo.profileChangePassword) {
        if (uiStateTodo.profileChangePassword is TodoActionUIState.Success) {
            SuspendHelper.showSnackBar(snackbarHost, SuspendHelper.SnackBarType.SUCCESS, "Kata sandi berhasil diubah")
        } else if (uiStateTodo.profileChangePassword is TodoActionUIState.Error) {
            SuspendHelper.showSnackBar(snackbarHost, SuspendHelper.SnackBarType.ERROR, (uiStateTodo.profileChangePassword as TodoActionUIState.Error).message)
        }
    }

    if(isLoading || profile == null){
        LoadingUI()
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        TopAppBarComponent(
            navController = navController,
            title = "Profil Saya",
            showBackButton = false,
            customMenuItems = listOf(
                TopAppBarMenuItem("Logout", Icons.AutoMirrored.Filled.Logout, null, onClick = { authViewModel.logout(authToken ?: "") })
            )
        )

        Box(modifier = Modifier.weight(1f)) {
            ProfileModernUI(
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
fun ProfileModernUI(
    profile: ResponseUserData,
    onSaveProfile: (String, String, String?) -> Unit,
    onSavePassword: (String, String) -> Unit,
    onSavePhoto: (Uri, Context) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(profile.name) }
    var username by remember { mutableStateOf(profile.username) }
    var about by remember { mutableStateOf(profile.about ?: "") }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onSavePhoto(uri, context)
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Header Section
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(
                brush = Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))
            ))

            Column(modifier = Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AsyncImage(
                        model = ToolsHelper.getUserImage(profile.id),
                        contentDescription = null,
                        modifier = Modifier.size(110.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape).background(Color.White),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.img_placeholder),
                        error = painterResource(R.drawable.img_placeholder)
                    )
                    IconButton(
                        onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape).border(2.dp, Color.White, CircleShape)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("@${profile.username}", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Form Section
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Card Informasi Akun
            ProfileCard(title = "Informasi Akun", icon = Icons.Default.Person) {
                ProfileTextField(value = name, onValueChange = { name = it }, label = "Nama Lengkap", icon = Icons.Default.Badge)
                ProfileTextField(value = username, onValueChange = { username = it }, label = "Username", icon = Icons.Default.AlternateEmail)
                ProfileTextField(value = about, onValueChange = { about = it }, label = "Tentang Saya", icon = Icons.Default.Info, isMultiline = true)

                Button(
                    onClick = { onSaveProfile(name, username, about) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Perbarui Profil")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Card Keamanan
            ProfileCard(title = "Keamanan", icon = Icons.Default.Lock) {
                ProfileTextField(value = oldPassword, onValueChange = { oldPassword = it }, label = "Kata Sandi Lama", icon = Icons.Default.VpnKey, isPassword = true)
                ProfileTextField(value = newPassword, onValueChange = { newPassword = it }, label = "Kata Sandi Baru", icon = Icons.Default.LockReset, isPassword = true)

                Button(
                    onClick = { onSavePassword(newPassword, oldPassword) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Shield, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ganti Kata Sandi")
                }
            }

            Spacer(Modifier.height(100.dp)) // Padding bawah agar tidak tertutup bottom nav
        }
    }
}

@Composable
fun ProfileCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Divider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray)
            content()
        }
    }
}

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    isMultiline: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        minLines = if (isMultiline) 3 else 1,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.LightGray
        )
    )
}
