package com.opensource.seebus.dto.response

class UserLocationResponseDto(
    val androidId: String,
    val nextStationName: String,
    val remainingStationCount: Int,
    val isArrived: Boolean
)
