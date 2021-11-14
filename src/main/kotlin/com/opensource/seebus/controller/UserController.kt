package com.opensource.seebus.controller

import com.opensource.seebus.dto.request.UserInfoRequestDto
import com.opensource.seebus.dto.request.UserLocationRequestDto
import com.opensource.seebus.service.UserService
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(private val userService: UserService) {
    @ApiOperation("선택 완료하고 한번만 통신한다")
    @PostMapping("/user")
    fun sendAndroidDeviceInfo(@RequestBody userInfoRequestDto: UserInfoRequestDto) {
        userService.addAndroidDeviceInfo(
            userInfoRequestDto.androidId,
            userInfoRequestDto.rtNm,
            userInfoRequestDto.startArsId,
            userInfoRequestDto.destinationName,
            userInfoRequestDto.destinationArsId
        )
    }

    @ApiOperation("선택 완료하고 목적지에 도착하기 전까지 5초마다 사용자 위치 보내기")
    @PostMapping("/location")
    fun sendUserLocation(@RequestBody userLocationRequestDto: UserLocationRequestDto) {
        userService.addUserLocation(
            userLocationRequestDto.androidId,
            userLocationRequestDto.longitude,
            userLocationRequestDto.latitude
        )
    }
}
