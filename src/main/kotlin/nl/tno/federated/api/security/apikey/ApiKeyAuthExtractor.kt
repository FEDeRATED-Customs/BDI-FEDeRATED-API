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

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.stereotype.Service
import java.util.*

@Service
class ApiKeyAuthExtractor(val apiKeyService: APIKeyService) {

    @Autowired
    private lateinit var environment: Environment

    fun extract(request: HttpServletRequest): Optional<Authentication> {
        val headerName = environment.getProperty("federated.node.api.security.xapikey.header")
        try {
            val providedKey = request.getHeader(headerName)
           /* if ("OPTIONS".equals(request.method, ignoreCase = true)) {
                return Optional.of(ApiKeyAuth(providedKey, AuthorityUtils.createAuthorityList("API_USER")))
            }*/
            apiKeyService.findAPIKey(providedKey)?.let {
                return Optional.of(ApiKeyAuth(providedKey, AuthorityUtils.createAuthorityList(it.roles)))
            }

            return Optional.empty()
        } catch (e: Exception ) {
            return Optional.empty()
        }
    }
}
