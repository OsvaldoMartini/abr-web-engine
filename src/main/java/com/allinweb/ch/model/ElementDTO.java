package com.allinweb.ch.model;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ElementDTO {
    private Integer id;
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
        this.id = other.id;
        this.typeElement = other.typeElement;
        this.tagName = other.tagName;
        this.xPath = other.xPath;
        this.someText = other.someText;
        this.attribId = other.attribId;
        this.attribName = other.attribName;
        this.coordinates = other.coordinates;
        if (other.attributeData != null) {
            this.attributeData = Arrays.copyOf(other.attributeData, other.attributeData.length);
        } else {
            this.attributeData = null;
        }
        this.customXPath = other.customXPath;
        this.iFrameXPath = other.iFrameXPath;
        this.shadowHost = other.shadowHost;
        this.shadowRoot = other.shadowRoot;
        this.nestedShadow = other.nestedShadow;
        this.cssSelector = other.cssSelector;
        this.attributeValue = other.attributeValue;
        this.attributeType = other.attributeType;
        this.searchAttributeValue = other.searchAttributeValue;
    }

    public ElementDTO deepCopy() {
        ElementDTO copy = new ElementDTO();
        copy.setId(this.id);
        copy.setSomeText(this.someText);
        copy.setTypeElement(this.typeElement);
        copy.setTagName(this.tagName);
        copy.setXPath(this.xPath);
        copy.setAttribId(this.attribId);
        copy.setAttribName(this.attribName);
        copy.setCoordinates(this.coordinates);
        if (this.attributeData != null) {
            copy.setAttributeData(Arrays.copyOf(this.attributeData, this.attributeData.length));
        } else {
            copy.setAttributeData(null);
        }
        copy.setCustomXPath(this.customXPath);
        copy.setIFrameXPath(this.iFrameXPath);
        copy.setShadowHost(this.shadowHost);
        copy.setShadowRoot(this.shadowRoot);
        copy.setNestedShadow(this.nestedShadow);
        copy.setCssSelector(this.cssSelector);
        copy.setAttributeValue(this.attributeValue);
        copy.setAttributeType(this.attributeType);
        copy.setSearchAttributeValue(this.searchAttributeValue);
        return copy;
    }
}
