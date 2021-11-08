package com.opensource.seebus.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.google.gson.GsonBuilder
import com.opensource.seebus.dto.request.PushNotificationRequestDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ExecutionException

@Service
class FCMService {
    private val logger = LoggerFactory.getLogger(FCMService::class.java)
    @Throws(InterruptedException::class, ExecutionException::class)
    fun sendMessageToToken(request: PushNotificationRequestDto) {
        val message = getPreconfiguredMessageToToken(request)
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonOutput = gson.toJson(message)
        val response = sendAndGetResponse(message)
        logger.info(
            "Sent message to token. Device token: " + request.token +
                ", " + response + " msg " + jsonOutput
        )
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    private fun sendAndGetResponse(message: Message): String {
        return FirebaseMessaging.getInstance().sendAsync(message).get()
    }

    private fun getPreconfiguredMessageToToken(request: PushNotificationRequestDto): Message {
        return getPreconfiguredMessageBuilder(request).setToken(request.token)
            .build()
    }

    private fun getPreconfiguredMessageBuilder(request: PushNotificationRequestDto): Message.Builder {
        return Message.builder()
            .setApnsConfig(null).setAndroidConfig(null).setNotification(
                Notification(request.title, request.message)
            )
    }
}
