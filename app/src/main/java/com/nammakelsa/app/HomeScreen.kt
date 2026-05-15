package com.nammakelsa.app

import android.annotation.SuppressLint
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.location.Geocoder
import java.util.Locale

data class Worker(
    val id: String = "",
    val name: String = "",
    val skill: String = "",
    val rate: String = "",
    val phone: String = "",
    val available: Boolean = false,
    val imageUrl: String = "",
    val galleryImages: List<String> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val experience: String = "5 Years",
    val jobsDone: String = "150+",
    val city: String = "Bangalore",
    var distance: Float = 0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: androidx.navigation.NavController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    var workerList by remember { mutableStateOf(listOf<Worker>()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLon by remember { mutableStateOf<Double?>(null) }
    var userAddress by remember { mutableStateOf("Bengaluru, Karnataka") }

    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf(
        CategoryItem("Painter", Icons.Default.Brush),
        CategoryItem("Plumber", Icons.Default.Build),
        CategoryItem("Electrician", Icons.Default.FlashOn),
        CategoryItem("Gardener", Icons.Default.Park),
        CategoryItem("Cleaner", Icons.Default.CleaningServices)
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getUserLocation(fusedLocationClient) { lat, lon ->
                userLat = lat
                userLon = lon
                fetchAddress(context, lat, lon) { address ->
                    userAddress = address
                }
            }
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getUserLocation(fusedLocationClient) { lat, lon ->
                userLat = lat
                userLon = lon
                fetchAddress(context, lat, lon) { address ->
                    userAddress = address
                }
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        db.collection("workers")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    workerList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Worker::class.java)?.copy(id = doc.id)
                    }
                }
                isLoading = false
            }
    }

    val filteredList = remember(workerList, searchQuery) {
        workerList.filter { worker ->
            worker.name.contains(searchQuery, ignoreCase = true) || 
            worker.skill.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- TOP SECTION: LOCATION ---
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                getUserLocation(fusedLocationClient) { lat, lon ->
                                    userLat = lat
                                    userLon = lon
                                    fetchAddress(context, lat, lon) { address ->
                                        userAddress = address
                                    }
                                }
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.LocationOn, 
                            contentDescription = null, 
                            tint = Color(0xFF2C3E90),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = userAddress,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown, 
                            contentDescription = null, 
                            tint = Color.Black
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // --- SEARCH BAR ---
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search for a service...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }
            }

            // --- CATEGORIES: POPULAR SERVICES ---
            item {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    Text(
                        text = "Popular Services",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(categories) { category ->
                            CategoryCard(category)
                        }
                    }
                }
            }

            // --- WORKER LIST: TOP RATED WORKERS ---
            item {
                Text(
                    text = "Top Rated Workers",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF2C3E90))
                    }
                }
            } else if (filteredList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No workers available", color = Color.Gray)
                    }
                }
            } else {
                val auth = FirebaseAuth.getInstance()
                val currentUserId = auth.currentUser?.uid ?: ""
                
                items(filteredList) { worker ->
                    WorkerCardRedesign(
                        worker = worker,
                        onClick = {
                            navController.navigate("worker_profile_view/${worker.id}")
                        },
                        onMessageClick = {
                            if (currentUserId.isNotEmpty()) {
                                val chatId = listOf(currentUserId, worker.id).sorted().joinToString("_")
                                val chatRef = db.collection("chats").document(chatId)
                                
                                chatRef.get().addOnSuccessListener { doc ->
                                    if (!doc.exists()) {
                                        // Fetch current user name for the chat metadata
                                        db.collection("users").document(currentUserId).get().addOnSuccessListener { userDoc ->
                                            val currentUserName = userDoc.getString("name") ?: "Customer"
                                            val chatData = hashMapOf(
                                                "participants" to listOf(currentUserId, worker.id),
                                                "workerName" to worker.name,
                                                "userName" to currentUserName, // Store customer name
                                                "workerId" to worker.id,
                                                "userId" to currentUserId,
                                                "createdAt" to com.google.firebase.Timestamp.now()
                                            )
                                            chatRef.set(chatData).addOnSuccessListener {
                                                navController.navigate("chat_screen/$chatId")
                                            }
                                        }
                                    } else {
                                        navController.navigate("chat_screen/$chatId")
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Please login to message", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCard(category: CategoryItem) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = category.icon, 
                    contentDescription = category.name,
                    tint = Color(0xFF2C3E90),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = category.name, fontSize = 12.sp, color = Color.Black)
    }
}

@Composable
fun WorkerCardRedesign(worker: Worker, onClick: () -> Unit, onMessageClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: Profile Image
            if (worker.imageUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(worker.imageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = Color(0xFFF3F4F6)
                ) {
                    Icon(
                        Icons.Default.Person, 
                        contentDescription = null, 
                        modifier = Modifier.padding(16.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // CENTER: Details
            Column(modifier = Modifier.weight(1f)) {
                Text(text = worker.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = worker.skill, color = Color.Gray, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                    Text(text = " ${String.format("%.1f", worker.rating)}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            // RIGHT: Rate + Call + Message
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "₹${worker.rate}/day", fontWeight = FontWeight.Bold, color = Color(0xFF2C3E90))
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    IconButton(
                        onClick = onMessageClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF6C4AB6), CircleShape)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "Message", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${worker.phone}")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF2C3E90), CircleShape)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

data class CategoryItem(val name: String, val icon: ImageVector)

@SuppressLint("MissingPermission")
private fun getUserLocation(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocationReceived: (Double, Double) -> Unit
) {
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            onLocationReceived(location.latitude, location.longitude)
        }
    }
}

private fun fetchAddress(
    context: android.content.Context,
    lat: Double,
    lon: Double,
    onAddressReceived: (String) -> Unit
) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val locality = address.locality ?: ""
            val subLocality = address.subLocality ?: ""
            val city = address.adminArea ?: ""
            
            val displayAddress = when {
                subLocality.isNotEmpty() && locality.isNotEmpty() -> "$subLocality, $locality"
                locality.isNotEmpty() -> locality
                else -> city
            }
            onAddressReceived(displayAddress.ifEmpty { "Location found" })
        } else {
            onAddressReceived("Address not found")
        }
    } catch (e: Exception) {
        onAddressReceived("Bengaluru, Karnataka")
    }
}
