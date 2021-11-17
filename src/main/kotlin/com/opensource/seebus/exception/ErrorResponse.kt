package com.opensource.seebus.exception

import org.springframework.http.HttpStatus
import java.time.LocalDateTime

class ErrorResponse(
    val timestamp: LocalDateTime,
    val httpStatus: HttpStatus,
    val errorCode: String,
    val message: String,
    val path: String
)
