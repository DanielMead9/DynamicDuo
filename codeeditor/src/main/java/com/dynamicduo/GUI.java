/*
*
* Copyright (C) 2025 Owen Forsyth and Daniel Mead
*
* This program is free software: you can redistribute it and/or modify 
* it under the terms of the GNU General Public License as published by 
* the Free Software Foundation, either version 3 of the License, or 
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, 
* but WITHOUT ANY WARRANTY; without even the implied warranty of 
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
* General Public License for more details.
*
* You should have received a copy of the GNU General Public License 
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*
*/

package com.dynamicduo;

import com.dynamicduo.proto.lexer.Lexer;
import com.dynamicduo.proto.parser.ProtocolParser;
import com.dynamicduo.proto.parser.ParseException;
import com.dynamicduo.proto.ast.ProtocolNode;
import com.dynamicduo.proto.render.SVG;
import com.dynamicduo.proto.render.SequenceDiagramFromAst;

import javax.swing.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.*;

import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.app.beans.SVGIcon;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class GUI extends JFrame implements KeyListener {

    private JTextArea headingArea, analysisArea, errorArea;
    private JScrollPane headingScroll, svgScroll, analysisScroll, errorScroll;

    private RSyntaxTextArea codeArea;
    private RTextScrollPane codeScroll;

    private String currentMode = "message";
    private final HashMap<String, String> modeBuffers = new HashMap<>();
    private JSplitPane splitPane, splitPane2, splitPane3, splitPane4;

    private JButton messageBtn, svgBtn, javaBtn, analysisBtn;
    private JButton uploadBtn, runBtn, saveBtn, displayBtn;

    private Analysis analysis;
    private String analysisStr, svgStr;

    private boolean executed = false, dark = false;
    private JLabel label = new JLabel();
    private double zoomFactor = 1.0;

    public GUI() {
        setTitle("Security Message App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        addKeyListener(this);
        setFocusable(true);

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

        // set button size and fonts
        messageBtn.setPreferredSize(new Dimension(105, 35));
        messageBtn.setFont(new Font("Verdana", Font.BOLD, 14));
        svgBtn.setPreferredSize(new Dimension(80, 35));
        svgBtn.setFont(new Font("Verdana", Font.BOLD, 14));
        javaBtn.setPreferredSize(new Dimension(120, 35));
        javaBtn.setFont(new Font("Verdana", Font.BOLD, 14));
        analysisBtn.setPreferredSize(new Dimension(105, 35));
        analysisBtn.setFont(new Font("Verdana", Font.BOLD, 14));

        // add to navigation panel
        navPanel.add(messageBtn);
        navPanel.add(svgBtn);
        navPanel.add(javaBtn);
        navPanel.add(analysisBtn);

        topPanel.add(navPanel);

        // Creates Button panel to place function buttons on
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        // Assigning buttons
        runBtn = new JButton("Run");
        saveBtn = new JButton("Save");
        uploadBtn = new JButton("Upload");
        displayBtn = new JButton("Dark Mode");

        // set button size and fonts
        runBtn.setPreferredSize(new Dimension(80, 35));
        runBtn.setFont(new Font("Verdana", Font.BOLD, 14));
        saveBtn.setPreferredSize(new Dimension(80, 35));
        saveBtn.setFont(new Font("Verdana", Font.BOLD, 14));
        uploadBtn.setPreferredSize(new Dimension(100, 35));
        uploadBtn.setFont(new Font("Verdana", Font.BOLD, 14));
        displayBtn.setPreferredSize(new Dimension(125, 35));
        displayBtn.setFont(new Font("Verdana", Font.BOLD, 14));

        // add to button panel
        buttonPanel.add(runBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(uploadBtn);
        buttonPanel.add(displayBtn);

        topPanel.add(buttonPanel);

        add(topPanel, BorderLayout.NORTH);

        // Set Up Header Area
        headingArea = new JTextArea(3, 100);
        headingArea.setFont(new Font("Consolas", Font.BOLD, 14));
        headingArea.setEditable(false);
        headingArea.setBackground(new Color(230, 230, 230));

        headingScroll = new JScrollPane(headingArea);
        headingScroll.getVerticalScrollBar().putClientProperty("JScrollBar.fastWheelScrolling", true);

        // Set up Analysis Area
        analysisArea = new JTextArea();
        analysisArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        analysisArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        analysisArea.setEditable(false);

        analysisScroll = new JScrollPane(analysisArea);
        analysisScroll.getVerticalScrollBar().putClientProperty("JScrollBar.fastWheelScrolling", true);

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
        codeScroll.getVerticalScrollBar().putClientProperty("JScrollBar.fastWheelScrolling", true);

        // Create Error handler area for Message mode
        errorArea = new JTextArea();
        errorArea.setRows(5);
        errorArea.setFont(new Font("Consolas", Font.BOLD, 14));
        errorArea.setEditable(false);
        errorArea.setBackground(new Color(230, 230, 230));
        errorArea.setText("Error Handler");

        errorScroll = new JScrollPane(errorArea);
        errorScroll.getVerticalScrollBar().putClientProperty("JScrollBar.fastWheelScrolling", true);

        // Tab Switches
        messageBtn.addActionListener(e -> switchMode("message"));
        svgBtn.addActionListener(e -> switchMode("svg"));
        javaBtn.addActionListener(e -> switchMode("java"));
        analysisBtn.addActionListener(e -> switchMode("analysis"));

        // --- Save button action (with extension suggestion + auto-append) ---
        saveBtn.addActionListener(e -> {

            // Suggest extension based on mode
            String ext = switch (currentMode) {
                case "java" -> ".java";
                case "analysis" -> ".txt";
                default -> ".txt";
            };

            // Saving the svg
            if (currentMode.equals("svg")) {
                if (svgStr == null) {
                    JOptionPane.showMessageDialog(this, "There is no SVG to save");
                }
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Save Graph as SVG");
                fileChooser.setSelectedFile(new File("graph.svg"));

                int option = fileChooser.showSaveDialog(this);

                if (option == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();

                    // Ensure it ends with .svg
                    if (!file.getName().toLowerCase().endsWith(".svg")) {
                        file = new File(file.getParentFile(), file.getName() + ".svg");
                    }

                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write(svgStr);
                        JOptionPane.showMessageDialog(this, "File saved: " + file.getAbsolutePath());
                    } catch (IOException sf) {
                        JOptionPane.showMessageDialog(this, "Error saving file: " + sf.getMessage());
                        sf.printStackTrace();
                    }

                    /*
                     * try {
                     * // Render and save SVG file
                     * Graphviz.fromGraph(svg.getGraph())
                     * .render(Format.SVG)
                     * .toFile(file);
                     * 
                     * JOptionPane.showMessageDialog(this, "File saved: " + file.getAbsolutePath());
                     * } catch (IOException ex) {
                     * JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage());
                     * ex.printStackTrace();
                     * }
                     */
                }

            } else { // Saving from any other file
                JFileChooser fileChooser = new JFileChooser();
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
                            writer.write(analysisArea.getText());
                        JOptionPane.showMessageDialog(this, "File saved: " + file.getAbsolutePath());
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage());
                    }
                }
            }
            refocus();
        });

        // Upload button that allows for txt or pdf files into message mode
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

                // reading in the pdf
                if (file.getName().toLowerCase().endsWith(".pdf")) {
                    try (PDDocument document = PDDocument.load(file)) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        String text = stripper.getText(document);
                        codeArea.setText(text);
                        JOptionPane.showMessageDialog(this, "File loaded: " + file.getAbsolutePath());
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this, "Error loading file: " + ex.getMessage());
                    }
                } else { // reading in txt
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
            refocus();
        });

        // Dark mode toggle
        displayBtn.addActionListener(e -> {
            // Toggle between light and dark mode
            if (dark) {
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
                analysisArea.setBackground(Color.WHITE);
                analysisArea.setForeground(Color.BLACK);
                errorArea.setBackground(new Color(230, 230, 230));
                errorArea.setForeground(Color.BLACK);
                displayBtn.setText("Dark Mode");
                dark = false;
                labelDark();

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
                analysisArea.setBackground(new Color(40, 44, 52));
                analysisArea.setForeground(Color.WHITE);
                errorArea.setBackground(Color.DARK_GRAY);
                errorArea.setForeground(Color.WHITE);
                displayBtn.setText("Light Mode");
                dark = true;
                labelDark();
            }
            refocus();

        });

        runBtn.addActionListener(e -> {

            String input = codeArea.getText();
            Lexer lexer = new Lexer(input);
            ProtocolParser parser = new ProtocolParser(lexer);

            try {
                ProtocolNode tree = parser.parse();

                System.out.println("=== AST ===");
                System.out.println(tree.pretty());

                // Use our adapter to create a nice sequence diagram SVG
                svgStr = SequenceDiagramFromAst.renderTwoParty(tree);
                executed = true;

                errorArea.setText("No errors detected.");

            } catch (ParseException pe) {
                System.err.println("Parse error: " + pe.getMessage());
                System.err.println("Line: " + pe.getLine());
                errorArea.setText("Parse error: " + pe.getMessage() + "\nLine: " + pe.getLine());
                executed = false;
            } catch (Exception re) {
                System.err.println("Render failed: " + re.getMessage());
                errorArea.setText("Render failed: " + re.getMessage());
                executed = false;
            }

            if (executed) {

                svgStr = svgStr.replace("stroke=\"transparent\"", "stroke=\"none\"");

                /*
                 * analysis = new Analysis(messageArr);
                 * analysisStr = analysis.getAnalysis();
                 */
                switchMode("svg");

            }

            JOptionPane.showMessageDialog(this, "Run Button pressed");

        });

        switchMode("message");
    }

    // Highlight the active mode button
    private void highlightActiveMode(JButton active) {
        JButton[] allButtons = { messageBtn, svgBtn, javaBtn, analysisBtn };
        for (JButton b : allButtons) {
            if (b == active) {
                b.setBackground(Color.LIGHT_GRAY);
            } else {
                b.setBackground(new Color(238, 238, 238)); // reset default
            }
        }
    }

    // Switch editor between modes and remember content
    private void switchMode(String newMode) {
        // Save current bottom section content into buffer
        if (currentMode.equals("java") || currentMode.equals("message")) {
            modeBuffers.put(currentMode, codeArea.getText());
        } else if (newMode.equals("analysis"))
            modeBuffers.put(currentMode, analysisArea.getText());

        // Change mode
        currentMode = newMode;

        // Restore buffer if exists, else start empty
        String content = modeBuffers.getOrDefault(newMode, "");
        if (newMode.equals("java") || newMode.equals("message"))
            codeArea.setText(content);
        else if (newMode.equals("analysis"))
            analysisArea.setText(content);

        zoomFactor = 1.0;

        // Set heading text and activate buttons
        switch (newMode) {
            case "svg" -> {
                headingArea.setText("SVG Mode\n(This is the SVG for the Message Passing)");
                highlightActiveMode(svgBtn);

                uploadBtn.setEnabled(false);
                runBtn.setEnabled(false);

                if (executed && svgStr != null) {

                    SVGUniverse universe = new SVGUniverse();
                    URI svgUri = universe.loadSVG(new StringReader(svgStr), "graph");

                    SVGIcon icon = new SVGIcon();
                    icon.setSvgUniverse(universe);
                    icon.setSvgURI(svgUri);

                    icon.setAntiAlias(true);
                    icon.setAutosize(SVGIcon.AUTOSIZE_BESTFIT);

                    label = new JLabel(icon);
                    label.revalidate();
                    label.repaint();

                } else {
                    // default if message hasn't been properly run
                    label.setIcon(null);
                    label.setText("No SVG generated yet. Please run the message first or check for errors.");

                }

                label.setHorizontalAlignment(JLabel.CENTER);

                svgScroll = new JScrollPane(label);
                svgScroll.getVerticalScrollBar().setUnitIncrement(15);
                svgScroll.revalidate();
                svgScroll.repaint();

                splitPane4 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, headingScroll, svgScroll);
                splitPane4.setResizeWeight(0.1);
                setCenterComponent(splitPane4);

                splitPane4.revalidate();
                splitPane4.repaint();

                zoom(splitPane4);
                splitPane4.addKeyListener(this);
                splitPane4.setFocusable(true);
                splitPane4.requestFocusInWindow();
                labelDark();

            }
            case "java" -> {
                headingArea.setText("Java Code \n(This is the starter java code)");
                highlightActiveMode(javaBtn);
                if (executed) {
                    InputStream in = getClass().getResourceAsStream("/StarterCode.txt");
                    File file = null;

                    try {
                        file = File.createTempFile("StarterCode", ".txt");
                        file.deleteOnExit();
                        FileOutputStream out = new FileOutputStream(file);
                        in.transferTo(out);
                    } catch (IOException fe) {
                        fe.printStackTrace();
                    }

                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            codeArea.append(line + "\n");
                        }
                    } catch (IOException fstart) {
                        fstart.printStackTrace();
                        JOptionPane.showMessageDialog(codeArea, "Error reading file: " + fstart.getMessage(),
                                "File Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    codeArea.setText("No code available. Please run the message first or check for errors.");
                }
                codeArea.setEditable(false);
                uploadBtn.setEnabled(false);
                runBtn.setEnabled(false);
                setUpCodeScroll();
                setCenterComponent(splitPane);
                zoom(splitPane);
                splitPane.addKeyListener(this);
                splitPane.setFocusable(true);
                splitPane.requestFocusInWindow();

            }
            case "analysis" -> {
                headingArea.setText("Analysis Mode\n(This is what parts of the message have been leaked)");
                if (executed) {
                    analysisArea.setText("Analysis Results\n" + analysisStr);
                } else {
                    analysisArea.setText("No analysis available. Please run the message first or check for errors.");
                }

                highlightActiveMode(analysisBtn);
                uploadBtn.setEnabled(false);
                runBtn.setEnabled(false);
                splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, headingScroll, analysisScroll);
                splitPane2.setResizeWeight(0.10);
                setCenterComponent(splitPane2);

                zoom(splitPane2);
                splitPane2.addKeyListener(this);
                splitPane2.setFocusable(true);
                splitPane2.requestFocusInWindow();

            }
            case "message" -> {
                headingArea.setText("Message Mode\nFiles can be uploaded as .txt or .pdf\n" +
                        "Syntax:");
                highlightActiveMode(messageBtn);
                codeArea.setEditable(true);
                uploadBtn.setEnabled(true);
                runBtn.setEnabled(true);
                setUpCodeScroll();
                splitPane3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPane, errorScroll);
                splitPane3.setResizeWeight(0.8);
                setCenterComponent(splitPane3);

                zoom(splitPane3);
                splitPane3.addKeyListener(this);
                splitPane3.setFocusable(true);
                splitPane3.requestFocusInWindow();

            }
        }
    }

    // sets up the center component of the frame
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

    // ensures that splitPane is set up properly
    private void setUpCodeScroll() {
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, headingScroll, codeScroll);

        splitPane.setResizeWeight(0.25);
    }

    // Sets up key pressed method to allow zoom
    @Override
    public void keyPressed(KeyEvent e) {
        JSplitPane ext = switch (currentMode) {
            case "java" -> splitPane;
            case "svg" -> splitPane4;
            case "analysis" -> splitPane2;
            default -> splitPane3;
        };
        if (e.getKeyCode() == KeyEvent.VK_EQUALS && e.isControlDown()) {
            zoomFactor += .1; // zoom in
            zoom(ext);

        } else if (e.getKeyCode() == KeyEvent.VK_MINUS && e.isControlDown()) {
            zoomFactor -= .1; // zoom out
            zoom(ext);

        } else if (e.getKeyCode() == KeyEvent.VK_0 && e.isControlDown()) {
            zoomFactor = 1.0; // reset zoom
            zoom(ext);

        }
    }

    // zooming in or out based on what page the user is on
    public void zoom(JSplitPane ext) {
        Component[] arr = new Component[3];
        arr[0] = ext.getTopComponent();
        arr[1] = ext.getBottomComponent();

        int num = 2;

        if (arr[0] instanceof JSplitPane inner) {
            arr[0] = inner.getTopComponent();
            arr[2] = inner.getBottomComponent();

            num = 3;
        }

        for (int i = 0; i < num; i++) {

            if (arr[i] instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) arr[i];
                JViewport viewport = scrollPane.getViewport();
                Component view = viewport.getView();

                if (view instanceof JTextArea textArea) {
                    textArea.setFont(textArea.getFont().deriveFont((float) (16f * zoomFactor)));
                } else if (view instanceof JLabel label && currentMode.equals("svg") &&
                        executed && svgStr != null) {
                    SVGUniverse universe = new SVGUniverse();
                    URI svgUri = universe.loadSVG(new StringReader(svgStr), "graph");

                    SVGIcon icon = new SVGIcon();
                    icon.setSvgUniverse(universe);
                    icon.setSvgURI(svgUri);

                    icon.setAntiAlias(true);
                    icon.setAutosize(SVGIcon.AUTOSIZE_BESTFIT);

                    // Original dimensions
                    int originalWidth = icon.getIconWidth();
                    int originalHeight = icon.getIconHeight();

                    // Apply zoom factor
                    int newWidth = (int) (originalWidth * zoomFactor);
                    int newHeight = (int) (originalHeight * zoomFactor);

                    icon.setPreferredSize(new Dimension(newWidth, newHeight));
                    label.setIcon(icon);
                } else if (view instanceof RSyntaxTextArea rSyntaxTextArea) {
                    rSyntaxTextArea.setFont(rSyntaxTextArea.getFont().deriveFont((float) (14f * zoomFactor)));
                } else if (view instanceof JLabel label) {
                    label.setFont(label.getFont().deriveFont((float) (14f * zoomFactor)));
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    // Setting the background of the label
    public void labelDark() {
        if (dark) {
            label.setBackground(new Color(40, 44, 52));
            label.setForeground(Color.WHITE);
        } else {
            label.setBackground(Color.LIGHT_GRAY);
            label.setForeground(Color.BLACK);
        }

        label.setOpaque(true);
    }

    // refocus the frame after a button is pressed
    public void refocus() {
        JSplitPane ext = switch (currentMode) {
            case "java" -> splitPane;
            case "svg" -> splitPane4;
            case "analysis" -> splitPane2;
            default -> splitPane3;
        };
        ext.requestFocusInWindow();
    }

}