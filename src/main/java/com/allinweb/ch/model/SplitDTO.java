package com.allinweb.ch.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class SplitDTO {

    private static final String ATTR_PREFIX = "AttrData:";

    private String type;
    private String sessionId;
    private String operationId;

    private Integer homeBankingId;
    private Integer botJobId;
    private String botJobName = "";

    private Integer blockId;
    private String blockName;
    private Integer blockOrderNumber;
    private Boolean blockActive;

    private Integer instructionId;
    private String instructionName;
    private Integer instructionOrderNumber;
    private Boolean instructionActive;

    private String actions;
    private String operation;

    private Integer variableId;
    private Integer parentId;
    private Integer parentBlockId;

    private String exportFile;
    private String appQueryApp;
    private String appQueryPackage;
    private List<String> packagesFound;
    private String appMainActivity;
    private Integer scrollTimes;
    private String deviceId;

    private String projectType;

    // Optional fields for SplitDTO
    private ElementDTO[] elementDetails;

    // Optional fields for BlockSplitDTO
    private DetailsDTO details;

    // Optional fields for UpdtateRows
    private List<UpdatedRow> updatedRows;
    // Optional fields for UpdtateRows
    private List<UpdatedRow> instructions;

    // Optional fields for BlockOrderDetail
    private List<BlockOrderDetailDTO> updatedBlocks;

    private String scannerType;

    private String sourceImage;
    private Map<String, FieldsToValidate> fieldsToValidate;

    public static InstructionLoad mapSplitToInstruction(SplitDTO split) {
        if (split == null) return null;

        InstructionLoad instruction = new InstructionLoad();

        // Map all relevant fields from SplitDTO to InstructionLoad
        instruction.setId(split.getInstructionId());
        instruction.setBotJobId(split.getBotJobId());
        instruction.setHomeBankingId(split.getHomeBankingId());
        instruction.setBlockId(split.getBlockId());
        instruction.setParentBlockId(split.getParentBlockId());
        instruction.setParentId(split.getParentId());
        instruction.setActions(split.getActions());
        instruction.setSessionId(split.getSessionId());
        instruction.setOperation(split.getOperationId());
        instruction.setInstructionOrderNumber(split.getInstructionOrderNumber());
        instruction.setBlockOrderNumber(split.getBlockOrderNumber());
        instruction.setName(split.getInstructionName());
        instruction.setBlockName(split.getBlockName() != null ? split.getBlockName() : split.getBotJobName());

        // Optional element details mapping (if needed)
        if (split.getElementDetails() != null) {
            // map or store element details in a list, depending on your business logic
        }

        // Flags or defaults
        instruction.setInstructionActive(true);

        return instruction;
    }

    public static void applyAttrDataFromReferences(SplitDTO splitDTO, InstructionLoad instruction) {
        if (splitDTO == null || instruction == null) return;

        List<ReferenceLoadDTO> refs = instruction.getReferenceLoadDTOList();
        if (refs == null || refs.isEmpty()) return;

        // Collect attributes (no duplicates by name; last wins)
        Map<String, String> attrs = new LinkedHashMap<>();
        String attribId = null;
        String attribName = null;

        for (ReferenceLoadDTO ref : refs) {
            if (ref == null || ref.getReferenceType() == null || ref.getValue() == null) continue;
            String refType = ref.getReferenceType().trim();

            if (refType.startsWith(ATTR_PREFIX)) {
                String name = refType.substring(ATTR_PREFIX.length()).trim();
                String value = ref.getValue();

                // keep raw values for AttributeData[]
                // Replace any previous value for this attribute name
                attrs.put(name, value);

                // legacy fields (will be overridden below with smarter locator)
                if (name.equalsIgnoreCase("id")) {
                    attribId = value; // temporary; may be replaced by computed XPath-like locator
                } else if (name.equalsIgnoreCase("name")) {
                    attribName = value;
                }
            }
        }

        if (attrs.isEmpty() && attribId == null && attribName == null) return;

        // ---- NEW: compute smarter attribId from collected attrs ----
        // expects: class (optional), id (resource-id), text/content-desc (optional)
        String computedLocator = buildAttribLocatorFromAttrs(attrs);
        if (!computedLocator.isBlank()) {
            attribId = computedLocator; // override the plain resource-id
        }

        // ===== OPTION A: fully reset elementDetails to a single element (no accumulation across calls)
        // If you want to ALWAYS avoid accumulation across calls, keep the next 2 lines enabled:
        splitDTO.setElementDetails(new ElementDTO[] {new ElementDTO()});

        // ===== OPTION B: keep existing elements but REPLACE their attributes (no duplicates inside)
        // If you prefer to keep existing elements, comment out OPTION A above and ensure there is at least one element:
        // ElementDTO[] elements = splitDTO.getElementDetails();
        // if (elements == null || elements.length == 0) {
        //     splitDTO.setElementDetails(new ElementDTO[] { new ElementDTO() });
        // }

        ElementDTO[] elements = splitDTO.getElementDetails();
        if (elements == null || elements.length == 0) {
            // safety: should not happen if OPTION A is enabled
            splitDTO.setElementDetails(new ElementDTO[] {new ElementDTO()});
            elements = splitDTO.getElementDetails();
        }

        // Build the replacement AttributeData[]
        AttributeData[] newAttrArray = attrs.entrySet().stream()
                .map(e -> new AttributeData(e.getKey(), e.getValue()))
                .toArray(AttributeData[]::new);

        // Assign data (REPLACE, do not append)
        for (ElementDTO el : elements) {
            if (el == null) continue;

            el.setAttributeData(newAttrArray); // <-- replace instead of append
            if (instruction.getId() != null) el.setId(instruction.getId());
            if (attribId != null) el.setAttribId(attribId); // <- now the smart locator
            if (attribName != null) el.setAttribName(attribName);
        }
    }

    // Builds: //<class>[@resource-id="..." and (@text="..." | @content-desc="...")]
    private static String buildAttribLocatorFromAttrs(Map<String, String> attrs) {
        if (attrs == null) return "";
        String cls = nz(attrs.get("class")); // optional
        String resId = nz(attrs.get("id")); // required to build
        String text = nz(attrs.get("text")); // optional
        String desc = nz(attrs.get("content-desc")); // optional

        // >>> NEW: if it's actually a Button, DON'T override attribId
        if (isButtonClassName(cls)) return "";

        if (resId.isEmpty()) return ""; // without resource-id we keep the old attribId

        // Base: //<class or *>[@resource-id="..."]
        StringBuilder sb = new StringBuilder("//")
                .append(cls.isEmpty() ? "*" : cls)
                .append("[@resource-id=\"")
                .append(resId.replace("\"", "\\\""))
                .append("\"]");

        // Prefer @text when present; else @content-desc (ignore literal "null")
        if (!text.isBlank() && !"null".equalsIgnoreCase(text)) {
            sb.insert(sb.length() - 1, " and @text=\"" + text.replace("\"", "\\\"") + "\"");
        } else if (!desc.isBlank() && !"null".equalsIgnoreCase(desc)) {
            sb.insert(sb.length() - 1, " and @content-desc=\"" + desc.replace("\"", "\\\"") + "\"");
        }

        return sb.toString();
    }

    private static boolean isButtonClassName(String cls) {
        if (cls == null) return false;
        cls = cls.trim();
        return "android.widget.Button".equals(cls)
                || cls.endsWith("Button") // e.g., AppCompatButton
                || "button".equalsIgnoreCase(cls); // in case class was normalized elsewhere
    }

    // tiny null-to-empty guard
    private static String nz(String s) {
        return (s == null) ? "" : s;
    }

    /**
     * Copies only non-null / non-blank fields from InstructionLoad into SplitDTO and its first ElementDTO.
     */
    public static void applyInstructionToSplit(SplitDTO splitDTO, InstructionLoad src) {
        if (splitDTO == null || src == null) return;

        // --- SplitDTO (top-level) ---
        if (src.getHomeBankingId() != null) splitDTO.setHomeBankingId(src.getHomeBankingId());
        if (src.getBotJobId() != null) splitDTO.setBotJobId(src.getBotJobId());
        if (hasText(src.getBotJobName())) splitDTO.setBotJobName(src.getBotJobName());

        if (src.getBlockId() != null) splitDTO.setBlockId(src.getBlockId());
        if (hasText(src.getBlockName())) splitDTO.setBlockName(src.getBlockName());
        if (src.getBlockOrderNumber() != null) splitDTO.setBlockOrderNumber(src.getBlockOrderNumber());
        if (src.getBlockActive() != null) splitDTO.setBlockActive(src.getBlockActive());

        if (src.getId() != null) splitDTO.setInstructionId(src.getId());
        if (hasText(src.getName())) splitDTO.setInstructionName(src.getName());
        if (src.getInstructionOrderNumber() != null)
            splitDTO.setInstructionOrderNumber(src.getInstructionOrderNumber());
        if (src.getInstructionActive() != null) splitDTO.setInstructionActive(src.getInstructionActive());

        if (hasText(src.getActions())) splitDTO.setActions(src.getActions());
        if (hasText(src.getOperation())) splitDTO.setOperation(src.getOperation());

        if (src.getVariableId() != null) splitDTO.setVariableId(src.getVariableId());
        if (src.getParentId() != null) splitDTO.setParentId(src.getParentId());
        if (src.getParentBlockId() != null) splitDTO.setParentBlockId(src.getParentBlockId());

        if (hasText(src.getExportFile())) splitDTO.setExportFile(src.getExportFile());
        // appQueryApp / appQueryPackage not present on InstructionLoad → not set here.

        // --- ElementDTO (first item in array) ---
        ElementDTO el = ensureFirstElement(splitDTO);

        // Choose which ID to mirror on the element:
        // If your ElementDTO.id represents the variable link, prefer variableId; otherwise use src.getId()
        if (src.getVariableId() != null) el.setId(src.getVariableId());
        else if (src.getId() != null) el.setId(src.getId());

        if (hasText(src.getType())) el.setTypeElement(src.getType());
        if (hasText(src.getTagName())) el.setTagName(src.getTagName());
        if (hasText(src.getXpath())) el.setXPath(src.getXpath()); // Lombok: field `xPath` -> setter `setXPath`
        if (hasText(src.getCoordinates())) el.setCoordinates(src.getCoordinates());
        if (hasText(src.getIFrameXPath())) el.setIFrameXPath(src.getIFrameXPath());
        if (hasText(src.getShadowHost())) el.setShadowHost(src.getShadowHost());
        if (hasText(src.getShadowRoot())) el.setShadowRoot(src.getShadowRoot());
        if (hasText(src.getCssSelector())) el.setCssSelector(src.getCssSelector());

        // Auto Scroll & Auto Enter
        if (hasText(src.getActions())) {

            String[] actions = src.getActions().split(":");

            for (String action : actions) {
                switch (action.trim()) {
                    case "A":
                        el.setAutoScroll("A");
                        break;
                    case "E":
                        el.setAutoEnter("E");
                        break;
                }
            }
        }

        // Fields without a clear mapping from InstructionLoad are left untouched:
        // someText, attribId, attribName, attributeData, customXPath, nestedShadow,
        // attributeValue, attributeType, searchAttributeValue.
    }

    // ---------- helpers ----------

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static ElementDTO ensureFirstElement(SplitDTO splitDTO) {
        ElementDTO[] arr = splitDTO.getElementDetails();
        if (arr == null || arr.length == 0) {
            ElementDTO e = new ElementDTO();
            splitDTO.setElementDetails(new ElementDTO[] {e});
            return e;
        }
        return arr[0];
    }
}
