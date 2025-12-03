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
package nl.tno.federated.api.event

import nl.tno.federated.api.event.type.EventType
import java.time.Instant.now
import java.util.*

/**
 * Event class containing the original event json, the event type, generated UUID and event RDF.
 */
data class EnrichedEvent(
    var eventJson: String,
    val eventType: EventType,
    val eventUUID: UUID,
    var eventRDF: String,
    var strippedEventRDF: String? = null,
    var recordedTime: Long? = now().epochSecond
)
