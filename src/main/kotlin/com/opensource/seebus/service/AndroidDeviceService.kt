package com.opensource.seebus.service

import com.opensource.seebus.model.AndroidDevice
import com.opensource.seebus.repository.AndroidDeviceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AndroidDeviceService(private val androidDeviceRepository: AndroidDeviceRepository) {
    @Transactional
    fun inputData(deviceId: String, deviceToken: String): AndroidDevice {
        val androidDevice = androidDeviceRepository.findByDeviceId(deviceId)
        if (androidDevice == null) {
            return androidDeviceRepository.save(
                AndroidDevice(
                    deviceId = deviceId,
                    deviceToken = deviceToken
                )
            )
        } else {
            androidDevice.deviceToken = deviceToken
            return androidDevice
        }
    }

    fun allData(): List<AndroidDevice> {
        return androidDeviceRepository.findAll()
    }
}
