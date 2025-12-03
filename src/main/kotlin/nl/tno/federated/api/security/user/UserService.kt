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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService ( private val userRepository: UserRepository) {

    companion object {
        private val log = LoggerFactory.getLogger(UserService::class.java)
        private var objectMapper = jacksonObjectMapper()
        private val encoder = BCryptPasswordEncoder()
    }

    fun findUser(username: String) : User? {
        return userRepository.findByUsername(username)?.let{ it -> User(it.username, "************", it.roles, it.isEnabled)}
    }

    fun addUser(user: User) {
        userRepository.findByUsername(user.username)?.let { throw UserManagementException("Username '${user.username}' is allready in use ") }
        userRepository.save(UserEntity(username = user.username, password = encoder.encode(user.password), roles = user.roles, isEnabled = user.isEnabled))
    }

    fun updateUser(username: String, user: User) {
        userRepository.findByUsername(username)?.let { it ->
            if (it.username != user.username)  it.username = user.username
            if (it.roles != user.roles)  it.roles = user.roles
            if (it.isEnabled != user.isEnabled)  it.isEnabled = user.isEnabled
            userRepository.save(it)
        }
    }

    fun updatePassword(username: String, password: String) {
        userRepository.findByUsername(username)?.let { it ->
            it.password = encoder.encode(password)
            userRepository.save(it)
        }
    }

    fun disableUser(username: String) {
        userRepository.findByUsername(username)?.let { it ->
            it.isEnabled = false
            userRepository.save(it)
        }
    }

    fun getUsers(): List<User> {
        return userRepository.findAll().map{it -> User(it.username, "************", it.roles , it.isEnabled)}
    }
}