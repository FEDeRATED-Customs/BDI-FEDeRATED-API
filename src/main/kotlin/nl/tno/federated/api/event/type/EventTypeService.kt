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

import org.springframework.stereotype.Service

class EventTypeServiceException(msg: String) : Exception(msg)

@Service
class EventTypeService(private val eventTypeRepository: EventTypeRepository) {

    fun addEventType(e: EventType): EventTypeEntity {
        val current = eventTypeRepository.findByEventType(eventType = e.eventType)
        if( current != null) throw EventTypeServiceException("EventType already exists: ${e.eventType}")
        return eventTypeRepository.saveAndFlush(EventTypeEntity(eventType = e.eventType, rml = e.rml, shacl = e.shacl, schemaDefinition = e.schemaDefinition) )
    }

    fun getAllEventTypes(): List<EventType> {
        return eventTypeRepository.findAll().map { EventType(eventType = it.eventType, rml = it.rml, shacl = it.shacl, schemaDefinition = it.schemaDefinition) }
    }

    fun deleteEventType(eventType: String) {
        val current = eventTypeRepository.findByEventType(eventType = eventType)
            ?: throw EventTypeServiceException("No EventType found: ${eventType}")
        eventTypeRepository.delete(current)
    }

    fun updateEventType(update: EventType) {
        val current = eventTypeRepository.findByEventType(eventType = update.eventType)
            ?: throw EventTypeServiceException("No EventType found: ${update.eventType}")
        val copy = current.copy(rml = update.rml, shacl = update.shacl)
        eventTypeRepository.saveAndFlush(copy)
    }

    fun getEventType(eventType: String): EventType? {
        return eventTypeRepository.findByEventType(eventType)?.let {
            EventType(
                eventType = it.eventType,
                rml = it.rml,
                shacl = it.shacl,
                schemaDefinition = it.schemaDefinition
            )
        }
    }
}