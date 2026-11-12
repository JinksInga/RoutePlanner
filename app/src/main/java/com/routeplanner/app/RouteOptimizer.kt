package com.routeplanner.app

import com.routeplanner.app.data.database.entity.DestinationEntity
import kotlin.math.*

object RouteOptimizer {

    /**
     * Calcula la distancia en metros entre dos puntos
     * mediante la fórmula de Haversine.
     */
    private fun calculateDistance(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {

        val earthRadius = 6371000.0

        val latDistance = Math.toRadians(latitude2 - latitude1)
        val lonDistance = Math.toRadians(longitude2 - longitude1)

        val a = sin(latDistance / 2).pow(2) +
                cos(Math.toRadians(latitude1)) *
                cos(Math.toRadians(latitude2)) *
                sin(lonDistance / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Ordena los destinos mediante el algoritmo
     * Greedy Nearest Neighbor.
     */
    fun optimizeNearestNeighbor(
        originLatitude: Double,
        originLongitude: Double,
        destinations: List<DestinationEntity>
    ): List<DestinationEntity> {

        // Solo podemos calcular distancias si tenemos coordenadas.
        val remainingDestinations = destinations
            .filter {
                it.latitude != null && it.longitude != null
            }
            .toMutableList()

        val optimizedRoute = mutableListOf<DestinationEntity>()

        var currentLatitude = originLatitude
        var currentLongitude = originLongitude

        // Repetimos hasta ordenar todos los destinos.
        while (remainingDestinations.isNotEmpty()) {

            val nearestDestination = remainingDestinations.minByOrNull { destination ->

                calculateDistance(
                    currentLatitude,
                    currentLongitude,
                    destination.latitude!!,
                    destination.longitude!!
                )

            } ?: break

            optimizedRoute.add(nearestDestination)
            remainingDestinations.remove(nearestDestination)

            // El destino elegido pasa a ser el nuevo punto de partida.
            currentLatitude = nearestDestination.latitude!!
            currentLongitude = nearestDestination.longitude!!
        }

        return optimizedRoute
    }
}