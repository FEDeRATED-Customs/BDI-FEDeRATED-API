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
 * contains: Controller to provide REST APIs to manage the event types
 *
 * @property eventTypeMapping EventTypeMapping , database mapping for event types
 */

package nl.tno.federated.api.controllers

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import nl.tno.federated.api.event.type.EventType
import nl.tno.federated.api.event.type.EventTypeMapping
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/event-types", produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "EventTypesController", description = "Returns info regarding the supported event types by this node.")
class EventTypesController(private val eventTypeMapping: EventTypeMapping) {

    @GetMapping
    fun getEventTypes(): List<EventType> {
        return eventTypeMapping.getEventTypes()
    }

    @PostMapping
    fun newEventType(@Valid @RequestBody eventType: EventType) {
        eventTypeMapping.addEventType(eventType)
    }

    @DeleteMapping("/{eventType}")
    fun deleteEventType(@PathVariable eventType: String) {
        eventTypeMapping.deleteEventType(eventType)
    }

    @GetMapping("/{type}/shacl", produces = ["text/turtle"])
    fun getShacl(@PathVariable type: String): String? {
        return eventTypeMapping.getEventTypes().firstOrNull { it.eventType == type }?.shacl
    }

    @PostMapping("/{type}/shacl", consumes = [MediaType.TEXT_PLAIN_VALUE])
    fun updateShacl(@PathVariable type: String, @RequestBody shacl: String) {
        eventTypeMapping.updateShacl(type, shacl)
    }

    @PostMapping("/{type}/schemadefinition", consumes = [MediaType.TEXT_PLAIN_VALUE])
    fun updateSchemaDefinition(@PathVariable type: String, @RequestBody schema: String) {
        eventTypeMapping.updateSchemaDefinition(type, schema)
    }

    @GetMapping("/{type}/schemadefinition", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getSchemaDefinition(@PathVariable type: String): String?  {
        return eventTypeMapping.getEventType(type)?.schemaDefinition
    }

    @GetMapping("/{type}/rml", produces = ["text/turtle"])
    fun getRml(@PathVariable type: String): String? {
        return eventTypeMapping.getEventType( type )?.rml
    }

    @PostMapping("/{type}/rml", consumes = [MediaType.TEXT_PLAIN_VALUE])
    fun updateRml(@PathVariable type: String, @RequestBody rml: String) {
        eventTypeMapping.updateRml(type, rml)
    }

    @GetMapping("/{type}/minimalRml", produces = ["text/turtle"])
    fun getMinimalRml(@PathVariable type: String): String? {
        return eventTypeMapping.getEventType( type )?.minimalRml
    }

    @PostMapping("/{type}/minialRml", consumes = [MediaType.TEXT_PLAIN_VALUE])
    fun updateMinimalRml(@PathVariable type: String, @RequestBody minimalRml: String) {
        eventTypeMapping.updateMinimalRml(type, minimalRml)
    }
}