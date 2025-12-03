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

package nl.tno.federated.api.security

import nl.tno.federated.api.security.apikey.APIKey
import nl.tno.federated.api.security.apikey.APIKeyService
import nl.tno.federated.api.security.apikey.ApiKeyAuthFilter
import nl.tno.federated.api.security.user.JPAUserDetailService
import nl.tno.federated.api.security.user.Roles
import nl.tno.federated.api.security.user.User
import nl.tno.federated.api.security.user.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@EnableWebSecurity
@Configuration
class SecurityConfig(private val jpaUserDetailService: JPAUserDetailService,
                     private val userService: UserService,
                     private val apiKeyService: APIKeyService)  {

    @Autowired
    private lateinit var environment: Environment


    fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(jpaUserDetailsService()).passwordEncoder(passwordEncoder())
    }

    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder {
        return object : BCryptPasswordEncoder() {
            override fun encode(rawPassword: CharSequence?): String {
                return BCryptPasswordEncoder().encode(rawPassword)
            }
            override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean {
                val result = BCryptPasswordEncoder().matches(rawPassword, encodedPassword)
                return result
            }
        }
    }

    @Bean
    fun jpaUserDetailsService(): JPAUserDetailService {

        // add admin user if not exists
        if (userService.findUser("admin") == null) {
            environment.getProperty("federated.node.api.security.userpass.adminpass")?.let {
                val user = User(
                    username = "admin",
                    password = it,
                    roles = Roles.API_ADMIN.role,
                    isEnabled = true
                )
                userService.addUser(user)
            }
        }

        environment.getProperty("federated.node.api.security.xapikey.dokey")?.let {
            if (apiKeyService.findAPIKey(it) == null) {
                apiKeyService.addAPIKey(APIKey(it, Roles.API_MESSAGE.role, true))
            }
        }

        return jpaUserDetailService
    }


    @Bean
    @ConditionalOnProperty(prefix = "federated.node.api.security", name = ["type"], havingValue = "none")
    @Throws(Exception::class)
    fun securityFilterChainAllAccess(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf {
                it.disable()
            }.cors {
                it.disable()
            }
            .authorizeHttpRequests { requests ->
                requests.anyRequest().permitAll()
            }
        return http.build()
    }

    @Bean
    @ConditionalOnProperty(prefix = "federated.node.api.security", name = ["type"], havingValue = "combined")
    @Throws(Exception::class)
    fun securityFilterChainCombined(http: HttpSecurity,apiKeyAuthFilter: ApiKeyAuthFilter): SecurityFilterChain {

        http
            .csrf {
                it.disable()
            }
            .cors {
                it.disable()
            }
            .httpBasic {
                it.realmName("API login")

            }
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers(HttpMethod.OPTIONS,"api/**").permitAll()
                    .requestMatchers("/*").permitAll()
                    .requestMatchers("/assets/**").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/v3/**").permitAll()
                    .requestMatchers("/api/message/**").hasAnyAuthority(Roles.API_MESSAGE.role,Roles.API_ADMIN.role)
                    .requestMatchers("/api/eventviewer/**").hasAnyAuthority(Roles.API_USER.role,Roles.API_ADMIN.role)
                    .requestMatchers("/api/distribution-rules/**").hasAnyAuthority(Roles.API_ADMIN.role)
                    .requestMatchers("/api/users/**").hasAnyAuthority(Roles.API_ADMIN.role)
                    .requestMatchers("/api/apikeys/**").hasAnyAuthority(Roles.API_ADMIN.role)
                    .requestMatchers("/event-viewer/**").hasAnyAuthority(Roles.API_USER.role,Roles.API_ADMIN.role)
                    .requestMatchers("/api/event-types/**").hasAnyAuthority(Roles.API_USER.role,Roles.API_ADMIN.role)
                    .requestMatchers("/api/events/**").hasAnyAuthority(Roles.API_USER.role,Roles.API_ADMIN.role)
                    .requestMatchers("/api/sparql/**").hasAnyAuthority(Roles.API_USER.role,Roles.API_ADMIN.role)
                    .requestMatchers("/api/webhooks/**").hasAnyAuthority(Roles.API_ADMIN.role)
                    .anyRequest().authenticated()
            }
            .userDetailsService(jpaUserDetailsService())
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter::class.java) // Add our custom filter
        return http.build()
    }

    @Bean
    @ConditionalOnProperty(prefix = "federated.node.api.security", name = ["type"], havingValue = "userpass")
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf {
                it.disable()
            }
            .cors {
                it.disable()
            }
            .httpBasic {
                it.realmName("API login")
            }
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers("/*").permitAll()
                    .requestMatchers("/assets/**").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/v3/**").permitAll()
                    .requestMatchers("/event-viewer/**").hasAnyAuthority(Roles.API_USER.role,Roles.API_ADMIN.role)
                    .requestMatchers("/api/message/**").hasAnyAuthority(Roles.API_MESSAGE.role,Roles.API_ADMIN.role)
                    .requestMatchers("/api/eventviewer/**").hasAnyAuthority(Roles.API_USER.role,Roles.API_ADMIN.role)
                    .requestMatchers("/api/users/**").hasAnyAuthority(Roles.API_ADMIN.role)
                    .requestMatchers("/api/apikeys/**").hasAnyAuthority(Roles.API_ADMIN.role)
                    .requestMatchers("/api/distribution-rules/**").hasAnyAuthority(Roles.API_ADMIN.role)
                    .requestMatchers("/api/event-types/**").hasAnyAuthority(Roles.API_ADMIN.role)
                    .requestMatchers("/api/events/**").hasAnyAuthority(Roles.API_USER.role,Roles.API_ADMIN.role)
                    .requestMatchers("/api/sparql/**").hasAnyAuthority(Roles.API_USER.role,Roles.API_ADMIN.role)
                    .requestMatchers("/api/webhooks/**").hasAnyAuthority(Roles.API_ADMIN.role)
                    .anyRequest().authenticated()
            }
            .userDetailsService(jpaUserDetailsService())
        return http.build()
    }

    @Bean
    @ConditionalOnProperty(prefix = "federated.node.api.security", name = ["type"], havingValue = "xapikey")
    fun securityFilterChainApiKey(http: HttpSecurity, apiKeyAuthFilter: ApiKeyAuthFilter): SecurityFilterChain {
        http
        .csrf {
            it.disable()
        }
        .cors {
            it.disable()
        }
        .securityMatcher("/**")
        .authorizeHttpRequests { requests ->
            requests
                .requestMatchers("/*").permitAll()
                .requestMatchers("/assets/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/**").permitAll()
                .requestMatchers("/event-viewer/**").permitAll()
                .requestMatchers("/event-viewer/**").hasAnyAuthority(Roles.API_USER.role,Roles.API_ADMIN.role)
                .requestMatchers("/api/message/**").hasAnyAuthority(Roles.API_MESSAGE.role,Roles.API_ADMIN.role)
                .requestMatchers("/api/eventviewer/**").hasAnyAuthority(Roles.API_USER.role,Roles.API_ADMIN.role)
                .requestMatchers("/api/users/**").hasAnyAuthority(Roles.API_ADMIN.role)
                .requestMatchers("/api/apikeys/**").hasAnyAuthority(Roles.API_ADMIN.role)
                .requestMatchers("/api/distribution-rules/**").hasAnyAuthority(Roles.API_ADMIN.role)
                .requestMatchers("/api/event-types/**").hasAnyAuthority(Roles.API_USER.role,Roles.API_ADMIN.role)
                .requestMatchers("/api/events/**").hasAnyAuthority(Roles.API_USER.role,Roles.API_ADMIN.role)
                .requestMatchers("/api/sparql/**").hasAnyAuthority(Roles.API_USER.role,Roles.API_ADMIN.role)
                .requestMatchers("/api/webhooks/**").hasAnyAuthority(Roles.API_ADMIN.role)
                .anyRequest().authenticated()
        }
        .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter::class.java) // Add our custom filter
        return http.build()
    }

    @Bean
    @ConditionalOnProperty(prefix = "federated.node.api.cors", name = ["enabled"], havingValue = "true")
    fun corsConfigurer(environment: Environment): WebMvcConfigurer? {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                val allowedOrigins = environment.getProperty("federated.node.api.cors.allowed-origins")
                if (!allowedOrigins.isNullOrEmpty()) {
                    registry.addMapping("/**").allowedOrigins(*allowedOrigins.split(",").toTypedArray())
                }
            }
        }
    }
}