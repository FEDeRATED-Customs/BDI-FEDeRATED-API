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

package nl.tno.federated.api

import nl.tno.federated.api.event.type.EventTypeMappingConfig
import nl.tno.federated.api.graphdb.config.GraphDBConfig
import nl.tno.federated.api.orchestrator.config.OrchestratorConfig
import nl.tno.federated.api.security.user.UserEntity
import nl.tno.federated.api.security.user.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * FEDeRATED Node API Spring Boot application.
 */

@EnableConfigurationProperties(EventTypeMappingConfig::class, OrchestratorConfig::class, GraphDBConfig::class)
@ComponentScan(basePackages = ["nl.tno.federated.api"])
@EnableScheduling
@SpringBootApplication(scanBasePackages = ["nl.tno.federated.api"])
class Server {

    @Bean(name = ["applicationEventMulticaster"])
    fun simpleApplicationEventMulticaster(): ApplicationEventMulticaster {
        val eventMulticaster = SimpleApplicationEventMulticaster()
        eventMulticaster.setTaskExecutor(SimpleAsyncTaskExecutor())
        return eventMulticaster
    }

}

/**
 * Starts our Spring Boot application.
 */
fun main(args: Array<String>) {
    SpringApplication.run(Server::class.java, *args)
}