package com.opensource.seebus.model

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class AndroidDevice(
    androidId: String,
    firebaseToken: String
) : DateEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    val androidId: String = androidId
    var firebaseToken: String = firebaseToken
}
