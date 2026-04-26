package com.allinweb.ch.model;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Row in {@code ocr_config_profile}. Scoping: home_url_id (most specific) > homebanking_id > is_default (global). */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OcrConfigProfile {
    private Integer id;
    private String name;
    private String description;
    private Integer homebankingId; // nullable
    private Integer homeUrlId; // nullable, more specific than homebankingId
    private boolean isDefault;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
