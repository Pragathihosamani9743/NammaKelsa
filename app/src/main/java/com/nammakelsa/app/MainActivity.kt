package com.nammakelsa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NammaKelsaTheme {
                NammaKelsaApp()
            }
        }
    }
}

@Composable
fun NammaKelsaApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar only on these screens
    val showBottomBar = currentRoute in listOf("home", "bookings", "messages", "profile", "worker_home")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    // Check if current user is worker or customer to decide Home destination
                    // For simplicity, we can check if currentRoute is worker_home
                    val isWorkerSide = currentRoute == "worker_home"
                    
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentRoute == "home" || currentRoute == "worker_home",
                        onClick = {
                            // If we are on worker side, "Home" should go to worker_home
                            // If we are on customer side, "Home" should go to home
                            // If we are on Messages/Profile/Bookings, we need to know the role.
                            // A quick fix is to navigate to "home" and let the role check (which we should add) redirect if needed.
                            // Or better, just navigate to the dashboard they came from.
                            
                            // Let's use a simpler logic: if they were on worker_home, go back there.
                            // However, we can't easily know the role here without more state.
                            // Let's just navigate to "home" and we can add a redirect logic or just let workers see the marketplace too.
                            
                            // Standard approach: workers stay on worker_home.
                            // We can use a role state.

                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Bookings") },
                        label = { Text("Bookings") },
                        selected = currentRoute == "bookings",
                        onClick = {
                            navController.navigate("bookings") {
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Chat, contentDescription = "Messages") },
                        label = { Text("Messages") },
                        selected = currentRoute == "messages",
                        onClick = {
                            navController.navigate("messages") {
                                launchSingleTop = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = currentRoute == "profile",
                        onClick = {
                            navController.navigate("profile") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "welcome",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("welcome") {
                WelcomeScreen(navController)
            }
            composable("login") {
                LoginScreen(
                    onLoginClick = { role ->
                        val destination = if (role == "worker") "worker_profile" else "home"
                        navController.navigate(destination) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onSignupClick = {
                        navController.navigate("signup")
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            composable("signup") {
                SignupScreen(
                    onSignupClick = { role ->
                        val destination = if (role == "worker") "worker_profile" else "home"
                        navController.navigate(destination) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onLoginClick = {
                        navController.popBackStack()
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
            composable("worker_profile") {
                WorkerProfileScreen(
                    onSaveSuccess = {
                        navController.navigate("worker_home") {
                            popUpTo("worker_profile") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                // Add a role check to redirect workers to worker_home if they land on "home"
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val userId = auth.currentUser?.uid ?: ""
                
                LaunchedEffect(userId) {
                    if (userId.isNotEmpty()) {
                        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
                            if (doc.getString("role") == "worker") {
                                navController.navigate("worker_home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        }
                    }
                }
                HomeScreen(navController)
            }
            composable("bookings") {
                BookingsScreen()
            }
            composable("messages") {
                MessagesScreen(navController)
            }
            composable("profile") {
                ProfileScreen(navController)
            }
            composable(
                route = "worker_profile_view/{workerId}",
                arguments = listOf(navArgument("workerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val workerId = backStackEntry.arguments?.getString("workerId") ?: ""
                WorkerProfileViewScreen(navController, workerId)
            }
            composable(
                route = "chat_screen/{chatId}",
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                ChatScreen(navController, chatId)
            }
            composable("worker_home") {
                WorkerDashboard(navController)
            }
        }
    }
}
