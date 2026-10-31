package com.routeplanner.app

import com.routeplanner.app.data.database.entity.DestinationEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testNearestNeighborOptimization() {

        // Origen ficticio: Madrid centro.
        val originLatitude = 40.4168
        val originLongitude = -3.7038

        // Tres destinos ficticios.
        val destinations = listOf(
            DestinationEntity(
                id = 1,
                address = "Destino A",
                latitude = 40.4200,
                longitude = -3.7000
            ),
            DestinationEntity(
                id = 2,
                address = "Destino B",
                latitude = 40.4500,
                longitude = -3.7100
            ),
            DestinationEntity(
                id = 3,
                address = "Destino C",
                latitude = 40.4250,
                longitude = -3.6800
            )
        )

        // Ejecutamos el algoritmo.
        val result = RouteOptimizer.optimizeNearestNeighbor(
            originLatitude,
            originLongitude,
            destinations
        )

        // Comprobamos el orden esperado.
        val expectedOrder = listOf(1, 3, 2)
        val actualOrder = result.map { it.id }

        assertEquals(expectedOrder, actualOrder)
    }
}