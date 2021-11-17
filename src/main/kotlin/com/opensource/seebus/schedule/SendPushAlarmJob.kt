package com.opensource.seebus.schedule

import com.opensource.seebus.dto.request.PushNotificationRequestDto
import com.opensource.seebus.firebase.PushNotificationService
import com.opensource.seebus.repository.AndroidDeviceRepository
import mu.KotlinLogging
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.springframework.stereotype.Component

@Component
class SendPushAlarmJob(
    private val pushNotificationService: PushNotificationService,
    private val androidDeviceRepository: AndroidDeviceRepository
) : Job {
    private val log = KotlinLogging.logger {}

    @Throws(JobExecutionException::class)
    override fun execute(context: JobExecutionContext?) {
        val androidDevice = androidDeviceRepository.findByAndroidId(context!!.jobDetail!!.key!!.name)!!
        log.info("푸시알림")
        pushNotificationService.sendPushNotificationToToken(
            PushNotificationRequestDto(
                "알림",
                "버스가 곧 도착합니다.",
                androidDevice.firebaseToken
            )
        )
        androidDevice.sendBusArrivedPushAlarm = true
    }
}
