package com.opensource.seebus.repository

import com.opensource.seebus.model.AndroidDevice
import com.opensource.seebus.model.RouteInfo
import org.springframework.data.jpa.repository.JpaRepository

interface RouteInfoRepository : JpaRepository<RouteInfo, Int> {
    fun findAllByAndroidDevice(androidDevice: AndroidDevice): List<RouteInfo>?
}
