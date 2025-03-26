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
    private String coordinates;
    private AttributeData[] attributeData;
    private String customXPath;
    private String iFrameXPath;
    private String shadowHost;
    private String shadowRoot;
    private String nestedShadow;
    private String cssSelector;
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
        this.coordinates = other.coordinates;
        this.attributeData = other.attributeData;
        this.customXPath = other.customXPath;
        this.iFrameXPath = other.iFrameXPath;
        this.shadowHost = other.shadowHost;
        this.shadowRoot = other.shadowRoot;
        this.cssSelector = other.cssSelector;
        this.attributeValue = other.attributeValue;
        this.attributeType = other.attributeType;
        this.searchAttributeValue = other.searchAttributeValue;
    }
}
