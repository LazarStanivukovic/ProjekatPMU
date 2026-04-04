package com.example.projekat.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Data class representing a selected location.
 */
data class LocationData(
    val lat: Double,
    val lng: Double,
    val name: String,
    val radius: Int = 100
)

/**
 * Location picker component for selecting a location via address search or current location.
 * No Google Maps API key required — uses Geocoder for address lookup.
 */
@Composable
fun LocationPicker(
    currentLocation: LocationData?,
    onLocationSelected: (LocationData?) -> Unit,
    modifier: Modifier = Modifier,
    titleColor: Color = Color.Unspecified,
    hintColor: Color = Color.Gray,
    iconTint: Color = Color.Gray,
    chipBg: Color = Color.White.copy(alpha = 0.6f)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Address>>(emptyList()) }
    var showSearchResults by remember { mutableStateOf(false) }
    var isGettingCurrentLocation by remember { mutableStateOf(false) }
    var radiusSlider by remember(currentLocation) { mutableFloatStateOf(currentLocation?.radius?.toFloat() ?: 100f) }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    // Permission launcher for fine location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            // Permission granted, get current location
            coroutineScope.launch {
                isGettingCurrentLocation = true
                val location = getCurrentLocation(context)
                if (location != null) {
                    val address = getAddressFromCoordinates(context, location.first, location.second)
                    onLocationSelected(
                        LocationData(
                            lat = location.first,
                            lng = location.second,
                            name = address ?: "Lat: ${location.first}, Lng: ${location.second}",
                            radius = radiusSlider.toInt()
                        )
                    )
                } else {
                    permissionDeniedMessage = "Nije moguce dobiti lokaciju"
                }
                isGettingCurrentLocation = false
            }
        } else {
            permissionDeniedMessage = "Dozvola za lokaciju je odbijena"
        }
    }

    // Background location permission launcher (for Android 10+)
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            showPermissionRationale = true
        }
    }

    Column(modifier = modifier) {
        // If location is already set, show it with option to remove
        if (currentLocation != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = chipBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentLocation.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = titleColor,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Radijus: ${currentLocation.radius}m",
                                style = MaterialTheme.typography.bodySmall,
                                color = hintColor
                            )
                        }
                        IconButton(
                            onClick = { onLocationSelected(null) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Ukloni lokaciju",
                                tint = iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Radius slider
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Radijus obavestenja: ${radiusSlider.toInt()}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = hintColor
                    )
                    Slider(
                        value = radiusSlider,
                        onValueChange = { radiusSlider = it },
                        onValueChangeFinished = {
                            onLocationSelected(currentLocation.copy(radius = radiusSlider.toInt()))
                        },
                        valueRange = 50f..500f,
                        steps = 8
                    )
                }
            }
        } else {
            // Search field and current location button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = chipBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Dodaj lokaciju za obavestenje...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = hintColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Address search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            showSearchResults = false
                        },
                        placeholder = { Text("Pretrazi adresu...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            }
                        }
                    )

                    // Search button
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    coroutineScope.launch {
                                        isSearching = true
                                        searchResults = searchAddress(context, searchQuery)
                                        showSearchResults = true
                                        isSearching = false
                                    }
                                }
                            },
                            enabled = searchQuery.isNotBlank() && !isSearching,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pretrazi")
                        }

                        // Current location button
                        Button(
                            onClick = {
                                val hasFineLocation = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                
                                val hasCoarseLocation = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasFineLocation || hasCoarseLocation) {
                                    coroutineScope.launch {
                                        isGettingCurrentLocation = true
                                        val location = getCurrentLocation(context)
                                        if (location != null) {
                                            val address = getAddressFromCoordinates(context, location.first, location.second)
                                            onLocationSelected(
                                                LocationData(
                                                    lat = location.first,
                                                    lng = location.second,
                                                    name = address ?: "Lat: ${location.first}, Lng: ${location.second}",
                                                    radius = 100
                                                )
                                            )
                                        }
                                        isGettingCurrentLocation = false
                                    }

                                    // Also request background location for geofencing (Android 10+)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        val hasBackgroundLocation = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                        
                                        if (!hasBackgroundLocation) {
                                            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                        }
                                    }
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            enabled = !isGettingCurrentLocation,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isGettingCurrentLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            } else {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Moja lokacija")
                            }
                        }
                    }

                    // Error message
                    permissionDeniedMessage?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Search results
                    AnimatedVisibility(visible = showSearchResults && searchResults.isNotEmpty()) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                text = "Rezultati pretrage:",
                                style = MaterialTheme.typography.labelSmall,
                                color = hintColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            searchResults.forEach { address ->
                                val addressLine = address.getAddressLine(0) ?: "Nepoznata adresa"
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable {
                                            onLocationSelected(
                                                LocationData(
                                                    lat = address.latitude,
                                                    lng = address.longitude,
                                                    name = addressLine,
                                                    radius = 100
                                                )
                                            )
                                            searchQuery = ""
                                            showSearchResults = false

                                            // Request background location for geofencing (Android 10+)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                val hasBackgroundLocation = ContextCompat.checkSelfPermission(
                                                    context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                                ) == PackageManager.PERMISSION_GRANTED

                                                if (!hasBackgroundLocation) {
                                                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                                }
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = iconTint
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = addressLine,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = titleColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // No results message
                    AnimatedVisibility(visible = showSearchResults && searchResults.isEmpty() && !isSearching) {
                        Text(
                            text = "Nema rezultata za \"$searchQuery\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = hintColor,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // Background location rationale dialog hint
        AnimatedVisibility(visible = showPermissionRationale) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Za lokacijska obavestenja u pozadini potrebna je dozvola \"Uvek dozvoli lokaciju\" u podesavanjima aplikacije.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

/**
 * Search for addresses using Geocoder.
 */
private suspend fun searchAddress(context: Context, query: String): List<Address> {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(query, 5) { addresses ->
                        continuation.resume(addresses)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, 5) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Get address string from coordinates using Geocoder.
 */
private suspend fun getAddressFromCoordinates(context: Context, lat: Double, lng: Double): String? {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine<String?> { continuation ->
                    geocoder.getFromLocation(lat, lng, 1) { addresses ->
                        val address = addresses.firstOrNull()?.getAddressLine(0)
                        continuation.resume(address)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                addresses?.firstOrNull()?.getAddressLine(0)
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Get current device location using FusedLocationProviderClient.
 */
private suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
    return withContext(Dispatchers.IO) {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val cancellationTokenSource = CancellationTokenSource()
            
            suspendCancellableCoroutine { continuation ->
                try {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { location ->
                        if (location != null) {
                            continuation.resume(Pair(location.latitude, location.longitude))
                        } else {
                            continuation.resume(null)
                        }
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                } catch (e: SecurityException) {
                    continuation.resume(null)
                }

                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
