package com.allinweb.ch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeUrlDTO {
    private Integer id;
    private String url;
    private Integer homeBankingId;
    private String orgName;
}
