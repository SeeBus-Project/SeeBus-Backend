package com.opensource.seebus.service

import com.opensource.seebus.dto.request.PushNotificationRequestDto
import com.opensource.seebus.exception.userService.AndroidDeviceNotFoundException
import com.opensource.seebus.exception.userService.InvalidDestinationArsId
import com.opensource.seebus.exception.userService.InvalidStationNameException
import com.opensource.seebus.exception.userService.UserInfoNotFoundException
import com.opensource.seebus.firebase.PushNotificationService
import com.opensource.seebus.model.UserInfo
import com.opensource.seebus.repository.AndroidDeviceRepository
import com.opensource.seebus.repository.UserInfoRepository
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
    private val pushNotificationService: PushNotificationService
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
    }

    @Transactional
    fun addAndroidDeviceInfo(
        androidId: String,
        rtNm: String,
        startArsId: String,
        destinationName: String,
        destinationArsId: String
    ) {
        val destinationGPS = getDestinationGPS(destinationName, destinationArsId)
        val androidDevice = androidDeviceRepository.findByAndroidId(androidId) ?: throw AndroidDeviceNotFoundException("$androidId 의 androidId는 아직 등록되지 않았습니다.")
        val userInfo = userInfoRepository.findByAndroidDevice(androidDevice)
        if (userInfo == null) {
            userInfoRepository.save(
                UserInfo(
                    androidDevice = androidDevice,
                    rtNm = rtNm,
                    startArsId = startArsId,
                    longitude = 0.0,
                    latitude = 0.0,
                    destinationGPSX = destinationGPS[0],
                    destinationGPSY = destinationGPS[1]
                )
            )
        } else {
            userInfo.rtNm = rtNm
            userInfo.startArsId = startArsId
            userInfo.destinationGPSX = destinationGPS[0]
            userInfo.destinationGPSY = destinationGPS[1]
        }
        var arrivalTime: MutableList<String> = getArrivalTime(startArsId, rtNm)
        println(arrivalTime[0])
        println(arrivalTime[1])

        if (arrivalTime[0].contains("곧 도착")) {
            pushNotificationService.sendPushNotificationToToken(
                PushNotificationRequestDto(
                    "알림",
                    "버스가 곧 도착합니다.",
                    androidDevice.firebaseToken
                )
            )
        } else if (arrivalTime[0].contains("운행종료")) {
            pushNotificationService.sendPushNotificationToToken(
                PushNotificationRequestDto(
                    "알림",
                    "운행 종료됬습니다.",
                    androidDevice.firebaseToken
                )
            )
        } else {
            // TODO 분초 나누기 traTime1 traTime2
//            val tmp=arrivalTime[0].split("분|초후|분후")
//            println(tmp[0])
//            println(tmp[1])
        }
    }

    private fun getDestinationGPS(destinationName: String, destinationArsId: String): MutableList<String> {
        var destinationGPS: MutableList<String> = ArrayList()
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
                    destinationGPS.add(getTagValue("tmX", eElement).toString())
                    destinationGPS.add(getTagValue("tmY", eElement).toString())
                    break
                }
            }
        }
        if (destinationGPS.isEmpty()) {
            throw InvalidDestinationArsId("$destinationArsId 버스는 $destinationName 을 지나지 않습니다.")
        }
        return destinationGPS
    }
    private fun getArrivalTime(startArsId: String, rtNm: String): MutableList<String> {
        var arrivalTime: MutableList<String> = ArrayList()
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
                    arrivalTime.add(getTagValue("arrmsg1", eElement).toString())
                    arrivalTime.add(getTagValue("arrmsg2", eElement).toString())
                    break
                }
            }
        }
        return arrivalTime
    }

    private fun getTagValue(tag: String, eElement: Element): String? {
        val nlList: NodeList = eElement.getElementsByTagName(tag).item(0).childNodes
        val nValue: Node = nlList.item(0) as Node
        return nValue.nodeValue
    }
}
