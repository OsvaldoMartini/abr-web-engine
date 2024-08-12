package com.allinweb.ch.dto;

import java.util.List;
import lombok.Data;

@Data
public class BlockLoadDTO {
    private int id;
    private int blockOrderNumber;
    private String name;
    private String description;
    private Integer typeId;
    private BotJobLoadDTO botJobLoadDTO;
    private List<BlockLoopInstructionLoadDTO> blockLoopInstructionLoadDTOS;

    @Override
    public String toString() {
        return "BlockLoadDTO{" + "id="
                + id + ", blockOrderNumber="
                + blockOrderNumber + ", name='"
                + name + '\'' + ", description='"
                + description + '\'' + ", typeId="
                + typeId + ", botJobLoadDTO="
                + botJobLoadDTO + ", blockLoopInstructionLoadDTOS="
                + blockLoopInstructionLoadDTOS + '}';
    }
}
