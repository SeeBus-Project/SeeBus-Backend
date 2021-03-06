package com.opensource.seebus.model

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
class UserInfo(
    androidDevice: AndroidDevice,
    rtNm: String,
    startStationName: String,
    startGPSX: Double,
    startGPSY: Double,
    longitude: Double,
    latitude: Double,
    destinationStationName: String,
    destinationGPSX: Double,
    destinationGPSY: Double
) : DateEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @ManyToOne
    @JoinColumn(name = "androidId")
    val androidDevice: AndroidDevice = androidDevice
    var rtNm: String = rtNm // 버스번호
    var startStationName = startStationName
    var startGPSX: Double = startGPSX
    var startGPSY: Double = startGPSY
    var longitude: Double = longitude
    var latitude: Double = latitude
    var destinationStationName = destinationStationName
    var destinationGPSX: Double = destinationGPSX
    var destinationGPSY: Double = destinationGPSY
}
