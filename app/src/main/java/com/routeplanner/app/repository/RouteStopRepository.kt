package com.routeplanner.app.repository

import com.routeplanner.app.data.database.dao.RouteStopDao
import com.routeplanner.app.data.database.entity.RouteStopEntity
import kotlinx.coroutines.flow.Flow

class RouteStopRepository(
    private val routeStopDao: RouteStopDao
) {

    // Insertar una parada
    suspend fun insert(stop: RouteStopEntity): Long {
        return routeStopDao.insert(stop)
    }

    // Insertar todas las paradas de una ruta
    suspend fun insertAll(stops: List<RouteStopEntity>) {
        routeStopDao.insertAll(stops)
    }

    // Modificar una parada
    suspend fun update(stop: RouteStopEntity) {
        routeStopDao.update(stop)
    }

    // Eliminar una parada
    suspend fun delete(stop: RouteStopEntity) {
        routeStopDao.delete(stop)
    }

    // Obtener todas las paradas de una ruta
    fun getStopsByRoute(routeId: Int): Flow<List<RouteStopEntity>> {
        return routeStopDao.getStopsByRoute(routeId)
    }

    // Obtener las paradas pendientes
    fun getPendingStops(routeId: Int): Flow<List<RouteStopEntity>> {
        return routeStopDao.getPendingStops(routeId)
    }

    // Obtener las paradas completadas
    fun getCompletedStops(routeId: Int): Flow<List<RouteStopEntity>> {
        return routeStopDao.getCompletedStops(routeId)
    }

    // Recuperar una parada concreta
    suspend fun getStop(id: Int): RouteStopEntity? {
        return routeStopDao.getById(id)
    }

    // Confirmar que una parada se ha completado
    suspend fun completeStop(stopId: Int) {
        routeStopDao.completeStop(stopId)
    }

    // Volver a marcar una parada como pendiente
    suspend fun reopenStop(stopId: Int) {
        routeStopDao.reopenStop(stopId)
    }

    // Contar todas las paradas
    suspend fun countStops(routeId: Int): Int {
        return routeStopDao.countStops(routeId)
    }

    // Contar las paradas completadas
    suspend fun countCompletedStops(routeId: Int): Int {
        return routeStopDao.countCompletedStops(routeId)
    }
}