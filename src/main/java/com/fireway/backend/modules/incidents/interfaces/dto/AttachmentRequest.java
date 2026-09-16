package com.fireway.backend.modules.incidents.interfaces.dto;
import jakarta.validation.constraints.NotBlank;
/** 신고 첨부 요청. key 는 POST /api/files/upload-url 응답의 key 그대로다. */
public record AttachmentRequest(@NotBlank String key) { }
