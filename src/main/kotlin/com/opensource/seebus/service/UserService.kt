package com.opensource.seebus.service

import com.opensource.seebus.dto.request.PushNotificationRequestDto
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

    @Transactional
    fun addUserLocation(androidId: String, longitude: Double, latitude: Double) {
        val androidDevice = androidDeviceRepository.findByAndroidId(androidId) ?: throw Exception("오류1")
        val userInfo = userInfoRepository.findByAndroidDevice(androidDevice) ?: throw Exception("오류2")
        userInfo.longitude = longitude
        userInfo.latitude = latitude
    }

    @Transactional
    fun addAndroidDeviceInfo(
        androidId: String,
        rtNm: String,
        startArsId: String,
        destinationGPSX: String,
        destinationGPSY: String
    ) {
        val androidDevice = androidDeviceRepository.findByAndroidId(androidId) ?: throw Exception("오류3")
        println(androidDevice.id)
        val userInfo = userInfoRepository.findByAndroidDevice(androidDevice)
        if (userInfo == null) {
            userInfoRepository.save(
                UserInfo(
                    androidDevice = androidDevice,
                    rtNm = rtNm,
                    startArsId = startArsId,
                    longitude = 0.0,
                    latitude = 0.0,
                    destinationGPSX = destinationGPSX,
                    destinationGPSY = destinationGPSY
                )
            )
        } else {
            userInfo.rtNm = rtNm
            userInfo.startArsId = startArsId
            userInfo.destinationGPSX = destinationGPSX
            userInfo.destinationGPSY = destinationGPSY
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
