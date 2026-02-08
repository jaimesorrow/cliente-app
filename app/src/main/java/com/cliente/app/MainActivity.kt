package com.cliente.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cliente.app.domain.model.Role
import com.cliente.app.ui.navigation.AppRoute
import com.cliente.app.ui.navigation.RouteGuard
import com.cliente.app.ui.theme.ClienteTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ClienteApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClienteApp() {
    ClienteTheme {
        val navController = rememberNavController()
        var role by remember { mutableStateOf(Role.OWNER) }
        val offline = remember { mutableStateOf(false) }
        val snackbarHost = remember { SnackbarHostState() }

        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("Clientè") }) },
            snackbarHost = { SnackbarHost(snackbarHost) },
        ) { padding ->
            Surface(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (offline.value) Text("Offline mode: cached reads only", color = MaterialTheme.colorScheme.error)
                    RowActions(
                        onToggleRole = { role = if (role == Role.OWNER) Role.STAFF else Role.OWNER },
                        onToggleOffline = { offline.value = !offline.value },
                        onContact = { action ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action))
                            startActivity(intent)
                        },
                    )
                    NavHost(navController = navController, startDestination = AppRoute.Legal.route) {
                        composable(AppRoute.Legal.route) {
                            ScreenStub("Legal Hub", "Terms/Privacy with versioned acceptance") { navController.navigate(AppRoute.Dashboard.route) }
                        }
                        composable(AppRoute.Dashboard.route) {
                            val routes = listOf(
                                AppRoute.Clients,
                                AppRoute.Services,
                                AppRoute.Appointments,
                                AppRoute.Calendar,
                                AppRoute.Templates,
                                AppRoute.Availability,
                                AppRoute.Analytics,
                                AppRoute.Settings,
                            )
                            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(routes) { route ->
                                    Button(
                                        onClick = {
                                            if (RouteGuard.canAccess(route, role)) navController.navigate(route.route)
                                            else navController.navigate("denied/${route.route}")
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { Text(route.route) }
                                }
                                if (BuildConfig.DEBUG) {
                                    item { Button(onClick = { navController.navigate(AppRoute.QaMatrix.route) }) { Text("QA Matrix") } }
                                }
                            }
                        }
                        composable("denied/{route}", arguments = listOf(navArgument("route") { type = NavType.StringType })) {
                            ScreenStub("Access denied", "Owner-only route") { navController.popBackStack() }
                        }
                        composable(AppRoute.Clients.route) { ScreenStub("Clients", "CRUD clients, notes timeline") { navController.popBackStack() } }
                        composable(AppRoute.Services.route) { Guarded(role, AppRoute.Services) { ScreenStub("Services", "Owner CRUD, archive only") { navController.popBackStack() } } }
                        composable(AppRoute.Appointments.route) { ScreenStub("Appointments", "Schedule/reschedule/cancel with audit") { navController.popBackStack() } }
                        composable(AppRoute.Calendar.route) { ScreenStub("Calendar", "Day/Week with safe restore") { navController.popBackStack() } }
                        composable(AppRoute.Templates.route) { ScreenStub("Templates", "Owner edits, staff applies") { navController.popBackStack() } }
                        composable(AppRoute.Availability.route) { Guarded(role, AppRoute.Availability) { ScreenStub("Availability", "Owner-only hours/time-off") { navController.popBackStack() } } }
                        composable(AppRoute.Analytics.route) { Guarded(role, AppRoute.Analytics) { ScreenStub("Analytics", "Estimated revenue snapshots") { navController.popBackStack() } } }
                        composable(AppRoute.Settings.route) {
                            Guarded(role, AppRoute.Settings) {
                                var showDelete by remember { mutableStateOf(false) }
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Settings (Owner)")
                                    Button(onClick = { showDelete = true }) { Text("Delete Account & Close Business") }
                                    Button(onClick = { navController.navigate(AppRoute.Legal.route) }) { Text("Logout") }
                                }
                                if (showDelete) {
                                    AlertDialog(
                                        onDismissRequest = { showDelete = false },
                                        title = { Text("Confirm closure") },
                                        text = { Text("Immediate deactivation. Purge in 30 days.") },
                                        confirmButton = { TextButton(onClick = { showDelete = false }) { Text("Confirm") } },
                                        dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
                                    )
                                }
                            }
                        }
                        if (BuildConfig.DEBUG) {
                            composable(AppRoute.QaMatrix.route) { QaMatrixScreen() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Guarded(role: Role, route: AppRoute, content: @Composable () -> Unit) {
    if (RouteGuard.canAccess(route, role)) content()
    else Text("Owner role required")
}

@Composable
private fun RowActions(onToggleRole: () -> Unit, onToggleOffline: () -> Unit, onContact: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Button(onClick = onToggleRole) { Text("Toggle Owner/Staff") } }
        item { Button(onClick = onToggleOffline) { Text("Toggle offline") } }
        item { Button(onClick = { onContact("tel:+123456789") }) { Text("Call client") } }
        item { Button(onClick = { onContact("smsto:+123456789") }) { Text("Text client") } }
        item { Button(onClick = { onContact("mailto:client@example.com") }) { Text("Email client") } }
    }
}

@Composable
private fun ScreenStub(title: String, subtitle: String, onBack: () -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(subtitle)
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun QaMatrixScreen() {
    val cases = remember {
        mutableStateListOf(
            "RBAC owner-only routes",
            "Conflict detection with time-off",
            "Audit immutability",
            "Delete account closure",
            "Back-stack leakage",
        )
    }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var filter by remember { mutableStateOf("") }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("QA Matrix (DEBUG)")
            androidx.compose.material3.OutlinedTextField(value = filter, onValueChange = { filter = it }, label = { Text("Filter") })
            Button(onClick = {
                val csv = "case\n" + cases.filter { it.contains(filter, ignoreCase = true) }.joinToString("\n")
                clipboard.setText(AnnotatedString(csv))
                scope.launch { snackbar.showSnackbar("Copied CSV") }
            }) { Text("Copy CSV") }
            cases.filter { it.contains(filter, ignoreCase = true) }.forEach { Text("• $it") }
        }
    }
}
