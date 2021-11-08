package com.opensource.seebus.repository

import com.opensource.seebus.model.AndroidDevice
import org.springframework.data.jpa.repository.JpaRepository

interface AndroidDeviceRepository : JpaRepository<AndroidDevice, Int> {
    fun findByDeviceId(androidId: String): AndroidDevice?
}
