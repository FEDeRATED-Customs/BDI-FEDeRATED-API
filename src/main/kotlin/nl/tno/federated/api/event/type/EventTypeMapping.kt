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
