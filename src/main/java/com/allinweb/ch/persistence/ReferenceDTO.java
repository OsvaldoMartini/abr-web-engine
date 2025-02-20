package com.allinweb.ch.persistence;

import javax.persistence.*;

@Entity
@Table(name = "reference")
// @SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "instructionReferenceSeq", allocationSize = 1)
public class ReferenceDTO extends BaseDTO {

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "value", length = 10000)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Ensures the foreign key is not null
    @JoinColumn(name = "instruction_id", nullable = false) // Adds non-null constraint on the foreign key
    private InstructionDTO instructionDTO;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "bot_job_id")
    private BotJobDTO botJobDTO;

    public BotJobDTO getBotJobDTO() {
        return botJobDTO;
    }

    public void setBotJobDTO(BotJobDTO botJobDTO) {
        this.botJobDTO = botJobDTO;
    }

    public ReferenceDTO() {
        super();
    }

    public ReferenceDTO(int id) {
        super(id);
    }

    public ReferenceDTO(InstructionDTO instruction) {
        instructionDTO = instruction;
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

    public InstructionDTO getBlockLoopInstructionDTO() {
        return instructionDTO;
    }

    public void setBlockLoopInstructionDTO(InstructionDTO instructionDTO) {
        this.instructionDTO = instructionDTO;
    }

    //    public static List<InstructionReferenceDTO> createReferencesFromSavedInstructionForInstruction(
    //            SavedBlockLoopInstructionDTO savedInstructionDTO, BlockLoopInstructionLoadDTO blockLoopInstructionDTO)
    // {
    //        List<SavedInstructionReferenceDTO> referenceList = ARSharedResources.getInstance()
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
