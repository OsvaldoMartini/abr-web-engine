package com.allinweb.ch.component.model;

import lombok.Getter;

@Getter
public class BlockOptions {
    private final String text;
    private final String value;
    private final Integer whereId;
    private final Integer blockId;

    public BlockOptions(String text, String value, Integer whereId, Integer blockId) {
        this.text = text;
        this.value = value;
        this.whereId = whereId; // Instruction or BlockOrderNumber
        this.blockId = blockId;
    }

    // Converter from BlockLoadDTO - BlockOptions
    public static BlockOptions fromBlockWithInstructionId(BlockLoadDTO block) {
        Integer firstInstructionId = null;

        if (block.getInstructionLoadDTOS() != null
                && !block.getInstructionLoadDTOS().isEmpty()) {
            InstructionLoadDTO firstInstr = block.getInstructionLoadDTOS().get(0);
            firstInstructionId = firstInstr.getId(); // adapt if InstructionLoadDTO has another identifier
        }

        return new BlockOptions(
                block.getBlockOrderNumber() + "# " + block.getName(), // text
                block.getName(),
                firstInstructionId, // instructionId
                block.getId() // blockId
                );
    }

    // Converter from BlockLoadDTO - BlockOptions
    public static BlockOptions fromBlockWithOrderNumber(BlockLoadDTO block) {
        return new BlockOptions(
                block.getBlockOrderNumber() + "# " + block.getName(), // text
                block.getName(), // value
                block.getBlockOrderNumber(), // instructionId
                block.getId() // blockId
                );
    }
}
