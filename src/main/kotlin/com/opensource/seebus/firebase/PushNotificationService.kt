package com.opensource.seebus.firebase

import com.opensource.seebus.dto.request.PushNotificationRequestDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PushNotificationService(private val fcmService: FCMService) {
    private val logger = LoggerFactory.getLogger(PushNotificationService::class.java)
    fun sendPushNotificationToToken(request: PushNotificationRequestDto) {
        try {
            fcmService.sendMessageToToken(request)
        } catch (e: Exception) {
            logger.error(e.message)
        }
    }
}
