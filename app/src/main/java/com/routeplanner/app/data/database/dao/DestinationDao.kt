package com.routeplanner.app.data.database.dao

import androidx.room.*
import com.routeplanner.app.data.database.entity.DestinationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DestinationDao {

    @Insert
    suspend fun insert(
        destination: DestinationEntity
    ): Long

    @Update
    suspend fun update(
        destination: DestinationEntity
    )

    @Delete
    suspend fun delete(
        destination: DestinationEntity
    )

    @Query("SELECT * FROM destinations ORDER BY address ASC")
    fun getAll(): Flow<List<DestinationEntity>>

    @Query("SELECT * FROM destinations WHERE id = :id")
    suspend fun getById(id: Int): DestinationEntity?

    @Query("""
        SELECT * FROM destinations
        WHERE address LIKE '%' || :query || '%'
        ORDER BY address ASC
    """)
    fun search(query: String): Flow<List<DestinationEntity>>
}
/*
* Un detalle interesante:
* utilizamos Flow, de manera que la pantalla podrá recibir automáticamente
* las actualizaciones cuando se añadan, modifiquen o eliminen direcciones.
* */