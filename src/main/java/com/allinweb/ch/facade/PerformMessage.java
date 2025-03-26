package com.allinweb.ch.facade;

import com.allinweb.ch.component.model.ElementDTO;
import com.allinweb.ch.component.model.InstructionLoadDTO;
import com.allinweb.ch.util.ARConstants;
import com.allinweb.ch.util.ARPropertyEnum;
import com.allinweb.ch.util.ARPropertyManager;
import com.google.common.base.Strings;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
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
import javafx.scene.text.Text;
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
        showCustomModalDialogDragWin11(
                criteria,
                "1. Verify if you are on the correct web page.",
                "2. Check if the page layout or content has been updated. (Page Refreshed)",
                "3. Consider increasing the wait time to ensure the page loads completely.",
                "4. Consider to Re Scanner or Re Select the Element!",
                true,
                "OK",
                null,
                0);
    }

    public void couldNotInputBotJobVeryFast(String criteria) {
        showCustomModalDialogDragWin11(
                criteria,
                "If fields are written to previous fields, it means they depend on parent data.",
                "Data loading delays may require waiting time.",
                "Our AI report analysis can precisely determine the necessary wait times.",
                "Schedule a consultation with our Commercial Advisor to get your free AI report",
                true,
                "OK",
                null,
                0);
    }

    public void multipleActionsElement(String criteria) {
        showCustomModalDialogDragWin11(
                criteria,
                "Attention Required!",
                "This element may require multiple actions.",
                "It likely needs a click action first — then open the options to type in it.",
                "For testing, always consider using \"TEST ACTIONS\" first to verify the element.",
                true,
                "OK",
                null,
                0);
    }

    public void errorMessage(String criteria, String msg1, String msg2, String msg3, String msg4, int height) {
        showCustomModalDialogDragWin11(criteria, msg1, msg2, msg3, msg4, true, "OK", null, height);
    }

    public void showCustomDialog(String title, String message) {
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

    public void showCustomModalDialog(String title, String message, String message2) {
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

    public void showCustomModalDialogDrag(String title, String message, String message2) {
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
                        + "</span><br>------------------------------<br><span style='color: blue;'>" + message2
                        + "</span></html>",
                SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(messageLabel, BorderLayout.CENTER);

        // OK button to close the dialog
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> dialog.dispose());
        panel.add(okButton, BorderLayout.SOUTH);

        // Add drag support
        addDragSupport(dialog, panel);

        // Add panel to dialog and set properties
        dialog.getContentPane().add(panel);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true); // This will block other input until the dialog is closed
    }

    public ARConstants.DialogModal showCustomModalDialog(
            String title,
            String message,
            String message2,
            String message3,
            String message4,
            boolean redMsg,
            String firstButton,
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
            dialog.setSize(380, 280);
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

        final ARConstants.DialogModal[] status = {ARConstants.DialogModal.NONE};

        if (!Strings.isNullOrEmpty(secondButton)) {

            // Create a JPanel for the buttons with horizontal layout
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0)); // Reduced horizontal gap to 5
            buttonPanel.setBackground(new Color(255, 218, 51)); // Light orange background
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Reduced padding to 10

            Dimension buttonSize = new Dimension(150, 20); // Set button width to 120 and height to 20

            // OK button with custom gradient background

            JButton okButton = new JButton(firstButton) {
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
                    status[0] = ARConstants.DialogModal.OK;
                }
            });

            // Stop button action listener
            stopButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.out.println("Stop button clicked!");
                    dialog.dispose();
                    status[0] = ARConstants.DialogModal.STOP;
                }
            });

            panel.add(buttonPanel, BorderLayout.SOUTH);
        } else {

            Dimension buttonSize = new Dimension(150, 20); // Set button width to 120 and height to 20

            // OK button with custom gradient background
            JButton okButton = new JButton(firstButton) {
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
                    status[0] = ARConstants.DialogModal.OK;
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

    public ARConstants.DialogModal showCustomModalDialogDrag(
            String title,
            String message,
            String message2,
            String message3,
            String message4,
            boolean redMsg,
            String firstButton,
            String secondButton,
            int height) {

        // Create a JDialog as a custom modal message dialog
        JDialog dialog = new JDialog((Frame) null, title, true); // Modal dialog
        dialog.setUndecorated(true); // Remove the default border

        // Set dialog size dynamically
        if (height > 0) {
            dialog.setSize(380, height);
        } else if (message2 != null && message3 == null && message4 == null) {
            dialog.setSize(380, 240);
        } else if (message2 != null && message3 != null && message4 == null) {
            dialog.setSize(380, 280);
        } else if (message2 != null && message3 != null && message4 != null) {
            dialog.setSize(380, 320);
        } else {
            dialog.setSize(380, 200);
        }

        dialog.setLocationRelativeTo(null); // Center on screen

        // Main panel
        JPanel panel = new JPanel();
        panel.setBackground(new Color(255, 218, 51)); // Light orange background
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setLayout(new BorderLayout());

        // Build the message
        String titleMessage = "<html><br><span style='color: blue;'>"
                + "<span style='font-size: 14px; font-weight: bold;'>" + title
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

        // Apply red color if redMsg is true
        if (redMsg) {
            concatenateMsg = concatenateMsg.replaceAll("blue", "red");
        }
        concatenateMsg = titleMessage + concatenateMsg;

        // Create a JLabel to display the formatted message
        JLabel messageLabel = new JLabel(concatenateMsg, SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(messageLabel, BorderLayout.CENTER);

        final ARConstants.DialogModal[] status = {ARConstants.DialogModal.NONE};

        // Create button panel if second button exists
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        buttonPanel.setBackground(new Color(255, 218, 51));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Dimension buttonSize = new Dimension(150, 20);

        // OK button
        JButton okButton = createStyledButton(firstButton);
        okButton.setPreferredSize(buttonSize);
        okButton.addActionListener(e -> {
            dialog.dispose();
            status[0] = ARConstants.DialogModal.OK;
        });
        buttonPanel.add(okButton);

        // Stop button if provided
        if (!Strings.isNullOrEmpty(secondButton)) {
            JButton stopButton = createStyledButton(secondButton);
            stopButton.setPreferredSize(buttonSize);
            stopButton.addActionListener(e -> {
                System.out.println("Stop button clicked!");
                dialog.dispose();
                status[0] = ARConstants.DialogModal.STOP;
            });
            buttonPanel.add(stopButton);
        }

        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add drag support
        addDragSupport(dialog, panel);

        // Add panel to dialog
        dialog.getContentPane().add(panel);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true); // This blocks other input until the dialog is closed

        return status[0];
    }

    /**
     * Creates a styled button with Windows 11 theme
     */
    public ARConstants.DialogModal showCustomModalDialogDragWin11(
            String title,
            String message,
            String message2,
            String message3,
            String message4,
            boolean redMsg,
            String firstButton,
            String secondButton,
            int height) {

        // Create a JDialog as a custom modal message dialog
        JDialog dialog = new JDialog((Frame) null, title, true); // Modal dialog
        dialog.setUndecorated(true); // Remove the default border

        // Set dialog size dynamically
        if (height > 0) {
            dialog.setSize(380, height);
        } else if (message2 != null && message3 == null && message4 == null) {
            dialog.setSize(380, 270);
        } else if (message2 != null && message3 != null && message4 == null) {
            dialog.setSize(380, 310);
        } else if (message2 != null && message3 != null && message4 != null) {
            dialog.setSize(380, 380);
        } else {
            dialog.setSize(380, 210);
        }

        dialog.setLocationRelativeTo(null); // Center on screen

        // Main panel
        JPanel panel = new JPanel();
        panel.setBackground(new Color(243, 243, 243)); // Windows 11 Light Gray
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)), // Border color
                BorderFactory.createEmptyBorder(20, 20, 20, 20))); // Padding
        panel.setLayout(new BorderLayout());

        // Build the message
        String titleMessage = "<html><br><span style='color: blue;'>"
                + "<span  style='font-size: 14px; font-weight: bold;'>" + title
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

        // Apply red color if redMsg is true
        if (redMsg) {
            concatenateMsg = concatenateMsg.replaceAll("blue", "#D32F2F");
        }
        concatenateMsg = titleMessage + concatenateMsg;

        // Create a JLabel to display the formatted message
        JLabel messageLabel = new JLabel(concatenateMsg, SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(messageLabel, BorderLayout.CENTER);

        final ARConstants.DialogModal[] status = {ARConstants.DialogModal.NONE};

        // Create button panel if second button exists
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        buttonPanel.setBackground(new Color(243, 243, 243)); // Windows 11 Light Gray
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Dimension buttonSize = new Dimension(150, 20);

        // OK button
        JButton okButton = createStyledButtonWin11(firstButton);
        okButton.setPreferredSize(buttonSize);
        okButton.addActionListener(e -> {
            dialog.dispose();
            status[0] = ARConstants.DialogModal.OK;
        });
        buttonPanel.add(okButton);

        // Stop button if provided
        if (!Strings.isNullOrEmpty(secondButton)) {
            JButton stopButton = createStyledButtonWin11(secondButton);
            stopButton.setPreferredSize(buttonSize);
            stopButton.addActionListener(e -> {
                System.out.println("Stop button clicked!");
                dialog.dispose();
                status[0] = ARConstants.DialogModal.STOP;
            });
            buttonPanel.add(stopButton);
        }

        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Add drag support
        addDragSupport(dialog, panel);

        // Add panel to dialog
        dialog.getContentPane().add(panel);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true); // This blocks other input until the dialog is closed

        return status[0];
    }

    // Helper method to create styled buttons
    private static JButton createStyledButton(String text) {
        return new JButton(text) {
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
    }

    /**
     * Creates a styled button with Windows 11 theme
     */
    private static JButton createStyledButtonWin11(String text) {
        JButton button = new JButton(text);

        // Windows 11 Theme Styling
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(0, 120, 212)); // Windows 11 blue
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12)); // Adjust padding
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Ensure UI updates properly
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.putClientProperty("JComponent.outline", null); // Prevents UI interference

        // Hover Effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0, 102, 180)); // Darker blue on hover
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0, 120, 212)); // Reset color
            }
        });

        // Ensure color is reset each time it's used
        button.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && button.isShowing()) {
                button.setBackground(new Color(0, 120, 212)); // Restore original color
            }
        });

        // Force UI update
        button.revalidate();
        button.repaint();

        return button;
    }

    // Method to add drag-and-drop support
    private static void addDragSupport(JDialog dialog, JPanel panel) {
        final Point mouseDownCompCoords = new Point();

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseDownCompCoords.setLocation(e.getPoint());
            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currCoords = e.getLocationOnScreen();
                dialog.setLocation(currCoords.x - mouseDownCompCoords.x, currCoords.y - mouseDownCompCoords.y);
            }
        });
    }

    public String renderInstructionActions(InstructionLoadDTO instruction) {
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

    public void outputJson(List<InstructionLoadDTO> blockLoopInstructions, String fileName) {
        // Get the directory path from ARPropertyManager
        String jsonPath = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.FOLDER_PATH_DB);

        List<InstructionLoadDTO> updatedList = new ArrayList<>(); // Create a new list for updated instructions

        for (InstructionLoadDTO instruction : blockLoopInstructions) {
            // Create a new InstructionLoadDTO object to avoid modifying the original
            InstructionLoadDTO updatedInstruction = new InstructionLoadDTO();

            // Copy original fields and add 1000 where necessary
            updatedInstruction.setHomeBankingId(instruction.getHomeBankingId() + 1000);
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
            updatedInstruction.setXpath(instruction.getXpath());
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

        // Serialize the list of InstructionLoadDTO to JSON
        String jsonData = gson.toJson(updatedList);

        // Create the file path
        String outputFilePath = jsonPath + "/" + fileName + ".json";

        // Write the JSON data to the file
        try (FileWriter writer = new FileWriter(outputFilePath)) {
            writer.write(jsonData);
            System.out.println("JSON file saved to: " + outputFilePath);
        } catch (IOException e) {
            System.err.println("Error writing JSON to file: " + e.getMessage());
        }
    }

    public void outputJsonElementDTO(ElementDTO[] elementDTO) {
        // Get the directory path from ARPropertyManager
        String jsonPath = ARPropertyManager.getInstance().getProperty(ARPropertyEnum.FOLDER_PATH_DB);

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

        // Serialize the list of elementDTO to JSON
        String jsonData = gson.toJson(elementDTO);

        // Create the file path
        String outputFilePath = jsonPath + "/elementDTO.json";

        // Write the JSON data to the file
        try (FileWriter writer = new FileWriter(outputFilePath)) {
            writer.write(jsonData);
            System.out.println("JSON file saved to: " + outputFilePath);
        } catch (IOException e) {
            System.err.println("Error writing JSON to file: " + e.getMessage());
        }
    }

    public void generalErrorIFrame(String xpath) {
        // Styled text elements
        Text titleText = new Text("Fail Searching IFrame Elements");
        titleText.setStyle("-fx-font-size: 18px; -fx-fill: blue;");

        Text errorText = new Text("Error: Attempt identify IFrame elements");
        errorText.setStyle("-fx-font-size: 18px; -fx-fill: red;");

        Text xpathText = new Text(xpath);
        xpathText.setStyle("-fx-font-size: 18px; -fx-fill: red;");

        // Create a container for the message
        VBox messageContainer = new VBox(5); // Adds spacing of 5px

        // Add relevant elements to the container
        messageContainer.getChildren().addAll(titleText, errorText);

        if (!Strings.isNullOrEmpty(xpath)) {
            messageContainer.getChildren().add(xpathText);
        }

        // Display the alert message
        showAlertCombinedVBOX(
                Alert.AlertType.WARNING,
                "iFrame Web Elements",
                "Action: Search iFrame Elements!",
                null,
                messageContainer);
    }
}
