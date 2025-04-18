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
        log.info("Add a new user")
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
    fun getAPIKeyss() : ResponseEntity<JsonNode?> {
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

