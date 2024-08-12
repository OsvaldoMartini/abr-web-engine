package com.allinweb.ch.dto;

import java.util.List;
import lombok.Data;

@Data
public class BotJobLoadDTO {
    private int id;
    private String name;
    private List<BlockLoadDTO> blockLoadDTOList;

    @Override
    public String toString() {
        return "BotJobLoadDTO{" + "id="
                + id + ", name='"
                + name + '\'' + ", blockLoadDTOList="
                + blockLoadDTOList + '}';
    }
}
