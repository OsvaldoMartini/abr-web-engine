package com.allinweb.ch.component.model;

import java.util.List;
import lombok.Data;

@Data
public class HomeBankingLoadDTO {

    private Integer id;
    private String url;
    private String name;
    private String priority;
    private String searchConfig;
    private String optionsConfig;
    private String cookies;
    private String driverSession;
    private String username;
    private String password;
    private List<HomeUrlDTO> homeUrlDTOS;
}
