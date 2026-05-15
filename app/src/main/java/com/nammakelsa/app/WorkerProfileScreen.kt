package com.nammakelsa.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkerProfileScreen(
    onSaveSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var skill by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var jobsDone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var available by remember { mutableStateOf(true) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var galleryUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationStatus by remember { mutableStateOf("Not captured") }

    var skillsList by remember { mutableStateOf(listOf<String>()) }
    var newSkillName by remember { mutableStateOf("") }
    var isAddingNewSkill by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val currentCount = galleryUris.size
        val availableSlots = 3 - currentCount
        if (availableSlots > 0) {
            galleryUris = (galleryUris + uris.take(availableSlots)).take(3)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            locationStatus = "Fetching..."
            captureLocation(fusedLocationClient, context) { lat, lon ->
                latitude = lat
                longitude = lon
                locationStatus = "Location Tagged"
            }
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        db.collection("skills")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    skillsList = snapshot.documents.map { it.getString("skillName") ?: "" }.filter { it.isNotEmpty() }
                }
            }
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Edit Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF2C3E90),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- REQUIREMENT 1: PROFILE IMAGE WITH EDIT OVERLAY ---
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.size(120.dp)
        ) {
            val painter = if (imageUri != null) {
                rememberAsyncImagePainter(imageUri)
            } else {
                painterResource(id = R.drawable.profile)
            }
            
            Image(
                painter = painter,
                contentDescription = "Profile Image",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .background(Color.White),
                contentScale = ContentScale.Crop
            )
            
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { imageLauncher.launch("image/*") },
                color = Color(0xFF2C3E90),
                shadowElevation = 4.dp
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Photo",
                    modifier = Modifier.padding(8.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- REQUIREMENT 2: INPUT FIELDS ---
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2C3E90),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2C3E90),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- REQUIREMENT 3: SELECTABLE SKILLS CHIPS ---
        Text(
            text = "Select Primary Skill",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            skillsList.forEach { skillOption ->
                FilterChip(
                    selected = skill == skillOption,
                    onClick = { skill = skillOption; isAddingNewSkill = false },
                    label = { Text(skillOption) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2C3E90),
                        selectedLabelColor = Color.White
                    )
                )
            }
            
            AssistChip(
                onClick = { isAddingNewSkill = true; skill = "" },
                label = { Text("+ Custom") },
                shape = RoundedCornerShape(20.dp)
            )
        }

        if (isAddingNewSkill) {
            OutlinedTextField(
                value = newSkillName,
                onValueChange = { newSkillName = it },
                label = { Text("Enter New Skill") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = { isAddingNewSkill = false }) {
                        Icon(Icons.Default.Clear, contentDescription = "Cancel", tint = Color.Red)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = rate,
            onValueChange = { rate = it },
            label = { Text("Daily Rate (₹)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2C3E90),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = experience,
            onValueChange = { experience = it },
            label = { Text("Experience (e.g. 5 Years)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2C3E90),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = jobsDone,
            onValueChange = { jobsDone = it },
            label = { Text("Jobs Done (e.g. 150+)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2C3E90),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("City (e.g. Bangalore)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2C3E90),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- REQUIREMENT 5: GALLERY WITH PREVIEW AND ADD BUTTON ---
        Text(
            text = "Work Gallery (Max 3)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            galleryUris.forEach { uri ->
                Box(contentAlignment = Alignment.TopEnd) {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { galleryUris = galleryUris.filter { it != uri } },
                        modifier = Modifier
                            .size(24.dp)
                            .offset(x = 8.dp, y = (-8).dp)
                    ) {
                        Surface(shape = CircleShape, color = Color.Red) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            
            if (galleryUris.size < 3) {
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { galleryLauncher.launch("image/*") },
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Add Photo", tint = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- REQUIREMENT 4: AVAILABILITY TOGGLE ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Available today", fontWeight = FontWeight.Medium)
            Switch(
                checked = available,
                onCheckedChange = { available = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2C3E90))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Location Tag Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, null, tint = Color(0xFF6C4AB6))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Service Location", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(locationStatus, fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                    Text("Tag", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- REQUIREMENT 6: FULL WIDTH ROUNDED SAVE BUTTON ---
        if (isUploading) {
            CircularProgressIndicator(color = Color(0xFF2C3E90))
        } else {
            Button(
                onClick = {
                    val finalSkill = if (isAddingNewSkill) newSkillName else skill
                    
                    if (name.isNotEmpty() && phone.isNotEmpty() && finalSkill.isNotEmpty() && rate.isNotEmpty() && latitude != null) {
                        val userId = auth.currentUser?.uid ?: ""
                        isUploading = true

                        scope.launch {
                            try {
                                var finalImageUrl = ""
                                val userId = auth.currentUser?.uid ?: ""
                                
                                if (userId.isEmpty()) {
                                    Toast.makeText(context, "Error: User ID not found", Toast.LENGTH_SHORT).show()
                                    isUploading = false
                                    return@launch
                                }

                                // Step 1: Upload Profile Image safely
                                if (imageUri != null) {
                                    val imageRef = storage.reference.child("worker_images/$userId.jpg")
                                    
                                    // Use continueWithTask to ensure downloadUrl is only called after upload finishes
                                    val urlTask = imageRef.putFile(imageUri!!).continueWithTask { task ->
                                        if (!task.isSuccessful) task.exception?.let { throw it }
                                        imageRef.downloadUrl
                                    }
                                    finalImageUrl = urlTask.await().toString()
                                }

                                // Step 2: Upload Gallery Images safely
                                val galleryUrls = mutableListOf<String>()
                                for ((index, uri) in galleryUris.withIndex()) {
                                    val ref = storage.reference.child("worker_gallery/$userId/image${index + 1}.jpg")
                                    val gUrlTask = ref.putFile(uri).continueWithTask { task ->
                                        if (!task.isSuccessful) task.exception?.let { throw it }
                                        ref.downloadUrl
                                    }
                                    galleryUrls.add(gUrlTask.await().toString())
                                }

                                // Step 3: Save to Firestore
                                saveWorkerDataWithGallery(
                                    db, userId, name, phone, finalSkill, rate, available, 
                                    finalImageUrl, galleryUrls, latitude!!, longitude!!, 
                                    experience, jobsDone, city,
                                    isAddingNewSkill, onSaveSuccess, context
                                )
                            } catch (e: Exception) {
                                Toast.makeText(context, "Upload Error: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isUploading = false
                            }
                        }
                    } else {
                        if (latitude == null) {
                            Toast.makeText(context, "Please tag your location first", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E90))
            ) {
                Text("Save Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@SuppressLint("MissingPermission")
private fun captureLocation(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    context: android.content.Context,
    onLocationCaptured: (Double, Double) -> Unit
) {
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            onLocationCaptured(location.latitude, location.longitude)
        } else {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { freshLocation ->
                    if (freshLocation != null) {
                        onLocationCaptured(freshLocation.latitude, freshLocation.longitude)
                    } else {
                        Toast.makeText(context, "GPS Signal weak", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}

private fun saveWorkerDataWithGallery(
    db: FirebaseFirestore,
    userId: String,
    name: String,
    phone: String,
    skill: String,
    rate: String,
    available: Boolean,
    imageUrl: String,
    galleryImages: List<String>,
    lat: Double,
    lon: Double,
    experience: String,
    jobsDone: String,
    city: String,
    isNewSkill: Boolean,
    onSuccess: () -> Unit,
    context: android.content.Context
) {
    if (isNewSkill) {
        db.collection("skills").add(hashMapOf("skillName" to skill))
    }

    val workerData = hashMapOf(
        "name" to name,
        "phone" to phone,
        "skill" to skill,
        "rate" to rate,
        "available" to available,
        "imageUrl" to imageUrl,
        "galleryImages" to galleryImages,
        "latitude" to lat,
        "longitude" to lon,
        "experience" to experience,
        "jobsDone" to jobsDone,
        "city" to city
    )

    db.collection("workers").document(userId).set(workerData)
        .addOnSuccessListener {
            Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
            onSuccess()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Database Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}
