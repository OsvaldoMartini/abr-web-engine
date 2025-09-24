package com.allinweb.ch.component.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttributeData {
    private String name;
    private String value;

    // Copy constructor
    public AttributeData(AttributeData other) {
        if (other != null) {
            this.name = other.name;
            this.value = other.value;
        }
    }
}
