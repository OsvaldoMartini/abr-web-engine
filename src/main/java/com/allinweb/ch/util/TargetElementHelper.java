package com.allinweb.ch.util;

import com.allinweb.ch.builder.WebElementAttributeEnum;
import com.allinweb.ch.builder.WebElementAttributeTypeValueEnum;
import com.allinweb.ch.builder.WebElementIcon;
import com.allinweb.ch.builder.WebElementTagNameEnum;
import com.allinweb.ch.facade.PerformActions;
import com.allinweb.ch.facade.PerformMessage;
import com.allinweb.ch.model.*;
import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TargetElementHelper {

    private static volatile TargetElementHelper instance;

    private static final Logger log = LoggerFactory.getLogger(TargetElementHelper.class);
    private static final Logger logOperations = LoggerFactory.getLogger("com.allinweb.operations");

    private static final PerformMessage performMessage = PerformMessage.getInstance();
    private PerformActions performActions;
    //    private ARViewBotJobScene arViewBotJobScene;
    //    private ARScannedElementPane arScannedElementPane;

    private TargetElementHelper() {
        // private constructor to enforce singleton
    }

    public static TargetElementHelper getInstance() {
        if (instance == null) {
            synchronized (TargetElementHelper.class) {
                if (instance == null) {
                    instance = new TargetElementHelper();
                }
            }
        }
        return instance;
    }

    /**
     * Initialize the helper with the necessary dependencies.
     */
    //    public void initialize(PerformActions performActions, ARScannedElementPane arScannedElementPane) {
    //        this.performActions = performActions;
    //        this.arScannedElementPane = arScannedElementPane;
    //    }

    public void initialize(PerformActions performActions) {
        this.performActions = performActions;
    }

    /**
     * Initialize the helper with the necessary dependencies.
     */
    //    public void initialize(ARViewBotJobScene arViewBotJobScene) {
    //        this.arViewBotJobScene = arViewBotJobScene;
    //    }

    /**
     * Extracts and defines a cloned TargetElement from the given ElementDTO.
     */
    //    public TargetElement extractPickClone(ElementDTO elementDTO) {
    //
    //        if (performActions == null || arScannedElementPane == null) {
    //            log.error("TargetElementHelper not initialized. Call initialize() first.");
    //            performMessage.errorMessage(
    //                    "AR Web Scanner Not Open",
    //                    "<span style='color: #D32F2F; font-weight: bold; font-size: 1.1em;'>Operation Failed!</span>
    // ❌",
    //                    "<span style='color: #E65100; font-weight: bold;'>Please select a Bot Job and open the
    // \"Scanner\"</span>",
    //                    "<span style='color: #1565C0; font-weight: bold;'>Scanner</span>.",
    //                    "<span style='font-style: italic;'>Details: Please select and open the Bot Job and \"Scanner\"
    // on AR Web Scanner.</span>",
    //                    0);
    //            return null;
    //        }
    //
    //        arScannedElementPane.xpathTextPrevious = elementDTO.getXPath();
    //
    //        TargetElement targetLocal = defineSearchReturn(elementDTO, null);
    //
    //        WebElement elementFound = performActions.findWebElement(targetLocal);
    //        if (targetLocal.getElement() == null && elementFound != null) {
    //            targetLocal.setElement(elementFound);
    //        }
    //
    //        // Save references for different coordinate strategies
    //        // 3 Different Coordinates // Original from JavaScript  // WebDriver Selenium ElementFound
    //        // FallBack React Computed
    //        // TO DO:   KEEP THE ORIGINALS  FROM ANDROID
    //        performActions.defineSavedReferenced(targetLocal);
    //
    //        // Define tag name/title
    //        targetLocal = defineNameTitles(targetLocal);
    //
    //        // Validate Shadow DOM or regular CSS selectors
    //        if (Strings.isNullOrEmpty(targetLocal.getShadowHost()) &&
    // Strings.isNullOrEmpty(targetLocal.getCssSelector())) {
    //
    //            TargetElement targetValidated = checkValidateSearchPriorities(targetLocal);
    //
    //            if (targetValidated.getElement() == null) {
    //                log.error("Cannot define this element. Try to get it again via 'Hover Pick Element' or 'Pick
    // One'.");
    //                performMessage.errorMessage(
    //                        "I Cannot define this element",
    //                        "I will use the Locator 'COORDINATES'",
    //                        "Try again using 'HOVER PICK ELEMENT' or 'PICK ONE'",
    //                        null,
    //                        null,
    //                        0);
    //                return null;
    //            }
    //
    //        } else if (!Strings.isNullOrEmpty(targetLocal.getCssSelector())) {
    //            targetLocal.setXPathWorkedFirst(ARConstants.REGULAR_XPATH);
    //        } else {
    //            targetLocal.setXPathWorkedFirst(ARConstants.SHADOW_DOM);
    //        }
    //
    //        // Update UI checkboxes and return final target
    //        arScannedElementPane.defineCheckBoxesClickable(targetLocal);
    //
    //        return targetLocal;
    //    }

    /**
     * Extracts and defines a cloned TargetElement from the given ElementDTO.
     */
    public TargetElement extractPickClone(ElementDTO elementDTO, boolean isBotJobContext) {

        TargetElement targetLocal = defineSearchReturn(elementDTO, null);

        // Save references for different coordinate strategies
        // 3 Different Coordinates // Original from JavaScript  // WebDriver Selenium ElementFound
        // FallBack React Computed
        // TO DO:   KEEP THE ORIGINALS  FROM ANDROID
        // Insert into savedReferences
        if (elementDTO.getXPath() != null) {
            targetLocal.getSavedReferences().put("xpath", elementDTO.getXPath());
        }

        if (elementDTO.getCoordinates() != null) {
            targetLocal.getSavedReferences().put("coordinates", elementDTO.getCoordinates());
        }

        if (elementDTO.getAttribId() != null) {
            targetLocal.getSavedReferences().put("attributeId", elementDTO.getAttribId());
        }

        if (isBotJobContext && elementDTO.getAttributeData() != null) {
            for (AttributeData attr : elementDTO.getAttributeData()) {
                if (attr != null && attr.getName() != null) {
                    targetLocal.getSavedReferences().put("AttrData:" + attr.getName(), attr.getValue());
                }
            }
        }

        // Define tag name/title
        targetLocal.setNameLabel(
                elementDTO.getSomeText() == null
                        ? elementDTO.getTagName()
                        : elementDTO.getSomeText().trim().replaceAll("\\s+", " "));
        targetLocal.setNameField(
                elementDTO.getSomeText() == null
                        ? elementDTO.getTagName()
                        : elementDTO.getSomeText().trim().replaceAll("\\s+", " "));

        targetLocal.setDefinedName(
                elementDTO.getSomeText() == null
                        ? elementDTO.getTagName()
                        : elementDTO.getSomeText().trim().replaceAll("\\s+", " "));

        // Validate Shadow DOM or regular CSS selectors
        targetLocal.setXPathWorkedFirst(ARConstants.REGULAR_XPATH);

        return targetLocal;
    }

    public TargetElement checkValidateSearchPriorities(TargetElement target) {
        WebElement elementValid = null;
        if (!Strings.isNullOrEmpty(target.getCurrentXPath())) {

            if (target.getForceCoordinates() != null && target.getForceCoordinates()) {
                // Try by coordinates
                try {
                    FieldData filedData = new FieldData("&EMPTY", "&EMPTY");
                    boolean passed = performActions.executeActionsAtCoordinates(
                            target.getCoordinates(), filedData, ARConstants.VISUALIZE, false);
                    if (passed) {
                        elementValid = performActions.getElementFromCoordinates(target.getCoordinates());
                        if (elementValid != null && elementValid.getTagName() != null) {
                            target.setElement(elementValid);
                        }

                        target.setXPathWorkedFirst(ARConstants.SEARCH_COORD);
                    }

                } catch (Exception e) {

                    log.warn(String.format("Cannot locate a Web Element with Name: %s", target.getAttribName()));
                }
            } else if (elementValid == null) {
                try {
                    elementValid = performActions.getCurrentDriver().findElement(By.xpath(target.getCurrentXPath()));
                    if (elementValid != null && elementValid.getTagName() != null) {
                        target.setElement(elementValid);
                        target.setXPathWorkedFirst(
                                ARConstants.REGULAR_XPATH); // BECAUSE OS LIMITATION OF ACCESS DB 255 CHARACTER
                    }
                } catch (Exception e) {

                    log.warn(String.format(
                            "Cannot locate a Web Element with Regular XPath: %s", target.getCurrentXPath()));
                }
            } else if (elementValid == null) {
                try {
                    elementValid = performActions.getCurrentDriver().findElement(By.xpath(target.getCustomXPath()));
                    if (elementValid != null && elementValid.getTagName() != null) {
                        target.setElement(elementValid);
                        target.setXPathWorkedFirst(
                                ARConstants.CUSTOM_XPATH); // BECAUSE OS LIMITATION OF ACCESS DB 255 CHARACTER
                    }
                } catch (Exception e) {

                    log.warn(String.format(
                            "Cannot locate a Web Element with Absolut XPath: %s", target.getAttributeData()));
                }
            } else {
                if (elementValid == null) {
                    //            if (searchReturn.getCurrentXPath().startsWith("id(")) {
                    if (!Strings.isNullOrEmpty(target.getAttribId())) {
                        try {
                            elementValid = performActions.getCurrentDriver().findElement(By.id(target.getAttribId()));
                            if (elementValid != null && elementValid.getTagName() != null) {
                                target.setElement(elementValid);
                                target.setXPathWorkedFirst(ARConstants.ATTRIBUTE_ID);
                                target.setAttributeType("id");
                                target.setAttributeValue(target.getAttribId());
                            }
                        } catch (Exception e) {

                            log.warn(String.format("Cannot locate a Web Element with ID: %s", target.getAttribId()));
                        }
                    }
                } else if (elementValid == null) {

                    if (!Strings.isNullOrEmpty(target.getAttribName())) {
                        try {
                            elementValid =
                                    performActions.getCurrentDriver().findElement(By.name(target.getAttribName()));
                            if (elementValid != null && elementValid.getTagName() != null) {
                                target.setElement(elementValid);
                                target.setAttributeType("name");
                                target.setXPathWorkedFirst(ARConstants.ATTRIBUTE_NAME);
                            }
                        } catch (Exception e) {

                            log.warn(
                                    String.format("Cannot locate a Web Element with Name: %s", target.getAttribName()));
                        }
                    }
                }
            }
        }

        target.setElement(elementValid);

        return target;
    }

    public TargetElement defineSearchReturn(ElementDTO elemenDTO, TargetElement targetDefine) {
        if (targetDefine == null || targetDefine.getElement() == null) {
            if (targetDefine == null) {
                targetDefine = new TargetElement();
            }

            targetDefine.setTagName(elemenDTO.getTagName());
            targetDefine.setNameLabel(elemenDTO.getNameLabel());
            targetDefine.setNameField(elemenDTO.getNameField());
            targetDefine.setDefinedName(elemenDTO.getDefinedName());

            // Reset Previous Values
            targetDefine.setAttribId(elemenDTO.getAttribId());
            targetDefine.setAttribName(elemenDTO.getAttribName());
            targetDefine.setSomeText(elemenDTO.getSomeText());
            targetDefine.setCoordinates(elemenDTO.getCoordinates());

            targetDefine.setXPath(elemenDTO.getXPath());
            targetDefine.setCurrentXPath(elemenDTO.getXPath());

            targetDefine.setIFrameXPath(elemenDTO.getIFrameXPath());

            targetDefine.setTagName(elemenDTO.getTagName());

            targetDefine.setShadowHost(elemenDTO.getShadowHost());
            targetDefine.setShadowRoot(elemenDTO.getShadowRoot());
            targetDefine.setCssSelector(elemenDTO.getCssSelector());
            targetDefine.setNestedShadow(elemenDTO.getNestedShadow());

            targetDefine.setSearchAttributeValue(elemenDTO.getSearchAttributeValue());

            targetDefine.setAttributeData(elemenDTO.getAttributeData());
            targetDefine.setCustomXPath(elemenDTO.getCustomXPath());

            if (!Strings.isNullOrEmpty(elemenDTO.getAttribId())) {
                targetDefine.setAttributeType("id");
                targetDefine.setAttributeValue(elemenDTO.getAttribId());
            } else if (!Strings.isNullOrEmpty(elemenDTO.getAttribName())) {
                targetDefine.setAttributeType("name");
                targetDefine.setAttributeValue(elemenDTO.getAttribName());
            } else {
                targetDefine.setAttributeType("");
                targetDefine.setAttributeValue("");
            }
            targetDefine.setIFrameElements(null);

            targetDefine.setXPathWorkedFirst(ARConstantsEngine.REGULAR_XPATH);

            // W3C 6 Headers
            String[] validHeaders = {"h1", "h2", "h3", "h4", "h5", "h6"};

            // links
            if (elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.LINK.getValue())) {
                elemenDTO.setTagName(WebElementTagNameEnum.ANCHOR.getValue());
            }

            if (elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.BUTTON.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.ANCHOR.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.OPTION.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.MAT_SELECT.getValue())) {
                targetDefine.setTagType(WebElementTagNameEnum.BUTTON);
                targetDefine.setIconType(WebElementIcon.CLICK);
            } else if (elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.INPUT.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.TEXT_AREA.getValue())) {
                targetDefine.setTagType(WebElementTagNameEnum.INPUT);
                targetDefine.setIconType(WebElementIcon.INSERT);
                targetDefine.setTagName("input");
            } else if (elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.PARAGRAPH.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.HEADER.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.LABEL.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.FOR_LABEL.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.DIV.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.STRONG.getValue())
                    || elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.SPAN.getValue())
                    || Arrays.asList(validHeaders)
                            .contains(elemenDTO.getTagName().toLowerCase())) {
                targetDefine.setTagType(WebElementTagNameEnum.OUTPUT);
                targetDefine.setIconType(WebElementIcon.OUTPUT);
                targetDefine.setTagName("label");
            } else if (elemenDTO.getTagName().equalsIgnoreCase(WebElementTagNameEnum.IFRAME.getValue())) {
                targetDefine.setTagType(WebElementTagNameEnum.IFRAME);
                targetDefine.setIconType(WebElementIcon.IFRAME);
            } else {
                targetDefine.setTagType(WebElementTagNameEnum.OUTPUT);
                targetDefine.setIconType(WebElementIcon.OUTPUT);
                targetDefine.setTagName("label");
            }
        }
        return targetDefine;
    }

    // TODO MORE INTELLIGENT  LOGIC
    public TargetElement defineNameTitles(TargetElement target) {

        try {
            String tagNameDefined = target.getDefinedName() != null ? target.getDefinedName() : target.getTagName();
            WebElement targetElem = target.getElement();

            // Check element tag names
            boolean isAnchor = target.getTagName().equalsIgnoreCase(WebElementTagNameEnum.ANCHOR.getValue());
            boolean isOption = target.getTagName().equalsIgnoreCase(WebElementTagNameEnum.OPTION.getValue());

            // Extract various attributes

            String labelAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.LABEL);
            String forLabelAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.FOR_LABEL);
            String classAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.CLASS);
            String typeAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.TYPE);
            String idAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.ID);
            String titleAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.TITLE);
            String disabledAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.DISABLED);
            String styleAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.STYLE);
            String dataTestIdAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.DATA_TEST_ID);

            String ariaLabelValue = extractAttribute(targetElem, WebElementAttributeEnum.ARIA_LABEL);
            String innerHTMLValue = extractAttribute(targetElem, WebElementAttributeEnum.INNER_HTML);
            String formControlNameAttributeValue =
                    extractAttribute(targetElem, WebElementAttributeEnum.FORM_CONTROL_NAME);
            String testIdAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.TEST_ID);
            String nameAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.NAME);
            String valueAttributeValue = extractAttribute(targetElem, WebElementAttributeEnum.VALUE);

            String hasMatLabelValue = extractAttribute(targetElem, WebElementAttributeEnum.MAT_LABEL);
            String hasMatInputValue = extractAttribute(targetElem, WebElementAttributeEnum.MAT_INPUT);
            String hasInputValue = extractAttribute(targetElem, WebElementAttributeEnum.MAT_INPUT);

            String valueHRefFile = extractFileExtension(extractAttribute(targetElem, WebElementAttributeEnum.HREF));

            String textLabel = targetElem.getText();

            // Determine boolean conditions
            boolean hasButton = target.getTagName().equalsIgnoreCase("button")
                    && isClickable(targetElem, tagNameDefined)
                    && !textLabel.isBlank();
            boolean hasAriaLabel = isValidString(ariaLabelValue);
            boolean hasInnerHTML = isValidString(innerHTMLValue) && !hasButton;
            boolean hasInnerHTMLTag = hasInnerHTML && (innerHTMLValue.contains("<") || innerHTMLValue.contains(">"));
            boolean hasFormControlName = isValidString(formControlNameAttributeValue);
            boolean hasTestId = isValidString(testIdAttributeValue);
            boolean hasName = isValidString(nameAttributeValue);
            boolean hasId = isValidString(idAttributeValue) && !hasButton;
            boolean hasValue = isValidString(valueAttributeValue);
            boolean hasHRefFile = isValidString(valueHRefFile);
            boolean hasParagraph =
                    !Strings.isNullOrEmpty(textLabel) && target.getTagName().equalsIgnoreCase("p");
            boolean hasSpan =
                    !Strings.isNullOrEmpty(textLabel) && target.getTagName().equalsIgnoreCase("span");
            boolean hasDiv =
                    !Strings.isNullOrEmpty(textLabel) && target.getTagName().equalsIgnoreCase("div");

            boolean isLabel = !Strings.isNullOrEmpty(labelAttributeValue);
            boolean isForLabel = !Strings.isNullOrEmpty(forLabelAttributeValue);

            boolean isElementHidden;
            try {
                isElementHidden = extractAttribute(targetElem, WebElementAttributeEnum.TYPE) != null
                        && extractAttribute(targetElem, WebElementAttributeEnum.TYPE)
                                .equalsIgnoreCase("hidden");

            } catch (Exception ignored) {
                isElementHidden = false;
            }

            target.setIsElementHidden(isElementHidden);

            // Set nameLabel and nameField based on conditions
            if (isLabel) {
                target = setElementText(target, labelAttributeValue, labelAttributeValue);
            } else if (isForLabel) {
                target = setElementText(target, forLabelAttributeValue, forLabelAttributeValue);
            } else if (isOption && hasValue) {
                target = setElementText(target, valueAttributeValue, valueAttributeValue);
            } else if (hasFormControlName) {
                target = setElementText(target, formControlNameAttributeValue, formControlNameAttributeValue);
            } else if (hasTestId) {
                target = setElementText(target, testIdAttributeValue, testIdAttributeValue);
            } else if (hasName) {
                target = setElementText(target, nameAttributeValue, nameAttributeValue);
            } else if (hasAriaLabel) {
                target = setElementText(target, ariaLabelValue, ariaLabelValue);
            } else if (isAnchor && hasInnerHTML && !hasInnerHTMLTag) {
                target = setElementText(target, innerHTMLValue, innerHTMLValue);
            } else if (hasId) {
                target = setElementText(target, idAttributeValue, idAttributeValue);
            } else if (hasHRefFile) {
                target = setElementText(target, valueHRefFile + " File", valueHRefFile + " File");
            } else if (hasParagraph) {
                target = setElementText(target, textLabel, tagNameDefined);
            } else if (hasButton) {
                target = setElementText(target, textLabel, tagNameDefined);
            } else if (hasSpan) {
                target = setElementText(target, textLabel, tagNameDefined);
            } else if (hasDiv) {
                target = setElementText(target, textLabel, tagNameDefined);
            } else if (!Strings.isNullOrEmpty(textLabel) && !Strings.isNullOrEmpty(tagNameDefined)) {
                target = setElementText(target, textLabel, tagNameDefined);
            } else if (!Strings.isNullOrEmpty(hasMatLabelValue)) {
                target = setElementText(target, hasMatLabelValue, hasMatLabelValue);
            } else if (!Strings.isNullOrEmpty(hasMatInputValue)) {
                target = setElementText(target, hasMatInputValue, hasMatInputValue);
            } else if (!Strings.isNullOrEmpty(hasInputValue)) {
                target = setElementText(target, hasInputValue, hasInputValue);
            } else if (!Strings.isNullOrEmpty(dataTestIdAttributeValue)) {
                target = setElementText(target, dataTestIdAttributeValue, dataTestIdAttributeValue);
            } else if (!Strings.isNullOrEmpty(titleAttributeValue)) {
                target = setElementText(target, titleAttributeValue, titleAttributeValue);
            } else if (!Strings.isNullOrEmpty(tagNameDefined) && tagNameDefined.equalsIgnoreCase("iFrame")) {
                target = setElementText(target, target.getTagName(), tagNameDefined);
            } else {
                //                if (tagNameDefined.equalsIgnoreCase("input")) {
                //                    target.setTagType(WebElementTagNameEnum.INPUT);
                //                }else  if (tagNameDefined.equalsIgnoreCase("input")) {
                //                    target.setTagType(WebElementTagNameEnum.OUTPUT);
                //                } else  if (tagNameDefined.equalsIgnoreCase("input")) {
                //                    target.setTagType(WebElementTagNameEnum.OUTPUT);
                //                }
                target = setElementText(target, target.getTagName(), ARConstantsEngine.VALUE_NO_IDENTIFICATION);
            }

        } catch (Exception e) {
            logOperations.warn("Cannot define Target Name Titles");
        }
        return target;
    }

    private TargetElement setElementText(TargetElement target, String nameLabelText, String nameFieldText) {
        target.setNameLabel(nameLabelText == null ? "" : nameLabelText.trim().replaceAll("\\s+", " "));
        target.setNameField(nameFieldText == null ? "" : nameFieldText.trim().replaceAll("\\s+", " "));

        String nameDefinedPriority = target.getNameLabel();
        if (!Strings.isNullOrEmpty(target.getAttribId())
                || !Strings.isNullOrEmpty(target.getAttribName())
                || !Strings.isNullOrEmpty(target.getSomeText())) {
            nameDefinedPriority = (!Strings.isNullOrEmpty(target.getSomeText())
                    ? PerformActions.truncateAndNormalize(target.getSomeText(), 30)
                    : !Strings.isNullOrEmpty(target.getAttribId())
                            ? target.getAttribId()
                            : !Strings.isNullOrEmpty(target.getAttribName())
                                    ? target.getAttribName()
                                    : nameDefinedPriority);
        }

        target.setDefinedName(nameDefinedPriority);

        return target;
    }

    public static String extractAttribute(WebElement element, WebElementAttributeEnum attributeEnum) {
        return element.getAttribute(attributeEnum.getValue());
    }

    public boolean isClickable(WebElement element, String tagNameDefined) {
        List<WebElementTagNameEnum> clickableTags = WebElementTagNameEnum.clickableTags();
        boolean isClickableTag =
                clickableTags.stream().anyMatch(t -> t.getValue().equalsIgnoreCase(tagNameDefined));
        List<WebElementAttributeTypeValueEnum> clickableValues = WebElementAttributeTypeValueEnum.getClickableValues();
        boolean isClickableValue = clickableValues.stream().anyMatch(v -> v.getValue()
                .equalsIgnoreCase(element.getAttribute(WebElementAttributeEnum.TYPE.getValue())));
        boolean isInputTag = tagNameDefined.equalsIgnoreCase(WebElementTagNameEnum.INPUT.getValue());
        return (isClickableTag && !isInputTag) || (isInputTag && isClickableValue && isClickableTag);
    }

    private boolean isValidString(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Extracts the file extension from the given string, considering it may be a path.
     *
     * @param input The string from which to extract the file extension.
     * @return The file extension if present and the string is identified as a file, otherwise an empty string.
     */
    public static String extractFileExtension(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // Find the last slash in the string
        int lastIndexOfSlash = input.lastIndexOf('/');

        // Get the substring after the last slash
        String lastSegment = lastIndexOfSlash == -1 ? input : input.substring(lastIndexOfSlash + 1);

        // If the last segment contains a period, it is considered a file
        int lastIndexOfDot = lastSegment.lastIndexOf('.');
        if (lastIndexOfDot == -1 || lastIndexOfDot == lastSegment.length() - 1) {
            return "";
        }

        // Extract the substring after the last period
        return lastSegment.substring(lastIndexOfDot + 1);
    }

    // ---------- helpers ----------

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static ElementDTO ensureFirstElement(SplitDTO splitDTO) {
        ElementDTO[] arr = splitDTO.getElementDetails();
        if (arr == null || arr.length == 0) {
            ElementDTO e = new ElementDTO();
            splitDTO.setElementDetails(new ElementDTO[] {e});
            return e;
        }
        return arr[0];
    }

    /**
     * Copies only non-null / non-blank fields from InstructionLoad into SplitDTO and its first ElementDTO.
     */
    private static void applyInstructionToSplit(SplitDTO splitDTO, InstructionLoad src) {
        if (splitDTO == null || src == null) return;

        // --- SplitDTO (top-level) ---
        if (src.getHomeBankingId() != null) splitDTO.setHomeBankingId(src.getHomeBankingId());
        if (src.getBotJobId() != null) splitDTO.setBotJobId(src.getBotJobId());
        if (hasText(src.getBotJobName())) splitDTO.setBotJobName(src.getBotJobName());

        if (src.getBlockId() != null) splitDTO.setBlockId(src.getBlockId());
        if (hasText(src.getBlockName())) splitDTO.setBlockName(src.getBlockName());
        if (src.getBlockOrderNumber() != null) splitDTO.setBlockOrderNumber(src.getBlockOrderNumber());
        if (src.getBlockActive() != null) splitDTO.setBlockActive(src.getBlockActive());

        if (src.getId() != null) splitDTO.setInstructionId(src.getId());
        if (hasText(src.getName())) splitDTO.setInstructionName(src.getName());
        if (src.getInstructionOrderNumber() != null)
            splitDTO.setInstructionOrderNumber(src.getInstructionOrderNumber());
        if (src.getInstructionActive() != null) splitDTO.setInstructionActive(src.getInstructionActive());

        if (hasText(src.getActions())) splitDTO.setActions(src.getActions());
        if (hasText(src.getOperation())) splitDTO.setOperation(src.getOperation());

        if (src.getVariableId() != null) splitDTO.setVariableId(src.getVariableId());
        if (src.getParentId() != null) splitDTO.setParentId(src.getParentId());
        if (src.getParentBlockId() != null) splitDTO.setParentBlockId(src.getParentBlockId());

        if (hasText(src.getExportFile())) splitDTO.setExportFile(src.getExportFile());
        // appQueryApp / appQueryPackage not present on InstructionLoad → not set here.

        // --- ElementDTO (first item in array) ---
        ElementDTO el = ensureFirstElement(splitDTO);

        // Choose which ID to mirror on the element:
        // If your ElementDTO.id represents the variable link, prefer variableId; otherwise use src.getId()
        if (src.getVariableId() != null) el.setId(src.getVariableId());
        else if (src.getId() != null) el.setId(src.getId());

        if (hasText(src.getType())) el.setTypeElement(src.getType());
        if (hasText(src.getTagName())) el.setTagName(src.getTagName());
        if (hasText(src.getXpath())) el.setXPath(src.getXpath()); // Lombok: field `xPath` -> setter `setXPath`
        if (hasText(src.getCoordinates())) el.setCoordinates(src.getCoordinates());
        if (hasText(src.getIFrameXPath())) el.setIFrameXPath(src.getIFrameXPath());
        if (hasText(src.getShadowHost())) el.setShadowHost(src.getShadowHost());
        if (hasText(src.getShadowRoot())) el.setShadowRoot(src.getShadowRoot());
        if (hasText(src.getCssSelector())) el.setCssSelector(src.getCssSelector());

        // Fields without a clear mapping from InstructionLoad are left untouched:
        // someText, attribId, attribName, attributeData, customXPath, nestedShadow,
        // attributeValue, attributeType, searchAttributeValue.
    }

    /**
     * Extracts and defines a cloned TargetElement from the given ElementDTO from performList.
     */
    public TargetElement extractPickClone(ElementDTO elementDTO, String ignore) {
        TargetElement targetLocal = defineSearchReturn(elementDTO, null);

        //        WebElement elementFound = performActions.findWebElement(targetLocal);
        //        if (targetLocal.getElement() == null && elementFound != null) {
        //            targetLocal.setElement(elementFound);
        //        }

        // Save references for different coordinate strategies
        // 3 Different Coordinates // Original from JavaScript  // WebDriver Selenium ElementFound
        // FallBack React Computed
        // TO DO:   KEEP THE ORIGINALS  FROM ANDROID
        performActions.defineSavedReferenced(targetLocal);

        // Define tag name/title
        //        targetLocal = defineNameTitles(targetLocal);

        // Validate Shadow DOM or regular CSS selectors
        if (Strings.isNullOrEmpty(targetLocal.getShadowHost()) && Strings.isNullOrEmpty(targetLocal.getCssSelector())) {

        } else if (!Strings.isNullOrEmpty(targetLocal.getCssSelector())) {
            targetLocal.setXPathWorkedFirst(ARConstantsEngine.REGULAR_XPATH);
        } else {
            targetLocal.setXPathWorkedFirst(ARConstantsEngine.SHADOW_DOM);
        }

        return targetLocal;
    }
}
