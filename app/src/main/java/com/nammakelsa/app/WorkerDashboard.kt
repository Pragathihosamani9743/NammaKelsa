package com.nammakelsa.app

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDashboard(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val userId = auth.currentUser?.uid ?: ""

    var workerData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            db.collection("workers").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null && snapshot.exists()) {
                        workerData = snapshot.data
                    }
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2C3E90),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            workerData?.let { data ->
                val rate = data["rate"] as? String ?: "0"
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "₹$rate / day",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF2C3E90)
                            )
                            Text(text = "Your Daily Rate", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        // Worker sees "Edit Profile" as their main action
                        Button(
                            onClick = { navController.navigate("worker_profile") },
                            modifier = Modifier
                                .height(50.dp)
                                .width(160.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E90))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Profile", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2C3E90))
            }
        } else if (workerData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No Profile Found", color = Color.Gray)
                    Button(onClick = { navController.navigate("worker_profile") }) {
                        Text("Create Profile")
                    }
                }
            }
        } else {
            val name = workerData!!["name"] as? String ?: "No Name"
            val skill = workerData!!["skill"] as? String ?: "No Skill"
            val available = workerData!!["available"] as? Boolean ?: false
            val imageUrl = workerData!!["imageUrl"] as? String ?: ""
            val gallery = workerData!!["galleryImages"] as? List<String> ?: emptyList()
            val rating = (workerData!!["rating"] as? Number)?.toDouble() ?: 0.0
            val ratingCount = (workerData!!["ratingCount"] as? Number)?.toInt() ?: 0

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color(0xFFF3F4F6))
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { Spacer(modifier = Modifier.height(24.dp)) }

                // --- 1. PROFILE HEADER ---
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = if (imageUrl.isNotEmpty()) imageUrl else R.drawable.profile,
                                error = painterResource(id = R.drawable.profile)
                            ),
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.White, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(text = skill, color = Color(0xFF6C4AB6), fontWeight = FontWeight.Medium)
                        
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                            Text(text = " ${String.format("%.1f", rating)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = " ($ratingCount reviews)", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // --- AVAILABILITY TOGGLE (Added for dashboard functionality) ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Work Availability", fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (available) "Visible to customers" else "Hidden from search",
                                    fontSize = 12.sp,
                                    color = if (available) Color(0xFF2E7D32) else Color.Red
                                )
                            }
                            Switch(
                                checked = available,
                                onCheckedChange = { isChecked ->
                                    db.collection("workers").document(userId).update("available", isChecked)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2C3E90))
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // --- 2. STATS ROW ---
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatBox(modifier = Modifier.weight(1f), label = "Experience", value = "5 Years")
                        StatBox(modifier = Modifier.weight(1f), label = "Jobs Done", value = "150+")
                        StatBox(modifier = Modifier.weight(1f), label = "Location", value = "Bangalore")
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // --- 3. ABOUT SECTION ---
                item {
                    SectionCard(title = "About") {
                        Text(
                            text = "Professional $skill with a focus on high-quality work and reliability. Available for local tasks in your area.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // --- 4. SKILLS SECTION ---
                item {
                    SectionCard(title = "Skills") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuggestionChip(
                                onClick = { },
                                label = { Text(skill) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // --- 5. WORK GALLERY ---
                if (gallery.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(text = "Work Gallery", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(gallery) { url ->
                                    Image(
                                        painter = rememberAsyncImagePainter(url),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(150.dp)
                                            .clip(RoundedCornerShape(16.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
