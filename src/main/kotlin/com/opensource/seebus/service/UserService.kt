package com.opensource.seebus.service

import com.opensource.seebus.dto.request.PushNotificationRequestDto
import com.opensource.seebus.dto.response.UserLocationResponseDto
import com.opensource.seebus.exception.userService.AndroidDeviceNotRegisterException
import com.opensource.seebus.exception.userService.InvalidDestinationArsId
import com.opensource.seebus.exception.userService.InvalidStationNameException
import com.opensource.seebus.exception.userService.RouteInfoNotFoundException
import com.opensource.seebus.exception.userService.UserAlreadyArrivedException
import com.opensource.seebus.exception.userService.UserInfoNotFoundException
import com.opensource.seebus.firebase.PushNotificationService
import com.opensource.seebus.model.AndroidDevice
import com.opensource.seebus.model.RouteInfo
import com.opensource.seebus.model.UserInfo
import com.opensource.seebus.repository.AndroidDeviceRepository
import com.opensource.seebus.repository.RouteInfoRepository
import com.opensource.seebus.repository.UserInfoRepository
import com.opensource.seebus.schedule.JobService
import com.opensource.seebus.schedule.ScheduleService
import org.quartz.Scheduler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.ObjectOutputStream
import java.net.Socket
import java.util.ArrayList
import javax.xml.parsers.DocumentBuilderFactory

@Service
class UserService(
    private val androidDeviceRepository: AndroidDeviceRepository,
    private val userInfoRepository: UserInfoRepository,
    private val routeInfoRepository: RouteInfoRepository,
    private val pushNotificationService: PushNotificationService,
    private val scheduleService: ScheduleService,
    private val scheduler: Scheduler,
    private val jobService: JobService
) {
    private final val getStationByUidItem: String = "http://ws.bus.go.kr/api/rest/stationinfo/getStationByUid?"
    private final val serviceKey: String = "serviceKey=DOXYaa9D5JuNL%2Bq5nNQz6k7zfejF8nQ%2BSRtJXfMZWeyzory0LmZQconDuzhLotGg5ptaYH8AemeIRDo36TfQ%2BQ%3D%3D"
    private final val getStationByNameList: String = "http://ws.bus.go.kr/api/rest/stationinfo/getStationByName?"
    private final val getStaionsByRouteList: String = "http://ws.bus.go.kr/api/rest/busRouteInfo/getStaionByRoute?"
    @Transactional
    fun addUserLocation(androidId: String, longitude: Double, latitude: Double): UserLocationResponseDto {
        // ??????????????? ??? ????????? ???????????? ???????????? ??????
        // 1????????? ????????? ????????? ???????????? ???????????? ??? ???????????????.
        // 2????????? ???????????? ????????? ??????????????? 2????????? ???????????????.
        // 3????????? ???????????? ????????? ??????????????? 3????????? ???????????????.
        // ?????? ????????? ??????
        var remainingStationCount = 0
        // ?????? ????????? ??????
        var nextStationCount = 0
        val androidDevice = androidDeviceRepository.findByAndroidId(androidId) ?: throw AndroidDeviceNotRegisterException("$androidId ??? androidId??? ?????? ???????????? ???????????????.")
        val userInfo = userInfoRepository.findByAndroidDevice(androidDevice) ?: throw UserInfoNotFoundException("???????????? ????????? ???????????? ????????? ????????? ????????? ????????????.")
        val routeInfo = routeInfoRepository.findAllByAndroidDevice(androidDevice) ?: throw RouteInfoNotFoundException("????????? ?????????????????? ????????? ????????????.")
        val routeInfoSize = routeInfo.size
        if (androidDevice.isArrived == true) {
            throw UserAlreadyArrivedException("${androidDevice.androidId} ???????????? ???????????? ??????????????????. ????????? ????????? ?????? ????????????.")
        }
        for (temp in 0 until routeInfoSize) {
            if (routeInfo[temp].userArrived == false) {
                remainingStationCount = routeInfoSize - temp
                nextStationCount = temp
                break
            }
        }
        userInfo.longitude = longitude
        userInfo.latitude = latitude
        // ????????? ????????? ????????? gps???????????? 100m????????? ??? ?????? ???????????? ?????????
        val distance = getDistance(
            routeInfo[nextStationCount].stationGPSY,
            routeInfo[nextStationCount].stationGPSX,
            latitude, longitude
        )

        if (distance <200.0) {
            if (remainingStationCount == 1 &&
                androidDevice.sendUserArrivedPushAlarmLeft1Station == false
            ) {
                // EC2 ????????????(TCP)
                val thread = ClientThread()
                thread.data[0] = "out"
                thread.data[1] = routeInfo[nextStationCount].stationName // ????????????????????????
                thread.data[2] = "no means data" // ???????????? ?????????
                thread.getPort = 5000
                thread.start()
                pushNotificationService.sendPushNotificationToToken(
                    PushNotificationRequestDto(
                        "??????",
                        "???????????? ??? ???????????????. ????????? ??????????????????.",
                        androidDevice.firebaseToken
                    )
                )
                androidDevice.sendUserArrivedPushAlarmLeft1Station = true
                androidDevice.isArrived = true
                jobService.deleteJob(scheduler, androidId)
            } else if (remainingStationCount == 2 &&
                androidDevice.sendUserArrivedPushAlarmLeft2Station == false
            ) {
                pushNotificationService.sendPushNotificationToToken(
                    PushNotificationRequestDto(
                        "??????",
                        "??????????????? 1????????? ???????????????.",
                        androidDevice.firebaseToken
                    )
                )
                androidDevice.sendUserArrivedPushAlarmLeft2Station = true
            } else if (remainingStationCount == 3 &&
                androidDevice.sendUserArrivedPushAlarmLeft3Station == false
            ) {
                pushNotificationService.sendPushNotificationToToken(
                    PushNotificationRequestDto(
                        "??????",
                        "??????????????? 2????????? ???????????????.",
                        androidDevice.firebaseToken
                    )
                )
                androidDevice.sendUserArrivedPushAlarmLeft3Station = true
            }
            routeInfo[nextStationCount].userArrived = true
        }
        return UserLocationResponseDto(androidId, routeInfo[nextStationCount].stationName, remainingStationCount, androidDevice.isArrived)
    }

    @Transactional
    fun addUserInfo(
        androidId: String,
        rtNm: String,
        startArsId: String,
        destinationName: String,
        destinationArsId: String
    ) {
        val androidDevice = androidDeviceRepository.findByAndroidId(androidId) ?: throw AndroidDeviceNotRegisterException("$androidId ??? androidId??? ?????? ???????????? ???????????????.")
        // ????????? ????????? gps ?????? ??????
        val destinationGPSList: MutableList<Double> = getDestinationGPS(destinationName, destinationArsId)
        // ????????? ??????1, ??????2, GPSX, GPSY
        val startInfoList: MutableList<String> = getStartInfo(startArsId, rtNm)
        // ?????? busRouteId, ??????????????? GPSX, ??????????????? GPSY, ??????????????? GPSX, ??????????????? GPSY
        val routeInfoList: MutableList<MutableList<String>> =
            getRouteInfo(startInfoList[5], startInfoList[2], startInfoList[3], destinationGPSList[0].toString(), destinationGPSList[1].toString())
        androidDevice.sendBusArrivedPushAlarm = false
        androidDevice.sendUserArrivedPushAlarmLeft1Station = false
        androidDevice.sendUserArrivedPushAlarmLeft2Station = false
        androidDevice.sendUserArrivedPushAlarmLeft3Station = false
        androidDevice.isArrived = false

        val traTime1 = startInfoList[0].toInt()
//        val traTime2 = startInfoList[1].toInt()
        scheduledPushAlarm(androidDevice, traTime1, rtNm)

        val userInfo = userInfoRepository.findByAndroidDevice(androidDevice)
        if (userInfo == null) {
            userInfoRepository.save(
                UserInfo(
                    androidDevice = androidDevice,
                    rtNm = rtNm,
                    startStationName = startInfoList[4],
                    startGPSX = startInfoList[2].toDouble(),
                    startGPSY = startInfoList[3].toDouble(),
                    longitude = 0.0,
                    latitude = 0.0,
                    destinationStationName = destinationName,
                    destinationGPSX = destinationGPSList[0],
                    destinationGPSY = destinationGPSList[1]
                )
            )
        } else {
            userInfo.rtNm = rtNm
            userInfo.startStationName = startInfoList[4]
            userInfo.startGPSX = startInfoList[2].toDouble()
            userInfo.startGPSY = startInfoList[3].toDouble()
            userInfo.longitude = 0.0
            userInfo.latitude = 0.0
            userInfo.destinationStationName = destinationName
            userInfo.destinationGPSX = destinationGPSList[0]
            userInfo.destinationGPSY = destinationGPSList[1]
        }

        val routeInfo = routeInfoRepository.findAllByAndroidDevice(androidDevice)
        if (routeInfo != null) {
            for (temp in 0 until routeInfo.size)
                routeInfoRepository.delete(routeInfo[temp]) // ?????? ?????? ??????
        }
        for (temp in 0 until routeInfoList[0].size) {
            routeInfoRepository.save(
                RouteInfo(
                    androidDevice = androidDevice,
                    stationName = routeInfoList[0][temp],
                    stationGPSX = routeInfoList[1][temp].toDouble(),
                    stationGPSY = routeInfoList[2][temp].toDouble()
                )
            )
        }
    }

    @Transactional
    fun sendGuideExitSignal(androidId: String) {
        val androidDevice = androidDeviceRepository.findByAndroidId(androidId) ?: throw AndroidDeviceNotRegisterException("$androidId ??? androidId??? ?????? ???????????? ???????????????.")
        jobService.deleteJob(scheduler, androidDevice.androidId)
        androidDevice.isArrived = true
    }

    private fun scheduledPushAlarm(androidDevice: AndroidDevice, traTime1: Int, rtNm: String) {
        if (traTime1 == 0) {
            pushNotificationService.sendPushNotificationToToken(
                PushNotificationRequestDto(
                    "??????",
                    "?????? ??????????????????.",
                    androidDevice.firebaseToken
                )
            )
            androidDevice.sendBusArrivedPushAlarm = true
            androidDevice.isArrived = true
        } else if (traTime1 <60) { // 60?????? ?????? ?????????
            pushNotificationService.sendPushNotificationToToken(
                PushNotificationRequestDto(
                    "??????",
                    "????????? 1????????? ???????????????.",
                    androidDevice.firebaseToken
                )
            )
            androidDevice.sendBusArrivedPushAlarm = true
        } else {
            pushNotificationService.sendPushNotificationToToken(
                PushNotificationRequestDto(
                    "??????",
                    "$rtNm ???????????? ${traTime1 / 60} ??? ${traTime1 % 60}??? ??? ???????????????.",
                    androidDevice.firebaseToken
                )
            )
            androidDevice.sendBusArrivedPushAlarm = true
            scheduleService.registerJob(scheduler, androidDevice.androidId, traTime1)
        }
    }
    private fun getDestinationGPS(destinationName: String, destinationArsId: String): MutableList<Double> {
        val destinationGPS: MutableList<Double> = ArrayList()
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc: Document = dBuilder.parse("$getStationByNameList$serviceKey&stSrch=$destinationName")
        doc.documentElement.normalize()
        val nList = doc.getElementsByTagName("itemList")
        if (nList.length == 0) {
            throw InvalidStationNameException("$destinationName ????????? ???????????? ????????????.")
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
            throw InvalidDestinationArsId("$destinationArsId ??? $destinationName ??? ???????????? ????????????.")
        }
        return destinationGPS
    }
    private fun getStartInfo(startArsId: String, rtNm: String): MutableList<String> {
        val startInfo: MutableList<String> = ArrayList()
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc: Document = dBuilder.parse("$getStationByUidItem$serviceKey&arsId=$startArsId")
        doc.documentElement.normalize()
        val nList = doc.getElementsByTagName("itemList")
        for (temp in 0 until nList.length) {
            val nNode = nList.item(temp)
            if (nNode.nodeType == Node.ELEMENT_NODE) {
                val eElement = nNode as Element
                if (getTagValue("rtNm", eElement).equals(rtNm)) {
                    // ?????? ??????
                    startInfo.add(getTagValue("traTime1", eElement).toString())
                    startInfo.add(getTagValue("traTime2", eElement).toString())
                    // ????????? GPS??????
                    startInfo.add(getTagValue("gpsX", eElement).toString())
                    startInfo.add(getTagValue("gpsY", eElement).toString())
                    // ????????? ????????? ??????
                    startInfo.add(getTagValue("stNm", eElement).toString())
                    // ????????? ?????? ?????? ID
                    startInfo.add(getTagValue("busRouteId", eElement).toString())
                    break
                }
            }
        }
        return startInfo
    }

    private fun getRouteInfo(
        busRouteId: String,
        startGPSX: String,
        startGPSY: String,
        destinationGPSX: String,
        destinationGPSY: String
    ):
        MutableList<MutableList<String>> {
            val routeInfo: MutableList<MutableList<String>> = ArrayList(ArrayList())
            routeInfo.add(ArrayList())
            routeInfo.add(ArrayList())
            routeInfo.add(ArrayList())

            var routeFlag = false
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc: Document = dBuilder.parse("$getStaionsByRouteList$serviceKey&busRouteId=$busRouteId")
            doc.documentElement.normalize()
            val nList = doc.getElementsByTagName("itemList")
            for (temp in 0 until nList.length) {
                val nNode = nList.item(temp)
                if (nNode.nodeType == Node.ELEMENT_NODE) {
                    val eElement = nNode as Element
                    if (getTagValue("gpsX", eElement).equals(startGPSX) &&
                        getTagValue("gpsY", eElement).equals(startGPSY) &&
                        routeFlag == false
                    ) {
                        routeFlag = true
                    } else if (routeFlag == true) {
                        if (getTagValue("gpsX", eElement).equals(destinationGPSX) &&
                            getTagValue("gpsY", eElement).equals(destinationGPSY)
                        ) {
                            // ????????? ??????
                            routeInfo[0].add(getTagValue("stationNm", eElement).toString())
                            // ?????? GPS??????
                            routeInfo[1].add(getTagValue("gpsX", eElement).toString())
                            routeInfo[2].add(getTagValue("gpsY", eElement).toString())
                            routeFlag = false
                            break
                        } else {
                            // ????????? ??????
                            routeInfo[0].add(getTagValue("stationNm", eElement).toString())
                            // ?????? GPS??????
                            routeInfo[1].add(getTagValue("gpsX", eElement).toString())
                            routeInfo[2].add(getTagValue("gpsY", eElement).toString())
                        }
                    }
                }
            }
            if (routeInfo.isEmpty()) {
                throw RouteInfoNotFoundException("????????? ???????????? ???????????? ?????? ??? ????????????.")
            }
            if (routeFlag == true) {
                throw RouteInfoNotFoundException("????????? ???????????? ???????????? ?????? ??? ????????????.")
            }
            return routeInfo
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

    // TCP ?????????
    internal class ClientThread : Thread() {
        var data = arrayOfNulls<String>(3)
        var getPort = 5000
        override fun run() {
//            val host2 = "183.101.12.31"
            val host = "ec2-3-35-208-56.ap-northeast-2.compute.amazonaws.com"
            try {
                val socket = Socket(host, getPort)
                val outstream = ObjectOutputStream(socket.getOutputStream()) // ????????? ?????? ????????? ??????
                outstream.writeObject(data[0]) // ?????? ???????????? ????????? ??????
                outstream.flush() // ??????

                // ??????????????? ??????
                outstream.writeObject(data[1]) // ?????? ???????????? ????????? ??????
                outstream.flush() // ??????

                // ???????????? ?????? ???????????? ???????????? ???????????? ????????????.
                // ????????? ????????????.
                outstream.writeObject(data[2]) // ?????? ???????????? ????????? ??????
                outstream.flush() // ??????

                outstream.close()
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
