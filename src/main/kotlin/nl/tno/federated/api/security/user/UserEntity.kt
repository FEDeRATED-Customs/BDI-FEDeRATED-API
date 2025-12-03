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
package nl.tno.federated.api.security.user

import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.*


enum class Roles(@JsonValue val role: String) {
    API_USER ("API_USER"),
    API_EVENT ("API_EVENT"),
    API_MESSAGE ("API_MESSAGE"),
    API_ADMIN ("API_ADMIN")
}

@Entity
@Table(name = "API_USERS")
data class UserEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "USERNAME")
    var username: String,
    @Column(name = "PASSWORD")
    var password: String,
    @Column(name = "ROLES")
    var roles: String = "API_USER",
    @Column(name = "ISENABLED")
    var isEnabled: Boolean = true,
)

