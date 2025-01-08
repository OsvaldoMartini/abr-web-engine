package com.allinweb.ch.component.model;

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
    private String blockDescription;
    private Integer homeBankingId;
    private Integer typeId;
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
