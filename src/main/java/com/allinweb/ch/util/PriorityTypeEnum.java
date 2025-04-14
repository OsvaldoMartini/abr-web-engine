package com.allinweb.ch.util;

public enum PriorityTypeEnum {
    attribute,
    attributeName,
    attributeID,
    searchAttribute,
    ByAttribute, // NEW
    xpath,
    coordinates,
    js_coordinates,
    cp_coordinates,
    allAttributes,
    ById,
    ByClassName,
    ByName,
    ByTagName,
    ByChained,
    ByLinkText,
    ByPartialLinkText,
    ByCssSelector, //      ".nav-menu li";
    ByXPath, //           "//input[@type='text']"
    ByLabels, //           "ByLabels
    ExecuteScript, //      "return document.getElementById('search-top')");
    createXPath, //         Generates XPath Recursive tom the Elements Found
    dynamic, //         Generates Dynamic Action -> Click, Hover, Etc.
    jsoup,
    json; //                JSOUP  Search Library Experimental

    public static PriorityTypeEnum getPriorityType(String priorityType) {
        for (PriorityTypeEnum val : PriorityTypeEnum.values()) {
            if (val.name().equalsIgnoreCase(priorityType)) {
                return val;
            }
        }
        throw new EnumConstantNotPresentException(PriorityTypeEnum.class, priorityType);
    }

    @Override
    public String toString() {
        return this.name();
    }
}
