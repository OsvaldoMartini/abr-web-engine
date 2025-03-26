package com.allinweb.ch.persistence;

import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "component_block")
@Data
public class ComponentBlockDTO extends BaseDTO {

    @Column(name = "home_banking_id")
    private Integer homeBankingId;

    @Column(name = "block_order_number")
    private int blockOrderNumber;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "type_id")
    private Integer typeId;

    @Column(name = "export_file", columnDefinition = "TEXT")
    private String exportFile;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "wait")
    private Integer wait;
}
