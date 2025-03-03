package com.allinweb.ch.persistence;

import java.io.Serializable;
import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "bot_job")
@Data
public class BotJobDTO extends BaseDTO implements Serializable {

    @Column(name = "name", unique = true)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "priority", columnDefinition = "TEXT")
    private String priority;

    @Column(name = "home_banking_id")
    private Integer homeBankingId;

    @Column(name = "active")
    private Integer active;
}
