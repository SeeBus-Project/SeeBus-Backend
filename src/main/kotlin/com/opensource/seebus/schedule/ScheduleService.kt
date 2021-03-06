package com.opensource.seebus.schedule

import mu.KotlinLogging
import org.quartz.JobBuilder
import org.quartz.JobDetail
import org.quartz.Scheduler
import org.springframework.stereotype.Service

@Service
class ScheduleService : JobService() {
    private val log = KotlinLogging.logger {}

    override fun registerJob(scheduler: Scheduler?, androidId: String, traTime1: Int): Boolean {
        var result = false
        try {
            val jobDetail: JobDetail =
                JobBuilder.newJob(SendPushAlarmJob::class.java).withIdentity(androidId).build()
            result = setJobSchedule(scheduler!!, jobDetail, androidId, traTime1)
        } catch (e: Exception) {
            log.error(e.message)
        }
//        log.info("스케쥴 성공 $firebaseToken")
        return result
    }
}
