/*
 *  Copyright (c) 2025 Netherlands Organization for Applied Scientific Research TNO
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package nl.tno.federated.api.controllers

import nl.tno.federated.api.event.InvalidEventDataException
import nl.tno.federated.api.event.mapper.UnsupportedEventTypeException
import nl.tno.federated.api.event.validation.JSONValidationException
import nl.tno.federated.api.event.validation.ShaclValidationException
import nl.tno.federated.api.util.InvalidRDFException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

/**
 * Partial implementation of https://www.rfc-editor.org/rfc/rfc7807
 *
 * In Spring Boot 3 this comes built-in
 */
data class ProblemDetail(val type: String?, val title: String?, val detail: String? = null)

@ControllerAdvice(basePackages = ["nl.tno.federated"])
class RestExceptionHandler {

    private val log = LoggerFactory.getLogger(RestExceptionHandler::class.java)

    @ExceptionHandler(Throwable::class)
    fun handleUncaught(t: Throwable): ResponseEntity<ProblemDetail> {
        log.info("Uncaught exception while executing request: {}", t.message, t)
        return ResponseEntity(ProblemDetail(type = t.javaClass.name, title = t.message, detail = "See error logs for more details."), HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(InvalidEventDataException::class)
    fun invalidEventDataException(e: InvalidEventDataException): ResponseEntity<ProblemDetail> {
        log.debug("Invalid Event data provided. Message: {}", e.message)
        return ResponseEntity(ProblemDetail(type = e.javaClass.name, title = e.message), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(JSONValidationException::class)
    fun jsonValidationException(e: JSONValidationException): ResponseEntity<ProblemDetail> {
        log.debug("JSON validation failed. Message: {}", e.message)
        return ResponseEntity(ProblemDetail(type = e.javaClass.name, title = e.message), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(ShaclValidationException::class)
    fun shaclValidationException(e: ShaclValidationException): ResponseEntity<ProblemDetail> {
        log.debug("SHACL validation failed. Message: {}", e.message)
        return ResponseEntity(ProblemDetail(type = e.javaClass.name, title = e.message), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(InvalidRDFException::class)
    fun invalidRDFException(e: InvalidRDFException): ResponseEntity<ProblemDetail> {
        log.debug("Invalid Event data provided. Message: {}", e.message)
        return ResponseEntity(ProblemDetail(type = e.javaClass.name, title = "Invalid RDF event data supplied, expected text/turtle.", detail = e.message), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(UnsupportedEventTypeException::class)
    fun unsupportedEventTypeException(e: UnsupportedEventTypeException): ResponseEntity<ProblemDetail> {
        log.debug("Unsupported Event type provided. Message: {}", e.message)
        return ResponseEntity(ProblemDetail(type = e.javaClass.name, title = "EventType that was supplied is not supported!", detail = e.message), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(InvalidPageCriteria::class)
    fun invalidPageCriteriaException(e: InvalidPageCriteria): ResponseEntity<ProblemDetail> {
        log.debug("Unsupported Event type provided. Message: {}", e.message)
        return ResponseEntity(ProblemDetail(type = e.javaClass.name, title = "Page size should be greater than zero.", detail = e.message), HttpStatus.BAD_REQUEST)
    }

}