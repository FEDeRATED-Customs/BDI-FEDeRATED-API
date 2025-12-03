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

import org.springframework.stereotype.Service

class EventTypeServiceException(msg: String) : Exception(msg)

@Service
class EventTypeService(private val eventTypeRepository: EventTypeRepository) {

    fun addEventType(e: EventType): EventTypeEntity {
        val current = eventTypeRepository.findByEventType(eventType = e.eventType)
        if( current != null) throw EventTypeServiceException("EventType already exists: ${e.eventType}")
        return eventTypeRepository.saveAndFlush(EventTypeEntity(eventType = e.eventType, rml = e.rml, shacl = e.shacl, schemaDefinition = e.schemaDefinition, minimalRml = e.minimalRml, minimize = e.minimize, eventLifeTime = e.eventLifeTime) )
    }

    fun getAllEventTypes(): List<EventType> {
        return eventTypeRepository.findAll().map { EventType(eventType = it.eventType, rml = it.rml, shacl = it.shacl, schemaDefinition = it.schemaDefinition , minimalRml = it.minimalRml, minimize = it.minimize, eventLifeTime = it.eventLifeTime) }
    }

    fun deleteEventType(eventType: String) {
        val current = eventTypeRepository.findByEventType(eventType = eventType)
            ?: throw EventTypeServiceException("No EventType found: ${eventType}")
        eventTypeRepository.delete(current)
    }

    fun updateEventType(update: EventType) {
        val current = eventTypeRepository.findByEventType(eventType = update.eventType)
            ?: throw EventTypeServiceException("No EventType found: ${update.eventType}")
        val copy = current.copy(rml = update.rml, shacl = update.shacl, minimalRml = update.minimalRml, minimize = update.minimize, eventLifeTime = update.eventLifeTime)
        eventTypeRepository.saveAndFlush(copy)
    }

    fun getEventType(eventType: String): EventType? {
        return eventTypeRepository.findByEventType(eventType)?.let {
            EventType(
                eventType = it.eventType,
                rml = it.rml,
                shacl = it.shacl,
                schemaDefinition = it.schemaDefinition,
                minimalRml = it.minimalRml,
                minimize = it.minimize,
                eventLifeTime = it.eventLifeTime

            )
        }
    }
}