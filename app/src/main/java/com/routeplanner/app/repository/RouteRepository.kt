package com.routeplanner.app.repository

import androidx.room.withTransaction
import com.routeplanner.app.data.database.RouteDatabase
import com.routeplanner.app.data.database.dao.RouteDao
import com.routeplanner.app.data.database.dao.RouteStopDao
import com.routeplanner.app.data.database.entity.RouteEntity
import com.routeplanner.app.data.database.entity.RouteStopEntity
import kotlinx.coroutines.flow.Flow

class RouteRepository(
    private val routeDao: RouteDao,
    private val routeStopDao: RouteStopDao,
    private val database: RouteDatabase
) {

    // Guardar una nueva ruta
    suspend fun insert(route: RouteEntity): Long {
        return database.withTransaction {
            routeDao.insert(route)
        }
    }

    // Modificar una ruta
    suspend fun update(route: RouteEntity) {
        routeDao.update(route)
    }

    // Eliminar una ruta
    suspend fun delete(route: RouteEntity) {
        routeDao.delete(route)
    }

    // Obtener todas las rutas
    fun getAllRoutes(): Flow<List<RouteEntity>> {
        return routeDao.getAll()
    }

    // Obtener una ruta concreta
    suspend fun getRouteById(routeId: Int): RouteEntity? {
        return routeDao.getById(routeId)
    }

    // Obtener la ruta activa o pausada
    suspend fun getCurrentRoute(): RouteEntity? {
        return routeDao.getCurrentRoute()
    }

    // Obtener todas las rutas activas o pausadas
    fun getCurrentRoutes(): Flow<List<RouteEntity>> {
        return routeDao.getCurrentRoutes()
    }

    // Obtener el historial de rutas finalizadas
    fun getFinishedRoutes(): Flow<List<RouteEntity>> {
        return routeDao.getFinishedRoutes()
    }

    // Pausar una ruta
    suspend fun pauseRoute(routeId: Int) {
        routeDao.updateStatus(
            routeId = routeId,
            newStatus = "PAUSED"
        )
    }

    // Reanudar una ruta
    suspend fun resumeRoute(routeId: Int) {
        database.withTransaction {
            routeDao.pauseAllActiveRoutes()
            routeDao.updateStatus(routeId, "ACTIVE")
        }
    }

    // Finalizar una ruta
    suspend fun finishRoute(routeId: Int) {
        routeDao.updateStatus(
            routeId = routeId,
            newStatus = "FINISHED"
        )
    }

    // Comprobar si existe una ruta activa o pausada
    suspend fun hasCurrentRoute(): Boolean {
        return routeDao.countCurrentRoutes() > 0
    }

    suspend fun createRouteWithStops(
        route: RouteEntity,
        stops: List<RouteStopEntity>
    ): Long {
        return database.withTransaction {

            routeDao.pauseAllActiveRoutes()

            val routeId = routeDao.insert(route)
            val routeStops = stops.map { stop ->
                stop.copy(
                    id = 0,
                    routeId = routeId.toInt()
                )
            }
            routeStopDao.insertAll(routeStops)
            routeId
        }
    }
}
/*
* ¿Qué hemos conseguido?
El repositorio nos permitirá:
- Crear rutas comprobando que no exista otra activa o pausada.
- Consultar rutas guardadas e historial.
- Recuperar la ruta en curso.
- Pausar, reanudar y finalizar recorridos.
- Eliminar rutas y sus paradas asociadas gracias a la relación que configuramos en Room.
* Además, utilizamos withTransaction para que la comprobación y la inserción de una ruta
  se realicen dentro de una misma transacción.
* Una nota: esta comprobación protege las creaciones realizadas mediante este repositorio,
  pero más adelante también tendremos que centralizar las operaciones de reanudación para
  garantizar que nunca se active una segunda ruta.
**/