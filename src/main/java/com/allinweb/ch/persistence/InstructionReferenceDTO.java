package com.allinweb.ch.persistence;

import javax.persistence.*;

@Entity
@Table(name = "instruction_reference")
// @SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "instructionReferenceSeq", allocationSize = 1)
public class InstructionReferenceDTO extends BaseDTO {

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "value", length = 1000)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Ensures the foreign key is not null
    @JoinColumn(name = "block_loop_instruction_id", nullable = false) // Adds non-null constraint on the foreign key
    private BlockLoopInstructionDTO blockLoopInstructionDTO;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "bot_job_id")
    private BotJobDTO botJobDTO;

    public BotJobDTO getBotJobDTO() {
        return botJobDTO;
    }

    public void setBotJobDTO(BotJobDTO botJobDTO) {
        this.botJobDTO = botJobDTO;
    }

    public InstructionReferenceDTO() {
        super();
    }

    public InstructionReferenceDTO(int id) {
        super(id);
    }

    public InstructionReferenceDTO(BlockLoopInstructionDTO instruction) {
        blockLoopInstructionDTO = instruction;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public BlockLoopInstructionDTO getBlockLoopInstructionDTO() {
        return blockLoopInstructionDTO;
    }

    public void setBlockLoopInstructionDTO(BlockLoopInstructionDTO blockLoopInstructionDTO) {
        this.blockLoopInstructionDTO = blockLoopInstructionDTO;
    }

    //    public static List<InstructionReferenceDTO> createReferencesFromSavedInstructionForInstruction(
    //            SavedBlockLoopInstructionDTO savedInstructionDTO, BlockLoopInstructionLoadDTO blockLoopInstructionDTO)
    // {
    //        List<SavedInstructionReferenceDTO> referenceList = ABRSharedResources.getInstance()
    //                .getEntityList(
    //                        SavedInstructionReferenceDTO.class,
    //                        savedReference ->
    //                                savedReference.getSavedBlockLoopInstructionDTO().getId()
    //                                        == savedInstructionDTO.getId());
    //        List<InstructionReferenceDTO> list = new ArrayList<>();
    //        referenceList.forEach(
    //                reference -> list.add(copyFromSavedReferenceForInstruction(reference, blockLoopInstructionDTO)));
    //        return list;
    //    }
    //
    //    private static InstructionReferenceDTO copyFromSavedReferenceForInstruction(
    //            SavedInstructionReferenceDTO savedReference, BlockLoopInstructionDTO instruction) {
    //        InstructionReferenceDTO saved = new InstructionReferenceDTO();
    //        saved.setValue(savedReference.getValue());
    //        saved.setReferenceType(savedReference.getReferenceType());
    //        saved.setBlockLoopInstructionDTO(instruction);
    //        return saved;
    //    }
}
