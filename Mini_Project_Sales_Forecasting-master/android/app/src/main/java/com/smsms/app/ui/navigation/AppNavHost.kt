package com.smsms.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smsms.app.auth.AuthManager
import com.smsms.app.ui.auth.LoginScreen
import com.smsms.app.ui.forecast.ForecastScreen
import com.smsms.app.ui.orders.CreateOrderScreen
import com.smsms.app.ui.orders.OrderDetailScreen
import com.smsms.app.ui.orders.OrderListScreen

object Routes {
    const val LOGIN = "login"
    const val ORDER_LIST = "orders"
    const val CREATE_ORDER = "orders/create"
    const val ORDER_DETAIL = "orders/{orderId}"
    const val FORECAST = "forecast"
    fun orderDetail(orderId: Int) = "orders/$orderId"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    // isReady is false only for the brief moment it takes to read the
    // persisted token off disk at app start — waiting for it avoids
    // flashing the login screen for an instant before an already-logged-
    // in user's session loads.
    val isReady by AuthManager.isReady.collectAsState()
    val session by AuthManager.session.collectAsState()

    if (!isReady) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (session != null) Routes.ORDER_LIST else Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.ORDER_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.ORDER_LIST) {
            OrderListScreen(
                onOrderClick = { orderId -> navController.navigate(Routes.orderDetail(orderId)) },
                onCreateOrderClick = { navController.navigate(Routes.CREATE_ORDER) },
                onForecastClick = { navController.navigate(Routes.FORECAST) }
            )
        }
        composable(Routes.FORECAST) {
            ForecastScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CREATE_ORDER) {
            CreateOrderScreen(onOrderCreated = { navController.popBackStack() })
        }
        composable(
            route = Routes.ORDER_DETAIL,
            arguments = listOf(navArgument("orderId") { type = NavType.IntType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getInt("orderId") ?: return@composable
            OrderDetailScreen(orderId = orderId, onBack = { navController.popBackStack() })
        }
    }

    // Handles logout happening from ANYWHERE past the login screen — a
    // deliberate "Log out" tap, or AuthInterceptor auto-clearing the
    // session after the backend returns 401 (e.g. an expired token).
    // Without this, the app would just keep showing stale screens while
    // every API call silently failed.
    LaunchedEffect(session) {
        if (session == null && navController.currentDestination?.route != Routes.LOGIN) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}
