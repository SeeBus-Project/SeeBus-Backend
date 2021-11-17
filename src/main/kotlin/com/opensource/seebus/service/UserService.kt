package com.opensource.seebus.service

import com.opensource.seebus.dto.request.PushNotificationRequestDto
import com.opensource.seebus.exception.userService.AndroidDeviceNotFoundException
import com.opensource.seebus.exception.userService.InvalidDestinationArsId
import com.opensource.seebus.exception.userService.InvalidStationNameException
import com.opensource.seebus.exception.userService.UserInfoNotFoundException
import com.opensource.seebus.firebase.PushNotificationService
import com.opensource.seebus.model.AndroidDevice
import com.opensource.seebus.model.UserInfo
import com.opensource.seebus.repository.AndroidDeviceRepository
import com.opensource.seebus.repository.UserInfoRepository
import com.opensource.seebus.schedule.ScheduleService
import org.quartz.Scheduler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.util.ArrayList
import javax.xml.parsers.DocumentBuilderFactory

@Service
class UserService(
    private val androidDeviceRepository: AndroidDeviceRepository,
    private val userInfoRepository: UserInfoRepository,
    private val pushNotificationService: PushNotificationService,
    private val scheduleService: ScheduleService,
    private val scheduler: Scheduler
) {
    private final val getStationByUid: String = "http://ws.bus.go.kr/api/rest/stationinfo/getStationByUid?"
    private final val serviceKey: String = "serviceKey=DOXYaa9D5JuNL%2Bq5nNQz6k7zfejF8nQ%2BSRtJXfMZWeyzory0LmZQconDuzhLotGg5ptaYH8AemeIRDo36TfQ%2BQ%3D%3D"
    private final val getStationByName: String = "http://ws.bus.go.kr/api/rest/stationinfo/getStationByName?"
    @Transactional
    fun addUserLocation(androidId: String, longitude: Double, latitude: Double) {
        val androidDevice = androidDeviceRepository.findByAndroidId(androidId) ?: throw AndroidDeviceNotFoundException("$androidId 의 androidId는 아직 등록되지 않았습니다.")
        val userInfo = userInfoRepository.findByAndroidDevice(androidDevice) ?: throw UserInfoNotFoundException("사용자의 정보가 저장되지 않아서 위치를 보낼수 없습니다.")
        userInfo.longitude = longitude
        userInfo.latitude = latitude
        // 사용자 위치와 목적지 gps계산해서 50m이내면 곧 도착 푸시알림 보내기
        val distance = getDistance(userInfo.destinationGPSY, userInfo.destinationGPSX, latitude, longitude)
        if (distance <100.0 && androidDevice.sendUserArrivedPushAlarm == false) {
            pushNotificationService.sendPushNotificationToToken(
                PushNotificationRequestDto(
                    "알림",
                    "목적지 근처입니다. 하차벨 누르겠습니다.",
                    androidDevice.firebaseToken
                )
            )
            androidDevice.sendUserArrivedPushAlarm = true
            androidDevice.isArrived = true
        }
    }

    @Transactional
    fun addUserInfo(
        androidId: String,
        rtNm: String,
        startArsId: String,
        destinationName: String,
        destinationArsId: String
    ) {
        // 도착지 정거장 gps 좌표 얻기
        val destinationGPS: MutableList<Double> = getDestinationGPS(destinationName, destinationArsId)
        // 출발지 시간1, 시간2, GPSX, GPSY
        val startInfo: MutableList<String> = getStartInfo(startArsId, rtNm)
//        println(startInfo[0])
//        println(startInfo[1])
        val androidDevice = androidDeviceRepository.findByAndroidId(androidId) ?: throw AndroidDeviceNotFoundException("$androidId 의 androidId는 아직 등록되지 않았습니다.")
        val userInfo = userInfoRepository.findByAndroidDevice(androidDevice)
        androidDevice.sendBusArrivedPushAlarm = false
        androidDevice.sendUserArrivedPushAlarm = false
        androidDevice.isArrived = false
        if (userInfo == null) {
            userInfoRepository.save(
                UserInfo(
                    androidDevice = androidDevice,
                    rtNm = rtNm,
                    startStationName = startInfo[4],
                    startGPSX = startInfo[2].toDouble(),
                    startGPSY = startInfo[3].toDouble(),
                    longitude = 0.0,
                    latitude = 0.0,
                    destinationStationName = destinationName,
                    destinationGPSX = destinationGPS[0],
                    destinationGPSY = destinationGPS[1]
                )
            )
        } else {
            userInfo.rtNm = rtNm
            userInfo.startStationName = startInfo[4]
            userInfo.startGPSX = startInfo[2].toDouble()
            userInfo.startGPSY = startInfo[3].toDouble()
            userInfo.longitude = 0.0
            userInfo.latitude = 0.0
            userInfo.destinationStationName = destinationName
            userInfo.destinationGPSX = destinationGPS[0]
            userInfo.destinationGPSY = destinationGPS[1]
        }

        val traTime1 = startInfo[0].toInt()
        val traTime2 = startInfo[1].toInt()
        scheduledPushAlarm(androidDevice, traTime1, traTime2)
    }

    @Transactional
    fun sendGuideExitSignal(androidId:String) {
        val androidDevice = androidDeviceRepository.findByAndroidId(androidId) ?: throw AndroidDeviceNotFoundException("$androidId 의 androidId는 아직 등록되지 않았습니다.")
        androidDevice.isArrived=true
    }

    private fun scheduledPushAlarm(androidDevice: AndroidDevice, traTime1: Int, traTime2: Int) {
        // traTime2 두번째 시간 차이가 많이 나서 우선은 사용 안함 회의때 물어보기
        if (traTime1 == 0) {
            pushNotificationService.sendPushNotificationToToken(
                PushNotificationRequestDto(
                    "알림",
                    "운행 종료됬습니다.",
                    androidDevice.firebaseToken
                )
            )
            androidDevice.sendBusArrivedPushAlarm = true
        } else if (traTime1 <60) { // 60으로 다시 바꿀것
            pushNotificationService.sendPushNotificationToToken(
                PushNotificationRequestDto(
                    "알림",
                    "버스가 곧 도착합니다.",
                    androidDevice.firebaseToken
                )
            )
            androidDevice.sendBusArrivedPushAlarm = true
        } else {
            scheduleService.registerJob(scheduler, androidDevice.androidId, traTime1)
        }
    }
    private fun getDestinationGPS(destinationName: String, destinationArsId: String): MutableList<Double> {
        val destinationGPS: MutableList<Double> = ArrayList()
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc: Document = dBuilder.parse("$getStationByName$serviceKey&stSrch=$destinationName")
        doc.documentElement.normalize()
        val nList = doc.getElementsByTagName("itemList")
        if (nList.length == 0) {
            throw InvalidStationNameException("$destinationName 이름의 정거장은 없습니다.")
        }
        for (temp in 0 until nList.length) {
            val nNode = nList.item(temp)
            if (nNode.nodeType == Node.ELEMENT_NODE) {
                val eElement = nNode as Element
                if (getTagValue("arsId", eElement).equals(destinationArsId)) {
                    destinationGPS.add(getTagValue("tmX", eElement)!!.toDouble())
                    destinationGPS.add(getTagValue("tmY", eElement)!!.toDouble())
                    break
                }
            }
        }
        if (destinationGPS.isEmpty()) {
            throw InvalidDestinationArsId("$destinationArsId 버스는 $destinationName 을 지나지 않습니다.")
        }
        return destinationGPS
    }
    private fun getStartInfo(startArsId: String, rtNm: String): MutableList<String> {
        val startInfo: MutableList<String> = ArrayList()
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc: Document = dBuilder.parse("$getStationByUid$serviceKey&arsId=$startArsId")
        doc.documentElement.normalize()
        val nList = doc.getElementsByTagName("itemList")
        for (temp in 0 until nList.length) {
            val nNode = nList.item(temp)
            if (nNode.nodeType == Node.ELEMENT_NODE) {
                val eElement = nNode as Element
                if (getTagValue("rtNm", eElement).equals(rtNm)) {
                    // 시간 넣기
                    startInfo.add(getTagValue("traTime1", eElement).toString())
                    startInfo.add(getTagValue("traTime2", eElement).toString())
                    // 출발지 GPS넣기
                    startInfo.add(getTagValue("gpsX", eElement).toString())
                    startInfo.add(getTagValue("gpsY", eElement).toString())
                    // 출발지 정거장 이름
                    startInfo.add(getTagValue("stNm", eElement).toString())
                    break
                }
            }
        }
        return startInfo
    }

    private fun getTagValue(tag: String, eElement: Element): String? {
        val nlList: NodeList = eElement.getElementsByTagName(tag).item(0).childNodes
        val nValue: Node = nlList.item(0) as Node
        return nValue.nodeValue
    }

    private fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val theta = lon1 - lon2
        var dist =
            Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(
                deg2rad(theta)
            )
        dist = Math.acos(dist)
        dist = rad2deg(dist)
        dist = dist * 60 * 1.1515
        dist = dist * 1609.344
        return dist
    }

    // This function converts decimal degrees to radians
    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    // This function converts radians to decimal degrees
    private fun rad2deg(rad: Double): Double {
        return rad * 180 / Math.PI
    }
}
