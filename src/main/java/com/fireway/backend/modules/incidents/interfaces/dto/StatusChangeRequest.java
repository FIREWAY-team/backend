package com.fireway.backend.modules.incidents.interfaces.dto;
import jakarta.validation.constraints.NotBlank;
/** {"status": "DISPATCHED"}. 대소문자는 가리지 않는다. */
public record StatusChangeRequest(@NotBlank String status) { }
