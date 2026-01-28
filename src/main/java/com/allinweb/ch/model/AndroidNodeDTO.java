package com.allinweb.ch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AndroidNodeDTO {
    // raw attributes
    private String className;
    private String resourceId;
    private String text;
    private String contentDesc;
    private Boolean clickable;
    private Boolean enabled;
    private Boolean password;
    private String bounds;

    // derived geometry
    private Integer x; // center X
    private Integer y; // center Y
    private Integer width;
    private Integer height;

    // locator proposals
    private String preferredLocatorStrategy; // "id" | "accessibilityId" | "androidUiAutomator" | "xpath"
    private String preferredLocatorValue;

    // robust fallback
    private String xpath;
}
