package com.illareklab.demodata

import android.app.Application
import com.illareklab.demodata.data.local.AppDatabase
import com.illareklab.demodata.data.repository.GpsRepository
import com.illareklab.demodata.data.session.SessionManager

class DemoDataApp : Application() {

    // La BD se crea una sola vez cuando se accede por primera vez
    val database by lazy { AppDatabase.getDatabase(this) }

    // El repositorio se construye sobre la misma instancia de la BD
    val gpsRepository by lazy {
        GpsRepository(database.gpsGoogleDao(), database.gpsSensorsDao())
    }

    // SessionManager también vive aquí para no duplicarlo en MainActivity
    val sessionManager by lazy { SessionManager(this) }
}