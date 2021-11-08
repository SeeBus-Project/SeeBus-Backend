package com.opensource.seebus.controller

import com.opensource.seebus.dto.request.PushNotificationRequestDto
import com.opensource.seebus.dto.response.PushNotificationResponseDto
import com.opensource.seebus.firebase.PushNotificationService
import io.swagger.annotations.ApiOperation
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PushNotificationController(private val pushNotificationService: PushNotificationService) {

    @ApiOperation("특정 디바이스에게만 푸시알림 보내기")
    @PostMapping("/notification/token")
    fun sendTokenNotification(@RequestBody request: PushNotificationRequestDto): PushNotificationResponseDto {
        pushNotificationService.sendPushNotificationToToken(request)
        return PushNotificationResponseDto(HttpStatus.OK.value(), "Notification has been sent.")
    }
}
