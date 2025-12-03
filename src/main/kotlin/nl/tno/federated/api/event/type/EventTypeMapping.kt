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

package nl.tno.federated.api.event.type

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component



@Component
class EventTypeMapping(
    private val config: EventTypeMappingConfig,
    private val eventTypeService: EventTypeService
) {

    private val log = LoggerFactory.getLogger(EventTypeMapping::class.java)

    fun addEventType(eventType: EventType) {
        val existing = getEventTypes().firstOrNull { it.eventType.equals(eventType.eventType, true) }
        if (existing != null)
            throw EventTypeMappingException("Existing EventType found with same name: ${eventType.eventType}")
        if (eventType.minimize == true && eventType.minimalRml == null)
            throw EventTypeMappingException("EventType requires a minimalRML if minimize is set to true")
        if (eventType.minimize == true && eventType.minimalRml != null &&
            !(eventType.rml.contains("#UUID") && eventType.minimalRml.contains("#UUID")))
                    throw EventTypeMappingException("EventType requires an UUID for the event in both normal as minimal RML if minimize is enabled.")
        eventTypeService.addEventType(eventType)
    }

    fun deleteEventType(eventType: String) {
        eventTypeService.deleteEventType(eventType)
    }

    fun getEventType(eventType: String): EventType? {
        return getEventTypes().find { it.eventType == eventType }
    }

    fun getEventTypes(): List<EventType> {
        val configured = config.types.map { it.toEventType() }
        val findAll = eventTypeService.getAllEventTypes()
        return configured + findAll
    }

    fun readShaclShapes(): List<String> {
        return eventTypeService.getAllEventTypes().mapNotNull { it.shacl }
    }

    fun updateShacl(eventType: String, shacl: String) {
        val current = getEventType(eventType)
            ?: throw EventTypeMappingException("EventType not found: ${eventType}")
        eventTypeService.updateEventType(current.copy(shacl = shacl))
    }

    fun updateSchemaDefinition(eventType: String, schema: String) {
        val current = getEventType(eventType)
            ?: throw EventTypeMappingException("EventType not found: ${eventType}")
        eventTypeService.updateEventType(current.copy(schemaDefinition = schema))
    }

    fun updateRml(eventType: String, rml: String) {
        val current = getEventType(eventType)
            ?: throw EventTypeMappingException("EventType not found: ${eventType}")
        eventTypeService.updateEventType(current.copy(rml = rml))
    }

    fun updateMinimalRml(eventType: String, minimalRml: String) {
        val current = getEventType(eventType)
            ?: throw EventTypeMappingException("EventType not found: ${eventType}")
        eventTypeService.updateEventType(current.copy(minimalRml = minimalRml))
    }

    fun updateEventLifeTime(eventType: String, eventLifeTime: Long) {
        val current = getEventType(eventType)
            ?: throw EventTypeMappingException("EventType not found: ${eventType}")
        eventTypeService.updateEventType(current.copy(eventLifeTime = eventLifeTime))
    }

}
