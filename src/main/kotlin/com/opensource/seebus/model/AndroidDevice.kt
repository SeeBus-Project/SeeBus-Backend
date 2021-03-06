package com.opensource.seebus.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany

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
    var sendUserArrivedPushAlarmLeft1Station: Boolean = false
    var sendUserArrivedPushAlarmLeft2Station: Boolean = false
    var sendUserArrivedPushAlarmLeft3Station: Boolean = false
    var sendBusArrivedPushAlarm: Boolean = false
    var isArrived = true
    @OneToMany(mappedBy = "androidDevice")
    @JsonIgnoreProperties("androidDevice")
    var userInfo: List<UserInfo>? = null
    @OneToMany(mappedBy = "androidDevice")
    @JsonIgnoreProperties("androidDevice")
    var routeInfo: List<RouteInfo>? = null
}
