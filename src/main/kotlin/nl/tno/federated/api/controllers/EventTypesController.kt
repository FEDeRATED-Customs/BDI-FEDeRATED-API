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

    @PostMapping("/{type}/eventlifetime/{eventlifetime}")
    fun updateEventLifeTime(@PathVariable type: String, @PathVariable eventlifetime: Long) {
        eventTypeMapping.updateEventLifeTime(type, eventlifetime)
    }

}