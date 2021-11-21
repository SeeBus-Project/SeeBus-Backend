package com.opensource.seebus.schedule

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
    abstract fun registerJob(scheduler: Scheduler?, androidId: String, traTime1: Int): Boolean

    fun deleteJob(scheduler: Scheduler, androidId: String) {
        try {
            scheduler.unscheduleJob(TriggerKey(androidId))
            log.info("스케쥴 삭제 완료 androidId : $androidId")
        } catch (e: Exception) {
            log.error("[[ERROR]] 잡 삭제 에러 !!! | jobId: {}| msg: {}", androidId, e.message)
        }
    }

    fun setJobSchedule(scheduler: Scheduler, jobDetail: JobDetail?, androidId: String, traTime1: Int): Boolean {
        try {
            val localDateTime = LocalDateTime.now().plusSeconds(traTime1.toLong() - 55)
            val cronExpression = localDateTime.format(DateTimeFormatter.ofPattern("ss mm HH dd MM ? yyyy"))
//            println(cronExpression)
//            println(localDateTime)
            // 기존에 같은 jobname으로 등록된 잡이 있는지 확인
            var overlap = false
            for (jobKey in scheduler.getJobKeys(null)) {
                if (jobKey.name.equals(androidId)) {
                    overlap = true
                }
            }
            // 기존에 등록된 잡이 있다면 해당 잡에 대한 기존 스케쥴 삭제하고 새 스케쥴을 추가
            if (overlap) {
                deleteJob(scheduler, androidId)
            }
            val trigger: Trigger = TriggerBuilder.newTrigger().withIdentity(androidId).withIdentity(androidId)
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)).build()
            scheduler.scheduleJob(jobDetail, trigger)
            log.info("$androidId 사용자에게 $localDateTime 에 푸시알림을 보냅니다.")
        } catch (e: SchedulerException) {
            log.error(
                "[[ERROR]] 잡 추가 에러 !!! | jobId: {} | jobNm: {} | msg: {}",
                androidId,
                e.message
            )
            return false
        }
        return true
    }
}
