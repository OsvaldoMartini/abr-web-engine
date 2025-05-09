package com.allinweb.ch.component.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class VariableUserDTO {
    private IntegerProperty id;
    private StringProperty type;
    private StringProperty name;
    private StringProperty value;
    private IntegerProperty botJobId;
    private IntegerProperty parentId;
    private StringProperty localFormat;
    private StringProperty delimiter;
    private StringProperty usedVars;

    public VariableUserDTO() {}

    public VariableUserDTO(
            Integer id,
            String type,
            String name,
            String value,
            Integer botJobId,
            Integer parentId,
            String localFormat,
            String delimiter,
            String usedVars) {
        this.id = new SimpleIntegerProperty(id);
        this.type = new SimpleStringProperty(type);
        this.name = new SimpleStringProperty(name);
        this.value = new SimpleStringProperty(value);
        this.botJobId = new SimpleIntegerProperty(botJobId);
        this.parentId = new SimpleIntegerProperty(parentId);
        this.localFormat = new SimpleStringProperty(localFormat);
        this.delimiter = new SimpleStringProperty(delimiter);
        this.usedVars = new SimpleStringProperty(usedVars);
    }

    public VariableUserDTO(Integer id, String type, String name, String value, Integer botJobId, Integer parentId) {
        this.id = new SimpleIntegerProperty(id);
        this.type = new SimpleStringProperty(type);
        this.name = new SimpleStringProperty(name);
        this.value = new SimpleStringProperty(value);
        this.botJobId = new SimpleIntegerProperty(botJobId);
        this.parentId = new SimpleIntegerProperty(parentId);
    }

    public Integer getId() {
        return id.get();
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public void setId(Integer id) {
        this.id.set(id);
    }

    public String getType() {
        return type.get();
    }

    public StringProperty typeProperty() {
        return type;
    }

    public void setType(String type) {
        this.type.set(type);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public String getValue() {
        return value.get();
    }

    public StringProperty valueProperty() {
        return value;
    }

    public void setValue(String value) {
        this.value.set(value);
    }

    public Integer getBotJobId() {
        return botJobId.get();
    }

    public IntegerProperty botJobIdProperty() {
        return botJobId;
    }

    public void setBotJobId(Integer botJobId) {
        this.botJobId.set(botJobId);
    }

    public Integer getParentId() {
        return parentId.get();
    }

    public IntegerProperty parentIdProperty() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId.set(parentId);
    }

    public String getUsedVars() {
        return usedVars.get();
    }

    public StringProperty usedVarsProperty() {
        return usedVars;
    }

    public void setUsedVars(String usedVars) {
        this.usedVars.set(usedVars);
    }

    public String getLocalFormat() {
        return localFormat.get();
    }

    public StringProperty localFormatProperty() {
        return localFormat;
    }

    public void setLocalFormat(String localFormat) {
        this.localFormat.set(localFormat);
    }

    public String getDelimiter() {
        return delimiter.get();
    }

    public StringProperty delimiterProperty() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter.set(delimiter);
    }
}
