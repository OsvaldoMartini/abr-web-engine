package com.allinweb.ch.component.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ElementDTO {
    private String typeElement;
    private String tagName;
    private String xPath;
    private String someText;
    private String attribId;
    private String attribName;
    private String coords;
    private String allAttributes;
    private String customXPath;
    private String iFrameXPath;
    private String attributeValue;
    private String attributeType;
    private String searchAttributeValue;

    // Copy Constructor
    public ElementDTO(ElementDTO other) {
        this.typeElement = other.typeElement;
        this.tagName = other.tagName;
        this.xPath = other.xPath;
        this.someText = other.someText;
        this.attribId = other.attribId;
        this.attribName = other.attribName;
        this.coords = other.coords;
        this.allAttributes = other.allAttributes;
        this.customXPath = other.customXPath;
        this.iFrameXPath = other.iFrameXPath;
        this.attributeValue = other.attributeValue;
        this.attributeType = other.attributeType;
        this.searchAttributeValue = other.searchAttributeValue;
    }
}
