package com.opensource.seebus.test

import com.opensource.seebus.dto.request.PushNotificationRequestDto
import com.opensource.seebus.firebase.PushNotificationService
import mu.KotlinLogging
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.springframework.stereotype.Component

@Component
class SendPushAlarmJob(
    private val pushNotificationService: PushNotificationService,
) : Job {
    private val log = KotlinLogging.logger {}

    @Throws(JobExecutionException::class)
    override fun execute(context: JobExecutionContext?) {
        log.info("푸시알림")
        pushNotificationService.sendPushNotificationToToken(
            PushNotificationRequestDto(
                "알림",
                "버스가 곧 도착합니다.",
                context!!.jobDetail!!.key!!.name
            )
        )
    }
}
