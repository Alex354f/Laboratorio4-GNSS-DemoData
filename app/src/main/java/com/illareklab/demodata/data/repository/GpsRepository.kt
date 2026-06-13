package com.illareklab.demodata.data.repository

import com.illareklab.demodata.data.local.dao.GpsGoogleDao
import com.illareklab.demodata.data.local.dao.GpsSensorsDao
import com.illareklab.demodata.data.local.entity.GpsGoogleEntity
import com.illareklab.demodata.data.local.entity.GpsSensorsEntity
import kotlinx.coroutines.flow.Flow

class GpsRepository(
    private val googleDao: GpsGoogleDao,
    private val sensorsDao: GpsSensorsDao
) {
    val googlePoints: Flow<List<GpsGoogleEntity>> = googleDao.observeAll()
    val sensorsPoints: Flow<List<GpsSensorsEntity>> = sensorsDao.observeAll()

    val googleCount: Flow<Int> = googleDao.observeCount()
    val sensorsCount: Flow<Int> = sensorsDao.observeCount()

    suspend fun saveGooglePoint(point: GpsGoogleEntity) = googleDao.insert(point)
    suspend fun saveSensorsPoint(point: GpsSensorsEntity) = sensorsDao.insert(point)

    suspend fun clearAll() {
        googleDao.deleteAll()
        sensorsDao.deleteAll()
    }
}