package com.allinweb.ch.persistence;

import java.io.Serializable;
import javax.persistence.*;
import lombok.Data;

@Entity()
@Table(name = "home_banking")
@Data
public class HomeBankingDTO extends BaseDTO implements Serializable {

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "name")
    @OrderBy("name DESC")
    private String name;

    @Column(name = "priority", columnDefinition = "TEXT")
    private String priority;

    @Column(name = "search_config", columnDefinition = "TEXT")
    private String searchConfig;

    @Column(name = "options_config", columnDefinition = "TEXT")
    private String optionsConfig;

    @Column(name = "cookies", columnDefinition = "TEXT")
    private String cookies;

    @Column(name = "driver_session", columnDefinition = "TEXT")
    private String driverSession;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;
}
