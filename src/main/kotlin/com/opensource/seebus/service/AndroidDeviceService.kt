package com.opensource.seebus.service

import com.opensource.seebus.model.AndroidDevice
import com.opensource.seebus.repository.AndroidDeviceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AndroidDeviceService(private val androidDeviceRepository: AndroidDeviceRepository) {
    @Transactional
    fun addAndroidDevice(androidId: String, firebaseToken: String): AndroidDevice {
        val androidDevice = androidDeviceRepository.findByAndroidId(androidId)
        if (androidDevice == null) {
            return androidDeviceRepository.save(
                AndroidDevice(
                    androidId = androidId,
                    firebaseToken = firebaseToken
                )
            )
        } else {
            androidDevice.firebaseToken = firebaseToken
            return androidDevice
        }
    }

    fun allData(): List<AndroidDevice> {
        return androidDeviceRepository.findAll()
    }
}
