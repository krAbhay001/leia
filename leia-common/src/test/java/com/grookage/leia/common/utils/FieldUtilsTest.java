package com.grookage.leia.common.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.grookage.leia.models.utils.MapperUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Calendar;
import java.util.Date;

class FieldUtilsTest {
	@Test
	void testFieldUtils() {
		final var fields = FieldUtils.getAllFields(TestData.class);
		Assertions.assertFalse(fields.isEmpty());
		Assertions.assertEquals(2, fields.size());
		Assertions.assertTrue(fields.stream().noneMatch(field -> field.getName().equals("phoneNumber")));
		Assertions.assertTrue(fields.stream().noneMatch(field -> field.getName().equals("CONSTANT")));
		Assertions.assertTrue(fields.stream().noneMatch(field -> field.getName().equals("exclusion")));
	}

	@Test
	void testGetAllSerializedFieldNames() {
		final var fieldVsSerializedMap = FieldUtils.getSerializedNameVsFieldMap(SerializedChildData.class);
		final var fieldNames = fieldVsSerializedMap.keySet();
		Assertions.assertTrue(fieldNames.contains("parent_name"));
		Assertions.assertTrue(fieldNames.contains("parentAge"));
		Assertions.assertTrue(fieldNames.contains("child_email"));
		Assertions.assertFalse(fieldNames.contains("ignored"));
		Assertions.assertFalse(fieldNames.contains("STATIC_FIELD"));
		Assertions.assertFalse(fieldNames.contains("transientField"));
		Assertions.assertEquals(3, fieldNames.size());
		Assertions.assertEquals(fieldNames.size(), fieldNames.stream().distinct().count());
	}

	@Test
	void testGetAllSerializedFieldNames_withSnakeCaseNaming() {
		final var fieldVsSerializedMap = FieldUtils.getSerializedNameVsFieldMap(SnakeCaseData.class);
		final var fieldNames = fieldVsSerializedMap.keySet();
		Assertions.assertTrue(fieldNames.contains("first_name"));
		Assertions.assertTrue(fieldNames.contains("last_name"));
		Assertions.assertTrue(fieldNames.contains("custom_id"));
		Assertions.assertEquals(3, fieldNames.size());
	}

	static class BaseData {
		static final String CONSTANT = "CONSTANT";
		String name;
		@JsonIgnore
		private String exclusion;
	}

	static class TestData extends BaseData {
		String email;
		transient String phoneNumber;
	}

	static class SerializedParentData {
		static final String STATIC_FIELD = "constant";
		@JsonProperty("parent_name")
		private String parentName;
		private int parentAge;
		@JsonIgnore
		private String ignored;
		transient String transientField;
	}

	static class SerializedChildData extends SerializedParentData {
		@JsonProperty("child_email")
		private String childEmail;
	}

	@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
	static class SnakeCaseData {
		private String firstName;
		private String lastName;
		@JsonProperty("custom_id")
		private String customId;
	}

	@Test
	void testMapperSerializesJavaTimeTypes() throws Exception {
		final var mapper = MapperUtils.mapper();

		Assertions.assertFalse(mapper.writeValueAsString(Instant.parse("2025-01-15T10:30:00Z")).startsWith("\""));
		Assertions.assertFalse(mapper.writeValueAsString(ZonedDateTime.of(2025, 5, 1, 12, 0, 0, 0, ZoneId.of("UTC"))).startsWith("\""));
		Assertions.assertFalse(mapper.writeValueAsString(OffsetDateTime.of(2025, 5, 1, 12, 0, 0, 0, ZoneOffset.UTC)).startsWith("\""));

		Assertions.assertTrue(mapper.writeValueAsString(LocalDate.of(2025, 6, 20)).startsWith("["));
		Assertions.assertTrue(mapper.writeValueAsString(LocalDateTime.of(2025, 3, 10, 14, 30)).startsWith("["));
		Assertions.assertTrue(mapper.writeValueAsString(LocalTime.of(14, 30, 0)).startsWith("["));

		Assertions.assertTrue(mapper.writeValueAsString(OffsetTime.of(14, 30, 0, 0, ZoneOffset.UTC)).startsWith("["));

		Assertions.assertFalse(mapper.writeValueAsString(new Date(1700000000000L)).startsWith("\""));
		Assertions.assertFalse(mapper.writeValueAsString(Calendar.getInstance()).startsWith("\""));
	}

}
