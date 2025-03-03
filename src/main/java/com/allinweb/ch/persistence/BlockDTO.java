package com.allinweb.ch.persistence;

import java.util.*;
import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "block")
@Data
public class BlockDTO extends BaseDTO {

    @Column(name = "bot_job_id")
    private Integer botJobId;

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
