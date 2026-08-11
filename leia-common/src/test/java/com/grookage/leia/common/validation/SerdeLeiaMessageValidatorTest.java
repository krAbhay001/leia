package com.grookage.leia.common.validation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grookage.leia.common.violation.LeiaMessageViolation;
import com.grookage.leia.models.attributes.StringAttribute;
import com.grookage.leia.models.schema.SchemaDetails;
import com.grookage.leia.models.schema.SchemaKey;
import com.grookage.leia.models.schema.SchemaValidationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerdeLeiaMessageValidatorTest {

	private static final SchemaKey SCHEMA_KEY = SchemaKey.builder()
			.namespace("marketplace")
			.schemaName("merchant_common_bfo")
			.version("v1")
			.orgId("testOrg")
			.type("default")
			.tenantId("testTenant")
			.build();
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final SchemaDetails SCHEMA_DETAILS = SchemaDetails.builder()
			.schemaKey(SCHEMA_KEY)
			.attributes(Set.of(new StringAttribute("milestone", false, Set.of())))
			.validationType(SchemaValidationType.STRICT)
			.build();
	private SerdeLeiaMessageValidator validator;

	@BeforeEach
	void setUp() {
		validator = new SerdeLeiaMessageValidator(
				schemaKey -> Optional.of(MilestoneEventData.class), MAPPER);
	}

	@Test
	void acceptsSubtype() throws Exception {
		assertAccepted("""
				{
				  "milestone": "CLIENT_ONLY_SUBTYPE",
				  "clientOnlyField": "value",
				  "priority": 1,
				  "status": "SUCCESS"
				}
				""");

        assertAccepted("""
				{
				  "milestone": "PARTNER_SUBTYPE",
				  "partnerId": "partner-1",
				  "retryable": true
				}
				""");

        assertAccepted("""
				{
				  "milestone": "PARTNER_SUBTYPE",
				  "partnerId": "partner-2",
				  "retryable": null
				}
				""");
	}

	@Test
	void rejectsInvalidPayloads() {
		assertAll(
				() -> assertRejected("""
						{
						  "milestone": "UNKNOWN_SUBTYPE"
						}
						""", "Could not resolve type id"),
				() -> assertRejected("""
						{
						  "milestone": "CLIENT_ONLY_SUBTYPE",
						  "unknownField": "value"
						}
						""", "Unrecognized field"),
				() -> assertRejected("""
						{
						  "milestone": "CLIENT_ONLY_SUBTYPE",
						  "priority": {
						    "value": 1
						  }
						}
						""", "Cannot deserialize value of type `int`"),
				() -> assertRejected("""
						{
						  "milestone": "CLIENT_ONLY_SUBTYPE",
						  "status": "UNKNOWN"
						}
						""", "not one of the values accepted for Enum class")
		);
	}

	@Test
	void skipsUnregisteredSchema() throws Exception {
		final var validatorWithoutRegisteredClass =
				new SerdeLeiaMessageValidator(schemaKey -> Optional.empty(), MAPPER);

		assertTrue(validatorWithoutRegisteredClass.validate(
				SCHEMA_DETAILS, readMessage("""
						{
						  "milestone": "UNKNOWN_SUBTYPE"
						}
						""")).isEmpty());
	}

	private void assertAccepted(final String payload) throws Exception {
		final var message = readMessage(payload);

		assertFalse(new DefaultLeiaMessageValidator().validate(SCHEMA_DETAILS, message).isEmpty());
		assertTrue(validator.validate(SCHEMA_DETAILS, message).isEmpty());
	}

	private void assertRejected(final String payload,
	                            final String expectedCause) throws Exception {
		final List<LeiaMessageViolation> violations =
				validator.validate(SCHEMA_DETAILS, readMessage(payload));

		assertEquals(1, violations.size());
		final var violation = violations.get(0);
		assertEquals(SCHEMA_KEY, violation.schemaKey());
		assertEquals("root", violation.fieldPath());
		assertTrue(violation.message().contains(
				"Deserialization into class MilestoneEventData failed"));
		assertTrue(violation.message().contains(expectedCause));
	}

	private JsonNode readMessage(final String payload) throws Exception {
		return MAPPER.readTree(payload);
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "milestone", visible = true)
	@JsonSubTypes({
			@JsonSubTypes.Type(value = ClientOnlyMilestoneEventData.class, name = "CLIENT_ONLY_SUBTYPE"),
			@JsonSubTypes.Type(value = PartnerMilestoneEventData.class, name = "PARTNER_SUBTYPE")
	})
	private static class MilestoneEventData {
		public String milestone;
	}

	private static class ClientOnlyMilestoneEventData extends MilestoneEventData {
		public String clientOnlyField;
		public int priority;
		public MilestoneStatus status;
	}

	private static class PartnerMilestoneEventData extends MilestoneEventData {
		public String partnerId;
		public boolean retryable;
	}

	private enum MilestoneStatus {
		SUCCESS,
		FAILED
	}
}
