package com.allinweb.ch.persistence;

import com.allinweb.ch.builder.WebElementIcon;
import com.allinweb.ch.builder.WebElementTagNameEnum;
import java.util.List;
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
    String allAttributes;
    String customXPath;
    String coords;
    String mainXPath;
    String mainCoordinates;
    String xPathWorkedFirst;
    String isCurrentXPathOK;
    String isAllAttributesOK;
    String isCustomXPathOK;
    String isCoordsOK;
    String iFrameXPath;
    List<String> iFrameElements;
    String someText;
    String attribId;
    String attribName;
    String attributeType;
    String attributeValue;
    String originalTagName;
    String definedName;
    WebElementTagNameEnum tagType;
    WebElementIcon iconType;
    WebElement element;
    Boolean isElementHidden;
    Boolean cloned;
    String searchAttributeValue;
    Boolean forceCoordinates;

    public TargetElement(TargetElement origin) {
        this.nameLabel = origin.nameLabel;
        this.nameField = origin.nameField;
        this.currentXPath = origin.currentXPath;
        this.allAttributes = origin.allAttributes;
        this.customXPath = origin.customXPath;
        this.coords = origin.coords;
        this.mainXPath = origin.mainXPath;
        this.mainCoordinates = origin.mainCoordinates;
        this.xPathWorkedFirst = origin.xPathWorkedFirst;
        this.isCurrentXPathOK = origin.isCurrentXPathOK;
        this.isAllAttributesOK = origin.isAllAttributesOK;
        this.isCustomXPathOK = origin.isCustomXPathOK;
        this.isCoordsOK = origin.isCoordsOK;
        this.iFrameXPath = origin.iFrameXPath;
        this.iFrameElements = origin.iFrameElements;
        this.someText = origin.someText;
        this.attribId = origin.attribId;
        this.attribName = origin.attribName;
        this.attributeType = origin.attributeType;
        this.attributeValue = origin.attributeValue;
        this.originalTagName = origin.originalTagName;
        this.definedName = origin.definedName;
        this.tagType = origin.tagType;
        this.iconType = origin.iconType;
        this.element = origin.element;
        this.isElementHidden = origin.isElementHidden;
        this.cloned = origin.cloned;
        this.searchAttributeValue = origin.searchAttributeValue;
        this.forceCoordinates = origin.forceCoordinates;
    }

    public void reset() {
        this.nameLabel = null;
        this.nameField = null;
        this.currentXPath = null;
        this.allAttributes = null;
        this.customXPath = null;
        this.coords = null;
        this.mainXPath = null;
        this.mainCoordinates = null;
        this.xPathWorkedFirst = null;
        this.isCurrentXPathOK = null;
        this.isAllAttributesOK = null;
        this.isCustomXPathOK = null;
        this.isCoordsOK = null;
        this.iFrameXPath = null;
        this.iFrameElements = null;
        this.someText = null;
        this.attribId = null;
        this.attribName = null;
        this.attributeType = null;
        this.attributeValue = null;
        this.originalTagName = null;
        this.definedName = null;
        this.tagType = null;
        this.iconType = null;
        this.element = null;
        this.isElementHidden = null;
        this.cloned = false;
        this.searchAttributeValue = null;
        this.forceCoordinates = false;
    }
}

/*
    targetElement = performAction.defineTargetNameTitles(targetElement);
    // First  Search for xPath
    TargetElement targetValidated = checkValidateSearchPriorities(targetElement);

    if (targetValidated.getElement() == null) {

                performMessage.errorMessage(
                "I Cannot defene this element",
                "I will use the Locato \"COORDINATES\"",
                "Try to get it again -> \"HOVER PICK  ELEMENT\" or \"PICK ONE \"",
                null,
                null,
                0);

                return null;
                }

    targetElement = defineTagType(targetElement);
    //iFrames
    private TargetElement defineTagTypeAdvanced( WebElement elementChild, String iFrameXPathScan, String xPathElementChild, TargetElement targetIFrames) {

*/

//                    try {
//                            if (targetElement.getDefinedName() == null) {
//                            performMessage.couldNotFindElement("No TagName");
//                            }
//                            } catch (Exception e) {
//                            performMessage.couldNotFindElement("No TagName");
//                            return;
//                            }

//
//
//
//
//
//        if (targetElement != null
//                && targetElement.getTagType().equals(WebElementTagNameEnum.OUTPUT)
//                && !Strings.isNullOrEmpty(targetElement.getDefinedName())) {
//                if (!Strings.isNullOrEmpty(innerHTMLValue)
//                && innerHTMLValue.equalsIgnoreCase(targetElement.getSomeText())) {
//                nameLabel.setText(targetElement.getDefinedName().trim() + "-(" + innerHTMLValue.trim() + ")");
//                } else if (!Strings.isNullOrEmpty(targetElement.getSomeText())) {
//                nameLabel.setText(targetElement.getDefinedName().trim() + "-("
//                + targetElement.getSomeText().trim() + ")");
//                } else if (!Strings.isNullOrEmpty(targetElement.getAttributeValue())) {
//                nameLabel.setText(targetElement.getDefinedName().trim() + "-("
//                + targetElement.getAttributeValue().trim() + ")");
//                } else {
//                nameLabel.setText(targetElement.getDefinedName().trim());
//                }
//                nameField.setText(targetElement.getDefinedName().trim());
//                } else if (targetElement != null && !Strings.isNullOrEmpty(targetElement.getDefinedName())) {
//                nameLabel.setText(targetElement.getDefinedName().trim());
//                nameField.setText(targetElement.getDefinedName().trim());
//                } else if (targetElement != null && !Strings.isNullOrEmpty(targetElement.getAttributeValue())) {
//                nameLabel.setText(targetElement.getAttributeValue().trim());
//                nameField.setText(targetElement.getAttributeValue().trim());
//                } else if (isOption && hasValue) {
//                nameLabel.setText(valueAttributeValue.trim());
//                nameField.setText(valueAttributeValue.trim());
//                } else if (hasFormControlName) {
//                nameLabel.setText(formControlNameAttributeValue.trim());
//                nameField.setText(formControlNameAttributeValue.trim());
//                } else if (hasTestId) {
//                nameLabel.setText(testIdAttributeValue.trim());
//                nameField.setText(testIdAttributeValue.trim());
//                } else if (hasName) {
//                nameLabel.setText(nameAttributeValue.trim());
//                nameField.setText(nameAttributeValue.trim());
//                } else if (hasAriaLabel) {
//                nameLabel.setText(ariaLabelValue.trim());
//                nameField.setText(ariaLabelValue.trim());
//                } else if (isAnchor && hasInnerHTML && !hasInnerHTMLTag) {
//                nameLabel.setText(innerHTMLValue.trim());
//                nameField.setText(innerHTMLValue.trim());
//                } else if (hasId) {
//                nameLabel.setText(idAttributeValue.trim());
//                nameField.setText(idAttributeValue.trim());
//                } else if (hasHRefFile) {
//                nameLabel.setText(valueHRefFile + " File".trim());
//                nameField.setText(valueHRefFile + " File".trim());
//                } else if (hasParagraph) {
//                nameLabel.setText(textLabel.trim());
//                nameField.setText(this.tagNameDefined.trim());
//                } else if (hasButton) {
//                nameLabel.setText(textLabel.trim());
//                nameField.setText(this.tagNameDefined.trim());
//                } else if (hasSpan) {
//                nameLabel.setText(textLabel.trim());
//                nameField.setText(this.tagNameDefined.trim());
//                } else if (hasDiv) {
//                nameLabel.setText(textLabel.trim());
//                nameField.setText(this.tagNameDefined.trim());
//                } else if (hasLabel) {
//                nameLabel.setText(textLabel.trim());
//                nameField.setText(this.tagNameDefined.trim());
//                } else if (this.tagNameDefined.equalsIgnoreCase("input")
//                || this.tagNameDefined.equalsIgnoreCase("button")
//                || this.tagNameDefined.equalsIgnoreCase("output")) {
//                nameLabel.setText(textLabel.trim());
//                nameField.setText(this.tagNameDefined.trim());
//                } else if (!Strings.isNullOrEmpty(element.getText())) {
//                nameLabel.setText(element.getText().trim());
//                nameField.setText(this.tagNameDefined.trim());
//                } else {
//                nameLabel.setText(ARConstants.DEFAULT_VALUE_NO_IDENTIFICATION);
//                nameField.setText(ARConstants.DEFAULT_VALUE_NO_IDENTIFICATION);
//                }
//                try {
//
//                String extRef = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.WEBDRIVER_EXT_REFERENCE);
//                if (extRef != null) {
//                String extRefSub = extRef.substring(extRef.indexOf("'") + 1, extRef.length() - 1);
//                // isIdElement.setValue(hasTestId &&
//                //
// testIdAttributeValue.equalsIgnoreCase("web-banking-payment-core.payment-details.external-reference"));
//                isIdElement.setValue(hasTestId && testIdAttributeValue.equalsIgnoreCase(extRefSub));
//                }
//                } catch (Exception ex) {
//                throw ex;
//                }
//
//                // Identify if the element is an INPUT, BUTTON, or LABEL
//                nameFieldTitle = nameField.getText();
//
//                boolean isElementHidden = false;
//                try {
//                isElementHidden = element.getAttribute("type") != null
//                && element.getAttribute("type").equalsIgnoreCase("hidden");
//                } catch (Exception ignored) {
//                }
//
//                boolean isInput = false;
//                boolean isButton = false;
//                boolean isLabel = false;
//
//                try {
//                isInput = this.tagNameDefined.equalsIgnoreCase("INPUT") && element.getAttribute("type") != null;
//                isButton = this.tagNameDefined.equalsIgnoreCase("BUTTON");
//                isLabel = this.tagNameDefined.equalsIgnoreCase("LABEL") && !Strings.isNullOrEmpty(element.getText());
//                } catch (Exception ignore) {
//
//                }
