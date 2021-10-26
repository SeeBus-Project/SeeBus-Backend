package com.opensource.seebus.model

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class AndroidDevice(
    deviceId: String,
    deviceToken: String
) : DateEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    val deviceId: String = deviceId
    var deviceToken: String = deviceToken
}
