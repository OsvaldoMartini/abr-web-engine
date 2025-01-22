package com.allinweb.ch.facade;

import com.allinweb.ch.component.model.BlockLoopInstructionLoadDTO;
import com.allinweb.ch.component.model.InstructionDTO;
import com.allinweb.ch.util.ABRConstants;
import com.allinweb.ch.util.ABRPropertyEnum;
import com.allinweb.ch.util.ABRPropertyManager;
import com.google.common.base.Strings;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javax.swing.*;

/**
 * PerformMessage.
 *
 * @author Osvaldo Martini
 * @version 1.0
 */
public class PerformMessage {
    // Static final variable to hold the singleton instance
    protected static final SingletonSupplier<PerformMessage> instance = () -> new PerformMessage();

    // Private constructor to prevent instantiation
    private PerformMessage() {
        // Initialize if necessary
    }

    // Public method to access the singleton instance
    public static PerformMessage getInstance() {
        return instance.get();
    }

    public void initializePerformMessages() {}

    public void couldNotFindElement(String criteria) {
        showCustomModalDialog(
                criteria,
                "1. Verify if you are on the correct web page.",
                "2. Check if the page layout or content has been updated. (Page Refreshed)",
                "3. Consider increasing the wait time to ensure the page loads completely.",
                "4. Consider to Re Scanner or Re Select the Element!",
                true,
                null,
                0);
    }

    public void errorMessage(String criteria, String msg1, String msg2, String msg3, String msg4, int height) {
        showCustomModalDialog(criteria, msg1, msg2, msg3, msg4, true, null, height);
    }

    public static void showCustomDialog(String title, String message) {
        // Create a JDialog as a custom message dialog
        JDialog dialog = new JDialog();
        dialog.setTitle(title);
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(null); // Center on screen
        dialog.setUndecorated(true); // Remove the default border

        // Style the dialog's main panel
        JPanel panel = new JPanel();
        panel.setBackground(new Color(255, 218, 51)); // Light orange background
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setLayout(new BorderLayout());

        // Style the message
        JLabel messageLabel =
                new JLabel("<html><span style='color: blue;'>" + message + "</span></html>", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(messageLabel, BorderLayout.CENTER);

        // OK button to close the dialog
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dialog.dispose());
        panel.add(okButton, BorderLayout.SOUTH);

        // Add panel to dialog and set properties
        dialog.getContentPane().add(panel);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
    }

    public static void showCustomModalDialog(String title, String message, String message2) {
        // Create a JDialog as a custom modal message dialog
        JDialog dialog = new JDialog((Frame) null, title, true); // true makes it modal
        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(null); // Center on screen
        dialog.setUndecorated(true); // Remove the default border

        // Style the dialog's main panel
        JPanel panel = new JPanel();
        panel.setBackground(new Color(255, 218, 51)); // Light orange background
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setLayout(new BorderLayout());

        // Style the message
        JLabel messageLabel = new JLabel(
                "<html><br><span style='color: blue;'>" + message
                        + "</span><<br>------------------------------<br><span style='color: blue;'>" + message2
                        + "</span></html>",
                SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(messageLabel, BorderLayout.CENTER);

        // OK button to close the dialog
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dialog.dispose());
        panel.add(okButton, BorderLayout.SOUTH);

        // Add panel to dialog and set properties
        dialog.getContentPane().add(panel);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true); // This will block other input until the dialog is closed
    }

    public static ABRConstants.DialogModal showCustomModalDialog(
            String title,
            String message,
            String message2,
            String message3,
            String message4,
            boolean redMsg,
            String secondButton,
            int height) {
        // Create a JDialog as a custom modal message dialog
        JDialog dialog = new JDialog((Frame) null, title, true); // true makes it modal
        if (height > 0) {
            dialog.setSize(350, height);
        } else if (message2 != null && message3 == null && message4 == null) {
            dialog.setSize(380, 210);
        } else if (message2 != null && message3 != null && message4 == null) {
            dialog.setSize(380, 250);
        } else if (message2 != null && message3 != null && message4 != null) {
            dialog.setSize(380, 260);
        } else {
            dialog.setSize(380, 150);
        }

        dialog.setLocationRelativeTo(null); // Center on screen
        dialog.setUndecorated(true); // Remove the default border

        // Style the dialog's main panel
        JPanel panel = new JPanel();
        panel.setBackground(new Color(255, 218, 51)); // Light orange background
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setLayout(new BorderLayout());

        // Build the message
        String titleMessage = "<html><br><span style='color: blue;'>";
        titleMessage += "<span style='font-size: 14px; font-weight: bold;'>" + title
                + "</span><br>------------------------------<br>";

        String concatenateMsg = "<span style='color: blue;'>" + message;
        if (message2 != null) {
            concatenateMsg +=
                    "</span><br>------------------------------<br><span style='color: blue;'>" + message2 + "</span>";
        } else {
            concatenateMsg += "</span><br>------------------------------<br><br>                            <br>";
        }

        if (message3 != null && message4 == null) {
            concatenateMsg +=
                    "<br>------------------------------<br><span style='color: blue;'>" + message3 + "</span></html>";
        } else if (message3 != null && message4 != null) {
            concatenateMsg += "<br>------------------------------<br><span style='color: blue;'>"
                    + message3 + "</span><br>------------------------------<br><span style='color: blue;'>"
                    + message4 + "</span><br><br></html>";
        } else {
            concatenateMsg += "</html>";
        }

        // Apply red color to message if redMsg is true
        if (redMsg) {
            concatenateMsg = concatenateMsg.replaceAll("blue", "red");
        }
        concatenateMsg = titleMessage + concatenateMsg;

        // Create a JLabel to display the formatted message
        JLabel messageLabel = new JLabel(concatenateMsg, SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(messageLabel, BorderLayout.CENTER);

        final ABRConstants.DialogModal[] status = {ABRConstants.DialogModal.NONE};

        if (!Strings.isNullOrEmpty(secondButton)) {

            // Create a JPanel for the buttons with horizontal layout
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0)); // Reduced horizontal gap to 5
            buttonPanel.setBackground(new Color(255, 218, 51)); // Light orange background
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Reduced padding to 10

            Dimension buttonSize = new Dimension(150, 20); // Set button width to 120 and height to 20

            // OK button with custom gradient background
            JButton okButton = new JButton("OK") {
                @Override
                protected void paintComponent(Graphics g) {
                    if (isOpaque()) {
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        GradientPaint gradient =
                                new GradientPaint(0, 0, Color.LIGHT_GRAY, getWidth(), getHeight(), Color.WHITE);
                        g2.setPaint(gradient);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                    super.paintComponent(g);
                }
            };
            okButton.setPreferredSize(buttonSize);
            okButton.setFocusPainted(false);
            buttonPanel.add(okButton);

            // Stop button with custom gradient background
            JButton stopButton = new JButton(secondButton) {
                @Override
                protected void paintComponent(Graphics g) {
                    if (isOpaque()) {
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        GradientPaint gradient =
                                new GradientPaint(0, 0, Color.LIGHT_GRAY, getWidth(), getHeight(), Color.WHITE);
                        g2.setPaint(gradient);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                    super.paintComponent(g);
                }
            };
            stopButton.setPreferredSize(buttonSize);
            stopButton.setFocusPainted(false);
            buttonPanel.add(stopButton);

            // OK button action listener
            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                    status[0] = ABRConstants.DialogModal.OK;
                }
            });

            // Stop button action listener
            stopButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.out.println("Stop button clicked!");
                    dialog.dispose();
                    status[0] = ABRConstants.DialogModal.STOP;
                }
            });

            panel.add(buttonPanel, BorderLayout.SOUTH);
        } else {

            Dimension buttonSize = new Dimension(150, 20); // Set button width to 120 and height to 20

            // OK button with custom gradient background
            JButton okButton = new JButton("OK") {
                @Override
                protected void paintComponent(Graphics g) {
                    if (isOpaque()) {
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        GradientPaint gradient =
                                new GradientPaint(0, 0, Color.LIGHT_GRAY, getWidth(), getHeight(), Color.WHITE);
                        g2.setPaint(gradient);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                    super.paintComponent(g);
                }
            };
            okButton.setPreferredSize(buttonSize);
            okButton.setFocusPainted(false);

            // OK button action listener
            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                    status[0] = ABRConstants.DialogModal.OK;
                }
            });

            panel.add(okButton, BorderLayout.SOUTH);
        }

        // Add panel to dialog and set properties
        dialog.getContentPane().add(panel);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true); // This will block other input until the dialog is closed

        return status[0];
    }

    public String renderInstructionActions(InstructionDTO instruction) {
        // List of valid actions
        List<String> validActions = Arrays.asList("SET", "GET", "CK", "E");

        // Handle the "CK" action with special formatting for operation
        if ("CK".equals(instruction.getActions()) && instruction.getOperation() != null) {
            String[] parts = instruction.getOperation().split(":");
            if (parts.length == 3) {
                String left = parts[0].trim();
                String middle = parts[1].trim();
                String right = parts[2].trim();

                // Handle special case where middle is "="
                if ("=".equals(middle)) {
                    return String.format("(%d)%s %s %s", instruction.getParentId(), left, middle, right);
                }
            }
        }

        // Handle operations for other actions (SET, GET)
        if (instruction.getOperation() != null && validActions.contains(instruction.getActions())) {
            String[] parts = instruction.getOperation().split(":");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim();
                return String.format("(%d)%s: %s", instruction.getParentId(), left, right);
            }
        }

        // Handle if the action is valid but has no operation
        if (validActions.contains(instruction.getActions())) {
            return instruction.getActions();
        }

        // Return empty string for no actions
        return "";
    }

    public boolean showAlertCombinedVBOX(
            Alert.AlertType alertType, String title, String header, String content, VBox combinedTextContainer) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setContent(combinedTextContainer);

        if (alertType.equals(Alert.AlertType.CONFIRMATION)) {
            alert.getButtonTypes().set(0, ButtonType.YES);
            alert.getButtonTypes().set(1, ButtonType.NO);
        }
        Optional<ButtonType> result = alert.showAndWait();

        if (alertType.equals(Alert.AlertType.CONFIRMATION)) {
            return result.isPresent() && result.get().equals(ButtonType.YES);
        } else {
            return result.isPresent() && result.get().equals(ButtonType.OK);
        }
    }

    public boolean showCombinedHBox(
            Alert.AlertType alertType, String title, String header, String content, HBox combinedTextContainer) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setContent(combinedTextContainer);

        if (alertType.equals(Alert.AlertType.CONFIRMATION)) {
            alert.getButtonTypes().set(0, ButtonType.YES);
            alert.getButtonTypes().set(1, ButtonType.NO);
        }
        Optional<ButtonType> result = alert.showAndWait();

        if (alertType.equals(Alert.AlertType.CONFIRMATION)) {
            return result.isPresent() && result.get().equals(ButtonType.YES);
        } else {
            return result.isPresent() && result.get().equals(ButtonType.OK);
        }
    }

    public void outputJson(List<BlockLoopInstructionLoadDTO> blockLoopInstructions) {
        // Get the directory path from ABRPropertyManager
        String jsonPath = ABRPropertyManager.getInstance().getProperty(ABRPropertyEnum.FOLDER_PATH_DB);

        List<BlockLoopInstructionLoadDTO> updatedList = new ArrayList<>(); // Create a new list for updated instructions

        for (BlockLoopInstructionLoadDTO instruction : blockLoopInstructions) {
            // Create a new BlockLoopInstructionLoadDTO object to avoid modifying the original
            BlockLoopInstructionLoadDTO updatedInstruction = new BlockLoopInstructionLoadDTO();

            // Copy original fields and add 1000 where necessary
            updatedInstruction.setId(instruction.getId() + 1000);
            updatedInstruction.setBotJobId(instruction.getBotJobId() + 1000);
            updatedInstruction.setBlockId(instruction.getBlockId() + 1000);
            updatedInstruction.setBlockOrderNumber(
                    instruction.getBlockOrderNumber()); // Copy without change (if needed)

            // Add 1000 to parentId if it's greater than 0
            if (instruction.getParentId() > 0) {
                updatedInstruction.setParentId(instruction.getParentId() + 1000);
            } else {
                updatedInstruction.setParentId(instruction.getParentId()); // Keep original if not greater than 0
            }

            // Copy other fields as is (no change)
            updatedInstruction.setBotJobName(instruction.getBotJobName());
            updatedInstruction.setInstructionOrderNumber(instruction.getInstructionOrderNumber());
            updatedInstruction.setActions(instruction.getActions());
            updatedInstruction.setName(instruction.getName());
            updatedInstruction.setPath(instruction.getPath());
            updatedInstruction.setDescription(instruction.getDescription());
            updatedInstruction.setOptional(instruction.getOptional());
            updatedInstruction.setBlockMarked(instruction.getBlockMarked());
            updatedInstruction.setDefaultValue(instruction.getDefaultValue());
            updatedInstruction.setActionCustomMaxWaitSec(instruction.getActionCustomMaxWaitSec());
            updatedInstruction.setOnHoldSeconds(instruction.getOnHoldSeconds());
            updatedInstruction.setCodified(instruction.getCodified());
            updatedInstruction.setExportToABR(instruction.getExportToABR());
            updatedInstruction.setExportToABR(instruction.getExportToABR());
            updatedInstruction.setExecuted(instruction.getExecuted());
            updatedInstruction.setPriority(instruction.getPriority());
            updatedInstruction.setOperation(instruction.getOperation());
            updatedInstruction.setExportFile(instruction.getExportFile());
            updatedInstruction.setBlockName(instruction.getBlockName());
            updatedInstruction.setBlockActive(instruction.getInstructionActive());
            updatedInstruction.setBlockWait(instruction.getBlockWait());
            updatedInstruction.setEditMode(instruction.getEditMode());
            updatedInstruction.setRefreshLoop(instruction.getRefreshLoop());
            updatedInstruction.setLoopOnly(instruction.getLoopOnly());
            updatedInstruction.setInstructionActive(instruction.getInstructionActive());

            // Add the updated instruction to the new list
            updatedList.add(updatedInstruction);
        }

        // Define Gson ExclusionStrategy to ignore specific fields
        ExclusionStrategy strategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                // Skip fields by name (e.g., 'botJobId', 'botJobName')
                return f.getName().equals("optional")
                        || f.getName().equals("blockMarked")
                        || f.getName().equals("editMode");
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        };

        // Initialize Gson with pretty printing for better readability
        Gson gson = new GsonBuilder()
                .setExclusionStrategies(strategy)
                .setPrettyPrinting()
                .create();

        // Serialize the list of BlockLoopInstructionLoadDTO to JSON
        String jsonData = gson.toJson(updatedList);

        // Create the file path
        String outputFilePath = jsonPath + "/blockLoopInstructions.json";

        // Write the JSON data to the file
        try (FileWriter writer = new FileWriter(outputFilePath)) {
            writer.write(jsonData);
            System.out.println("JSON file saved to: " + outputFilePath);
        } catch (IOException e) {
            System.err.println("Error writing JSON to file: " + e.getMessage());
        }
    }
}
