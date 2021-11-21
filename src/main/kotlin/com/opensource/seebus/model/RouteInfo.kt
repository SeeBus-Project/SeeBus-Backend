package com.opensource.seebus.model

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
class RouteInfo(
    androidDevice: AndroidDevice,
    stationName: String,
    stationGPSX: Double,
    stationGPSY: Double,
) : DateEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @ManyToOne
    @JoinColumn(name = "androidId")
    val androidDevice: AndroidDevice = androidDevice
    var stationName: String = stationName
    var stationGPSX: Double = stationGPSX
    var stationGPSY: Double = stationGPSY
    var userArrived: Boolean = false
}
