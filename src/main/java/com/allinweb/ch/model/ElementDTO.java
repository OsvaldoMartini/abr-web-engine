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
    private String nameLabel;
    private String nameField;
    private String definedName;
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
    // One sentinel per bit in force_coordinates, so downstream code can branch on
    // a single flag without parsing the full string. Value is the flag letter
    // ("S" / "E" / "T" / "N" / "F") when the bit is set, null when cleared.
    // Source of truth is still InstructionLoad.forceCoordinates - these fields
    // are derived in SplitDTO.applyInstructionToSplit and must stay in sync.
    private String autoScroll; // "S" when set
    private String autoEnter; // "E" when set
    private String autoTab; // "T" when set
    private String autoNext; // "N" when set
    private String autoForceCoords; // "F" when set

    // Combined F/E/T/N/S bitstring sent from GridItemScann so the user can set
    // per-element flags before the DTO is promoted to an InstructionLoad on
    // NEW_ELEMENT_DTO / SEND_ALL_ELEMENTS_DTO. Empty/null means "no flags
    // fall back to the pane's checkbox state".
    private String forceCoordinates;

    // >>> MINIMAL ADD: Android-specific nested data <<<
    private AndroidNodeDTO[] androidData;

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
        this.autoScroll = other.autoScroll;
        this.autoEnter = other.autoEnter;
        this.autoTab = other.autoTab;
        this.autoNext = other.autoNext;
        this.autoForceCoords = other.autoForceCoords;

        // >>> MINIMAL ADD: copy androidData array <<<
        if (other.androidData != null) {
            this.androidData = Arrays.copyOf(other.androidData, other.androidData.length);
        } else {
            this.androidData = null;
        }
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
        copy.setAutoScroll(this.autoScroll);
        copy.setAutoEnter(this.autoEnter);
        copy.setAutoTab(this.autoTab);
        copy.setAutoNext(this.autoNext);
        copy.setAutoForceCoords(this.autoForceCoords);

        // >>> MINIMAL ADD: deep copy androidData array <<<
        if (this.androidData != null) {
            copy.setAndroidData(Arrays.copyOf(this.androidData, this.androidData.length));
        } else {
            copy.setAndroidData(null);
        }

        return copy;
    }
}
