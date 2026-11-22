package com.example.ui.hub

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.core.TigerStrings
import com.example.ui.theme.CobaltInfo
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TigerGold
import com.example.ui.theme.TigerOrange
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

private data class MapPinItem(
    val name: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double,
    val isFriend: Boolean
)

@SuppressLint("MissingPermission")
@Composable
fun TigerMapScreen(
    strings: TigerStrings,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var myLat by remember { mutableDoubleStateOf(24.7136) }
    var myLng by remember { mutableDoubleStateOf(46.6753) }
    var shareLiveLocation by remember { mutableStateOf(true) }
    var gpsStatus by remember { mutableStateOf("Riyadh Center (Tap GPS for live fix)") }

    val pins = remember(myLat, myLng) {
        listOf(
            MapPinItem("Omar Al-Mansoor", "Live Friend • 0.8 km away", myLat + 0.008, myLng + 0.006, true),
            MapPinItem("Layla Tech Hub", "Co-working & AI Cafe • 1.2 km", myLat - 0.007, myLng + 0.011, false),
            MapPinItem("Tiger Souq Plaza", "Verified Merchants • 2.1 km", myLat + 0.012, myLng - 0.009, false)
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    myLat = loc.latitude
                    myLng = loc.longitude
                    gpsStatus = "Live GPS: ${String.format("%.4f", myLat)}, ${String.format("%.4f", myLng)}"
                } else {
                    gpsStatus = "GPS enabled • awaiting satellite fix"
                }
            }
        } else {
            gpsStatus = "Location permission denied • showing default coordinates"
        }
    }

    val hasMapsApiKey = remember {
        val key = BuildConfig.MAPS_API_KEY
        key.isNotBlank() && !key.startsWith("YOUR_")
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(strings.hubTigerMap, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        // Map Viewport (Google Maps Compose SDK when API key configured, interactive GPS Radar fallback otherwise)
        Card(modifier = Modifier.fillMaxWidth().height(250.dp), shape = RoundedCornerShape(22.dp)) {
            if (hasMapsApiKey) {
                val cameraState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(LatLng(myLat, myLng), 13f)
                }
                GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraState) {
                    Marker(state = MarkerState(position = LatLng(myLat, myLng)), title = "You (Live)")
                    pins.forEach { p ->
                        Marker(state = MarkerState(position = LatLng(p.lat, p.lng)), title = p.name, snippet = p.subtitle)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B1324))) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        for (r in 1..4) {
                            drawCircle(Color(0xFF1E293B), radius = r * 55f, center = Offset(cx, cy), style = Stroke(2f))
                        }
                        drawLine(Color(0xFF1E293B), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1.5f)
                        drawLine(Color(0xFF1E293B), Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1.5f)
                        // User Live Pulse
                        drawCircle(TigerOrange.copy(alpha = 0.25f), radius = 34f, center = Offset(cx, cy))
                        drawCircle(TigerOrange, radius = 12f, center = Offset(cx, cy))
                        // Nearby Friends & Places
                        drawCircle(EmeraldSuccess, radius = 10f, center = Offset(cx + 85f, cy - 52f))
                        drawCircle(CobaltInfo, radius = 10f, center = Offset(cx - 95f, cy + 44f))
                        drawCircle(TigerGold, radius = 10f, center = Offset(cx + 70f, cy + 68f))
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                    ) {
                        Text(gpsStatus, color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }
        }

        // Controls
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ShareLocation, contentDescription = null, tint = EmeraldSuccess)
                Text(strings.pick("مشاركة موقعي المباشر مع الأصدقاء", "Share Live GPS with Friends"), style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = shareLiveLocation, onCheckedChange = { shareLiveLocation = it })
        }

        Button(
            onClick = {
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                )
            },
            modifier = Modifier.fillMaxWidth().testTag("locate_my_gps_btn")
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.pick("تحديث موقعي المباشر (GPS)", "Acquire Live GPS Location"))
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(pins) { pin ->
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (pin.isFriend) EmeraldSuccess else TigerOrange)
                            Column {
                                Text(pin.name, fontWeight = FontWeight.Bold)
                                Text(pin.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedButton(onClick = {
                            val uri = Uri.parse("geo:${pin.lat},${pin.lng}?q=${Uri.encode(pin.name)}")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }) {
                            Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.pick("اتجاهات", "Go"))
                        }
                    }
                }
            }
        }
    }
}
