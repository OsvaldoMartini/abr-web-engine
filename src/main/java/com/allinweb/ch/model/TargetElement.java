package com.allinweb.ch.model;

import com.allinweb.ch.builder.WebElementIcon;
import com.allinweb.ch.builder.WebElementTagNameEnum;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openqa.selenium.WebElement;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TargetElement {
    String nameLabel;
    String nameField;
    String currentXPath;
    AttributeData[] attributeData;
    String customXPath;
    String coordinates;
    String XPath;
    String xPathWorkedFirst;
    String iFrameXPath;
    List<String> iFrameElements;
    String shadowHost;
    String shadowRoot;
    String nestedShadow;
    String cssSelector;
    String someText;
    String attribId;
    String attribName;
    String attributeType;
    String attributeValue;
    String tagName;
    String definedName;
    WebElementTagNameEnum tagType;
    WebElementIcon iconType;
    WebElement element;
    Boolean isElementHidden;
    Boolean cloned;
    String searchAttributeValue;
    String autoScroll;
    String autoEnter;
    Boolean forceCoordinates = true;
    Map<String, String> savedReferences = new HashMap<>();
    Integer instructionId;
    Boolean clickElement = true;

    public TargetElement(TargetElement origin) {
        this.nameLabel = origin.nameLabel;
        this.nameField = origin.nameField;
        this.currentXPath = origin.currentXPath;
        this.attributeData = origin.attributeData;
        this.customXPath = origin.customXPath;
        this.coordinates = origin.coordinates;
        this.XPath = origin.XPath;
        this.xPathWorkedFirst = origin.xPathWorkedFirst;
        this.iFrameXPath = origin.iFrameXPath;
        this.iFrameElements = origin.iFrameElements;
        this.shadowHost = origin.shadowHost;
        this.shadowRoot = origin.shadowRoot;
        this.nestedShadow = origin.nestedShadow;
        this.cssSelector = origin.cssSelector;
        this.someText = origin.someText;
        this.attribId = origin.attribId;
        this.attribName = origin.attribName;
        this.attributeType = origin.attributeType;
        this.attributeValue = origin.attributeValue;
        this.tagName = origin.tagName;
        this.definedName = origin.definedName;
        this.tagType = origin.tagType;
        this.iconType = origin.iconType;
        this.element = origin.element;
        this.isElementHidden = origin.isElementHidden;
        this.cloned = origin.cloned;
        this.searchAttributeValue = origin.searchAttributeValue;
        this.forceCoordinates = origin.forceCoordinates;
        this.savedReferences = origin.savedReferences;
        this.instructionId = origin.instructionId;
        this.clickElement = origin.clickElement;
    }

    public void reset() {
        this.nameLabel = null;
        this.nameField = null;
        this.currentXPath = null;
        this.attributeData = null;
        this.customXPath = null;
        this.coordinates = null;
        this.XPath = null;
        this.xPathWorkedFirst = null;
        this.iFrameXPath = null;
        this.iFrameElements = null;
        this.shadowHost = null;
        this.shadowRoot = null;
        this.nestedShadow = null;
        this.cssSelector = null;
        this.someText = null;
        this.attribId = null;
        this.attribName = null;
        this.attributeType = null;
        this.attributeValue = null;
        this.tagName = null;
        this.definedName = null;
        this.tagType = null;
        this.iconType = null;
        this.element = null;
        this.isElementHidden = null;
        this.cloned = false;
        this.searchAttributeValue = null;
        this.forceCoordinates = false;
        this.savedReferences = new HashMap<>();
        this.instructionId = null;
        this.clickElement = false;
    }

    public TargetElement deepCopy() {
        TargetElement copy = new TargetElement();

        copy.nameLabel = this.nameLabel;
        copy.nameField = this.nameField;
        copy.currentXPath = this.currentXPath;

        // Deep copy AttributeData[]
        if (this.attributeData != null) {
            copy.attributeData = new AttributeData[this.attributeData.length];
            for (int i = 0; i < this.attributeData.length; i++) {
                copy.attributeData[i] = this.attributeData[i] != null ? new AttributeData(this.attributeData[i]) : null;
            }
        }

        copy.customXPath = this.customXPath;
        copy.coordinates = this.coordinates;
        copy.XPath = this.XPath;
        copy.xPathWorkedFirst = this.xPathWorkedFirst;
        copy.iFrameXPath = this.iFrameXPath;

        // Defensive copy of List
        copy.iFrameElements = this.iFrameElements != null ? List.copyOf(this.iFrameElements) : null;

        copy.shadowHost = this.shadowHost;
        copy.shadowRoot = this.shadowRoot;
        copy.nestedShadow = this.nestedShadow;
        copy.cssSelector = this.cssSelector;
        copy.someText = this.someText;
        copy.attribId = this.attribId;
        copy.attribName = this.attribName;
        copy.attributeType = this.attributeType;
        copy.attributeValue = this.attributeValue;
        copy.tagName = this.tagName;
        copy.definedName = this.definedName;
        copy.tagType = this.tagType;
        copy.iconType = this.iconType;

        // WebElement is not safely cloneable - just reference it
        copy.element = this.element;

        copy.isElementHidden = this.isElementHidden;
        copy.cloned = this.cloned;
        copy.searchAttributeValue = this.searchAttributeValue;
        copy.forceCoordinates = this.forceCoordinates;

        // Defensive copy of Map
        copy.savedReferences = this.savedReferences != null ? new HashMap<>(this.savedReferences) : new HashMap<>();

        copy.instructionId = this.instructionId;
        copy.clickElement = this.clickElement;

        return copy;
    }
}
