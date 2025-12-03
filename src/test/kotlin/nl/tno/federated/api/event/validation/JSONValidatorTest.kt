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
package nl.tno.federated.api.event.validation

import org.junit.Test
import org.springframework.core.io.ClassPathResource

class JSONValidatorTest
{

    @Test
    fun validate() {
        val schema = String(ClassPathResource("test-data/MinimalEventSchema.json").inputStream.readBytes())
        val json = String(ClassPathResource("test-data/MinimalEvent.json").inputStream.readBytes())

        val validator = JSONValidator()
        validator.validateJSON(json,schema)
    }

    @Test(expected = JSONValidationException::class)
    fun validateExceptionThrown() {
        val schema = String(ClassPathResource("test-data/MinimalEventSchema.json").inputStream.readBytes())
        val json = String(ClassPathResource("test-data/IncorrectMinimalEvent.json").inputStream.readBytes())

        val validator = JSONValidator()
        validator.validateJSON(json, schema)
    }


}