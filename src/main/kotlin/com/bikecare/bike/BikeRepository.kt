package com.bikecare.bike

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface BikeRepository : JpaRepository<Bike, Long> {

    // na detail kola patřícího uživateli
    fun findByIdAndOwnerId(id: Long, ownerId: Long): Optional<Bike>

    // na výpis všech kol daného uživatele (co volá BikeController)
    fun findAllByOwnerId(ownerId: Long): List<Bike>
}
