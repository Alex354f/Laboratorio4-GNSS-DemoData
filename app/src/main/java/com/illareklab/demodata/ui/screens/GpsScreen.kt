package com.illareklab.demodata.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illareklab.demodata.ui.viewmodel.ComparativeGpsRecord
import com.illareklab.demodata.ui.viewmodel.GpsViewModel
import com.illareklab.demodata.services.GpsCaptureService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// Función matemática de Haversine para calcular la distancia (Ejercicio 3)
fun calcularHaversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0 // Radio de la Tierra en metros
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GpsScreen(viewModel: GpsViewModel) {
    val context = LocalContext.current
    val permisos = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val estadoPermisos = rememberMultiplePermissionsState(permissions = permisos)
    var capturando by remember { mutableStateOf(false) }

    val googlePoints  by viewModel.googlePoints.collectAsStateWithLifecycle()
    val sensorsPoints by viewModel.sensorsPoints.collectAsStateWithLifecycle()
    val history       by viewModel.comparativeHistory.collectAsStateWithLifecycle()

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!estadoPermisos.allPermissionsGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Se requieren permisos de ubicación.", color = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { estadoPermisos.launchMultiplePermissionRequest() }) { Text("Conceder permisos") }
                }
            }
            return@Column
        }

        // =============================================================
        // FILA DE BOTONES DE CONTROL (CAPTURAR + BOTÓN LIMPIAR HISTORIAL)
        // =============================================================
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    capturando = !capturando
                    val intent = Intent(context, GpsCaptureService::class.java)
                    if (capturando) context.startForegroundService(intent) else context.stopService(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (capturando) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(if (capturando) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (capturando) "Detener" else "Capturar (5s)")
            }

            // AQUÍ ESTÁ EL BOTÓN INTERFICIAL DE LIMPIEZA (EJERCICIO 4)
            FilledTonalButton(onClick = { viewModel.clearAllHistory() }) {
                Icon(Icons.Default.Delete, contentDescription = "Limpiar todo")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Limpiar")
            }
        }

        // Fila de Contadores Numéricos
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Google FLP", style = MaterialTheme.typography.titleSmall)
                    Text("${googlePoints.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sensores GNSS", style = MaterialTheme.typography.titleSmall)
                    Text("${sensorsPoints.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("Historial Comparativo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(items = history, key = { it.timestamp }) { record ->
                ComparativeCaptureCard(record, dateFormat)
            }
        }
    }
}

@Composable
fun ComparativeCaptureCard(record: ComparativeGpsRecord, dateFormat: SimpleDateFormat) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Instante: ${dateFormat.format(Date(record.timestamp))}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)

                // Muestra la distancia calculada por Haversine (Ejercicio 3)
                if (record.google?.latitude != null && record.sensors?.latitude != null) {
                    val dist = calcularHaversine(record.google.latitude, record.google.longitude ?: 0.0, record.sensors.latitude, record.sensors.longitude ?: 0.0)
                    Text("Δ d: ${String.format(Locale.getDefault(), "%.2f", dist)} m", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                // Panel Izquierdo: Google FLP
                Column(modifier = Modifier.weight(1f)) {
                    Text("GOOGLE FLP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (record.google != null) {
                        Text("Lat: ${record.google.latitude}", style = MaterialTheme.typography.bodySmall)
                        Text("Lon: ${record.google.longitude}", style = MaterialTheme.typography.bodySmall)
                        Text("Prec: ±${record.google.accuracy}m", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Buscando...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                // Panel Derecho: Sensores
                Column(modifier = Modifier.weight(1f)) {
                    Text("SENSOR GNSS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    if (record.sensors != null) {
                        if (record.sensors.latitude != null) {
                            Text("Lat: ${record.sensors.latitude}", style = MaterialTheme.typography.bodySmall)
                            Text("Lon: ${record.sensors.longitude}", style = MaterialTheme.typography.bodySmall)

                            // Mostrar satélites (Ejercicio 2)
                            Text("Satélites: ${record.sensors.satellites ?: "N/D"}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("SIN SEÑAL", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Text("Buscando...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}