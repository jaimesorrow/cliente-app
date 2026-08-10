package com.myclientscheduler.cliente.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.myclientscheduler.cliente.BuildConfig
import com.myclientscheduler.cliente.core.rbac.AppRoute
import com.myclientscheduler.cliente.core.rbac.PermissionPolicy
import com.myclientscheduler.cliente.ui.theme.ClienteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClienteTheme {
                val vm: MainViewModel = viewModel()
                ClienteApp(vm)
            }
        }
    }
}

@Composable
private fun ClienteApp(vm: MainViewModel) {
    val nav = rememberNavController()
    val user by vm.user.collectAsStateWithLifecycle()
    val offline by vm.offline.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (offline) Text("Offline mode: reads cached, writes queued/blocked.", modifier = Modifier.padding(12.dp))
            SafeNavHost(nav = nav, vm = vm)
        }
    }
}

@Composable
private fun SafeNavHost(nav: NavHostController, vm: MainViewModel) {
    val user by vm.user.collectAsStateWithLifecycle()
    NavHost(navController = nav, startDestination = AppRoute.Dashboard.path) {
        composable(AppRoute.Dashboard.path) {
            HubScreen("Clientè", listOf(
                AppRoute.Clients, AppRoute.Appointments, AppRoute.Calendar,
                AppRoute.Templates, AppRoute.Services, AppRoute.Availability,
                AppRoute.Analytics, AppRoute.Settings, AppRoute.Legal,
            ) + if (BuildConfig.DEBUG) listOf(AppRoute.QA) else emptyList(), nav, user.role.name)
        }
        composable(AppRoute.Clients.path) { BasicScreen("Clients CRM") }
        composable(AppRoute.Appointments.path) { BasicScreen("Appointments") }
        composable(AppRoute.Calendar.path) { BasicScreen("Calendar Day/Week") }
        composable(AppRoute.Templates.path) { BasicScreen("Templates") }
        composable(AppRoute.Services.path) { OwnerGuard(user = user.role.name, allowed = PermissionPolicy.canAccessRoute(AppRoute.Services, user)) }
        composable(AppRoute.Availability.path) { OwnerGuard(user = user.role.name, allowed = PermissionPolicy.canAccessRoute(AppRoute.Availability, user)) }
        composable(AppRoute.Analytics.path) { OwnerGuard(user = user.role.name, allowed = PermissionPolicy.canAccessRoute(AppRoute.Analytics, user)) }
        composable(AppRoute.Settings.path) { OwnerGuard(user = user.role.name, allowed = PermissionPolicy.canAccessRoute(AppRoute.Settings, user)) }
        composable(AppRoute.Legal.path) { LegalScreen() }
        composable(AppRoute.QA.path) { if (BuildConfig.DEBUG) DebugQaScreen(vm) else BasicScreen("Unavailable") }
    }
}

@Composable
private fun HubScreen(title: String, routes: List<AppRoute>, nav: NavHostController, role: String) {
    val entry by nav.currentBackStackEntryAsState()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(20.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text("Role: $role • Manage schedule with precision.")
        routes.forEach { route ->
            Button(onClick = { nav.navigate(route.path) }) { Text(route.path) }
        }
        Text("Current: ${entry?.destination?.route}")
    }
}

@Composable private fun BasicScreen(title: String) { Text(title, modifier = Modifier.padding(20.dp)) }

@Composable
private fun OwnerGuard(user: String, allowed: Boolean) {
    if (allowed) BasicScreen("Owner module") else Text("Owner only area. Your role: $user", modifier = Modifier.padding(20.dp))
}

@Composable
private fun LegalScreen() {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Terms v1.0 accepted timestamp stored at account level.")
        Text("Privacy v1.0 accepted timestamp stored at account level.")
        Text("Legal account entity: MyClientScheduler")
    }
}

@Composable
private fun DebugQaScreen(vm: MainViewModel) {
    val csv by vm.qaCsv.collectAsStateWithLifecycle()
    val filter by vm.qaFilter.collectAsStateWithLifecycle()
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("QA Matrix (Debug)")
        Text("Filter: $filter")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all", "security", "appointments", "navigation").forEach { key ->
                OutlinedButton(onClick = { vm.setQaFilter(key) }) { Text("Filter $key") }
            }
        }
        Text(csv)
        Button(onClick = { vm.copyQaCsv() }) { Text("Copy CSV") }
    }
}

fun openDialIntent(number: String): Intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
fun openSmsIntent(number: String): Intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
fun openEmailIntent(email: String): Intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
