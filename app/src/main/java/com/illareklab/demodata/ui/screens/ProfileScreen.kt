package com.illareklab.demodata.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.illareklab.demodata.ui.viewmodel.ComparativeGpsRecord // Importación requerida
import com.illareklab.demodata.ui.viewmodel.GpsViewModel          // Importación requerida
import com.illareklab.demodata.ui.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.*

// Definición de las 3 sub-vistas internas del perfil usando sealed class
private sealed class ProfileViewState {
    object Menu       : ProfileViewState()
    object MyProfile  : ProfileViewState()
    object MyActivity : ProfileViewState()
}

data class OpcionPerfil(val id: Int, val titulo: String, val descripcion: String, val icono: ImageVector)

// ── PANTALLA RAÍZ: máquina de estados interna ──
@Composable
fun ProfileScreen(
    sessionVm: SessionViewModel,
    gpsVm: GpsViewModel, // 1. EJERCICIO 5: Se añade el ViewModel de GPS como parámetro
    onLogout: () -> Unit
) {
    var viewState by remember { mutableStateOf<ProfileViewState>(ProfileViewState.Menu) }
    val username by sessionVm.username.collectAsStateWithLifecycle()

    // 2. EJERCICIO 5: Recolectamos el historial unificado y comparativo en tiempo real
    val history by gpsVm.comparativeHistory.collectAsStateWithLifecycle()

    when (viewState) {
        is ProfileViewState.Menu -> ProfileMenu(
            username             = username ?: "Estudiante San Marcos",
            onNavigateToProfile  = { viewState = ProfileViewState.MyProfile },
            onNavigateToActivity = { viewState = ProfileViewState.MyActivity },
            onLogoutClick        = onLogout
        )
        is ProfileViewState.MyProfile -> MyProfileScreen(
            sessionVm = sessionVm,
            username  = username ?: "N/A",
            onBack    = { viewState = ProfileViewState.Menu }
        )
        is ProfileViewState.MyActivity -> MyActivityScreen(
            history = history, // 3. EJERCICIO 5: Se inyecta la lista real al historial
            onBack  = { viewState = ProfileViewState.Menu }
        )
    }
}

// ── SUB-PANTALLA 1: MENÚ PRINCIPAL DEL PERFIL ──
@Composable
private fun ProfileMenu(
    username: String,
    onNavigateToProfile: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }

    val opciones = remember {
        listOf(
            OpcionPerfil(1, "Mis datos",              "Información del estudiante y dispositivo", Icons.Default.Person),
            OpcionPerfil(2, "Historial de Actividad", "Registros consolidados del sistema",       Icons.Default.Receipt)
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // Cabecera con Avatar Circular
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(username, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("9no Ciclo — UNMSM", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        // Opciones de menú como Cards navegables
        opciones.forEach { opcion ->
            Card(
                onClick  = { if (opcion.id == 1) onNavigateToProfile() else onNavigateToActivity() },
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                ListItem(
                    headlineContent  = { Text(opcion.titulo,       fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(opcion.descripcion) },
                    leadingContent   = { Icon(opcion.icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent  = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                    colors           = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón de Cerrar Sesión semántico
        OutlinedButton(
            onClick  = { mostrarDialogo = true },
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar sesión")
        }
    }

    // AlertDialog de confirmación para evitar cierres accidentales
    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title   = { Text("¿Confirmar cierre de sesión?") },
            text    = { Text("Tus preferencias visuales del dispositivo se conservarán.") },
            confirmButton = {
                TextButton(onClick = { mostrarDialogo = false; onLogoutClick() }) {
                    Text("Sí, cerrar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
            }
        )
    }
}

// ── SUB-PANTALLA 2: MIS DATOS Y CONFIGURACIÓN DE MODO OSCURO ──
@Composable
private fun MyProfileScreen(sessionVm: SessionViewModel, username: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val isDarkModePref by sessionVm.isDarkMode.collectAsStateWithLifecycle()
    val isDark = isDarkModePref ?: isSystemInDarkTheme()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Regresar") }
            Text("Mis Datos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        // Metadatos pedagógicos del sistema y almacenamiento local
        ProfileMetadataItem("Nombre de Usuario",        username)
        ProfileMetadataItem("Rol de Acceso",            "Estudiante / Evaluador")
        ProfileMetadataItem("Directorio Local Interno", context.filesDir.absolutePath)
        ProfileMetadataItem("Fabricante del Equipo",    Build.MANUFACTURER.uppercase())
        ProfileMetadataItem("Modelo del Dispositivo",   Build.MODEL)
        ProfileMetadataItem("Versión de Android",       "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Card para el control del Modo Oscuro Persistente
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            ListItem(
                headlineContent   = { Text("Modo oscuro", fontWeight = FontWeight.SemiBold) },
                supportingContent = { Text("Forzar aspecto visual nocturno") },
                trailingContent   = {
                    Switch(
                        checked         = isDark,
                        onCheckedChange = { sessionVm.setDarkMode(it) }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }
    }
}

// ── SUB-PANTALLA 3: HISTORIAL DE ACTIVIDAD (POBLADO REAL - EJERCICIO 5) ──
@Composable
private fun MyActivityScreen(
    history: List<ComparativeGpsRecord>, // Recibe el estado unificado
    onBack: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Regresar") }
            Text("Historial de Actividad", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        // Si no se han tomado muestras en la base de datos de Room
        if (history.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text  = "No hay registros GNSS consolidados en la base de datos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // EJERCICIO 5: Lista eficiente (LazyColumn) mostrando el log consolidado
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier            = Modifier.fillMaxSize()
            ) {
                items(items = history, key = { it.timestamp }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Row(
                            modifier             = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text       = dateFormat.format(Date(item.timestamp)),
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text  = "FLP Lat: ${item.google?.latitude ?: "Buscando"} · Sens Lat: ${item.sensors?.latitude ?: "Sin Señal"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Indicador rápido de estado sincronizado
                            Text(
                                text       = "G: ${if(item.google != null) "✓" else "✗"} | S: ${if(item.sensors?.latitude != null) "✓" else "✗"}",
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── COMPONENTE AUXILIAR: FILA DE METADATO ──
@Composable
private fun ProfileMetadataItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
    }
}