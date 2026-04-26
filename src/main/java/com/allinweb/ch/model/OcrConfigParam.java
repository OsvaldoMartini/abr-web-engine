package com.allinweb.ch.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Row in {@code ocr_config_param}. Flat key/value so new params need no schema migration. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OcrConfigParam {
    private Integer id;
    private Integer profileId;
    private String
            category; // correlation | engine | screenshot | preprocessing | color_mapping | button_detection | output
    private String name;
    private String valueType; // int | double | string | bool | json
    private String value; // raw storage; JSON-encoded for 'json' type
}
