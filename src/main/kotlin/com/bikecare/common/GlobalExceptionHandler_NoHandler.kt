package com.bikecare.common

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.NoHandlerFoundException
import java.time.Instant

@RestControllerAdvice
class GlobalNotFoundHandler {

    data class ApiError(
        val timestamp: Instant = Instant.now(),
        val status: Int,
        val error: String,
        val message: String,
        val path: String,
        val errors: List<Map<String, String>> = emptyList()
    )

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandler(ex: NoHandlerFoundException, req: HttpServletRequest): ResponseEntity<ApiError> {
        val body = ApiError(
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = "No handler for ${ex.httpMethod} ${ex.requestURL}",
            path = req.requestURI
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body)
    }
}
