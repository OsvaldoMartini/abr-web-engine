package com.allinweb.ch.persistence;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class JobUserDTO {
    private StringProperty id;
    private StringProperty description;
    private StringProperty name;
    private StringProperty homeBakk;

    public JobUserDTO(String id, String description, String name, String homeBakk) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.description = new SimpleStringProperty(description);
        this.homeBakk = new SimpleStringProperty(homeBakk);
    }

    public String getId() {
        return id.get();
    }

    public StringProperty idProperty() {
        return id;
    }

    public void setId(String id) {
        this.id.set(id);
    }

    public String getDescription() {
        return description.get();
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public void setDescription(String description) {
        this.description.set(description);
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

    public String getHomeBakk() {
        return homeBakk.get();
    }

    public StringProperty homeBakkProperty() {
        return homeBakk;
    }

    public void setHomeBakk(String homeBakk) {
        this.homeBakk.set(homeBakk);
    }
}
