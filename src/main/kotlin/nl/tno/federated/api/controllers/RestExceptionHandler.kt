// FEDeRATED node
//
// Copyright (c) 2024-2025 Netherlands Organization for Applied Scientific Research TNO
//
// This file is part of the FEDeRATED Node API.
//
// This software is provided to Dutch Customs under the following terms:
//
// * Dutch Customs is granted a non-exclusive, non-transferable, and non-sublicensable license
//   to use, modify, and maintain the software for internal purposes only.
//
// * Dutch Customs is not permitted to disclose, distribute, or share the software, in whole or in
//   part, to any third party without prior written consent from TNO.
//
// * Any modifications made by Dutch Customs to the software must include this copyright and
//   license notice.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
// IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
// FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
// BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/**
 *
 * contains: Handlers for REST API exceptions
 *
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