package com.allinweb.ch.model;

public record InputInfo(
        String tag,
        String id,
        String name,
        String type,
        String labelText,
        String identifier,
        String printable,
        String controlKind, // TYPE | OPEN_DROPDOWN | SELECT_OPTION
        boolean isEditable) {}
