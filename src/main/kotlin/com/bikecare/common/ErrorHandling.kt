package com.bikecare.common

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class ApiFieldError(val field: String, val message: String)
data class ApiError(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String?,
    val path: String,
    val errors: List<ApiFieldError> = emptyList()
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, req: HttpServletRequest): ResponseEntity<ApiError> {
        val errors = ex.bindingResult.fieldErrors.map {
            ApiFieldError(field = it.field, message = it.defaultMessage ?: "Invalid value")
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, errors)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException, req: HttpServletRequest): ResponseEntity<ApiError> {
        val errors = ex.constraintViolations.map {
            ApiFieldError(field = it.propertyPath.toString(), message = it.message)
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, errors)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(ex: DataIntegrityViolationException, req: HttpServletRequest) =
        build(HttpStatus.CONFLICT, "Data integrity violation", req)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(ex: HttpMessageNotReadableException, req: HttpServletRequest) =
        build(HttpStatus.BAD_REQUEST, "Malformed JSON request", req)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException, req: HttpServletRequest) =
        build(HttpStatus.FORBIDDEN, "Access denied", req)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArg(ex: IllegalArgumentException, req: HttpServletRequest): ResponseEntity<ApiError> {
        val status = if (ex.message == "Forbidden") HttpStatus.FORBIDDEN else HttpStatus.BAD_REQUEST
        val msg = if (ex.message == "Forbidden") "Access denied" else ex.message ?: "Bad request"
        return build(status, msg, req)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException, req: HttpServletRequest) =
        build(HttpStatus.NOT_FOUND, "Not found", req)

    @ExceptionHandler(Exception::class)
    fun handleOther(ex: Exception, req: HttpServletRequest) =
        build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req)

    private fun build(
        status: HttpStatus,
        message: String,
        req: HttpServletRequest,
        errors: List<ApiFieldError> = emptyList()
    ): ResponseEntity<ApiError> {
        // Použij explicitní metody (Jakarta Servlet API)
        val path = runCatching { req.getRequestURI() }
            .getOrElse { runCatching { req.getRequestURL().toString() }.getOrDefault("") }

        val body = ApiError(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = path,
            errors = errors
        )
        return ResponseEntity.status(status).body(body)
    }
}
