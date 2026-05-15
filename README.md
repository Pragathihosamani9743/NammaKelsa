# NammaKelsa 👷‍♂️🔧

NammaKelsa is a local labor marketplace Android application that connects customers directly with skilled workers such as painters, plumbers, electricians, gardeners, and cleaners.

The app helps daily wage workers create a digital identity and allows customers to discover nearby workers easily without middlemen.

---

## 🚀 Features

### 👤 Authentication
- Firebase Email Authentication
- Login & Signup
- Role-based access:
  - Customer
  - Worker

---

### 🛠 Worker Features
- Create/Edit Worker Profile
- Upload Profile Photo
- Upload Work Gallery Images
- Add Skills
- Set Daily Rate
- Toggle Availability
- Add Location
- Real-time profile updates

---

### 🏠 Customer Features
- Search workers
- Filter by skills
- View worker profiles
- Call workers directly
- Book workers
- View bookings
- Send messages

---

### 💬 Chat System
- Real-time messaging using Firebase Firestore
- Customer ↔ Worker communication

---

### 📍 Location Features
- GPS-based location fetching
- Nearby worker discovery

---

## 📱 Screens

- Welcome Screen
- Login Screen
- Signup Screen
- Home Screen
- Worker Dashboard
- Worker Profile
- Booking Screen
- Message Screen

---

## 🧰 Tech Stack

### Frontend
- Kotlin
- Jetpack Compose
- Material 3

### Backend
- Firebase Authentication
- Firebase Firestore
- Firebase Storage

### Other Libraries
- Coil (Image Loading)
- Google Play Services Location

---

## 🔥 Firebase Services Used

- Authentication
- Firestore Database
- Firebase Storage

---

## 📂 Project Structure

```plaintext
com.nammakelsa.app
│
├── MainActivity.kt
├── LoginScreen.kt
├── SignupScreen.kt
├── HomeScreen.kt
├── WorkerProfileScreen.kt
├── WorkerDashboard.kt
├── BookingScreen.kt
├── MessageScreen.kt
└── theme/
