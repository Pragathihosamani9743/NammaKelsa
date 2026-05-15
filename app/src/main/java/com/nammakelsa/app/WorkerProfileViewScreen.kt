package com.nammakelsa.app

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProfileViewScreen(navController: NavController, workerId: String) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    var worker by remember { mutableStateOf<Worker?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isBooking by remember { mutableStateOf(false) }

    LaunchedEffect(workerId) {
        db.collection("workers").document(workerId).get()
            .addOnSuccessListener { document ->
                worker = document.toObject(Worker::class.java)?.copy(id = document.id)
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Worker Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            worker?.let { w ->
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
                                text = "₹${w.rate} / day",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF2C3E90)
                            )
                            Text(text = "Daily Rate", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        
                        if (isBooking) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF2C3E90))
                        } else {
                            Button(
                                onClick = {
                                    val currentUser = auth.currentUser
                                    if (currentUser == null) {
                                        Toast.makeText(context, "Please login to book", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    isBooking = true
                                    val currentDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                                    val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

                                    val bookingData = hashMapOf(
                                        "userId" to currentUser.uid,
                                        "workerId" to w.id,
                                        "workerName" to w.name,
                                        "skill" to w.skill,
                                        "date" to currentDate,
                                        "time" to currentTime,
                                        "status" to "Upcoming"
                                    )

                                    db.collection("bookings")
                                        .add(bookingData)
                                        .addOnSuccessListener {
                                            isBooking = false
                                            Toast.makeText(context, "Booking successful!", Toast.LENGTH_LONG).show()
                                            
                                            // Also open dialer for direct communication as before
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${w.phone}")
                                            }
                                            context.startActivity(intent)
                                            
                                            navController.navigate("bookings") {
                                                popUpTo("home")
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            isBooking = false
                                            Toast.makeText(context, "Booking failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                },
                                modifier = Modifier
                                    .height(50.dp)
                                    .width(160.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E90))
                            ) {
                                Text("Book Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
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
        } else if (worker == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Worker not found")
            }
        } else {
            val w = worker!!
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- HEADER ---
                if (w.imageUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(w.imageUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.White, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        color = Color(0xFFF3F4F6)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = w.name.take(1), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = w.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = w.skill,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF6C4AB6),
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                    Text(
                        text = " ${String.format("%.1f", w.rating)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(text = " (${w.ratingCount} reviews)", color = Color.Gray, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- STATS ROW ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatBox(modifier = Modifier.weight(1f), label = "Experience", value = "5 Years")
                    StatBox(modifier = Modifier.weight(1f), label = "Jobs Done", value = "200+")
                    StatBox(modifier = Modifier.weight(1f), label = "Location", value = "Bangalore")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- ABOUT SECTION ---
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "About", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Professional ${w.skill.lowercase()} with extensive experience in providing top-notch services. Dedicated to quality and customer satisfaction.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- SKILLS ---
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Skills", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(
                            onClick = { },
                            label = { Text(w.skill) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- WORK GALLERY ---
                if (w.galleryImages.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Work Gallery", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(w.galleryImages) { url ->
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
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
