/*
 * Copyright (c) 2024. Koushik R <rkoushik.14@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.grookage.leia.common.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grookage.leia.common.violation.LeiaMessageViolation;
import com.grookage.leia.common.violation.LeiaMessageViolationImpl;
import com.grookage.leia.models.schema.SchemaDetails;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

@Slf4j
public class SerdeLeiaMessageValidator implements LeiaMessageValidator {

	private final LeiaSchemaClassProvider schemaClassProvider;
	private final ObjectMapper mapper;

	public SerdeLeiaMessageValidator(final LeiaSchemaClassProvider schemaClassProvider, final ObjectMapper mapper) {
        this.schemaClassProvider = Objects.requireNonNull(schemaClassProvider, "LeiaSchemaClassProvider must not be null");
        this.mapper = Objects.requireNonNull(mapper, "ObjectMapper must not be null");
	}

	@Override
	public List<LeiaMessageViolation> validate(final SchemaDetails schemaDetails, final JsonNode message) {
		final var schemaKey = schemaDetails.getSchemaKey();
		final var registeredClass = schemaClassProvider.getKlass(schemaKey).orElse(null);
		if (registeredClass == null) {
			log.debug("No registered class found for schemaKey {}; skipping Serde validation",
					schemaKey.getReferenceId());
			return List.of();
		}

		try {
			mapper.treeToValue(message, registeredClass);
			return List.of();
		} catch (Exception exception) {
			log.debug("Serde validation failed for schemaKey {} and class {}",
					schemaKey.getReferenceId(), registeredClass.getName(), exception);
			return List.of(LeiaMessageViolationImpl.builder()
					.schemaKey(schemaKey)
					.fieldPath("root")
					.message("Deserialization into class " + registeredClass.getSimpleName()
							+ " failed: " + exception.getMessage())
					.build());
		}
	}
}
