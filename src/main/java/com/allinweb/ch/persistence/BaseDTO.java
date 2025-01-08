package com.allinweb.ch.persistence;

import javax.persistence.*;

@MappedSuperclass
public class BaseDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idgen")
    @Column(name = "id")
    private int id;

    protected BaseDTO() {}

    protected BaseDTO(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
