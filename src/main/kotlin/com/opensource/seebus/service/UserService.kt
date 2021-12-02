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
        // 목적지까지 몇 정거장 남았는지 알려주는 변수
        // 1이라면 하차벨 울리고 푸시알림 목적지에 곧 도착합니다.
        // 2이라면 푸시알림 보내고 목적지까지 2정거장 남았습니다.
        // 3이라면 푸시알림 보내고 목적지까지 3정거장 남았습니다.
        // 남은 정류장 개수
        var remainingStationCount = 0
        // 다음 정류장 순서
        var nextStationCount = 0
        val androidDevice = androidDeviceRepository.findByAndroidId(androidId) ?: throw AndroidDeviceNotRegisterException("$androidId 의 androidId는 아직 등록되지 않았습니다.")
        val userInfo = userInfoRepository.findByAndroidDevice(androidDevice) ?: throw UserInfoNotFoundException("사용자의 정보가 저장되지 않아서 위치를 보낼수 없습니다.")
        val routeInfo = routeInfoRepository.findAllByAndroidDevice(androidDevice) ?: throw RouteInfoNotFoundException("목적지 정류장까지의 루트가 없습니다.")
        val routeInfoSize = routeInfo.size
        if (androidDevice.isArrived == true) {
            throw UserAlreadyArrivedException("${androidDevice.androidId} 사용자가 목적지에 도착했습니다. 더이상 위치를 받지 않습니다.")
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
        // 사용자 위치와 목적지 gps계산해서 100m이내면 곧 도착 푸시알림 보내기
        val distance = getDistance(
            routeInfo[nextStationCount].stationGPSY,
            routeInfo[nextStationCount].stationGPSX,
            latitude, longitude
        )

        if (distance <200.0) {
            if (remainingStationCount == 1 &&
                androidDevice.sendUserArrivedPushAlarmLeft1Station == false
            ) {
                // EC2 신호전달(TCP)
                val thread = ClientThread()
                thread.data[0] = "out"
                thread.data[1] = routeInfo[nextStationCount].stationName // 도착지정거장이름
                thread.data[2] = "no means data" // 의미없는 데이터
                thread.getPort = 5000
                thread.start()
                pushNotificationService.sendPushNotificationToToken(
                    PushNotificationRequestDto(
                        "알림",
                        "목적지에 곧 도착합니다. 하차벨 누르겠습니다.",
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
                        "알림",
                        "목적지까지 1정거장 남았습니다.",
                        androidDevice.firebaseToken
                    )
                )
                androidDevice.sendUserArrivedPushAlarmLeft2Station = true
            } else if (remainingStationCount == 3 &&
                androidDevice.sendUserArrivedPushAlarmLeft3Station == false
            ) {
                pushNotificationService.sendPushNotificationToToken(
                    PushNotificationRequestDto(
                        "알림",
                        "목적지까지 2정거장 남았습니다.",
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
        val androidDevice = androidDeviceRepository.findByAndroidId(androidId) ?: throw AndroidDeviceNotRegisterException("$androidId 의 androidId는 아직 등록되지 않았습니다.")
        // 도착지 정거장 gps 좌표 얻기
        val destinationGPSList: MutableList<Double> = getDestinationGPS(destinationName, destinationArsId)
        // 출발지 시간1, 시간2, GPSX, GPSY
        val startInfoList: MutableList<String> = getStartInfo(startArsId, rtNm)
        // 경로 busRouteId, 출발정류장 GPSX, 출발정류장 GPSY, 도착정류장 GPSX, 도착정류장 GPSY
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
                routeInfoRepository.delete(routeInfo[temp]) // 기존 경로 삭제
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
        val androidDevice = androidDeviceRepository.findByAndroidId(androidId) ?: throw AndroidDeviceNotRegisterException("$androidId 의 androidId는 아직 등록되지 않았습니다.")
        jobService.deleteJob(scheduler, androidDevice.androidId)
        androidDevice.isArrived = true
    }

    private fun scheduledPushAlarm(androidDevice: AndroidDevice, traTime1: Int, rtNm: String) {
        if (traTime1 == 0) {
            pushNotificationService.sendPushNotificationToToken(
                PushNotificationRequestDto(
                    "알림",
                    "운행 종료됬습니다.",
                    androidDevice.firebaseToken
                )
            )
            androidDevice.sendBusArrivedPushAlarm = true
            androidDevice.isArrived = true
        } else if (traTime1 <60) { // 60으로 다시 바꿀것
            pushNotificationService.sendPushNotificationToToken(
                PushNotificationRequestDto(
                    "알림",
                    "버스가 1분내로 도착합니다.",
                    androidDevice.firebaseToken
                )
            )
            androidDevice.sendBusArrivedPushAlarm = true
        } else {
            pushNotificationService.sendPushNotificationToToken(
                PushNotificationRequestDto(
                    "알림",
                    "$rtNm 번버스가 ${traTime1 / 60} 분 ${traTime1 % 60}초 후 도착합니다.",
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
            throw InvalidDestinationArsId("$destinationArsId 는 $destinationName 를 가리키지 않습니다.")
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
                    // 시간 넣기
                    startInfo.add(getTagValue("traTime1", eElement).toString())
                    startInfo.add(getTagValue("traTime2", eElement).toString())
                    // 출발지 GPS넣기
                    startInfo.add(getTagValue("gpsX", eElement).toString())
                    startInfo.add(getTagValue("gpsY", eElement).toString())
                    // 출발지 정거장 이름
                    startInfo.add(getTagValue("stNm", eElement).toString())
                    // 출발지 버스 노선 ID
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
                            // 정거장 이름
                            routeInfo[0].add(getTagValue("stationNm", eElement).toString())
                            // 경로 GPS넣기
                            routeInfo[1].add(getTagValue("gpsX", eElement).toString())
                            routeInfo[2].add(getTagValue("gpsY", eElement).toString())
                            routeFlag = false
                            break
                        } else {
                            // 정거장 이름
                            routeInfo[0].add(getTagValue("stationNm", eElement).toString())
                            // 경로 GPS넣기
                            routeInfo[1].add(getTagValue("gpsX", eElement).toString())
                            routeInfo[2].add(getTagValue("gpsY", eElement).toString())
                        }
                    }
                }
            }
            if (routeInfo.isEmpty()) {
                throw RouteInfoNotFoundException("경로에 해당하는 출발지를 찾을 수 없습니다.")
            }
            if (routeFlag == true) {
                throw RouteInfoNotFoundException("경로에 해당하는 목적지를 찾을 수 없습니다.")
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

    // TCP 쓰레드
    internal class ClientThread : Thread() {
        var data = arrayOfNulls<String>(3)
        var getPort = 5000
        override fun run() {
//            val host2 = "183.101.12.31"
            val host = "ec2-3-35-208-56.ap-northeast-2.compute.amazonaws.com"
            try {
                val socket = Socket(host, getPort)
                val outstream = ObjectOutputStream(socket.getOutputStream()) // 소켓의 출력 스트림 참조
                outstream.writeObject(data[0]) // 출력 스트림에 데이터 넣기
                outstream.flush() // 출력

                // 도착정류장 전송
                outstream.writeObject(data[1]) // 출력 스트림에 데이터 넣기
                outstream.flush() // 출력

                // 승차벨과 같게 만들어서 쓸데없는 데이터를 넣어준다.
                // 나중에 삭제예정.
                outstream.writeObject(data[2]) // 출력 스트림에 데이터 넣기
                outstream.flush() // 출력

                outstream.close()
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
