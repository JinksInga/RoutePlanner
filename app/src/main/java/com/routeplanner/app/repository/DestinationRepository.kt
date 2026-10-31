package com.routeplanner.app.repository

import com.routeplanner.app.data.database.dao.DestinationDao
import com.routeplanner.app.data.database.entity.DestinationEntity
import kotlinx.coroutines.flow.Flow

class DestinationRepository(
    private val destinationDao: DestinationDao
) {

    fun getAllDestinations(): Flow<List<DestinationEntity>> {
        return destinationDao.getAll()
    }

    suspend fun insert(destination: DestinationEntity): Long {
        return destinationDao.insert(destination)
    }

    suspend fun update(destination: DestinationEntity) {
        destinationDao.update(destination)
    }

    suspend fun delete(destination: DestinationEntity) {
        destinationDao.delete(destination)
    }

    fun searchDestinations(query: String): Flow<List<DestinationEntity>> {
        return destinationDao.search(query)
    }

    suspend fun getDestination(id: Int): DestinationEntity? {
        return destinationDao.getById(id)
    }
}
/*
* El repositorio será el intermediario entre la base de datos y el resto de la aplicación.
* */