package com.allinweb.ch.builder;

import com.allinweb.ch.util.ARWebUtil;
import java.util.HashMap;
import java.util.Map;
import org.openqa.selenium.WebElement;

public class WebElementScriptBuilder {

    private static final String DOUBLE_APEX = "\"";

    private Map<Integer, String> elementReferenceMap = new HashMap<>();

    private WebElement currentElementSelected;

    public WebElementScriptBuilder() {}

    public String createSetStyleScript(String cssToApply) {
        String elementReference = getReference();
        return elementReference + ".setAttribute('style', '" + cssToApply + "')";
    }

    public String extractAttributesScript() {
        String elementReference = getReference();
        return "return Array.from(" + elementReference + ".attributes, ({name,value}) => name)";
    }

    public String elementReferenceScript() {
        return "return " + getReference();
    }

    private String getReference() {
        /*for (WebElementAttributeEnum attribute : WebElementAttributeEnum.values()){
            String attributeValue = currentElementSelected.getAttribute(attribute.getValue());
            boolean hasValueInAttribute = attributeValue != null && !attributeValue.isBlank();
            if(hasValueInAttribute) {
                return getReference(attribute);
            }
        }*/
        return getXPathReference();
    }

    private String getReference(WebElementAttributeEnum attributeReference) {
        return "document.querySelector(" + DOUBLE_APEX + currentElementSelected.getTagName() + "["
                + attributeReference.getValue() + "='"
                + currentElementSelected.getAttribute(attributeReference.getValue()) + "']" + DOUBLE_APEX + ")";
    }

    private String getXPathReference() {
        return "document.evaluate(" + DOUBLE_APEX + extractXPath() + DOUBLE_APEX
                + ", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue";
    }

    private String extractXPath() {
        String xPath = null;
        Integer hashCode = this.currentElementSelected.hashCode();
        if (elementReferenceMap.containsKey(hashCode)) {
            xPath = elementReferenceMap.get(hashCode);
        }
        if (xPath == null) {
            String path = ARWebUtil.extractWebElementXPath(currentElementSelected);
            elementReferenceMap.put(hashCode, path);
            xPath = elementReferenceMap.get(hashCode);
        }
        return xPath;
    }

    public WebElementScriptBuilder addElement(WebElement element) {
        if (!elementReferenceMap.containsKey(element.hashCode())) {
            elementReferenceMap.put(element.hashCode(), null);
        }
        this.currentElementSelected = element;
        return this;
    }
}
