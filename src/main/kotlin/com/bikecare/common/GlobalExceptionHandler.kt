package com.bikecare.common

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class ValidationError(
    val field: String,
    val message: String
)

data class ApiErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String?,
    val path: String,
    val errors: List<ValidationError> = emptyList()
)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    private fun build(
        status: HttpStatus,
        request: HttpServletRequest,
        message: String?,
        errors: List<ValidationError> = emptyList()
    ): ResponseEntity<ApiErrorResponse> {
        val body = ApiErrorResponse(
            timestamp = Instant.now(),
            status = status.value(),
            error = status.reasonPhrase, // POZOR: importuj org.springframework.http.HttpStatus
            message = message,
            path = request.requestURI,
            errors = errors
        )
        return ResponseEntity.status(status).body(body)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.map { fe: FieldError ->
            ValidationError(fe.field, fe.defaultMessage ?: "Invalid value")
        }
        val globalErrors = ex.bindingResult.globalErrors.map { ge ->
            ValidationError(ge.objectName, ge.defaultMessage ?: "Invalid value")
        }
        return build(HttpStatus.BAD_REQUEST, request, "Validation failed", fieldErrors + globalErrors)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val errors = ex.constraintViolations.map { v ->
            ValidationError(v.propertyPath.toString(), v.message)
        }
        return build(HttpStatus.BAD_REQUEST, request, "Validation failed", errors.toList())
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> =
        build(HttpStatus.BAD_REQUEST, request, "Malformed JSON request")

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(
        ex: NoSuchElementException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> =
        build(HttpStatus.NOT_FOUND, request, ex.message ?: "Resource not found")

    @ExceptionHandler(IllegalArgumentException::class, IllegalStateException::class)
    fun handleBadRequest(
        ex: RuntimeException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> =
        build(HttpStatus.BAD_REQUEST, request, ex.message)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> =
        build(HttpStatus.FORBIDDEN, request, "Forbidden")

    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        log.error("Unhandled exception on {} {}", request.method, request.requestURI, ex)
        return build(HttpStatus.INTERNAL_SERVER_ERROR, request, "Unexpected error")
    }
}
