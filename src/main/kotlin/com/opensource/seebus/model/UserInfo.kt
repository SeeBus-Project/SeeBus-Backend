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
    startArsId: String,
    longitude: Double,
    latitude: Double,
    destinationGPSX: String,
    destinationGPSY: String
) : DateEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @ManyToOne
    @JoinColumn(name = "androidId")
    val androidDevice: AndroidDevice = androidDevice
    var rtNm: String = rtNm // 버스번호
    var startArsId: String = startArsId
    var longitude: Double = longitude
    var latitude: Double = latitude
    var destinationGPSX: String = destinationGPSX
    var destinationGPSY: String = destinationGPSY
}
