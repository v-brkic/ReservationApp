package com.example.reservationapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

// -------------------- 1) Boje i tema (Bordò gradijent background) --------------------
private val BordeauxDark = Color(0xFF400010)
private val BordeauxMid = Color(0xFF800020)
private val BordeauxLight = Color(0xFFFFCDD2)

private val MyColorScheme = darkColorScheme(
    primary = BordeauxMid,
    onPrimary = Color.White,
    secondary = BordeauxMid,
    onSecondary = Color.White,
    background = Color(0xFF1A1A1A),
    onBackground = Color(0xFFEAEAEA),
    surface = Color(0xFF2B2B2B),
    onSurface = Color.White
)

// -------------------- 2) Model rezervacije --------------------
data class Reservation(
    val id: String = "",
    val date: Long = System.currentTimeMillis(),
    val clientName: String = "",
    val clientPhone: String = "",
    val notes: String = "",
    val numberOfCars: Int = 1,
    val carType: String = "",
    val status: String = "pending",  // "pending", "accepted", "declined", ...
    val done: Boolean = false
)

// -------------------- 3) Ekrani za admin --------------------
enum class Screen {
    HOME,
    CALENDAR,
    STATS
}

// -------------------- 4) MainActivity --------------------
class MainActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val COLLECTION_RESERVATIONS = "reservations"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Inicijaliziraj Firebase

        setContent {
            // Omotaj sve u MaterialTheme s our MyColorScheme
            MaterialTheme(colorScheme = MyColorScheme) {
                // Pozadinski linearni gradijent (vertikalni)
                val backgroundBrush = Brush.verticalGradient(
                    colors = listOf(BordeauxDark, BordeauxMid, BordeauxLight),
                    startY = 0f,
                    endY = 2000f
                )

                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundBrush)
                ) {
                    // Dohvati listu rezervacija
                    val (reservations, setReservations) = remember {
                        mutableStateOf<List<Reservation>>(emptyList())
                    }

                    LaunchedEffect(true) {
                        db.collection(COLLECTION_RESERVATIONS)
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) return@addSnapshotListener
                                val list = snapshot?.documents?.mapNotNull { doc ->
                                    doc.toObject(Reservation::class.java)?.copy(id = doc.id)
                                } ?: emptyList()
                                setReservations(list)
                            }
                    }

                    // Fiktivni login
                    var isLoggedIn by remember { mutableStateOf(false) }
                    var currentScreen by remember { mutableStateOf(Screen.HOME) }

                    if (!isLoggedIn) {
                        // GOST (klijent) - nova početna
                        GuestHomeScreen(
                            onReservationSubmitted = { addReservationToFirestore(it) },
                            onFakeAdminClick = { isLoggedIn = true }
                        )
                    } else {
                        // ADMIN
                        EmployerScaffold(
                            currentScreen = currentScreen,
                            onScreenChange = { currentScreen = it },
                            reservations = reservations,
                            onAccept = { updateStatus(it, "accepted") },
                            onDecline = { updateStatus(it, "declined") },
                            onToggleDone = { r, d -> updateDoneField(r, d) },
                            onLogout = { isLoggedIn = false }
                        )
                    }
                }
            }
        }
    }

    // Dodaj rezervaciju u Firestore
    private fun addReservationToFirestore(res: Reservation) {
        db.collection(COLLECTION_RESERVATIONS)
            .add(res)
            .addOnSuccessListener {
                Toast.makeText(this, "Rezervacija poslana!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Neuspjelo slanje.", Toast.LENGTH_SHORT).show()
            }
    }

    // Ažuriraj status
    private fun updateStatus(res: Reservation, newStatus: String) {
        if (res.id.isNotBlank()) {
            db.collection(COLLECTION_RESERVATIONS)
                .document(res.id)
                .update("status", newStatus)
                .addOnFailureListener {
                    Toast.makeText(this, "Neuspjelo ažuriranje statusa.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Ažuriraj "done"
    private fun updateDoneField(res: Reservation, done: Boolean) {
        if (res.id.isNotBlank()) {
            db.collection(COLLECTION_RESERVATIONS)
                .document(res.id)
                .update("done", done)
                .addOnFailureListener {
                    Toast.makeText(this, "Neuspjelo ažuriranje 'done'.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

// -------------------- 5) GuestHomeScreen s velikom slikom, ime firme, gradijent pozadina --------------------
@Composable
fun GuestHomeScreen(
    onReservationSubmitted: (Reservation) -> Unit,
    onFakeAdminClick: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 50.dp), // malo razmaka
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Slika - zamijeni s tvojim R.drawable ako imaš
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Naslovna slika",
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(16.dp))

        // Naziv
        Text(
            text = "Albus Solutions",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        // Slogan
        Text(
            text = "ambiciozan, temeljit i precizan",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
        )

        Spacer(Modifier.height(24.dp))

        // Gumb "Rezerviraj"
        Button(onClick = { showDialog = true }) {
            Text("Rezerviraj")
        }

        Spacer(Modifier.height(16.dp))

        // Fiktivni "Upad u admin"
        TextButton(onClick = onFakeAdminClick) {
            Icon(Icons.Filled.Login, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Prijava")
        }
    }

    if (showDialog) {
        ReservationDialog(
            onDismiss = { showDialog = false },
            onSubmit = {
                onReservationSubmitted(it)
                showDialog = false
            }
        )
    }
}

// -------------------- 6) ReservationDialog klijenta --------------------
@Composable
fun ReservationDialog(
    onDismiss: () -> Unit,
    onSubmit: (Reservation) -> Unit
) {
    val context = LocalContext.current
    var clientName by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var numberOfCars by remember { mutableStateOf("1") }
    var carType by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                selectedDate = LocalDate.of(y, m+1, d)
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Book your car now", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Ime i prezime") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = clientPhone,
                    onValueChange = { clientPhone = it },
                    label = { Text("Kontakt telefon") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = numberOfCars,
                    onValueChange = { numberOfCars = it },
                    label = { Text("Broj auta") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = carType,
                    onValueChange = { carType = it },
                    label = { Text("Vrsta/ime auta") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = selectedDate.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Datum rezervacije") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Bilješke (opcionalno)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (clientName.isBlank() || clientPhone.isBlank()) {
                    Toast.makeText(context, "Molimo ispunite ime i telefon.", Toast.LENGTH_SHORT).show()
                    return@TextButton
                }
                val cCount = numberOfCars.toIntOrNull() ?: 1
                val dateMillis = selectedDate
                    .atStartOfDay(ZoneId.systemDefault())
                    .toEpochSecond() * 1000

                val newRes = Reservation(
                    date = dateMillis,
                    clientName = clientName,
                    clientPhone = clientPhone,
                    notes = notes,
                    numberOfCars = cCount,
                    carType = carType
                )
                onSubmit(newRes)
            }) {
                Text("Pošalji")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Otkaži")
            }
        }
    )
}

// -------------------- 7) Scaffold za admin (bordò topBar i bottomBar) --------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerScaffold(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    reservations: List<Reservation>,
    onAccept: (Reservation) -> Unit,
    onDecline: (Reservation) -> Unit,
    onToggleDone: (Reservation, Boolean) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rezervacije - Admin") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Odjava", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BordeauxMid,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = BordeauxMid) {
                NavigationBarItem(
                    selected = currentScreen == Screen.HOME,
                    onClick = { onScreenChange(Screen.HOME) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Home") },
                    colors = navBarItemColors(currentScreen == Screen.HOME)
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.CALENDAR,
                    onClick = { onScreenChange(Screen.CALENDAR) },
                    icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    label = { Text("Calendar") },
                    colors = navBarItemColors(currentScreen == Screen.CALENDAR)
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.STATS,
                    onClick = { onScreenChange(Screen.STATS) },
                    icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                    label = { Text("Stats") },
                    colors = navBarItemColors(currentScreen == Screen.STATS)
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        ) {
            when (currentScreen) {
                Screen.HOME -> EmployerHomeScreen(reservations, onAccept, onDecline)
                Screen.CALENDAR -> CalendarScreenAdmin(
                    reservations = reservations,
                    onToggleDone = onToggleDone
                )
                Screen.STATS -> StatsScreen(reservations)
            }
        }
    }
}

// Pomoćna funkcija boja za bottom nav
@Composable
fun navBarItemColors(selected: Boolean): NavigationBarItemColors {
    val white = Color.White
    val unselected = white.copy(alpha = 0.7f)
    return NavigationBarItemDefaults.colors(
        selectedIconColor = white,
        selectedTextColor = white,
        unselectedIconColor = unselected,
        unselectedTextColor = unselected,
        indicatorColor = BordeauxMid.copy(alpha = 0.4f)
    )
}

// -------------------- 8) HOME (admin) - pending rezervacije --------------------
@Composable
fun EmployerHomeScreen(
    reservations: List<Reservation>,
    onAccept: (Reservation) -> Unit,
    onDecline: (Reservation) -> Unit
) {
    val pending = reservations.filter { it.status == "pending" }
    if (pending.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Trenutno nema novih upita za rezervaciju.")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(pending) { res ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Klijent: ${res.clientName}", style = MaterialTheme.typography.titleMedium)
                        Text("Tel: ${res.clientPhone}")

                        val localDate = getLocalDateFromMillis(res.date)
                        Text("Datum: $localDate")

                        Text("Broj auta: ${res.numberOfCars}")
                        Text("Vrsta auta: ${res.carType}")

                        AnimatedVisibility(res.notes.isNotEmpty()) {
                            Text("Bilješke: ${res.notes}")
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = { onAccept(res) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("Accept")
                            }
                            OutlinedButton(
                                onClick = { onDecline(res) },
                                border = BorderStroke(1.dp, Color.Red),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Text("Decline")
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------- 9) CalendarScreenAdmin (tjedni prikaz, današnji dan naglašen) --------------------
@Composable
fun CalendarScreenAdmin(
    reservations: List<Reservation>,
    onToggleDone: (Reservation, Boolean) -> Unit
) {
    // Generiramo 7 dana od danas
    val weekDays = (0 until 7).map { offset -> LocalDate.now().plusDays(offset.toLong()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    // Mjesečni pop-up - ako želiš, možeš ga zadržati ili izbaciti
    var showMonthDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Gornja ikona za Mjesečni prikaz (ako želiš)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { showMonthDialog = true }) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Month Calendar")
            }
        }

        Text(
            text = "Tjedni prikaz (7 dana)",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Listamo 7 dana
        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(weekDays) { day ->
                val dayRes = reservations.filter { getLocalDateFromMillis(it.date) == day }
                val isToday = (day == LocalDate.now())

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { selectedDay = day }
                        .border(
                            width = if (isToday) 2.dp else 1.dp,
                            color = if (isToday) Color.Cyan else Color.Gray,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isToday) "Danas: $day" else "$day",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isToday) Color.Cyan else MaterialTheme.colorScheme.onSurface
                        )
                        Text("Rezervacija: ${dayRes.size}")
                    }
                }
            }
        }
    }

    // Dijalog s rezervacijama
    if (selectedDay != null) {
        DailyReservationsDialog(
            day = selectedDay!!,
            reservations = reservations,
            onToggleDone = { res, checked -> onToggleDone(res, checked) },
            onDismiss = { selectedDay = null }
        )
    }

    // Eventualni mjesečni prikaz (ako želiš, ili makni)
    if (showMonthDialog) {
        MonthCalendarDialog(
            reservations = reservations,
            onToggleDone = onToggleDone,
            onDismiss = { showMonthDialog = false }
        )
    }
}

// Mjesečni pop-up (ostaviš ili brišeš, po želji)
@Composable
fun MonthCalendarDialog(
    reservations: List<Reservation>,
    onToggleDone: (Reservation, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    // Za primjer, generiramo 42 slot-a
    val now = LocalDate.now()
    val yearMonth = YearMonth.of(now.year, now.month)
    val cells = generateMonthCells(yearMonth)

    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column {
                Text("${yearMonth.month} ${yearMonth.year}", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))

                // Dani u tjednu
                val dayNames = listOf("Pon","Uto","Sri","Čet","Pet","Sub","Ned")
                Row(modifier = Modifier.fillMaxWidth()) {
                    dayNames.forEach {
                        Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 6 redova x 7 stupaca = 42
                val rows = cells.chunked(7)
                rows.forEach { rowCells ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowCells.forEach { dateOpt ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .padding(2.dp)
                                    .border(1.dp, Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dateOpt != null) {
                                    // Rezervacije za taj dan
                                    val dayRes = reservations.filter { getLocalDateFromMillis(it.date) == dateOpt }
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { selectedDay = dateOpt },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${dateOpt.dayOfMonth}\n(${dayRes.size})", textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zatvori")
            }
        }
    )

    if (selectedDay != null) {
        DailyReservationsDialog(
            day = selectedDay!!,
            reservations = reservations,
            onToggleDone = onToggleDone,
            onDismiss = { selectedDay = null }
        )
    }
}

// Funkcija za generiranje 42 slot-a
fun generateMonthCells(yearMonth: YearMonth): List<LocalDate?> {
    val firstOfMonth = yearMonth.atDay(1)
    val dayOfWeekValue = firstOfMonth.dayOfWeek.value // pon=1,... ned=7
    val daysBefore = (dayOfWeekValue - 1).coerceAtLeast(0)
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = 42

    val list = mutableListOf<LocalDate?>()
    repeat(daysBefore) { list.add(null) }
    for (d in 1..daysInMonth) {
        list.add(firstOfMonth.withDayOfMonth(d))
    }
    while (list.size < totalCells) {
        list.add(null)
    }
    return list
}

// -------------------- 10) Ekran Statistika --------------------
@Composable
fun StatsScreen(reservations: List<Reservation>) {
    val total = reservations.size
    val accepted = reservations.count { it.status == "accepted" }
    val declined = reservations.count { it.status == "declined" }
    val pending = reservations.count { it.status == "pending" }
    val doneCount = reservations.count { it.done }

    // Uspješnost
    val successRatio = if (total > 0) (accepted.toFloat() / total.toFloat()) * 100f else 0f
    val successRatioText = String.format("%.1f", successRatio)

    // “Zarada” - npr. 40€ po odrađenoj rezervaciji
    val totalEarnings = doneCount * 40.0
    val totalEarningsText = String.format("%.2f", totalEarnings)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NAPREDNA STATISTIKA",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        // 1) Osnovni pregled
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pregled Rezervacija",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                Text("• Ukupno: $total", fontWeight = FontWeight.SemiBold)
                Text("• Prihvaćeno: $accepted", fontWeight = FontWeight.SemiBold)
                Text("• Odbijeno: $declined", fontWeight = FontWeight.SemiBold)
                Text("• Na čekanju: $pending", fontWeight = FontWeight.SemiBold)
                Text("• Odrađeno: $doneCount", fontWeight = FontWeight.SemiBold)
            }
        }

        // 2) Uspješnost
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Uspješnost rezervacija",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Prihvaćeno $successRatioText%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4CAF50)
                    )
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = successRatio / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF4CAF50)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Od $total rezervacija, $accepted prihvaćeno.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // 3) Zarada
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ZARADA",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = Color(0xFFE53935) // crvenkasta boja za “brutalnost”
                )
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "$totalEarningsText €",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFFE53935)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Ukupno zarađeno od $doneCount odrađenih poslova",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}



// -------------------- 11) Dijalog s rezervacijama (po danu) --------------------
@Composable
fun DailyReservationsDialog(
    day: LocalDate,
    reservations: List<Reservation>,
    onToggleDone: (Reservation, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val dayRes = reservations.filter {
        getLocalDateFromMillis(it.date) == day
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rezervacije za $day", style = MaterialTheme.typography.titleLarge) },
        text = {
            if (dayRes.isEmpty()) {
                Text("Nema rezervacija za ovaj dan.")
            } else {
                LazyColumn {
                    items(dayRes) { res ->
                        ReservationItemWithDone(res) { checked ->
                            onToggleDone(res, checked)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

// Kartica s checkboxom "done"
@Composable
fun ReservationItemWithDone(
    res: Reservation,
    onToggleDone: (Boolean) -> Unit
) {
    val targetColor = if (res.done) Color.Gray else MaterialTheme.colorScheme.surface
    val animColor by animateColorAsState(targetValue = targetColor)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = animColor),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row {
                Checkbox(
                    checked = res.done,
                    onCheckedChange = { onToggleDone(it) },
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text("Klijent: ${res.clientName}", style = MaterialTheme.typography.titleMedium)
            }
            val localDate = getLocalDateFromMillis(res.date)
            Text("Datum: $localDate")
            Text("Auta: ${res.numberOfCars} [${res.carType}]")
            Text("Status: ${res.status}")
            if (res.notes.isNotEmpty()) {
                Text("Bilješke: ${res.notes}")
            }
        }
    }
}

// -------------------- 12) Utility: pretvorba epoch -> LocalDate --------------------
fun getLocalDateFromMillis(millis: Long): LocalDate {
    return Date(millis).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}