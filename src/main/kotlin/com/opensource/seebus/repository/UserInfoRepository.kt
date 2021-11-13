package com.opensource.seebus.repository

import com.opensource.seebus.model.AndroidDevice
import com.opensource.seebus.model.UserInfo
import org.springframework.data.jpa.repository.JpaRepository

interface UserInfoRepository : JpaRepository<UserInfo, Int> {
    fun findByAndroidDevice(androidDevice: AndroidDevice): UserInfo?
}
