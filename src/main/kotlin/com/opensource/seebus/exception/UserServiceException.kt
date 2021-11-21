package com.opensource.seebus.exception

import com.opensource.seebus.exception.userService.AndroidDeviceNotRegisterException
import com.opensource.seebus.exception.userService.InvalidDestinationArsId
import com.opensource.seebus.exception.userService.InvalidStationNameException
import com.opensource.seebus.exception.userService.RouteInfoNotFoundException
import com.opensource.seebus.exception.userService.UserAlreadyArrivedException
import com.opensource.seebus.exception.userService.UserInfoNotFoundException
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime
import javax.servlet.http.HttpServletRequest

@RestControllerAdvice
class UserServiceException {
    private val log = KotlinLogging.logger {}
    @ExceptionHandler(InvalidStationNameException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidStationNameException(exception: InvalidStationNameException, request: HttpServletRequest): ErrorResponse {
        log.warn { "요청 URI : " + request.requestURI + " 에러 메시지 : " + exception.message }
        return ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "유저서비스 오류 1번", exception.message!!, request.requestURI)
    }

    @ExceptionHandler(InvalidDestinationArsId::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidDestinationArsId(exception: InvalidDestinationArsId, request: HttpServletRequest): ErrorResponse {
        log.warn { "요청 URI : " + request.requestURI + " 에러 메시지 : " + exception.message }
        return ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "유저서비스 오류 2번", exception.message!!, request.requestURI)
    }

    @ExceptionHandler(AndroidDeviceNotRegisterException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleInvalidAndroidIdException(exception: AndroidDeviceNotRegisterException, request: HttpServletRequest): ErrorResponse {
        log.warn { "요청 URI : " + request.requestURI + " 에러 메시지 : " + exception.message }
        return ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "유저서비스 오류 3번", exception.message!!, request.requestURI)
    }

    @ExceptionHandler(UserInfoNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleUserInfoNotFoundException(exception: UserInfoNotFoundException, request: HttpServletRequest): ErrorResponse {
        log.warn { "요청 URI : " + request.requestURI + " 에러 메시지 : " + exception.message }
        return ErrorResponse(LocalDateTime.now(), HttpStatus.NOT_FOUND, "유저서비스 오류 4번", exception.message!!, request.requestURI)
    }

    @ExceptionHandler(RouteInfoNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleRouteInfoNotFoundException(exception: RouteInfoNotFoundException, request: HttpServletRequest): ErrorResponse {
        log.warn { "요청 URI : " + request.requestURI + " 에러 메시지 : " + exception.message }
        return ErrorResponse(LocalDateTime.now(), HttpStatus.NOT_FOUND, "유저서비스 오류 5번", exception.message!!, request.requestURI)
    }

    @ExceptionHandler(UserAlreadyArrivedException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleUserAlreadyArrivedException(exception: UserAlreadyArrivedException, request: HttpServletRequest): ErrorResponse {
        log.warn { "요청 URI : " + request.requestURI + " 에러 메시지 : " + exception.message }
        return ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "유저서비스 오류 6번", exception.message!!, request.requestURI)
    }
}
