package com.kafe.android.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kafe.android.auth.GoogleSignInCancelled
import com.kafe.android.auth.requestGoogleIdToken
import com.kafe.android.viewmodel.KafeViewModel
import kotlinx.coroutines.launch

private val CoffeePrimary = Color(0xFF6F4E37)
private val LightCoffee = Color(0xFFEFEBE9)
private val DarkCoffee = Color(0xFF4E3629)

enum class AuthMode {
    LOGIN, REGISTER, FORGOT_PASSWORD
}

@Composable
fun AuthScreen(
    viewModel: KafeViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun switchTo(newMode: AuthMode) {
        errorMessage = null
        infoMessage = null
        mode = newMode
    }

    val onAuthResult: (Result<Unit>) -> Unit = { result ->
        isBusy = false
        result.onSuccess { onAuthSuccess() }
            .onFailure { errorMessage = it.localizedMessage ?: "Sign-in failed" }
    }

    // role is used only if this Google account is new to Kafe
    fun continueWithGoogle(role: String) {
        errorMessage = null
        isBusy = true
        scope.launch {
            try {
                val googleIdToken = requestGoogleIdToken(context)
                viewModel.signInWithGoogle(googleIdToken, role, onAuthResult)
            } catch (e: GoogleSignInCancelled) {
                isBusy = false
            } catch (e: Exception) {
                isBusy = false
                errorMessage = e.localizedMessage ?: "Google sign-in failed"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LightCoffee.copy(alpha = 0.5f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo or Icon
                Text(
                    text = "☕",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = when (mode) {
                        AuthMode.LOGIN -> "Welcome Back"
                        AuthMode.REGISTER -> "Create Account"
                        AuthMode.FORGOT_PASSWORD -> "Forgot Password"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkCoffee,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (infoMessage != null) {
                    Text(
                        text = infoMessage!!,
                        color = CoffeePrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CoffeePrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                AnimatedContent(targetState = mode, label = "AuthForms") { currentMode ->
                    when (currentMode) {
                        AuthMode.LOGIN -> {
                            LoginForm(
                                isBusy = isBusy,
                                onLogin = { email, password ->
                                    errorMessage = null
                                    isBusy = true
                                    viewModel.login(email, password, onAuthResult)
                                },
                                onGoogleClick = { continueWithGoogle("CUSTOMER") },
                                onRegisterClick = { switchTo(AuthMode.REGISTER) },
                                onForgotClick = { switchTo(AuthMode.FORGOT_PASSWORD) }
                            )
                        }
                        AuthMode.REGISTER -> {
                            RegisterForm(
                                isBusy = isBusy,
                                onRegister = { name, email, password, role ->
                                    errorMessage = null
                                    isBusy = true
                                    viewModel.register(name, email, password, role, onAuthResult)
                                },
                                onGoogleClick = { role -> continueWithGoogle(role) },
                                onLoginClick = { switchTo(AuthMode.LOGIN) }
                            )
                        }
                        AuthMode.FORGOT_PASSWORD -> {
                            ForgotPasswordForm(
                                isBusy = isBusy,
                                onSendLink = { email ->
                                    errorMessage = null
                                    isBusy = true
                                    viewModel.forgotPassword(email) { result ->
                                        isBusy = false
                                        result.onSuccess { message ->
                                            infoMessage = message
                                            mode = AuthMode.LOGIN
                                        }.onFailure {
                                            errorMessage = it.localizedMessage ?: "Could not send the reset email"
                                        }
                                    }
                                },
                                onBackClick = { switchTo(AuthMode.LOGIN) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Email Address") },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CoffeePrimary) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Password") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CoffeePrimary) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GoogleButton(enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(modifier = Modifier.weight(1f))
        Text("  or  ", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        Divider(modifier = Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("G", fontWeight = FontWeight.Black, color = Color(0xFF4285F4), fontSize = 18.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text("Continue with Google", fontWeight = FontWeight.Bold, color = DarkCoffee)
    }
}

@Composable
fun LoginForm(
    isBusy: Boolean,
    onLogin: (String, String) -> Unit,
    onGoogleClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        EmailField(email) { email = it }
        Spacer(modifier = Modifier.height(12.dp))
        PasswordField(password) { password = it }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onForgotClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Forgot Password?", color = CoffeePrimary, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onLogin(email, password) },
            enabled = !isBusy && email.isNotBlank() && password.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = CoffeePrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isBusy) "Signing in…" else "Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        GoogleButton(enabled = !isBusy, onClick = onGoogleClick)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account?", color = Color.Gray)
            TextButton(onClick = onRegisterClick) {
                Text("Register", color = CoffeePrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterForm(
    isBusy: Boolean,
    onRegister: (String, String, String, String) -> Unit,
    onGoogleClick: (String) -> Unit,
    onLoginClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("CUSTOMER") }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CoffeePrimary) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        EmailField(email) { email = it }
        Spacer(modifier = Modifier.height(12.dp))
        PasswordField(password) { password = it }
        Spacer(modifier = Modifier.height(14.dp))

        Text("Account Type:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedRole == "CUSTOMER",
                onClick = { selectedRole = "CUSTOMER" },
                label = { Text("Customer 👤", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CoffeePrimary,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedRole == "MERCHANT",
                onClick = { selectedRole = "MERCHANT" },
                label = { Text("Shop Owner 🏪", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CoffeePrimary,
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onRegister(name, email, password, selectedRole) },
            enabled = !isBusy && name.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = CoffeePrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isBusy) "Creating account…" else "Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        GoogleButton(enabled = !isBusy, onClick = { onGoogleClick(selectedRole) })
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account?", color = Color.Gray)
            TextButton(onClick = onLoginClick) {
                Text("Login", color = CoffeePrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ForgotPasswordForm(
    isBusy: Boolean,
    onSendLink: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Enter your account's email address and we'll send you a link to reset your password.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        EmailField(email) { email = it }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onSendLink(email) },
            enabled = !isBusy && email.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = CoffeePrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Reset Link", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Login", color = CoffeePrimary, fontWeight = FontWeight.Bold)
        }
    }
}
