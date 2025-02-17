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

package nl.tno.federated.api.security

import nl.tno.federated.api.security.apikey.ApiKeyAuthFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@EnableWebSecurity
@Configuration
class SecurityConfig( ) {

    @Autowired
    private lateinit var environment: Environment

    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder {
        return BCryptPasswordEncoder()
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
    fun userDetailsService(): UserDetailsService {
        val user: UserDetails =
            User.withUsername(environment.getProperty("federated.node.api.security.userpass.username","api"))
                .password(environment.getProperty("federated.node.api.security.userpass.password","\$2a\$04\$QSdb8yrtXowsJMBf/.Nkku/85wceyamR4LPArNCwE264bXtATef8m"))
                .roles("API_USER")
                .build()

        return InMemoryUserDetailsManager(user)
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
                    .requestMatchers("/*").permitAll()
                    .requestMatchers("/assets/**").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/v3/**").permitAll()
                    .requestMatchers("/api/message/**").hasAuthority("XAPIKEY")
                    .requestMatchers("/api/eventviewer/**").hasRole("API_USER")
                    .requestMatchers("/api/distribution-rules/**").hasRole("API_USER")
                    .requestMatchers("/event-viewer/**").hasRole("API_USER")
                    .requestMatchers("/api/event-types/**").hasRole("API_USER")
                    .requestMatchers("/api/events/**").hasRole("API_USER")
                    .requestMatchers("/api/sparql/**").hasRole("API_USER")
                    .requestMatchers("/api/webhooks/**").hasRole("API_USER")
            }
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
                    .requestMatchers("/event-viewer/**").hasRole("API_USER")
                    .requestMatchers("/api/message/**").hasRole("API_USER")
                    .requestMatchers("/api/eventviewer/**").hasRole("API_USER")
                    .requestMatchers("/api/distribution-rules/**").hasRole("API_USER")
                    .requestMatchers("/api/event-types/**").hasRole("API_USER")
                    .requestMatchers("/api/events/**").hasRole("API_USER")
                    .requestMatchers("/api/sparql/**").hasRole("API_USER")
                    .requestMatchers("/api/webhooks/**").hasRole("API_USER")
                    .anyRequest().authenticated()
            }
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