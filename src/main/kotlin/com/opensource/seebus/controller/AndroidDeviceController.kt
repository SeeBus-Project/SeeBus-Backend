package com.opensource.seebus.controller

import com.opensource.seebus.dto.request.AndroidDeviceRequestDto
import com.opensource.seebus.model.AndroidDevice
import com.opensource.seebus.service.AndroidDeviceService
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class AndroidDeviceController(private val androidDeviceService: AndroidDeviceService) {
    @ApiOperation("서버시간 제공")
    @GetMapping("/")
    fun time(): LocalDateTime {
        return LocalDateTime.now()
    }

    @ApiOperation("안드로이드 디바이스 정보 확인하기")
    @GetMapping("/device")
    fun androidDevice(): List<AndroidDevice> {
        return androidDeviceService.allData()
    }

    @ApiOperation("안드로이드 디바이스 정보 보내기")
    @PostMapping("/device")
    fun sendAndroidDeviceInfo(@RequestBody androidDeviceRequestDto: AndroidDeviceRequestDto): AndroidDevice {
        return androidDeviceService.inputData(androidDeviceRequestDto.deviceId, androidDeviceRequestDto.deviceToken)
    }
}
