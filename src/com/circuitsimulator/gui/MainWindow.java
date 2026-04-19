package com.circuitsimulator.gui;

import com.circuitsimulator.circuit.Circuit;
import com.circuitsimulator.circuit.Node;
import com.circuitsimulator.components.*;
import com.circuitsimulator.file.CircuitFileManager;
import com.circuitsimulator.validator.CircuitValidator;
import com.circuitsimulator.validator.TruthTableGenerator;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

/**
 * Main application window for the Digital Circuit Simulator.
 * Provides component library, canvas, toolbar, and all simulation controls.
 */
public class MainWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private Circuit circuit;
    private CircuitPanel circuitPanel;
    private JMenuBar menuBar;
    private JLabel statusLabel;
    private JLabel shortCircuitLabel;
    private JLabel componentCountLabel;

    // Counter for auto-placement to avoid overlaps
    private int nextPlacementX = 150;
    private int nextPlacementY = 100;
    private static final int PLACEMENT_STEP_X = 120;
    private static final int PLACEMENT_STEP_Y = 100;
    private static final int MAX_X = 900;

    public MainWindow() {
        setTitle("Digital Circuit Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        
        circuit = new Circuit("New Circuit");
        
        initializeUI();
    }

    private void initializeUI() {
        // Menu Bar
        menuBar = createMenuBar();
        setJMenuBar(menuBar);

        // Main Panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // TOP: Toolbar
        JToolBar toolbar = createToolbar();
        mainPanel.add(toolbar, BorderLayout.NORTH);

        // LEFT: Component Library
        JPanel leftPanel = createComponentLibrary();
        mainPanel.add(leftPanel, BorderLayout.WEST);

        // CENTER: Circuit Canvas
        JPanel canvasPanel = new JPanel(new BorderLayout());
        JLabel canvasTitle = new JLabel("  Circuit Canvas  —  Right-click port to drag wire  |  Left-click to select/drag  |  Click switch to toggle");
        canvasTitle.setFont(new Font("Arial", Font.BOLD, 11));
        canvasTitle.setBackground(new Color(230, 230, 230));
        canvasTitle.setOpaque(true);
        canvasTitle.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        canvasPanel.add(canvasTitle, BorderLayout.NORTH);
        
        circuitPanel = new CircuitPanel(circuit);
        JScrollPane canvasScroll = new JScrollPane(circuitPanel);
        canvasScroll.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(150, 150, 150)));
        canvasPanel.add(canvasScroll, BorderLayout.CENTER);
        mainPanel.add(canvasPanel, BorderLayout.CENTER);

        // BOTTOM: Status Bar
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(44, 62, 80));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Left side status
        statusLabel = new JLabel("  Ready");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        bottomPanel.add(statusLabel, BorderLayout.WEST);
        
        // Center: component count
        componentCountLabel = new JLabel("Components: 0 | Connections: 0");
        componentCountLabel.setForeground(new Color(180, 200, 220));
        componentCountLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        componentCountLabel.setHorizontalAlignment(JLabel.CENTER);
        bottomPanel.add(componentCountLabel, BorderLayout.CENTER);
        
        // Right side: short circuit indicator
        shortCircuitLabel = new JLabel("Short Circuits: 0  ");
        shortCircuitLabel.setForeground(new Color(100, 255, 100));
        shortCircuitLabel.setFont(new Font("Arial", Font.BOLD, 11));
        shortCircuitLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        bottomPanel.add(shortCircuitLabel, BorderLayout.EAST);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(new Color(44, 62, 80));
        toolbar.setPreferredSize(new Dimension(0, 50));

        Color darkBlue = new Color(44, 62, 80);
        Color lightBlue = new Color(52, 152, 219);
        Color green = new Color(39, 174, 96);
        Color orange = new Color(230, 126, 34);
        
        // Left side - File operations
        JButton newBtn = new JButton("New Circuit");
        styleToolbarButton(newBtn, darkBlue);
        newBtn.addActionListener(e -> newCircuit());
        toolbar.add(newBtn);

        JButton saveBtn = new JButton("Save");
        styleToolbarButton(saveBtn, darkBlue);
        saveBtn.addActionListener(e -> saveCircuit());
        toolbar.add(saveBtn);

        JButton openBtn = new JButton("Open");
        styleToolbarButton(openBtn, darkBlue);
        openBtn.addActionListener(e -> openCircuit());
        toolbar.add(openBtn);

        toolbar.addSeparator(new Dimension(20, 0));
        
        // Connection by node number
        JButton connectBtn = new JButton("Connect Nodes");
        styleToolbarButton(connectBtn, orange);
        connectBtn.setPreferredSize(new Dimension(130, 40));
        connectBtn.addActionListener(e -> connectByNodeNumber());
        toolbar.add(connectBtn);

        toolbar.add(Box.createHorizontalGlue());

        // Right side - Simulate, Validate, Truth Table
        JButton validateBtn = new JButton("Validate");
        styleToolbarButton(validateBtn, darkBlue);
        validateBtn.addActionListener(e -> validateCircuit());
        toolbar.add(validateBtn);

        JButton simulateBtn = new JButton("Simulate");
        styleToolbarButton(simulateBtn, green);
        simulateBtn.addActionListener(e -> simulateCircuit());
        toolbar.add(simulateBtn);

        JButton truthTableBtn = new JButton("Truth Table");
        styleToolbarButton(truthTableBtn, lightBlue);
        truthTableBtn.addActionListener(e -> generateTruthTable());
        toolbar.add(truthTableBtn);

        return toolbar;
    }
    
    private void styleToolbarButton(JButton btn, Color bgColor) {
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setFont(new Font("Arial", Font.PLAIN, 11));
        btn.setPreferredSize(new Dimension(100, 40));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        Color hoverColor = bgColor.brighter();
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverColor);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bgColor);
            }
        });
    }

    private JPanel createComponentLibrary() {
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBackground(Color.WHITE);
        
        // Title
        JLabel titleLabel = new JLabel("Component Library");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        titleLabel.setBackground(new Color(230, 230, 230));
        titleLabel.setOpaque(true);
        wrapperPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Component grid panel - use BoxLayout for vertical stacking with sections
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        listPanel.setBackground(Color.WHITE);
        
        // Section: I/O Components
        addSectionLabel(listPanel, "I/O Components");
        addComponentItem(listPanel, "Switch", "Switch");
        addComponentItem(listPanel, "LED", "LED");
        
        // Section: Power
        addSectionLabel(listPanel, "Power Sources");
        addComponentItem(listPanel, "VCC (High)", "VCC");
        addComponentItem(listPanel, "Ground (Low)", "Ground");
        
        // Section: Basic Gates
        addSectionLabel(listPanel, "Basic Gates");
        addComponentItem(listPanel, "AND Gate", "AND");
        addComponentItem(listPanel, "OR Gate", "OR");
        addComponentItem(listPanel, "NOT Gate", "NOT");
        addComponentItem(listPanel, "XOR Gate", "XOR");
        
        // Section: Universal Gates
        addSectionLabel(listPanel, "Universal Gates");
        addComponentItem(listPanel, "NAND Gate", "NAND");
        addComponentItem(listPanel, "NOR Gate", "NOR");
        addComponentItem(listPanel, "XNOR Gate", "XNOR");
        
        // Spacer
        listPanel.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        wrapperPanel.add(scroll, BorderLayout.CENTER);
        wrapperPanel.setPreferredSize(new Dimension(180, 0));
        
        return wrapperPanel;
    }
    
    private void addSectionLabel(JPanel parent, String text) {
        JLabel label = new JLabel("  " + text);
        label.setFont(new Font("Arial", Font.BOLD, 10));
        label.setForeground(new Color(100, 100, 100));
        label.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        label.setBorder(BorderFactory.createEmptyBorder(8, 0, 2, 0));
        parent.add(label);
    }
    
    private void addComponentItem(JPanel parent, String displayName, String componentType) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        panel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        
        // Icon (small colored indicator)
        JPanel iconDot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color dotColor = getIconColor(componentType);
                g2.setColor(dotColor);
                g2.fillRoundRect(2, 4, 14, 14, 4, 4);
                g2.setColor(dotColor.darker());
                g2.drawRoundRect(2, 4, 14, 14, 4, 4);
                
                // Draw symbol letter
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 9));
                String ch = componentType.substring(0, 1);
                if (componentType.equals("Ground")) ch = "G";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(ch, 9 - fm.stringWidth(ch)/2, 15);
            }
        };
        iconDot.setPreferredSize(new Dimension(20, 22));
        iconDot.setOpaque(false);
        panel.add(iconDot, BorderLayout.WEST);
        
        JLabel label = new JLabel("  " + displayName);
        label.setFont(new Font("Arial", Font.PLAIN, 11));
        panel.add(label, BorderLayout.CENTER);
        
        // Click handler
        Color normalBg = new Color(245, 245, 245);
        Color hoverBg = new Color(210, 230, 250);
        
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                addComponentFromLibrary(componentType);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBackground(hoverBg);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBackground(normalBg);
            }
        });
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        parent.add(panel);
    }
    
    private Color getIconColor(String type) {
        switch (type) {
            case "Switch": return new Color(52, 152, 219);
            case "LED": return new Color(231, 76, 60);
            case "VCC": return new Color(255, 80, 80);
            case "Ground": return new Color(100, 100, 100);
            case "AND": return new Color(46, 204, 113);
            case "OR": return new Color(155, 89, 182);
            case "NOT": return new Color(241, 196, 15);
            case "XOR": return new Color(230, 126, 34);
            case "NAND": return new Color(26, 188, 156);
            case "NOR": return new Color(142, 68, 173);
            case "XNOR": return new Color(22, 160, 133);
            default: return Color.GRAY;
        }
    }
    
    /**
     * Gets next available placement position that doesn't overlap existing components.
     */
    private Point getNextPlacementPosition() {
        // Check if the default position overlaps with any existing component
        int tryX = nextPlacementX;
        int tryY = nextPlacementY;
        
        boolean overlap = true;
        int attempts = 0;
        
        while (overlap && attempts < 50) {
            overlap = false;
            for (com.circuitsimulator.components.Component comp : circuit.getComponents()) {
                int dx = Math.abs(comp.getXPos() - tryX);
                int dy = Math.abs(comp.getYPos() - tryY);
                if (dx < 100 && dy < 80) {
                    overlap = true;
                    break;
                }
            }
            if (overlap) {
                tryX += PLACEMENT_STEP_X;
                if (tryX > MAX_X) {
                    tryX = 150;
                    tryY += PLACEMENT_STEP_Y;
                }
                attempts++;
            }
        }
        
        // Update for next placement
        nextPlacementX = tryX + PLACEMENT_STEP_X;
        if (nextPlacementX > MAX_X) {
            nextPlacementX = 150;
            nextPlacementY = tryY + PLACEMENT_STEP_Y;
        }
        
        return new Point(tryX, tryY);
    }
    
    private void addComponentFromLibrary(String componentType) {
        if (componentType == null || componentType.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a component type", 
                    "No Component Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Default component name
        String defaultName = componentType.toLowerCase() + "_" + (circuit.getTotalComponents() + 1);
        
        // Show input dialog
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("Component Type:"));
        panel.add(new JLabel(componentType));
        panel.add(new JLabel("Name:"));
        JTextField textField = new JTextField(defaultName, 15);
        panel.add(textField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
                "Add " + componentType, JOptionPane.OK_CANCEL_OPTION);
        
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        
        String compName = textField.getText().trim();
        if (compName.isEmpty()) {
            compName = defaultName;
        }

        String compId = "C" + System.currentTimeMillis() % 100000;
        com.circuitsimulator.components.Component comp = null;

        try {
            switch (componentType) {
                case "Switch":
                    comp = new Switch(compId, compName);
                    break;
                case "LED":
                    comp = new LED(compId, compName);
                    break;
                case "AND":
                    comp = new ANDGate(compId, compName, 2);
                    break;
                case "OR":
                    comp = new ORGate(compId, compName, 2);
                    break;
                case "NOT":
                    comp = new NOTGate(compId, compName);
                    break;
                case "XOR":
                    comp = new XORGate(compId, compName, 2);
                    break;
                case "NAND":
                    comp = new NANDGate(compId, compName, 2);
                    break;
                case "NOR":
                    comp = new NORGate(compId, compName, 2);
                    break;
                case "XNOR":
                    comp = new XNORGate(compId, compName, 2);
                    break;
                case "VCC":
                    comp = new VCC(compId, compName);
                    break;
                case "Ground":
                    comp = new Ground(compId, compName);
                    break;
            }

            if (comp != null) {
                // Find non-overlapping position
                Point pos = getNextPlacementPosition();
                comp.setPosition(pos.x, pos.y);

                circuit.addComponent(comp);
                circuitPanel.repaint();
                updateStatusBar();
                statusLabel.setText("  Added: " + comp.toString() + " at (" + pos.x + "," + pos.y + ")");
                
                // Show node numbers so user can connect them
                StringBuilder nodeInfo = new StringBuilder();
                nodeInfo.append("Component: ").append(comp.getName()).append("\n");
                if (!comp.getInputNodes().isEmpty()) {
                    nodeInfo.append("Input Nodes: ");
                    for (Node n : comp.getInputNodes()) {
                        nodeInfo.append(n.getNodeNumber()).append(" ");
                    }
                    nodeInfo.append("\n");
                }
                if (!comp.getOutputNodes().isEmpty()) {
                    nodeInfo.append("Output Nodes: ");
                    for (Node n : comp.getOutputNodes()) {
                        nodeInfo.append(n.getNodeNumber()).append(" ");
                    }
                }
                JOptionPane.showMessageDialog(this, nodeInfo.toString(),
                        "Component Added", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error adding component: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Connect two components by entering node numbers.
     * This implements the "number to number correspondence" requirement.
     */
    private void connectByNodeNumber() {
        if (circuit.getTotalComponents() < 2) {
            JOptionPane.showMessageDialog(this, "Need at least 2 components to connect.",
                    "Cannot Connect", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Build info about all available nodes
        StringBuilder info = new StringBuilder();
        info.append("Available Nodes:\n");
        info.append("────────────────────────────────\n");
        for (com.circuitsimulator.components.Component comp : circuit.getComponents()) {
            info.append(comp.getName()).append(" (").append(comp.getComponentType()).append("):\n");
            for (Node n : comp.getInputNodes()) {
                info.append("   INPUT  Node ").append(n.getNodeNumber()).append("\n");
            }
            for (Node n : comp.getOutputNodes()) {
                info.append("   OUTPUT Node ").append(n.getNodeNumber()).append("\n");
            }
        }
        
        // Dialog to enter from and to node numbers
        JPanel dialogPanel = new JPanel(new BorderLayout(5, 5));
        
        JTextArea infoArea = new JTextArea(info.toString());
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        infoArea.setBackground(new Color(245, 245, 245));
        JScrollPane infoScroll = new JScrollPane(infoArea);
        infoScroll.setPreferredSize(new Dimension(350, 200));
        dialogPanel.add(infoScroll, BorderLayout.CENTER);
        
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        inputPanel.add(new JLabel("From (Output) Node #:"));
        JTextField fromField = new JTextField();
        inputPanel.add(fromField);
        inputPanel.add(new JLabel("To (Input) Node #:"));
        JTextField toField = new JTextField();
        inputPanel.add(toField);
        dialogPanel.add(inputPanel, BorderLayout.SOUTH);
        
        int result = JOptionPane.showConfirmDialog(this, dialogPanel,
                "Connect Nodes by Number", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result != JOptionPane.OK_OPTION) return;
        
        try {
            int fromNum = Integer.parseInt(fromField.getText().trim());
            int toNum = Integer.parseInt(toField.getText().trim());
            
            Node fromNode = circuit.findNodeByNumber(fromNum);
            Node toNode = circuit.findNodeByNumber(toNum);
            
            if (fromNode == null) {
                JOptionPane.showMessageDialog(this, "Node " + fromNum + " not found!",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (toNode == null) {
                JOptionPane.showMessageDialog(this, "Node " + toNum + " not found!",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Validate: from should be output, to should be input
            if (!circuit.isOutputNode(fromNode)) {
                JOptionPane.showMessageDialog(this, "Node " + fromNum + " is not an output node!",
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!circuit.isInputNode(toNode)) {
                JOptionPane.showMessageDialog(this, "Node " + toNum + " is not an input node!",
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Make the connection
            circuit.connect(fromNode, toNode);
            circuit.simulate();
            circuitPanel.updateWiresFromCircuit();
            circuitPanel.repaint();
            updateStatusBar();
            
            JOptionPane.showMessageDialog(this, 
                "✓ Connected!\nNode " + fromNum + " → Node " + toNum,
                "Connection Made", JOptionPane.INFORMATION_MESSAGE);
            
            statusLabel.setText("  Connected: Node " + fromNum + " → Node " + toNum);
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers!",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem newCircuit = new JMenuItem("New");
        newCircuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newCircuit.addActionListener(e -> newCircuit());
        
        JMenuItem openCircuit = new JMenuItem("Open");
        openCircuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openCircuit.addActionListener(e -> openCircuit());
        
        JMenuItem saveCircuit = new JMenuItem("Save");
        saveCircuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveCircuit.addActionListener(e -> saveCircuit());
        
        JMenuItem exportText = new JMenuItem("Export as Text");
        exportText.addActionListener(e -> exportAsText());
        
        JMenuItem exitApp = new JMenuItem("Exit");
        exitApp.addActionListener(e -> System.exit(0));

        fileMenu.add(newCircuit);
        fileMenu.addSeparator();
        fileMenu.add(openCircuit);
        fileMenu.add(saveCircuit);
        fileMenu.add(exportText);
        fileMenu.addSeparator();
        fileMenu.add(exitApp);

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        
        JMenuItem clearAll = new JMenuItem("Clear All");
        clearAll.addActionListener(e -> clearCircuit());
        
        JMenuItem deleteSelected = new JMenuItem("Delete Selected");
        deleteSelected.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        deleteSelected.addActionListener(e -> deleteSelected());
        
        JMenuItem connectNodes = new JMenuItem("Connect by Node Number...");
        connectNodes.addActionListener(e -> connectByNodeNumber());

        editMenu.add(clearAll);
        editMenu.add(deleteSelected);
        editMenu.addSeparator();
        editMenu.add(connectNodes);

        // Circuit Menu
        JMenu circuitMenu = new JMenu("Circuit");
        
        JMenuItem validate = new JMenuItem("Validate");
        validate.addActionListener(e -> validateCircuit());
        
        JMenuItem simulate = new JMenuItem("Simulate");
        simulate.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        simulate.addActionListener(e -> simulateCircuit());
        
        JMenuItem truthTable = new JMenuItem("Generate Truth Table");
        truthTable.addActionListener(e -> generateTruthTable());
        
        JMenuItem checkShortCircuit = new JMenuItem("Check Short Circuits");
        checkShortCircuit.addActionListener(e -> checkShortCircuits());

        circuitMenu.add(validate);
        circuitMenu.add(simulate);
        circuitMenu.addSeparator();
        circuitMenu.add(truthTable);
        circuitMenu.add(checkShortCircuit);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> showAbout());
        
        JMenuItem howToUse = new JMenuItem("How to Use");
        howToUse.addActionListener(e -> showHowToUse());

        helpMenu.add(howToUse);
        helpMenu.add(about);

        bar.add(fileMenu);
        bar.add(editMenu);
        bar.add(circuitMenu);
        bar.add(helpMenu);

        return bar;
    }

    private void deleteSelected() {
        com.circuitsimulator.components.Component selected = circuitPanel.getSelectedComponent();
        if (selected != null) {
            circuit.removeComponent(selected);
            circuitPanel.clearSelection();
            circuitPanel.updateWiresFromCircuit();
            circuitPanel.repaint();
            updateStatusBar();
            statusLabel.setText("  Deleted: " + selected.toString());
        } else {
            JOptionPane.showMessageDialog(this, "Please select a component to delete (click on it first)",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void validateCircuit() {
        CircuitValidator validator = new CircuitValidator(circuit);
        boolean valid = validator.validate();
        
        String title = valid ? "✓ Validation Passed" : "✗ Validation Failed";
        int msgType = valid ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE;
        
        JTextArea textArea = new JTextArea(validator.getValidationReport());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 300));
        
        JOptionPane.showMessageDialog(this, scrollPane, title, msgType);
        
        updateStatusBar();
        statusLabel.setText("  Validation completed - " + (valid ? "PASSED" : "FAILED"));
    }

    private void simulateCircuit() {
        circuit.simulate();
        circuitPanel.repaint();
        updateStatusBar();
        statusLabel.setText("  Simulation completed");
    }
    
    private void checkShortCircuits() {
        circuit.simulate(); // Simulate first to get current states
        List<String> shortCircuits = circuit.detectShortCircuits();
        
        if (shortCircuits.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "✓ No short circuits detected!\n\nAll connections are valid.",
                "Short Circuit Check", JOptionPane.INFORMATION_MESSAGE);
            shortCircuitLabel.setText("Short Circuits: 0  ");
            shortCircuitLabel.setForeground(new Color(100, 255, 100));
        } else {
            StringBuilder msg = new StringBuilder();
            msg.append("✗ SHORT CIRCUITS DETECTED!\n\n");
            for (String sc : shortCircuits) {
                msg.append("• ").append(sc).append("\n");
            }
            JOptionPane.showMessageDialog(this, msg.toString(),
                    "Short Circuit Alert", JOptionPane.ERROR_MESSAGE);
            shortCircuitLabel.setText("Short Circuits: " + shortCircuits.size() + "  ");
            shortCircuitLabel.setForeground(new Color(255, 80, 80));
        }
    }

    private void generateTruthTable() {
        if (circuit.getTotalComponents() == 0) {
            JOptionPane.showMessageDialog(this, "Circuit is empty! Add components first.",
                    "No Circuit", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        TruthTableGenerator generator = new TruthTableGenerator(circuit);
        String table = generator.getTruthTableFormatted();
        
        JTextArea textArea = new JTextArea(table);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        textArea.setBackground(new Color(250, 250, 250));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(this, scrollPane, 
                "Truth Table - All Input Combinations", JOptionPane.INFORMATION_MESSAGE);
        statusLabel.setText("  Truth table generated");
    }

    private void newCircuit() {
        String name = JOptionPane.showInputDialog(this, "Enter circuit name:", "New Circuit");
        if (name != null && !name.trim().isEmpty()) {
            circuit.clear();
            circuit.setCircuitName(name.trim());
            circuitPanel.setCircuit(circuit);
            circuitPanel.repaint();
            updateStatusBar();
            nextPlacementX = 150;
            nextPlacementY = 100;
            statusLabel.setText("  New circuit: " + name);
        }
    }

    private void saveCircuit() {
        JFileChooser chooser = new JFileChooser("circuits");
        chooser.setFileFilter(new FileNameExtensionFilter("Circuit files (*.cir)", "cir"));
        
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".cir")) {
                file = new File(file.getAbsolutePath() + ".cir");
            }
            
            if (CircuitFileManager.saveCircuit(circuit, file.getAbsolutePath())) {
                JOptionPane.showMessageDialog(this, "Circuit saved successfully to:\n" + file.getAbsolutePath(),
                        "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                statusLabel.setText("  Saved: " + file.getName());
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save circuit",
                        "Save Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openCircuit() {
        JFileChooser chooser = new JFileChooser("circuits");
        chooser.setFileFilter(new FileNameExtensionFilter("Circuit files (*.cir)", "cir"));
        
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            Circuit loadedCircuit = CircuitFileManager.loadCircuit(file.getAbsolutePath());
            
            if (loadedCircuit != null) {
                circuit = loadedCircuit;
                circuitPanel.setCircuit(circuit);
                circuitPanel.updateWiresFromCircuit();
                circuitPanel.repaint();
                updateStatusBar();
                statusLabel.setText("  Loaded: " + file.getName());
            } else {
                JOptionPane.showMessageDialog(this, "Failed to load circuit",
                        "Load Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportAsText() {
        JFileChooser chooser = new JFileChooser("circuits");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }
            
            CircuitFileManager.exportAsText(circuit, file.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Circuit exported successfully",
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            statusLabel.setText("  Exported: " + file.getName());
        }
    }

    private void clearCircuit() {
        int result = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to clear the entire circuit?\nThis cannot be undone.",
                "Confirm Clear", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            circuit.clear();
            circuitPanel.clearSelection();
            circuitPanel.updateWiresFromCircuit();
            circuitPanel.repaint();
            updateStatusBar();
            nextPlacementX = 150;
            nextPlacementY = 100;
            statusLabel.setText("  Circuit cleared");
        }
    }

    private void updateStatusBar() {
        int switches = 0, leds = 0, gates = 0;
        for (com.circuitsimulator.components.Component comp : circuit.getComponents()) {
            if (comp instanceof Switch) switches++;
            else if (comp instanceof LED) leds++;
            else if (comp instanceof LogicGate) gates++;
        }
        
        componentCountLabel.setText("Components: " + circuit.getTotalComponents() + 
                " | Connections: " + circuit.getConnections().size() +
                " | Switches: " + switches + " | Gates: " + gates + " | LEDs: " + leds);
        
        // Check short circuits
        List<String> shortCircuits = circuit.detectShortCircuits();
        if (shortCircuits.isEmpty()) {
            shortCircuitLabel.setText("Short Circuits: 0  ");
            shortCircuitLabel.setForeground(new Color(100, 255, 100));
        } else {
            shortCircuitLabel.setText("Short Circuits: " + shortCircuits.size() + "  ");
            shortCircuitLabel.setForeground(new Color(255, 80, 80));
        }
    }
    
    private void showHowToUse() {
        String help = 
            "=== HOW TO USE THE DIGITAL CIRCUIT SIMULATOR ===\n\n" +
            "1. ADD COMPONENTS:\n" +
            "   Click on a component in the left sidebar to add it.\n" +
            "   Components are auto-placed to avoid overlapping.\n" +
            "   Each component's node numbers are shown.\n\n" +
            "2. CONNECT COMPONENTS (Two methods):\n" +
            "   a) By Node Number: Click 'Connect Nodes' button or\n" +
            "      Edit → Connect by Node Number. Enter the output\n" +
            "      node # and input node # to connect.\n" +
            "   b) By Drag: Right-click on an output port (red dot)\n" +
            "      and drag to an input port (blue dot).\n\n" +
            "3. INTERACT WITH SWITCHES:\n" +
            "   Left-click on a switch to toggle it ON/OFF.\n" +
            "   The circuit simulates automatically.\n\n" +
            "4. SIMULATE:\n" +
            "   Click 'Simulate' or press F5 to run simulation.\n" +
            "   LEDs change color based on input signals.\n\n" +
            "5. TRUTH TABLE:\n" +
            "   Click 'Truth Table' to generate all combinations.\n\n" +
            "6. VALIDATE:\n" +
            "   Click 'Validate' to check for errors.\n" +
            "   Click 'Check Short Circuits' for short circuit analysis.\n\n" +
            "7. SAVE/LOAD:\n" +
            "   Use File → Save/Open or toolbar buttons.\n\n" +
            "NODE NUMBERING:\n" +
            "   Each port has a unique number shown on the port.\n" +
            "   Blue ports = Inputs | Red ports = Outputs\n" +
            "   Use these numbers to connect components.\n\n" +
            "AUTO-CONNECTIONS:\n" +
            "   • Unconnected LED inputs → auto-connected to Ground\n" +
            "   • Unconnected switch outputs → auto-connected to VCC\n";
        
        JTextArea textArea = new JTextArea(help);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(550, 450));
        
        JOptionPane.showMessageDialog(this, scrollPane,
                "How to Use", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "Digital Circuit Simulator v2.0\n\n" +
                "A Swing-based GUI application for designing\n" +
                "and simulating digital logic circuits.\n\n" +
                "Features:\n" +
                "• Logic gates: AND, OR, NOT, XOR, NAND, NOR, XNOR\n" +
                "• Interactive switches and LED indicators\n" +
                "• VCC and Ground power sources\n" +
                "• Number-based node connection scheme\n" +
                "• Drag-to-connect wire drawing\n" +
                "• Circuit validation & short circuit detection\n" +
                "• Truth table generation (all combinations)\n" +
                "• Auto-connect: open LEDs→GND, open switches→VCC\n" +
                "• Save and load circuits\n" +
                "• Multi-level combinational circuit simulation\n",
                "About", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to default
        }
        
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
