package com.allinweb.ch.model;

import java.util.List;
import lombok.Data;

@Data
public class BotJobLoadDTO {
    private Integer id;
    private String name;
    private Integer botJobId;
    private String description;
    private String priority;
    private Integer blockOrderNumber;
    private String blockName;
    private Integer blockId;
    private String blockDescription;
    private Integer homeBankingId;
    private Integer homeUrlId;
    private Integer typeId;
    private boolean active;
    private List<BlockLoadDTO> blockLoadDTOList;
    private HomeBankingLoadDTO homeBankingLoadDTO;

    @Override
    public String toString() {
        return "BotJobLoadDTO{" + "id="
                + id + ", name='"
                + name + '\'' + ", blockLoadDTOList="
                + blockLoadDTOList + '}';
    }
}
