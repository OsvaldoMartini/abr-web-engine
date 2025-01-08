package com.allinweb.ch.persistence;

import javax.persistence.*;

@MappedSuperclass
public class BaseDTO {

    @Id
    // @GeneratedValue(strategy = GenerationType.AUTO, generator = "idgen")
    @Column(name = "id")
    private Integer id;

    protected BaseDTO() {}

    protected BaseDTO(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
