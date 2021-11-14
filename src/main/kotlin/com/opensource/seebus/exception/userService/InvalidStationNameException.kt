package com.opensource.seebus.exception.userService

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.lang.RuntimeException

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidStationNameException(errorMessage: String) : RuntimeException(errorMessage)
