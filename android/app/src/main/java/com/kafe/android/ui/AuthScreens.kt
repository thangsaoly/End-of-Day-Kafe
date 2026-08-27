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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kafe.android.viewmodel.KafeViewModel

private val CoffeePrimary = Color(0xFF6F4E37)
private val LightCoffee = Color(0xFFEFEBE9)
private val DarkCoffee = Color(0xFF4E3629)

enum class AuthMode {
    LOGIN, REGISTER, FORGOT_PASSWORD, RESET_PASSWORD
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
    var forgotEmail by remember { mutableStateOf("") } // to pass to reset password screen

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
                        AuthMode.RESET_PASSWORD -> "Reset Password"
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
                                onLogin = { phone, password ->
                                    errorMessage = null
                                    viewModel.login(phone, password) { result ->
                                        result.onSuccess { onAuthSuccess() }
                                            .onFailure { errorMessage = it.localizedMessage ?: "Invalid phone number or password" }
                                    }
                                },
                                onRegisterClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    mode = AuthMode.REGISTER
                                },
                                onForgotClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    mode = AuthMode.FORGOT_PASSWORD
                                }
                            )
                        }
                        AuthMode.REGISTER -> {
                            RegisterForm(
                                onRegister = { name, phone, password, role ->
                                    errorMessage = null
                                    viewModel.register(name, phone, password, role) { result ->
                                        result.onSuccess { onAuthSuccess() }
                                            .onFailure { errorMessage = it.localizedMessage ?: "Registration failed" }
                                    }
                                },
                                onLoginClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    mode = AuthMode.LOGIN
                                }
                            )
                        }
                        AuthMode.FORGOT_PASSWORD -> {
                            ForgotPasswordForm(
                                onSendCode = { email ->
                                    errorMessage = null
                                    forgotEmail = email
                                    viewModel.forgotPassword(email) { result ->
                                        result.onSuccess {
                                            infoMessage = "Verification code '654321' sent to email!"
                                            mode = AuthMode.RESET_PASSWORD
                                        }
                                        .onFailure { errorMessage = it.localizedMessage ?: "Failed to find linked email" }
                                    }
                                },
                                onBackClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    mode = AuthMode.LOGIN
                                }
                            )
                        }
                        AuthMode.RESET_PASSWORD -> {
                            ResetPasswordForm(
                                email = forgotEmail,
                                onReset = { email, code, newPassword ->
                                    errorMessage = null
                                    viewModel.resetPassword(email, code, newPassword) { result ->
                                        result.onSuccess {
                                            infoMessage = "Password reset successfully! Please log in."
                                            mode = AuthMode.LOGIN
                                        }
                                        .onFailure { errorMessage = it.localizedMessage ?: "Incorrect reset code or email" }
                                    }
                                },
                                onBackClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    mode = AuthMode.LOGIN
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginForm(
    onLogin: (String, String) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotClick: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = CoffeePrimary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CoffeePrimary) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onForgotClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Forgot Password?", color = CoffeePrimary, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onLogin(phoneNumber, password) },
            colors = ButtonDefaults.buttonColors(containerColor = CoffeePrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Demo Login Helpers
        Text("Quick Demo Accounts:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    phoneNumber = "012345678"
                    password = "password"
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Customer 👤", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = {
                    phoneNumber = "099888777"
                    password = "password"
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Merchant 🏪", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
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
    onRegister: (String, String, String, String) -> Unit,
    onLoginClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
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
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = CoffeePrimary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CoffeePrimary) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
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
            onClick = { onRegister(name, phoneNumber, password, selectedRole) },
            colors = ButtonDefaults.buttonColors(containerColor = CoffeePrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
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
    onSendCode: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Enter your linked email address to receive a password reset code.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CoffeePrimary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onSendCode(email) },
            colors = ButtonDefaults.buttonColors(containerColor = CoffeePrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Verification Code", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun ResetPasswordForm(
    email: String,
    onReset: (String, String, String) -> Unit,
    onBackClick: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Enter the code sent to your email and your new password.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("Verification Code") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CoffeePrimary) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CoffeePrimary) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onReset(email, code, newPassword) },
            colors = ButtonDefaults.buttonColors(containerColor = CoffeePrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Reset Password", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
