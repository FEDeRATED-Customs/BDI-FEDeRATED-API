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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import nl.tno.federated.api.security.apikey.APIKey
import nl.tno.federated.api.security.apikey.APIKeyService
import nl.tno.federated.api.security.user.User
import nl.tno.federated.api.security.user.UserService
import nl.tno.federated.api.util.toJsonNode
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@Tag(name = "UserController", description = "Manages the users and API keys")
class UserController (private val userService: UserService, private val apiKeyService: APIKeyService) {

    companion object {
        private val log = LoggerFactory.getLogger(UserController::class.java)
        private var objectMapper = jacksonObjectMapper().registerModules(JavaTimeModule())
    }

    @GetMapping("/users")
    fun getUsers() : ResponseEntity<JsonNode?> {
        log.info("Get all users")
        return ResponseEntity.ok(userService.getUsers().toJsonNode(objectMapper))
    }

    @PostMapping("/users")
    fun newUser(@Valid @RequestBody user: User) {
        log.info("Add a new User")
        userService.addUser(user)
    }

    @PostMapping("/users/{username}")
    fun changeUser(@PathVariable username: String, @Valid @RequestBody user: User) {
        log.info("update a user")
        userService.updateUser(username, user)
    }

    @PostMapping("/users/{username}/password")
    fun changePassword(@PathVariable username: String, @RequestParam password: String) {
        log.info("Change a password")
        userService.updatePassword(username, password)
    }


    @DeleteMapping("/users/{username}/")
    fun disableUser(@PathVariable username: String) {
        log.info("Disable an user")
        userService.disableUser(username)
    }

    @GetMapping("/apikeys")
    fun getAPIKeys() : ResponseEntity<JsonNode?> {
        log.info("Get all APIKeys")
        return ResponseEntity.ok(apiKeyService.getAPIKeys().toJsonNode(objectMapper))
    }

    @PostMapping("/apikeys")
    fun newAPIKey(@Valid @RequestBody key: APIKey) {
        log.info("Add a new APIKey")
        apiKeyService.addAPIKey(key)
    }

    @PostMapping("/apikeys/random")
    fun newRandomAPIKey(@RequestParam roles: String) : ResponseEntity<JsonNode?> {
        log.info("Add a new randomAPIKey")
        return ResponseEntity.ok(apiKeyService.addRandomAPIKey(roles).toJsonNode(objectMapper))
    }

    @PostMapping("/apikeys/{key}")
    fun changeAPIKey(@PathVariable key: String, @Valid @RequestBody apiKey: APIKey) {
        log.info("change a APIkey")
        apiKeyService.updateAPIKey(key, apiKey)
    }

    @DeleteMapping("/apikeys/{key}/")
    fun disableAPIKey(@PathVariable key: String) {
        log.info("Disable an APIKey")
        apiKeyService.disableAPIKey(key)
    }


}

