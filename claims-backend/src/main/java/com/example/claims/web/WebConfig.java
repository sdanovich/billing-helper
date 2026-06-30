package com.example.claims.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Serialize {@code Page} responses via the stable DTO form (a {@code page} object with
 * number/size/totalElements), rather than the deprecated {@code PageImpl} JSON whose shape
 * Spring warns is unstable. Keeps the Android paging contract predictable.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class WebConfig {
}
