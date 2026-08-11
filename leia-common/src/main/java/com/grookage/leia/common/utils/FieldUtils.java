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

package com.grookage.leia.common.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.experimental.UtilityClass;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class FieldUtils {
	public List<Field> getAllFields(final Class<?> type) {
		List<Field> fields = new ArrayList<>();
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            fields.addAll(getClassFields(c));
		}
		return fields;
	}

    public List<Field> getClassFields(final Class<?> klass) {
        return Arrays.stream(klass.getDeclaredFields())
                .filter(field -> !isNonSerializable(field))
                .toList();
    }

	private boolean isNonSerializable(final Field field) {
		if (field.isAnnotationPresent(JsonIgnore.class)) {
			return true;
		}
		int modifiers = field.getModifiers();
		return Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers);
	}

	public Optional<Field> filter(final String name,
	                              final List<Field> fields) {
		return fields.stream()
				.filter(each -> each.getName().equals(name))
				.findFirst();
	}
    public Set<Class<?>> getImmediateSubTypes(Reflections reflections, Class<?> klass) {
        return reflections.getSubTypesOf(klass).stream()
                .filter(each -> each.getSuperclass().equals(klass))
                .collect(Collectors.toSet());
    }
}
