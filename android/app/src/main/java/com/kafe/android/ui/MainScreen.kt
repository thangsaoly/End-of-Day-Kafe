package com.kafe.android.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kafe.android.model.Reservation
import com.kafe.android.ui.components.DealCard
import com.kafe.android.viewmodel.KafeViewModel
import com.kafe.android.viewmodel.UiEvent
import com.kafe.android.viewmodel.UiState
import kotlinx.coroutines.flow.collectLatest

private val CoffeePrimary = Color(0xFF6F4E37)
private val LightCoffee = Color(0xFFEFEBE9)
private val DarkCoffee = Color(0xFF4E3629)
private val EmeraldGreen = Color(0xFF2E7D32)
private val WarningYellow = Color(0xFFFBC02D)
private val ErrorRed = Color(0xFFD32F2F)

enum class ScreenTab {
    DEALS, RESERVATIONS, PROFILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: KafeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(ScreenTab.DEALS) }
    var activeReservationDetail by remember { mutableStateOf<Reservation?>(null) }
    var showLoginWarningDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen for events
    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.ReservationSuccess -> {
                    activeReservationDetail = event.reservation
                    snackbarHostState.showSnackbar(
                        message = "Reservation confirmed!",
                        duration = SnackbarDuration.Short
                    )
                }
                is UiEvent.ReservationError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
                is UiEvent.ClaimSuccess -> {
                    snackbarHostState.showSnackbar(
                        message = "Success! Reservation ${event.reservation.claimCode} marked as COMPLETED (Picked Up).",
                        duration = SnackbarDuration.Short
                    )
                }
                is UiEvent.ClaimError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (selectedTab) {
                                ScreenTab.DEALS -> "End-of-Day Kafe Rescue"
                                ScreenTab.RESERVATIONS -> if (viewModel.isMerchantMode) "Shop Owner Counter" else "My Reservations"
                                ScreenTab.PROFILE -> "My Account"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (viewModel.isMerchantMode && selectedTab == ScreenTab.RESERVATIONS) "🏪" else "☕", fontSize = 20.sp)
                    }
                },
                actions = {
                    if (selectedTab == ScreenTab.DEALS) {
                        IconButton(
                            onClick = { viewModel.loadDeals() },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh available deals",
                                tint = Color.White
                            )
                        }
                    } else if (selectedTab == ScreenTab.RESERVATIONS && viewModel.isLoggedIn) {
                        IconButton(
                            onClick = {
                                if (viewModel.isMerchantMode) {
                                    viewModel.loadMerchantReservations()
                                } else {
                                    viewModel.loadReservations()
                                }
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh reservations",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CoffeePrimary)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == ScreenTab.DEALS,
                    onClick = { selectedTab = ScreenTab.DEALS },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Surplus Deals Feed") },
                    label = { Text("Deals") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CoffeePrimary,
                        selectedTextColor = CoffeePrimary,
                        indicatorColor = LightCoffee
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == ScreenTab.RESERVATIONS,
                    onClick = { selectedTab = ScreenTab.RESERVATIONS },
                    icon = { Icon(Icons.Default.List, contentDescription = "My Reservations List") },
                    label = { Text("Reservations") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CoffeePrimary,
                        selectedTextColor = CoffeePrimary,
                        indicatorColor = LightCoffee
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == ScreenTab.PROFILE,
                    onClick = { selectedTab = ScreenTab.PROFILE },
                    icon = { Icon(Icons.Default.Person, contentDescription = "User Profile and Settings") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CoffeePrimary,
                        selectedTextColor = CoffeePrimary,
                        indicatorColor = LightCoffee
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LightCoffee.copy(alpha = 0.3f))
        ) {
            when (selectedTab) {
                ScreenTab.DEALS -> {
                    DealsTab(
                        viewModel = viewModel,
                        onLoginRequired = { showLoginWarningDialog = true }
                    )
                }
                ScreenTab.RESERVATIONS -> {
                    ReservationsTab(
                        viewModel = viewModel,
                        onLoginClick = { selectedTab = ScreenTab.PROFILE }
                    )
                }
                ScreenTab.PROFILE -> {
                    ProfileTab(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Login Warning Dialog
    if (showLoginWarningDialog) {
        AlertDialog(
            onDismissRequest = { showLoginWarningDialog = false },
            title = { Text("Authentication Required", fontWeight = FontWeight.Bold) },
            text = { Text("You must be logged in to claim surplus deal reservations.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLoginWarningDialog = false
                        selectedTab = ScreenTab.PROFILE
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeePrimary)
                ) {
                    Text("Go to Login")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginWarningDialog = false }) {
                    Text("Cancel", color = CoffeePrimary)
                }
            }
        )
    }

    // Claim Code Details Confirmation Dialog
    if (activeReservationDetail != null) {
        val reservation = activeReservationDetail!!
        AlertDialog(
            onDismissRequest = { activeReservationDetail = null },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🎉", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Reservation Confirmed!",
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = EmeraldGreen
                    )
                }
            },
            text = {
                val clipboardManager = LocalClipboardManager.current
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = reservation.deal.cafe.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = reservation.deal.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "YOUR CLAIM CODE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LightCoffee)
                            .border(1.dp, CoffeePrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(reservation.claimCode))
                                Toast.makeText(context, "Claim code copied!", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = reservation.claimCode,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = DarkCoffee,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Copy claim code to clipboard",
                            tint = CoffeePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Show this claim code to the store counter staff during the pickup window: ${reservation.deal.pickupStart} - ${reservation.deal.pickupEnd}.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeReservationDetail = null
                        selectedTab = ScreenTab.RESERVATIONS
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoffeePrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View My Reservations", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun DealsTab(
    viewModel: KafeViewModel,
    onLoginRequired: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search cafes or food items...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search deals") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search query")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CoffeePrimary,
                unfocusedBorderColor = Color.LightGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        when (val state = uiState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CoffeePrimary)
                }
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Connection Error",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadDeals() },
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeePrimary)
                    ) {
                        Text("Retry Connection")
                    }
                }
            }
            is UiState.Success -> {
                val filteredDeals = state.deals.filter { deal ->
                    searchQuery.isEmpty() ||
                            deal.title.contains(searchQuery, ignoreCase = true) ||
                            deal.cafe.name.contains(searchQuery, ignoreCase = true)
                }

                if (filteredDeals.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No deals match your search." else "No surplus deals nearby right now.\nCheck back later!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredDeals, key = { it.id }) { deal ->
                            DealCard(
                                deal = deal,
                                onReserveClick = { id ->
                                    if (viewModel.isLoggedIn) {
                                        viewModel.reserveDeal(id)
                                    } else {
                                        onLoginRequired()
                                    }
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
fun ReservationsTab(
    viewModel: KafeViewModel,
    onLoginClick: () -> Unit
) {
    if (!viewModel.isLoggedIn) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "Reservations list icon",
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Track your reservations in one place",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Log in to view active claim codes or switch to shop owner mode.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(containerColor = CoffeePrimary)
            ) {
                Text("Login / Register")
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (viewModel.isMerchantMode) {
            MerchantReservationsTab(viewModel = viewModel)
        } else {
            CustomerReservationsTab(viewModel = viewModel)
        }
    }
}

@Composable
fun CustomerReservationsTab(viewModel: KafeViewModel) {
    if (viewModel.isRefreshingReservations) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CoffeePrimary)
        }
        return
    }

    val reservations by viewModel.reservationsState.collectAsState()
    if (reservations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "You haven't reserved any deals yet.\nStart exploring!",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(reservations, key = { it.id }) { reservation ->
                ReservationCard(reservation = reservation)
            }
        }
    }
}

@Composable
fun MerchantReservationsTab(viewModel: KafeViewModel) {
    val merchantReservations by viewModel.merchantReservationsState.collectAsState()
    var inputCode by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Claim Code Scanner / Manual Entry Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verify Customer Pickup",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Verify Customer Claim Code",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkCoffee
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it.uppercase() },
                        placeholder = { Text("e.g. KAFE-A1B2") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CoffeePrimary,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (inputCode.isNotBlank()) {
                                viewModel.claimReservationByCode(inputCode)
                                inputCode = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Mark Picked Up", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (viewModel.isRefreshingMerchantReservations) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CoffeePrimary)
            }
            return
        }

        if (merchantReservations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No customer reservations found for your shop counter.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(merchantReservations, key = { it.id }) { reservation ->
                    MerchantReservationCard(
                        reservation = reservation,
                        onClaimClick = { id -> viewModel.claimReservation(id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MerchantReservationCard(
    reservation: Reservation,
    onClaimClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (reservation.status) {
        "PENDING" -> WarningYellow
        "COMPLETED" -> EmeraldGreen
        "EXPIRED" -> ErrorRed
        else -> Color.Gray
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Customer: ${reservation.user.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkCoffee
                    )
                    Text(
                        text = "Phone: ${reservation.user.phoneNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.1f))
                        .border(1.dp, statusColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (reservation.status == "COMPLETED") "PICKED UP" else reservation.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = reservation.deal.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LightCoffee.copy(alpha = 0.6f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CLAIM CODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = reservation.claimCode,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkCoffee
                    )
                }

                if (reservation.status == "PENDING") {
                    Button(
                        onClick = { onClaimClick(reservation.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Picked Up", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else if (reservation.status == "COMPLETED") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Picked Up",
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReservationCard(
    reservation: Reservation,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val statusColor = when (reservation.status) {
        "PENDING" -> WarningYellow
        "COMPLETED" -> EmeraldGreen
        "EXPIRED" -> ErrorRed
        else -> Color.Gray
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reservation.deal.cafe.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkCoffee
                )

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.1f))
                        .border(1.dp, statusColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = reservation.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Text(
                text = reservation.deal.title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Claim Code Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LightCoffee.copy(alpha = 0.5f))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(reservation.claimCode))
                        Toast.makeText(context, "Claim code copied!", Toast.LENGTH_SHORT).show()
                    }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CLAIM CODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = reservation.claimCode,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkCoffee
                    )
                }
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Copy claim code",
                    tint = CoffeePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Price: $${String.format("%.2f", reservation.deal.discountPrice)}",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen,
                    fontSize = 14.sp
                )
                Text(
                    text = "Pickup window: ${reservation.deal.pickupStart} - ${reservation.deal.pickupEnd}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ProfileTab(
    viewModel: KafeViewModel
) {
    if (!viewModel.isLoggedIn) {
        AuthScreen(
            viewModel = viewModel,
            onAuthSuccess = {}
        )
        return
    }

    val reservations by viewModel.reservationsState.collectAsState()
    var emailInput by remember { mutableStateOf("") }
    var codeInput by remember { mutableStateOf("") }
    var emailStateMsg by remember { mutableStateOf<String?>(null) }
    var codeStateMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "☕", fontSize = 64.sp, modifier = Modifier.padding(bottom = 8.dp))

        Text(
            text = "Welcome Back!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = DarkCoffee
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Account Details Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Profile Info",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CoffeePrimary
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account Type:", color = Color.Gray)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (viewModel.isMerchantMode) CoffeePrimary.copy(alpha = 0.1f) else EmeraldGreen.copy(alpha = 0.1f))
                            .border(1.dp, if (viewModel.isMerchantMode) CoffeePrimary else EmeraldGreen, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (viewModel.isMerchantMode) "Shop Owner 🏪" else "Customer 👤",
                            color = if (viewModel.isMerchantMode) CoffeePrimary else EmeraldGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                val latestUser = reservations.firstOrNull()?.user
                val noShows = latestUser?.noShowCount ?: 0
                val emailVerified = latestUser?.emailConfirmed ?: false
                val emailStr = latestUser?.email ?: "No email linked"

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Linked Email:", color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = emailStr,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (emailVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Email Verified",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("No-Show Count:", color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$noShows / 3",
                            fontWeight = FontWeight.Bold,
                            color = if (noShows > 0) ErrorRed else Color.Black
                        )
                        if (noShows > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning: No Show penalty active",
                                tint = WarningYellow,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Email Link / Verification Section
        val latestUser = reservations.firstOrNull()?.user
        val isConfirmed = latestUser?.emailConfirmed ?: false
        val linkedEmail = latestUser?.email

        if (linkedEmail == null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Add Email (For Password Resets)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkCoffee
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (emailStateMsg != null) {
                        Text(
                            text = emailStateMsg!!,
                            color = CoffeePrimary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.linkEmail(emailInput) { result ->
                                result.onSuccess {
                                    emailStateMsg = "Code '123456' generated. Please verify!"
                                    viewModel.loadReservations()
                                }.onFailure {
                                    emailStateMsg = "Error: ${it.localizedMessage}"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send Verification Code")
                    }
                }
            }
        } else if (!isConfirmed) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Verify Email",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkCoffee
                    )
                    Text(
                        text = "Enter code sent to: $linkedEmail",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        label = { Text("Verification Code") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (codeStateMsg != null) {
                        Text(
                            text = codeStateMsg!!,
                            color = if (codeStateMsg!!.startsWith("Success")) EmeraldGreen else ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.verifyEmail(linkedEmail, codeInput) { result ->
                                result.onSuccess {
                                    codeStateMsg = "Success: Email successfully verified!"
                                    viewModel.loadReservations()
                                }.onFailure {
                                    codeStateMsg = "Error: Incorrect verification code!"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoffeePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verify Code")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout Account", fontWeight = FontWeight.Bold)
        }
    }
}
