package com.allinweb.ch.component.model;

import lombok.Getter;

@Getter
public class FormatOption {
    private final String text;
    private final String value;

    public FormatOption(String text, String value) {
        this.text = text;
        this.value = value;
    }
}
