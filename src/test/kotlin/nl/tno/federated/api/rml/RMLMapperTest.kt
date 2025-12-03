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
package nl.tno.federated.api.rml

import org.junit.Test
import org.springframework.core.io.ClassPathResource
import java.nio.charset.Charset
import kotlin.test.assertNotNull

class RMLMapperTest {

    private val rmlMapper = RMLMapper()
    private val loadEventJson = ClassPathResource("test-data/MinimalEvent.json").getContentAsString(Charset.defaultCharset())
    private val rml = ClassPathResource("rml/MinimalEvent.ttl").getContentAsString(Charset.defaultCharset())

    @Test
    fun testMinimalEvent() {
        val result = rmlMapper.createTriples(loadEventJson, rml)
        println(result)
        assertNotNull(result)
        // TODO more assertions here
    }

}