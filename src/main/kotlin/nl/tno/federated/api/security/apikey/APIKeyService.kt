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
package nl.tno.federated.api.security.apikey

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class APIKeyService ( private val apiKeyRepository: APIKeyRepository) {

    companion object {
        private val log = LoggerFactory.getLogger(APIKeyService::class.java)
        private var objectMapper = jacksonObjectMapper()
        private val encoder = BCryptPasswordEncoder()
    }

    fun findAPIKey(key: String) : APIKey? {
        return apiKeyRepository.findByApiKey(key)?.let{ APIKey(it.apiKey,it.roles,it.isEnabled)}
    }

    fun addAPIKey(key: APIKey) {
        apiKeyRepository.save(APIKeyEntity(apiKey = key.apiKey, roles = key.roles, isEnabled = key.isEnabled))
    }


    fun addRandomAPIKey(roles: String) : APIKey {
       return apiKeyRepository.save(APIKeyEntity(apiKey = "API_${getRandomString(12)}", roles = roles, isEnabled = true)).let {
           APIKey(it.apiKey,it.roles,it.isEnabled)
       }
    }

    fun updateAPIKey(key: String, apiKey: APIKey) {
        apiKeyRepository.findByApiKey(key)?.let {
            if (it.apiKey != apiKey.apiKey)  it.apiKey = apiKey.apiKey
            if (it.roles != apiKey.roles)  it.roles = apiKey.roles
            if (it.isEnabled != apiKey.isEnabled)  it.isEnabled = apiKey.isEnabled
            apiKeyRepository.save(it)
        }
    }


    fun disableAPIKey(key: String) {
        apiKeyRepository.findByApiKey(key)?.let {
            it.isEnabled = false
            apiKeyRepository.save(it)
        }
    }

    fun getAPIKeys(): List<APIKey> {
        return apiKeyRepository.findAll().map{ APIKey(it.apiKey, it.roles , it.isEnabled)}
    }

    private fun getRandomString(length: Int) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}