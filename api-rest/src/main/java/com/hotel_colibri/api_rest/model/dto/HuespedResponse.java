package com.hotel_colibri.api_rest.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Huesped en JSON-LD (schema.org/Person). telephone se omite si no hay. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HuespedResponse(
        @JsonProperty("@context") String context,
        @JsonProperty("@type") String type,
        @JsonProperty("@id") String id,
        Long identifier,
        String name,
        String email,
        String telephone) {
}
