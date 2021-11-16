package com.opensource.seebus.test

import mu.KotlinLogging
import org.quartz.CronScheduleBuilder
import org.quartz.JobDetail
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.TriggerKey
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
abstract class JobService {
    private val log = KotlinLogging.logger {}
    abstract fun registerJob(scheduler: Scheduler?, firebaseToken: String, traTime1: Int): Boolean

    fun deleteJob(scheduler: Scheduler, firebaseToken: String) {
        try {
            scheduler.unscheduleJob(TriggerKey(firebaseToken))
        } catch (e: Exception) {
            log.error("[[ERROR]] 잡 삭제 에러 !!! | jobId: {}| msg: {}", firebaseToken, e.message)
        }
    }

    fun setJobSchedule(scheduler: Scheduler, jobDetail: JobDetail?, firebaseToken: String, traTime1: Int): Boolean {
        try {
            val localDateTime = LocalDateTime.now().plusSeconds(traTime1.toLong() - 55)
            val cronExpression = localDateTime.format(DateTimeFormatter.ofPattern("ss mm HH dd MM ? yyyy"))
//            println(cronExpression)
//            println(localDateTime)
            // 기존에 같은 jobname으로 등록된 잡이 있는지 확인
            var overlap = false
            for (jobKey in scheduler.getJobKeys(null)) {
                if (jobKey.name.equals(firebaseToken)) {
                    overlap = true
                }
            }
            // 기존에 등록된 잡이 있다면 해당 잡에 대한 기존 스케쥴 삭제하고 새 스케쥴을 추가
            if (overlap) {
//                println("삭제")
                deleteJob(scheduler, firebaseToken)
                val trigger: Trigger = TriggerBuilder.newTrigger().withIdentity(firebaseToken)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)).build()
                scheduler.scheduleJob(jobDetail, trigger)
            } else {
//                println("최초")
                val trigger: Trigger = TriggerBuilder.newTrigger().withIdentity(firebaseToken)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)).build()
                scheduler.scheduleJob(jobDetail, trigger)
            }
            log.info("$firebaseToken 사용자에게 $cronExpression 시간에 푸시알림을 보냅니다.")
        } catch (e: SchedulerException) {
            log.error(
                "[[ERROR]] 잡 추가 에러 !!! | jobId: {} | jobNm: {} | msg: {}",
                firebaseToken,
                e.message
            )
            return false
        }
        return true
    }
}
