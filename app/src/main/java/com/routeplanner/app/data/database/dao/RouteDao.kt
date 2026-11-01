package com.routeplanner.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.routeplanner.app.data.database.entity.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    // Crear una ruta
    @Insert
    suspend fun insert(route: RouteEntity): Long

    // Modificar una ruta
    @Update
    suspend fun update(route: RouteEntity)

    // Eliminar una ruta
    @Delete
    suspend fun delete(route: RouteEntity)

    // Obtener todas las rutas
    @Query("SELECT * FROM routes ORDER BY createdAt DESC")
    fun getAll(): Flow<List<RouteEntity>>

    // Obtener una ruta por su identificador
    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getById(id: Int): RouteEntity?

    // Obtener la ruta activa o pausada
    @Query("""
        SELECT * FROM routes
        WHERE status IN ('ACTIVE', 'PAUSED')
        LIMIT 1
    """)
    suspend fun getCurrentRoute(): RouteEntity?

    // Obtener rutas finalizadas
    @Query("""
        SELECT * FROM routes
        WHERE status = 'FINISHED'
        ORDER BY updatedAt DESC
    """)
    fun getFinishedRoutes(): Flow<List<RouteEntity>>

    // Cambiar el estado de una ruta
    @Query("""
        UPDATE routes
        SET status = :newStatus,
            updatedAt = :updatedAt
        WHERE id = :routeId
    """)
    suspend fun updateStatus(
        routeId: Int,
        newStatus: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    // Comprobar si existe una ruta activa o pausada
    @Query("""
        SELECT COUNT(*) FROM routes
        WHERE status IN ('ACTIVE', 'PAUSED')
    """)
    suspend fun countCurrentRoutes(): Int
}
/*
* ¿Qué funciones tendremos?
  Función	Utilidad
 - insert()	Guardar una ruta
 - update()	Modificar sus datos
 - delete()	Eliminar una ruta
 - getAll()	Consultar todas las rutas
 - getById()	Recuperar una ruta concreta
 - getCurrentRoute()	Recuperar la ruta activa o pausada
 - getFinishedRoutes()	Consultar el historial
 - updateStatus()	Pausar, reanudar o finalizar
 - countCurrentRoutes()	Comprobar si ya existe una ruta en curso
* Una consideración importante: la comprobación de que solo haya una ruta activa o pausada
  deberá realizarse de forma segura al guardar o activar una ruta.
  El method countCurrentRoutes() nos ayudará, pero por sí solo no garantiza esa regla
  si dos operaciones ocurren simultáneamente.
  Lo resolveremos al implementar el repositorio.*/