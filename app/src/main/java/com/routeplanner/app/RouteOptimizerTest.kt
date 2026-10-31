package com.routeplanner.app

import com.routeplanner.app.data.database.entity.DestinationEntity

fun main() {

    // Punto de partida ficticio: Madrid centro.
    val originLatitude = 40.4168
    val originLongitude = -3.7038

    // Creamos tres destinos ficticios.
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
    val optimizedRoute = RouteOptimizer.optimizeNearestNeighbor(
        originLatitude,
        originLongitude,
        destinations
    )

    // Mostramos el resultado.
    println("ORDEN DE LA RUTA:")

    optimizedRoute.forEachIndexed { index, destination ->
        println("${index + 1}. ${destination.address}")
    }
}