package com.routeplanner.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.routeplanner.app.data.database.entity.RouteStopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteStopDao {

    // Insertar una parada
    @Insert
    suspend fun insert(stop: RouteStopEntity): Long

    // Insertar varias paradas
    @Insert
    suspend fun insertAll(stops: List<RouteStopEntity>)

    // Modificar una parada
    @Update
    suspend fun update(stop: RouteStopEntity)

    // Eliminar una parada
    @Delete
    suspend fun delete(stop: RouteStopEntity)

    // Obtener todas las paradas de una ruta, respetando su orden
    @Query("""
        SELECT * FROM route_stops
        WHERE routeId = :routeId
        ORDER BY stopOrder ASC
    """)
    fun getStopsByRoute(routeId: Int): Flow<List<RouteStopEntity>>

    // Obtener las paradas pendientes
    @Query("""
        SELECT * FROM route_stops
        WHERE routeId = :routeId
        AND isCompleted = 0
        ORDER BY stopOrder ASC
    """)
    fun getPendingStops(routeId: Int): Flow<List<RouteStopEntity>>

    // Obtener las paradas completadas, por orden de visita
    @Query("""
        SELECT * FROM route_stops
        WHERE routeId = :routeId
        AND isCompleted = 1
        ORDER BY completedAt ASC
    """)
    fun getCompletedStops(routeId: Int): Flow<List<RouteStopEntity>>

    // Obtener una parada concreta
    @Query("""
        SELECT * FROM route_stops
        WHERE id = :stopId
    """)
    suspend fun getById(stopId: Int): RouteStopEntity?

    // Marcar una parada como completada
    @Query("""
        UPDATE route_stops
        SET isCompleted = 1,
            completedAt = :completedAt
        WHERE id = :stopId
    """)
    suspend fun completeStop(
        stopId: Int,
        completedAt: Long = System.currentTimeMillis()
    )

    // Volver a marcar una parada como pendiente
    @Query("""
        UPDATE route_stops
        SET isCompleted = 0,
            completedAt = NULL
        WHERE id = :stopId
    """)
    suspend fun reopenStop(stopId: Int)

    // Contar paradas de una ruta
    @Query("""
        SELECT COUNT(*) FROM route_stops
        WHERE routeId = :routeId
    """)
    suspend fun countStops(routeId: Int): Int

    // Contar paradas completadas
    @Query("""
        SELECT COUNT(*) FROM route_stops
        WHERE routeId = :routeId
        AND isCompleted = 1
    """)
    suspend fun countCompletedStops(routeId: Int): Int
}
/*
* ¿Qué podremos hacer con este DAO?
- Recuperar todas las paradas respetando el orden original.
- Consultar pendientes y completadas por separado.
- Confirmar manualmente una parada.
- Volver a dejar una parada como pendiente si fuera necesario.
- Consultar cuántas paradas tiene una ruta y cuántas se han completado.
* Una ventaja importante es que las paradas completadas se ordenarán por fecha de visita,
  mientras que las pendientes conservarán el orden calculado de la ruta.
**/