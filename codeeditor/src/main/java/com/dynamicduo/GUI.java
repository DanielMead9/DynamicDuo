package com.dynamicduo;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.HashMap;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class GUI extends JFrame {

    private JTextArea editorArea, headingArea, errorArea;
    private JScrollPane headingScroll, editorScroll, errorScroll;

    private RSyntaxTextArea codeArea;
    private RTextScrollPane codeScroll;

    private String currentMode = "message"; // start on Message tab
    private final HashMap<String, String> modeBuffers = new HashMap<>();
    JSplitPane splitPane, splitPane2, splitPane3;

    // Mode buttons (need references for highlighting)
    private JButton messageBtn, svgBtn, javaBtn, analysisBtn;
    private JButton uploadBtn, runBtn, saveBtn, displayBtn;

    public GUI() {
        setTitle("Security Message App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // creates top panel to place all buttons on
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(1, 2));

        // Creates Nav panel to allow for change between modes
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        messageBtn = new JButton("Message");
        svgBtn = new JButton("SVG");
        javaBtn = new JButton("Java Code");
        analysisBtn = new JButton("Analysis");

        navPanel.add(messageBtn);
        navPanel.add(svgBtn);
        navPanel.add(javaBtn);
        navPanel.add(analysisBtn);

        topPanel.add(navPanel);

        // Creates Button panel to place function buttons on
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        runBtn = new JButton("Run");
        saveBtn = new JButton("Save");
        uploadBtn = new JButton("Upload");
        displayBtn = new JButton("Dark Mode");

        buttonPanel.add(runBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(uploadBtn);
        buttonPanel.add(displayBtn);

        topPanel.add(buttonPanel);

        add(topPanel, BorderLayout.NORTH);

        // Split editor (heading + main editor)
        headingArea = new JTextArea(3, 100);
        headingArea.setFont(new Font("Consolas", Font.BOLD, 14));
        headingArea.setEditable(false);
        headingArea.setBackground(new Color(230, 230, 230));

        editorArea = new JTextArea();
        editorArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        editorArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        headingScroll = new JScrollPane(headingArea);
        editorScroll = new JScrollPane(editorArea);

        // Code Screen
        codeArea = new RSyntaxTextArea(20, 60);
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeArea.setCodeFoldingEnabled(true);
        codeArea.setAntiAliasingEnabled(true);
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        codeArea.setBracketMatchingEnabled(true);
        codeArea.setAutoIndentEnabled(true);

        codeArea.setHighlightCurrentLine(true);
        codeArea.setBackground(Color.WHITE);
        codeArea.setForeground(Color.BLACK);
        codeArea.setCaretColor(Color.BLACK);
        codeArea.setCurrentLineHighlightColor(Color.LIGHT_GRAY);

        codeScroll = new RTextScrollPane(codeArea);
        codeScroll.getGutter().setLineNumberColor(Color.BLACK);
        codeScroll.getGutter().setBackground(Color.WHITE);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, headingScroll, codeScroll);
        splitPane.setDividerLocation(100); // initial height for heading
        splitPane.setResizeWeight(0.2); // heading ~10%, editor ~90%

        // Create Error handler area for Message mode
        errorArea = new JTextArea();
        errorArea.setRows(5);
        errorArea.setFont(new Font("Consolas", Font.BOLD, 14));
        errorArea.setEditable(false);
        errorArea.setBackground(new Color(230, 230, 230));
        errorArea.setText("Error Handler");

        errorScroll = new JScrollPane(errorArea);

        splitPane3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPane, errorScroll);
        splitPane3.setDividerLocation(450); // initial height for heading
        splitPane3.setResizeWeight(0.8);
        add(splitPane3, BorderLayout.CENTER);

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
                    if (currentMode.equals("java") || currentMode.equals("message"))
                        writer.write(codeArea.getText());
                    else
                        writer.write(editorArea.getText());
                    JOptionPane.showMessageDialog(this, "File saved: " + file.getAbsolutePath());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage());
                }
            }
        });

        // Upload button that allows for txt files
        uploadBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showOpenDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();

                // Warn if extension mismatches
                if (!file.getName().toLowerCase().endsWith(".txt") && !file.getName().toLowerCase().endsWith(".pdf")) {
                    int choice = JOptionPane.showConfirmDialog(
                            this,
                            "This file does not appear to be a .txt or .pdf.\nDo you still want to load it?",
                            "File Extension Warning",
                            JOptionPane.YES_NO_OPTION);
                    if (choice != JOptionPane.YES_OPTION) {
                        return; // cancel loading
                    }
                }

                if (file.getName().toLowerCase().endsWith(".pdf")) {
                    try (PDDocument document = PDDocument.load(file)) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        String text = stripper.getText(document);
                        codeArea.setText(text);
                        JOptionPane.showMessageDialog(this, "File loaded: " + file.getAbsolutePath());
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this, "Error loading file: " + ex.getMessage());
                    }
                } else {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        StringBuilder content = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                        codeArea.setText(content.toString());
                        modeBuffers.put(currentMode, content.toString());
                        JOptionPane.showMessageDialog(this, "File loaded: " + file.getAbsolutePath());
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this, "Error loading file: " + ex.getMessage());
                    }
                }

            }
        });

        displayBtn.addActionListener(e -> {
            // Toggle between light and dark mode
            if (codeArea.getBackground().equals(new Color(40, 44, 52))) {
                // Switch to light mode
                codeScroll.getGutter().setLineNumberColor(Color.BLACK);
                codeScroll.getGutter().setBackground(Color.WHITE);
                navPanel.setBackground(Color.WHITE);
                buttonPanel.setBackground(Color.WHITE);
                codeArea.setBackground(Color.WHITE);
                codeArea.setForeground(Color.BLACK);
                codeArea.setCaretColor(Color.BLACK);
                codeArea.setCurrentLineHighlightColor(Color.LIGHT_GRAY);
                headingArea.setBackground(new Color(230, 230, 230));
                headingArea.setForeground(Color.BLACK);
                editorArea.setBackground(Color.WHITE);
                editorArea.setForeground(Color.BLACK);
                errorArea.setBackground(new Color(230, 230, 230));
                errorArea.setForeground(Color.BLACK);
                displayBtn.setText("Dark Mode");

            } else {
                // Switch to dark mode
                codeScroll.getGutter().setLineNumberColor(Color.WHITE);
                codeScroll.getGutter().setBackground(new Color(40, 44, 52));
                navPanel.setBackground(Color.DARK_GRAY);
                buttonPanel.setBackground(Color.DARK_GRAY);
                codeArea.setBackground(new Color(40, 44, 52));
                codeArea.setForeground(Color.WHITE);
                codeArea.setCaretColor(Color.WHITE);
                codeArea.setCurrentLineHighlightColor(new Color(50, 56, 66));
                headingArea.setBackground(Color.DARK_GRAY);
                headingArea.setForeground(Color.WHITE);
                editorArea.setBackground(new Color(40, 44, 52));
                editorArea.setForeground(Color.WHITE);
                errorArea.setBackground(Color.DARK_GRAY);
                errorArea.setForeground(Color.WHITE);
                displayBtn.setText("Light Mode");
            }
            revalidate();
            repaint();
        });

        runBtn.addActionListener(e ->{
           JOptionPane.showMessageDialog(this, "Run Button pressed");

        });

        switchMode("message");
    }

    // Highlight the active mode button
    private void highlightActiveMode(JButton active) {
        JButton[] allButtons = { messageBtn, svgBtn, javaBtn, analysisBtn };
        for (JButton b : allButtons) {
            if (b == active) {
                b.setBackground(Color.GRAY);
            } else {
                b.setBackground(Color.WHITE); // reset default
            }
        }
    }

    // Switch editor between modes and remember content
    private void switchMode(String newMode) {
        // Save current bottom section content into buffer

        if (currentMode.equals("java") || currentMode.equals("message")) {
            modeBuffers.put(currentMode, codeArea.getText());
        } else
            modeBuffers.put(currentMode, editorArea.getText());

        // Change mode
        currentMode = newMode;

        // Restore buffer if exists, else start empty
        String content = modeBuffers.getOrDefault(newMode, "");
        if (newMode.equals("java") || newMode.equals("message"))
            codeArea.setText(content);
        else
            editorArea.setText(content);

        // Set heading text and activate buttons
        switch (newMode) {
            case "svg" -> {
                headingArea.setText("SVG Mode\n(This is the SVG for the Message Passing)");
                highlightActiveMode(svgBtn);
                editorArea.setEditable(false);
                uploadBtn.setEnabled(false);
                runBtn.setEnabled(false);
                splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, headingScroll, editorScroll);
                splitPane2.setDividerLocation(100); // initial height for heading
                splitPane2.setResizeWeight(0.2); // heading ~10%, editor ~90%
                setCenterComponent(splitPane2);

            }
            case "java" -> {
                headingArea.setText("Java Code \n(This is the starter java code)");
                highlightActiveMode(javaBtn);
                codeArea.setEditable(false);
                uploadBtn.setEnabled(false);
                runBtn.setEnabled(false);
                splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, headingScroll, codeScroll);
                splitPane.setDividerLocation(100); // initial height for heading
                splitPane.setResizeWeight(0.2); // heading ~10%, editor ~90%
                setCenterComponent(splitPane);

            }
            case "analysis" -> {
                headingArea.setText("Analysis Mode\n(This is what parts of the message have been leaked)");
                highlightActiveMode(analysisBtn);
                codeArea.setEditable(false);
                uploadBtn.setEnabled(false);
                runBtn.setEnabled(false);
                splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, headingScroll, editorScroll);
                splitPane2.setDividerLocation(100); // initial height for heading
                splitPane2.setResizeWeight(0.2); // heading ~10%, editor ~90%
                setCenterComponent(splitPane2);

            }
            case "message" -> {
                headingArea.setText("Message Mode\nFiles can be uploaded as .txt or .pdf\n" +
                        "Syntax:");
                highlightActiveMode(messageBtn);
                codeArea.setEditable(true);
                uploadBtn.setEnabled(true);
                runBtn.setEnabled(true);
                splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, headingScroll, codeScroll);
                splitPane.setDividerLocation(100); // initial height for heading
                splitPane.setResizeWeight(0.2); // heading ~10%, editor ~90%
                splitPane3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPane, errorScroll);
                splitPane3.setDividerLocation(475);
                splitPane3.setResizeWeight(0.9);
                setCenterComponent(splitPane3);

            }
        }
    }

    private void setCenterComponent(Component comp) {
        Container contentPane = getContentPane();
        BorderLayout layout = (BorderLayout) contentPane.getLayout();
        Component oldCenter = layout.getLayoutComponent(BorderLayout.CENTER);

        if (oldCenter != null) {
            contentPane.remove(oldCenter);
        }

        contentPane.add(comp, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

}