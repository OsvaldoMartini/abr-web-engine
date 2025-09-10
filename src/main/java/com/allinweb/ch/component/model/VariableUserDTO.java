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
    private StringProperty parentName;
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
            String parentName,
            String localFormat,
            String delimiter,
            String usedVars) {
        this.id = new SimpleIntegerProperty(id);
        this.type = new SimpleStringProperty(type);
        this.name = new SimpleStringProperty(name);
        this.value = new SimpleStringProperty(value);
        this.botJobId = new SimpleIntegerProperty(botJobId);
        this.parentId = new SimpleIntegerProperty(parentId);
        this.parentName = new SimpleStringProperty(parentName);
        this.localFormat = new SimpleStringProperty(localFormat);
        this.delimiter = new SimpleStringProperty(delimiter);
        this.usedVars = new SimpleStringProperty(usedVars);
    }

    public VariableUserDTO(
            Integer id, String type, String name, String value, Integer botJobId, Integer parentId, String parentName) {
        this.id = new SimpleIntegerProperty(id);
        this.type = new SimpleStringProperty(type);
        this.name = new SimpleStringProperty(name);
        this.value = new SimpleStringProperty(value);
        this.botJobId = new SimpleIntegerProperty(botJobId);
        this.parentId = new SimpleIntegerProperty(parentId);
        this.parentName = new SimpleStringProperty(parentName);
    }

    public Integer getId() {
        return id.get();
    }

    public void setId(Integer id) {
        this.id.set(id);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public String getType() {
        return type.get();
    }

    public void setType(String type) {
        this.type.set(type);
    }

    public StringProperty typeProperty() {
        return type;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getValue() {
        return value.get();
    }

    public void setValue(String value) {
        this.value.set(value);
    }

    public StringProperty valueProperty() {
        return value;
    }

    public Integer getBotJobId() {
        return botJobId.get();
    }

    public void setBotJobId(Integer botJobId) {
        this.botJobId.set(botJobId);
    }

    public IntegerProperty botJobIdProperty() {
        return botJobId;
    }

    public Integer getParentId() {
        return parentId.get();
    }

    public void setParentId(Integer parentId) {
        this.parentId.set(parentId);
    }

    public IntegerProperty parentIdProperty() {
        return parentId;
    }

    public String getParentName() {
        return parentName.get();
    }

    public void setParentName(String parentName) {
        this.parentName.set(parentName);
    }

    public StringProperty parentNameProperty() {
        return parentName;
    }

    public String getUsedVars() {
        return usedVars.get();
    }

    public void setUsedVars(String usedVars) {
        this.usedVars.set(usedVars);
    }

    public StringProperty usedVarsProperty() {
        return usedVars;
    }

    public String getLocalFormat() {
        return localFormat.get();
    }

    public void setLocalFormat(String localFormat) {
        this.localFormat.set(localFormat);
    }

    public StringProperty localFormatProperty() {
        return localFormat;
    }

    public String getDelimiter() {
        return delimiter.get();
    }

    public void setDelimiter(String delimiter) {
        this.delimiter.set(delimiter);
    }

    public StringProperty delimiterProperty() {
        return delimiter;
    }
}
