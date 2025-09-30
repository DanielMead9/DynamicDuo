package DynamicDuo.codeeditor.src.main.java.com.dynamicduo;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.HashMap;

public class GUI extends JFrame {

    private JTextArea editorArea;      // editable bottom section
    private JTextArea headingArea;     // read-only top section
    private String currentMode = "message"; // start on Message tab
    private final HashMap<String, String> modeBuffers = new HashMap<>();

    // Mode buttons (need references for highlighting)
    private JButton messageBtn, svgBtn, javaBtn, analysisBtn;
    private JButton uploadBtn, executeBtn; // reference for enabling/disabling

    public GUI() {
        setTitle("Mini Code Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // --- Top toolbar with modes ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        messageBtn = new JButton("Message");
        svgBtn = new JButton("SVG");
        javaBtn = new JButton("Java Code");
        analysisBtn = new JButton("Analysis");

        topPanel.add(messageBtn);
        topPanel.add(svgBtn);
        topPanel.add(javaBtn);
        topPanel.add(analysisBtn);

        add(topPanel, BorderLayout.NORTH);

        // --- Split editor (heading + main editor) ---
        headingArea = new JTextArea(3, 60);
        headingArea.setFont(new Font("Consolas", Font.BOLD, 14));
        headingArea.setEditable(false);
        headingArea.setBackground(new Color(230, 230, 230));

        editorArea = new JTextArea();
        editorArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        editorArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JScrollPane headingScroll = new JScrollPane(headingArea);
        JScrollPane editorScroll = new JScrollPane(editorArea);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, headingScroll, editorScroll);
        splitPane.setDividerLocation(60);    // initial height for heading
        splitPane.setResizeWeight(0.1);      // heading ~10%, editor ~90%
        add(splitPane, BorderLayout.CENTER);

        // --- Right panel with buttons ---
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        executeBtn = new JButton("Execute");
        JButton saveBtn = new JButton("Save");
        uploadBtn = new JButton("Upload");

        // Center buttons vertically
        buttonPanel.add(Box.createVerticalGlue());
        buttonPanel.add(executeBtn);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(saveBtn);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(uploadBtn);
        buttonPanel.add(Box.createVerticalGlue());

        add(buttonPanel, BorderLayout.EAST);

        // --- Mode switching logic ---
        messageBtn.addActionListener(e -> switchMode("message"));
        svgBtn.addActionListener(e -> switchMode("svg"));
        javaBtn.addActionListener(e -> switchMode("java"));
        analysisBtn.addActionListener(e -> switchMode("analysis"));

        // --- Save button action (with extension suggestion + auto-append) ---
        saveBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();

            // Suggest extension based on mode
            String ext = switch (currentMode) {
                case "java" -> ".java";
                case "svg" -> ".svg";
                case "analysis" -> ".txt";
                default -> ".txt";
            };

            fileChooser.setSelectedFile(new File("untitled" + ext));
            int option = fileChooser.showSaveDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();

                // Auto-append extension if missing
                if (!file.getName().toLowerCase().endsWith(ext)) {
                    file = new File(file.getAbsolutePath() + ext);
                }

                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(editorArea.getText());
                    JOptionPane.showMessageDialog(this, "File saved: " + file.getAbsolutePath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage());
                }
            }
        });

        // --- Upload button action (with extension check) ---
        uploadBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showOpenDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();

                // Expected extension based on mode
                String expectedExt = switch (currentMode) {
                    case "java" -> ".java";
                    case "svg" -> ".svg";
                    case "analysis" -> ".txt";
                    default -> ".txt";
                };

                // Warn if extension mismatches
                if (!file.getName().toLowerCase().endsWith(expectedExt)) {
                    int choice = JOptionPane.showConfirmDialog(
                            this,
                            "This file does not appear to be a " + expectedExt + " file.\nDo you still want to load it?",
                            "File Extension Warning",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (choice != JOptionPane.YES_OPTION) {
                        return; // cancel loading
                    }
                }

                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    editorArea.setText(content.toString());
                    modeBuffers.put(currentMode, content.toString());
                    JOptionPane.showMessageDialog(this, "File loaded: " + file.getAbsolutePath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error loading file: " + ex.getMessage());
                }
            }
        });

        // --- Default mode setup: start on Message ---
        switchMode("message");
    }

    // Highlight the active mode button
    private void highlightActiveMode(JButton active) {
        JButton[] allButtons = { messageBtn, svgBtn, javaBtn, analysisBtn };
        for (JButton b : allButtons) {
            if (b == active) {
                b.setBackground(Color.LIGHT_GRAY);
            } else {
                b.setBackground(null); // reset default
            }
        }
    }

    // Switch editor between modes and remember content
    private void switchMode(String newMode) {
        // Save current bottom section content into buffer
        modeBuffers.put(currentMode, editorArea.getText());

        // Change mode
        currentMode = newMode;

        // Restore buffer if exists, else start empty
        String content = modeBuffers.getOrDefault(newMode, "");
        editorArea.setText(content);

        // Set heading text
        switch (newMode) {
            case "svg" -> {
                headingArea.setText("SVG Editor Mode\n(Write or paste SVG markup below)");
                highlightActiveMode(svgBtn);
                editorArea.setEditable(false);
                uploadBtn.setEnabled(false);
                executeBtn.setEnabled(false);
            }
            case "java" -> {
                headingArea.setText("Java Code Editor Mode\n(Type Java code below)");
                highlightActiveMode(javaBtn);
                editorArea.setEditable(false);
                uploadBtn.setEnabled(false);
                executeBtn.setEnabled(false);
            }
            case "analysis" -> {
                headingArea.setText("Analysis Mode\n(This is what parts of the message have been leaked)");
                editorArea.setEditable(false);
                highlightActiveMode(analysisBtn);
                uploadBtn.setEnabled(false);
                executeBtn.setEnabled(false);
            }
            case "message" -> {
                headingArea.setText("Message Mode\n(Write message in the folowing format");
                highlightActiveMode(messageBtn);
                editorArea.setEditable(true);
                uploadBtn.setEnabled(true);
                executeBtn.setEnabled(true);
            }
        }
    }


}
