package com.allinweb.ch.util;

import com.allinweb.ch.model.ElementDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsScanResultDTO {
    private boolean ok;

    @JsonProperty("elements")
    private List<ElementDTO> elements;

    private Integer totalElements;
    private String error;
}
