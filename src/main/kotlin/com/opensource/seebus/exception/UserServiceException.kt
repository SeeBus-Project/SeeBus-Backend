package com.opensource.seebus.exception

import com.opensource.seebus.dto.response.ErrorResponse
import com.opensource.seebus.exception.userService.AndroidDeviceNotFoundException
import com.opensource.seebus.exception.userService.InvalidDestinationArsId
import com.opensource.seebus.exception.userService.InvalidStationNameException
import com.opensource.seebus.exception.userService.UserInfoNotFoundException
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime
import javax.servlet.http.HttpServletRequest

@RestControllerAdvice
class UserServiceException {
    private val log = KotlinLogging.logger {}
    @ExceptionHandler(InvalidStationNameException::class)
    fun handleInvalidStationNameException(exception: InvalidStationNameException, request: HttpServletRequest): ErrorResponse {
        log.warn { "요청 URI : " + request.requestURI + " 에러 메시지 : " + exception.message }
        return ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "유저서비스 오류 1번", exception.message!!, request.requestURI)
    }

    @ExceptionHandler(InvalidDestinationArsId::class)
    fun handleInvalidDestinationArsId(exception: InvalidDestinationArsId, request: HttpServletRequest): ErrorResponse {
        log.warn { "요청 URI : " + request.requestURI + " 에러 메시지 : " + exception.message }
        return ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "유저서비스 오류 2번", exception.message!!, request.requestURI)
    }

    @ExceptionHandler(AndroidDeviceNotFoundException::class)
    fun handleInvalidAndroidIdException(exception: AndroidDeviceNotFoundException, request: HttpServletRequest): ErrorResponse {
        log.warn { "요청 URI : " + request.requestURI + " 에러 메시지 : " + exception.message }
        return ErrorResponse(LocalDateTime.now(), HttpStatus.NOT_FOUND, "유저서비스 오류 3번", exception.message!!, request.requestURI)
    }

    @ExceptionHandler(UserInfoNotFoundException::class)
    fun handleInvalidAndroidIdException(exception: UserInfoNotFoundException, request: HttpServletRequest): ErrorResponse {
        log.warn { "요청 URI : " + request.requestURI + " 에러 메시지 : " + exception.message }
        return ErrorResponse(LocalDateTime.now(), HttpStatus.NOT_FOUND, "유저서비스 오류 4번", exception.message!!, request.requestURI)
    }
}
