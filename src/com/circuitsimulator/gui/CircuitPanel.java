package com.circuitsimulator.gui;

import com.circuitsimulator.circuit.Circuit;
import com.circuitsimulator.circuit.Wire;
import com.circuitsimulator.components.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * The drawing panel for the circuit canvas.
 * Handles rendering of components, wires, ports, and drag interactions.
 */
public class CircuitPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private Circuit circuit;
    private com.circuitsimulator.components.Component selectedComponent;
    private Wire selectedWire;
    private Point lastMousePos;
    private static final int GRID_SIZE = 20;
    private static final int COMPONENT_SIZE = 50;
    private static final int WIRE_CLICK_TOLERANCE = 8;
    private Map<com.circuitsimulator.components.Component, ComponentRenderer> renderers;
    private List<Wire> wires;
    
    // Terminal tracking for drag-to-connect
    private static class Terminal {
        com.circuitsimulator.components.Component component;
        boolean isOutput;
        int portIndex;
        Point position;
        
        Terminal(com.circuitsimulator.components.Component comp, boolean isOutput, int portIndex, Point pos) {
            this.component = comp;
            this.isOutput = isOutput;
            this.portIndex = portIndex;
            this.position = pos;
        }
    }
    
    // Drag-to-connect wires
    private Terminal dragStartTerminal;
    private Terminal dragOverTerminal;
    private Point currentMousePos;
    private boolean isDrawingWire = false;

    public CircuitPanel(Circuit circuit) {
        this.circuit = circuit;
        this.renderers = new HashMap<>();
        this.wires = new ArrayList<>();
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1200, 800));
        
        updateWiresFromCircuit();
        initializeEventHandlers();
    }

    private void initializeEventHandlers() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                
                // Right-click to start drawing wire from terminal
                if (SwingUtilities.isRightMouseButton(e)) {
                    dragStartTerminal = getTerminalAt(e.getPoint());
                    if (dragStartTerminal != null) {
                        currentMousePos = e.getPoint();
                        isDrawingWire = true;
                        selectedWire = null;
                        selectedComponent = null;
                        repaint();
                    }
                } else {
                    // Left-click to select component or wire
                    selectedWire = getWireAt(e.getPoint());
                    
                    if (selectedWire != null) {
                        // Deselect all wires first, then select this one
                        for (Wire w : wires) w.setSelected(false);
                        selectedWire.setSelected(true);
                        selectedComponent = null;
                        isDrawingWire = false;
                        repaint();
                    } else {
                        // Deselect all wires
                        for (Wire w : wires) w.setSelected(false);
                        
                        // Try to select component
                        com.circuitsimulator.components.Component comp = getCompAt(e.getPoint());
                        
                        if (comp != null && comp instanceof Switch) {
                            // Toggle switch on click
                            ((Switch) comp).toggle();
                            circuit.simulate();
                            selectedComponent = comp;
                            lastMousePos = e.getPoint();
                            isDrawingWire = false;
                            repaint();
                        } else {
                            selectedComponent = comp;
                            lastMousePos = e.getPoint();
                            isDrawingWire = false;
                            repaint();
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDrawingWire && dragStartTerminal != null) {
                    dragOverTerminal = getTerminalAt(e.getPoint());
                    if (dragOverTerminal != null && dragOverTerminal.component != dragStartTerminal.component) {
                        if (dragStartTerminal.isOutput && !dragOverTerminal.isOutput) {
                            makeConnection(dragStartTerminal, dragOverTerminal);
                        } else if (!dragStartTerminal.isOutput && dragOverTerminal.isOutput) {
                            makeConnection(dragOverTerminal, dragStartTerminal);
                        } else {
                            JOptionPane.showMessageDialog(CircuitPanel.this,
                                "Cannot connect: must connect OUTPUT → INPUT",
                                "Invalid Connection", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                    isDrawingWire = false;
                    dragStartTerminal = null;
                    dragOverTerminal = null;
                }
                lastMousePos = null;
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                currentMousePos = e.getPoint();
                
                if (isDrawingWire) {
                    dragOverTerminal = getTerminalAt(e.getPoint());
                    repaint();
                } else if (selectedComponent != null && lastMousePos != null) {
                    int dx = e.getX() - lastMousePos.x;
                    int dy = e.getY() - lastMousePos.y;
                    
                    selectedComponent.setPosition(
                        selectedComponent.getXPos() + dx,
                        selectedComponent.getYPos() + dy
                    );
                    
                    lastMousePos = e.getPoint();
                    repaint();
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    if (selectedWire != null) {
                        deleteWire(selectedWire);
                        selectedWire = null;
                        repaint();
                    } else if (selectedComponent != null) {
                        circuit.removeComponent(selectedComponent);
                        renderers.remove(selectedComponent);
                        selectedComponent = null;
                        updateWiresFromCircuit();
                        repaint();
                    }
                }
            }
        });

        setFocusable(true);
    }
    
    private void makeConnection(Terminal fromTerminal, Terminal toTerminal) {
        try {
            java.util.List<com.circuitsimulator.circuit.Node> fromNodes = fromTerminal.component.getOutputNodes();
            java.util.List<com.circuitsimulator.circuit.Node> toNodes = toTerminal.component.getInputNodes();
            
            if (fromTerminal.portIndex < fromNodes.size() && toTerminal.portIndex < toNodes.size()) {
                com.circuitsimulator.circuit.Node fromNode = fromNodes.get(fromTerminal.portIndex);
                com.circuitsimulator.circuit.Node toNode = toNodes.get(toTerminal.portIndex);
                
                circuit.connect(fromNode, toNode);
                circuit.simulate();
                updateWiresFromCircuit();
                JOptionPane.showMessageDialog(this, 
                    "✓ Connected!\nNode " + fromNode.getNodeNumber() + " → Node " + toNode.getNodeNumber(),
                    "Wire Connected", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Cannot connect: " + ex.getMessage(),
                "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private Terminal getTerminalAt(Point p) {
        for (com.circuitsimulator.components.Component comp : circuit.getComponents()) {
            int x = comp.getXPos();
            int y = comp.getYPos();
            int size = COMPONENT_SIZE;
            
            // Check input ports (left side)
            java.util.List<com.circuitsimulator.circuit.Node> inputs = comp.getInputNodes();
            for (int i = 0; i < inputs.size(); i++) {
                int portY = y - size/2 + (i+1) * (size / (inputs.size() + 1));
                int portX = x - size/2 - 15;
                if (Math.abs(p.x - portX) < 15 && Math.abs(p.y - portY) < 15) {
                    return new Terminal(comp, false, i, new Point(portX, portY));
                }
            }
            
            // Check output ports (right side)
            java.util.List<com.circuitsimulator.circuit.Node> outputs = comp.getOutputNodes();
            for (int i = 0; i < outputs.size(); i++) {
                int portY = y - size/2 + (i+1) * (size / (outputs.size() + 1));
                int portX = x + size/2 + 15;
                if (Math.abs(p.x - portX) < 15 && Math.abs(p.y - portY) < 15) {
                    return new Terminal(comp, true, i, new Point(portX, portY));
                }
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawGrid(g2);
        drawConnections(g2);
        drawComponents(g2);
        drawPorts(g2);
        
        // Draw active wire being dragged
        if (isDrawingWire && dragStartTerminal != null && currentMousePos != null) {
            drawDraggingWire(g2);
        }
        
        // Draw help text if empty
        if (circuit.getTotalComponents() == 0) {
            g2.setColor(new Color(180, 180, 180));
            g2.setFont(new Font("Arial", Font.ITALIC, 16));
            g2.drawString("Click a component in the left sidebar to add it here", 250, 350);
            g2.setFont(new Font("Arial", Font.ITALIC, 13));
            g2.drawString("Right-click ports to draw wires  |  Left-click to select & drag", 250, 380);
        }
    }
    
    private void drawDraggingWire(Graphics2D g) {
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
                                    0, new float[]{8, 4}, 0));
        g.setColor(new Color(255, 165, 0));
        
        int x1 = dragStartTerminal.position.x;
        int y1 = dragStartTerminal.position.y;
        int x2 = currentMousePos.x;
        int y2 = currentMousePos.y;
        
        g.drawLine(x1, y1, x2, y2);
        
        // Start circle
        g.setStroke(new BasicStroke(2));
        g.setColor(new Color(255, 200, 0));
        g.fillOval(x1 - 6, y1 - 6, 12, 12);
        g.setColor(Color.BLACK);
        g.drawOval(x1 - 6, y1 - 6, 12, 12);
        
        // End circle (hover)
        if (dragOverTerminal != null) {
            g.setColor(new Color(100, 255, 100));
            g.fillOval(x2 - 7, y2 - 7, 14, 14);
            g.setColor(new Color(0, 150, 0));
            g.setStroke(new BasicStroke(2));
            g.drawOval(x2 - 7, y2 - 7, 14, 14);
        } else {
            g.setColor(new Color(200, 200, 200, 150));
            g.fillOval(x2 - 5, y2 - 5, 10, 10);
        }
    }
    
    private void drawPorts(Graphics2D g) {
        for (com.circuitsimulator.components.Component comp : circuit.getComponents()) {
            int x = comp.getXPos();
            int y = comp.getYPos();
            int size = COMPONENT_SIZE;
            
            // Draw input ports (left side) - blue
            java.util.List<com.circuitsimulator.circuit.Node> inputs = comp.getInputNodes();
            for (int i = 0; i < inputs.size(); i++) {
                int portY = y - size/2 + (i+1) * (size / (inputs.size() + 1));
                int portX = x - size/2 - 15;
                com.circuitsimulator.circuit.Node node = inputs.get(i);
                
                // Port fill color based on state
                if (node.getState()) {
                    g.setColor(new Color(100, 200, 255));
                } else {
                    g.setColor(new Color(50, 100, 160));
                }
                g.fillOval(portX - 8, portY - 8, 16, 16);
                
                // Border
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(2));
                g.drawOval(portX - 8, portY - 8, 16, 16);
                
                // State indicator inside
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 10));
                String stateChar = node.getState() ? "1" : "0";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(stateChar, portX - fm.stringWidth(stateChar)/2, portY + 4);
                
                // Node number label (above the port)
                g.setColor(new Color(0, 80, 160));
                g.setFont(new Font("Arial", Font.PLAIN, 9));
                String nodeLabel = "" + node.getNodeNumber();
                fm = g.getFontMetrics();
                g.drawString(nodeLabel, portX - fm.stringWidth(nodeLabel)/2, portY - 12);
                
                // Connection line from port to component body
                g.setColor(new Color(100, 100, 100));
                g.setStroke(new BasicStroke(1.5f));
                g.drawLine(portX + 8, portY, x - size/2, portY);
            }
            
            // Draw output ports (right side) - red
            java.util.List<com.circuitsimulator.circuit.Node> outputs = comp.getOutputNodes();
            for (int i = 0; i < outputs.size(); i++) {
                int portY = y - size/2 + (i+1) * (size / (outputs.size() + 1));
                int portX = x + size/2 + 15;
                com.circuitsimulator.circuit.Node node = outputs.get(i);
                
                if (node.getState()) {
                    g.setColor(new Color(255, 100, 100));
                } else {
                    g.setColor(new Color(150, 50, 50));
                }
                g.fillOval(portX - 8, portY - 8, 16, 16);
                
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(2));
                g.drawOval(portX - 8, portY - 8, 16, 16);
                
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 10));
                String stateChar = node.getState() ? "1" : "0";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(stateChar, portX - fm.stringWidth(stateChar)/2, portY + 4);
                
                // Node number label (above the port)
                g.setColor(new Color(160, 0, 0));
                g.setFont(new Font("Arial", Font.PLAIN, 9));
                String nodeLabel = "" + node.getNodeNumber();
                fm = g.getFontMetrics();
                g.drawString(nodeLabel, portX - fm.stringWidth(nodeLabel)/2, portY - 12);
                
                // Connection line from component body to port
                g.setColor(new Color(100, 100, 100));
                g.setStroke(new BasicStroke(1.5f));
                g.drawLine(x + size/2, portY, portX - 8, portY);
            }
        }
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(235, 235, 235));
        g.setStroke(new BasicStroke(0.5f));
        
        for (int x = 0; x < getWidth(); x += GRID_SIZE) {
            g.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += GRID_SIZE) {
            g.drawLine(0, y, getWidth(), y);
        }
    }

    private void drawConnections(Graphics2D g) {
        // Update wire endpoint positions
        for (Wire wire : wires) {
            com.circuitsimulator.circuit.Connection conn = wire.getConnection();
            com.circuitsimulator.circuit.Node fromNode = conn.getFromNode();
            com.circuitsimulator.circuit.Node toNode = conn.getToNode();

            Point fromPoint = findNodeEndpoint(fromNode);
            Point toPoint = findNodeEndpoint(toNode);
            
            wire.setStartPoint(fromPoint);
            wire.setEndPoint(toPoint);
        }
        
        // Draw all wires
        for (Wire wire : wires) {
            com.circuitsimulator.circuit.Connection conn = wire.getConnection();
            com.circuitsimulator.circuit.Node fromNode = conn.getFromNode();

            Point fromPoint = wire.getStartPoint();
            Point toPoint = wire.getEndPoint();

            if (fromPoint != null && toPoint != null) {
                if (wire.isSelected()) {
                    g.setColor(new Color(255, 220, 0));
                    g.setStroke(new BasicStroke(4));
                } else if (fromNode.getState()) {
                    g.setColor(new Color(220, 50, 50));
                    g.setStroke(new BasicStroke(2.5f));
                } else {
                    g.setColor(new Color(50, 100, 180));
                    g.setStroke(new BasicStroke(2));
                }
                
                // Draw wire as straight line
                g.drawLine(fromPoint.x, fromPoint.y, toPoint.x, toPoint.y);
                
                // Junction dots
                g.setColor(new Color(30, 30, 30));
                g.fillOval(fromPoint.x - 3, fromPoint.y - 3, 6, 6);
                g.fillOval(toPoint.x - 3, toPoint.y - 3, 6, 6);
                
                // Wire label (node number) at midpoint
                int labelX = (fromPoint.x + toPoint.x) / 2 - 12;
                int labelY = (fromPoint.y + toPoint.y) / 2 - 8;
                g.setColor(new Color(255, 255, 220, 220));
                g.fillRoundRect(labelX, labelY, 24, 16, 4, 4);
                g.setColor(new Color(100, 100, 100));
                g.setStroke(new BasicStroke(1));
                g.drawRoundRect(labelX, labelY, 24, 16, 4, 4);
                
                g.setColor(new Color(0, 0, 0));
                g.setFont(new Font("Arial", Font.PLAIN, 9));
                String label = "" + fromNode.getNodeNumber();
                FontMetrics fm = g.getFontMetrics();
                g.drawString(label, labelX + (24 - fm.stringWidth(label))/2, labelY + 12);
            }
        }
    }

    private void drawComponents(Graphics2D g) {
        for (com.circuitsimulator.components.Component comp : circuit.getComponents()) {
            ComponentRenderer renderer = renderers.computeIfAbsent(comp, 
                c -> new ComponentRenderer(c, COMPONENT_SIZE));
            
            renderer.draw(g, comp, comp == selectedComponent);
        }
    }

    private Point findNodeEndpoint(com.circuitsimulator.circuit.Node node) {
        for (com.circuitsimulator.components.Component comp : circuit.getComponents()) {
            int x = comp.getXPos();
            int y = comp.getYPos();
            int size = COMPONENT_SIZE;
            
            if (comp.getInputNodes().contains(node)) {
                int index = comp.getInputNodes().indexOf(node);
                int numInputs = comp.getInputNodes().size();
                int portY = y - size/2 + (index+1) * (size / (numInputs + 1));
                return new Point(x - size/2 - 15, portY);
            }
            if (comp.getOutputNodes().contains(node)) {
                int index = comp.getOutputNodes().indexOf(node);
                int numOutputs = comp.getOutputNodes().size();
                int portY = y - size/2 + (index+1) * (size / (numOutputs + 1));
                return new Point(x + size/2 + 15, portY);
            }
        }
        return null;
    }

    public com.circuitsimulator.components.Component getCompAt(Point p) {
        for (com.circuitsimulator.components.Component comp : circuit.getComponents()) {
            int x = comp.getXPos();
            int y = comp.getYPos();
            Rectangle2D bounds = new Rectangle2D.Double(
                x - COMPONENT_SIZE / 2,
                y - COMPONENT_SIZE / 2,
                COMPONENT_SIZE,
                COMPONENT_SIZE
            );
            if (bounds.contains(p)) {
                return comp;
            }
        }
        return null;
    }

    public void clearSelection() {
        selectedComponent = null;
        selectedWire = null;
        for (Wire w : wires) w.setSelected(false);
        repaint();
    }
    
    /**
     * Public method to update wires from circuit connections.
     * Called after connecting/disconnecting/loading.
     */
    public void updateWiresFromCircuit() {
        wires.clear();
        selectedWire = null;
        
        for (com.circuitsimulator.circuit.Connection conn : circuit.getConnections()) {
            wires.add(new Wire(conn));
        }
    }
    
    private Wire getWireAt(Point p) {
        for (Wire wire : wires) {
            if (wire.contains(p, WIRE_CLICK_TOLERANCE)) {
                return wire;
            }
        }
        return null;
    }
    
    private void deleteWire(Wire wire) {
        circuit.removeConnection(wire.getConnection());
        wires.remove(wire);
        circuit.simulate();
        JOptionPane.showMessageDialog(this, 
            "✓ Wire deleted!\n" + wire.toString(),
            "Wire Deleted", JOptionPane.INFORMATION_MESSAGE);
    }

    public com.circuitsimulator.components.Component getSelectedComponent() {
        return selectedComponent;
    }

    public void setCircuit(Circuit circuit) {
        this.circuit = circuit;
        this.renderers.clear();
        updateWiresFromCircuit();
    }

    // ========================================================================
    // Component Renderer - draws each component type
    // ========================================================================
    private static class ComponentRenderer {
        private int size;

        ComponentRenderer(com.circuitsimulator.components.Component comp, int size) {
            this.size = size;
        }

        void draw(Graphics2D g, com.circuitsimulator.components.Component comp, boolean selected) {
            int x = comp.getXPos();
            int y = comp.getYPos();

            // Selection highlight
            if (selected) {
                g.setColor(new Color(255, 220, 0, 60));
                g.fillRoundRect(x - size/2 - 5, y - size/2 - 5, size + 10, size + 10, 8, 8);
                g.setColor(new Color(255, 200, 0));
                g.setStroke(new BasicStroke(3));
                g.drawRoundRect(x - size/2 - 3, y - size/2 - 3, size + 6, size + 6, 6, 6);
            }

            // Draw component body
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2));
            
            Rectangle2D rect = new Rectangle2D.Double(
                x - size / 2, y - size / 2, size, size
            );

            if (comp instanceof Switch) {
                drawSwitch(g, x, y, size, (Switch) comp);
            } else if (comp instanceof LED) {
                drawLED(g, x, y, size, (LED) comp);
            } else if (comp instanceof VCC) {
                drawVCC(g, x, y, size);
            } else if (comp instanceof Ground) {
                drawGround(g, x, y, size);
            } else if (comp instanceof LogicGate) {
                drawGate(g, x, y, size, comp);
            } else {
                // Generic component
                g.setColor(new Color(220, 220, 220));
                g.fillRect((int)(rect.getX()), (int)(rect.getY()), 
                          (int)(rect.getWidth()), (int)(rect.getHeight()));
                g.setColor(Color.BLACK);
                g.drawRect((int)(rect.getX()), (int)(rect.getY()), 
                          (int)(rect.getWidth()), (int)(rect.getHeight()));
            }

            // Component name label below
            g.setColor(new Color(50, 50, 50));
            g.setFont(new Font("Arial", Font.BOLD, 10));
            FontMetrics fm = g.getFontMetrics();
            String label = comp.getName();
            if (label.length() > 12) label = label.substring(0, 12);
            g.drawString(label, x - fm.stringWidth(label) / 2, y + size / 2 + 15);
        }

        private void drawSwitch(Graphics2D g, int x, int y, int size, Switch sw) {
            // Background
            g.setColor(new Color(230, 245, 255));
            g.fillRoundRect(x - size/2, y - size/2, size, size, 8, 8);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(x - size/2, y - size/2, size, size, 8, 8);
            
            // Switch symbol
            g.setStroke(new BasicStroke(2.5f));
            g.drawLine(x - 15, y, x - 5, y);
            g.drawLine(x + 5, y, x + 15, y);
            
            if (sw.isOpen()) {
                g.setColor(new Color(200, 50, 50));
                g.drawLine(x - 5, y, x + 3, y - 10);
                g.setFont(new Font("Arial", Font.BOLD, 9));
                g.drawString("OFF", x - 9, y - 14);
            } else {
                g.setColor(new Color(50, 180, 50));
                g.drawLine(x - 5, y, x + 5, y);
                g.setFont(new Font("Arial", Font.BOLD, 9));
                g.drawString("ON", x - 7, y - 14);
            }
            
            // Terminals
            g.setColor(Color.BLACK);
            g.fillOval(x - 7, y - 2, 4, 4);
            g.fillOval(x + 3, y - 2, 4, 4);
        }

        private void drawLED(Graphics2D g, int x, int y, int size, LED led) {
            // Background
            g.setColor(new Color(255, 240, 240));
            g.fillRoundRect(x - size/2, y - size/2, size, size, 8, 8);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(x - size/2, y - size/2, size, size, 8, 8);
            
            // LED triangle (diode symbol)
            int triHeight = size - 14;
            int triWidth = size - 10;
            
            int[] xPoints = {x - triWidth/2, x + triWidth/2, x};
            int[] yPoints = {y + triHeight/2, y + triHeight/2, y - triHeight/2};
            
            if (led.isLit()) {
                // Glowing effect
                g.setColor(new Color(255, 80, 80, 40));
                g.fillOval(x - size/2 + 2, y - size/2 + 2, size - 4, size - 4);
                
                g.setColor(new Color(255, 50, 50));
                g.fillPolygon(xPoints, yPoints, 3);
            } else {
                g.setColor(new Color(180, 180, 180));
                g.fillPolygon(xPoints, yPoints, 3);
            }
            
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2));
            g.drawPolygon(xPoints, yPoints, 3);
            
            // Cathode bars
            g.setStroke(new BasicStroke(2));
            g.drawLine(x - triWidth/2 - 2, y + triHeight/2 + 2, x + triWidth/2 + 2, y + triHeight/2 + 2);
            g.drawLine(x - triWidth/2 - 2, y + triHeight/2 + 5, x + triWidth/2 + 2, y + triHeight/2 + 5);
            
            // Light rays when lit
            if (led.isLit()) {
                g.setColor(new Color(255, 150, 0));
                g.setStroke(new BasicStroke(1.5f));
                g.drawLine(x - 10, y - 14, x - 15, y - 20);
                g.drawLine(x + 10, y - 14, x + 15, y - 20);
                g.drawLine(x, y - 16, x, y - 22);
            }
        }
        
        private void drawVCC(Graphics2D g, int x, int y, int size) {
            // Background
            g.setColor(new Color(255, 230, 230));
            g.fillRoundRect(x - size/2, y - size/2, size, size, 8, 8);
            g.setColor(new Color(200, 50, 50));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(x - size/2, y - size/2, size, size, 8, 8);
            
            // VCC symbol: upward arrow with horizontal bar
            g.setColor(new Color(220, 50, 50));
            g.setStroke(new BasicStroke(3));
            g.drawLine(x, y + 12, x, y - 8);
            g.drawLine(x - 10, y - 8, x + 10, y - 8);
            
            // Arrow head
            g.fillPolygon(
                new int[]{x - 5, x + 5, x},
                new int[]{y - 3, y - 3, y - 12},
                3
            );
            
            // Label
            g.setColor(new Color(180, 0, 0));
            g.setFont(new Font("Arial", Font.BOLD, 10));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("VCC", x - fm.stringWidth("VCC")/2, y + 22);
        }
        
        private void drawGround(Graphics2D g, int x, int y, int size) {
            // Background
            g.setColor(new Color(235, 235, 235));
            g.fillRoundRect(x - size/2, y - size/2, size, size, 8, 8);
            g.setColor(new Color(80, 80, 80));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(x - size/2, y - size/2, size, size, 8, 8);
            
            // Ground symbol: vertical line + 3 horizontal bars decreasing width
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2.5f));
            g.drawLine(x, y - 12, x, y);
            g.drawLine(x - 14, y + 2, x + 14, y + 2);
            g.drawLine(x - 9, y + 7, x + 9, y + 7);
            g.drawLine(x - 4, y + 12, x + 4, y + 12);
            
            // Label
            g.setColor(new Color(80, 80, 80));
            g.setFont(new Font("Arial", Font.BOLD, 9));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("GND", x - fm.stringWidth("GND")/2, y - 15);
        }

        private void drawGate(Graphics2D g, int x, int y, int size, com.circuitsimulator.components.Component gate) {
            String type = gate.getComponentType();
            g.setStroke(new BasicStroke(2));
            
            switch(type) {
                case "AND":  drawANDGate(g, x, y, size); break;
                case "OR":   drawORGate(g, x, y, size); break;
                case "NOT":  drawNOTGate(g, x, y, size); break;
                case "XOR":  drawXORGate(g, x, y, size); break;
                case "NAND": drawNANDGate(g, x, y, size); break;
                case "NOR":  drawNORGate(g, x, y, size); break;
                case "XNOR": drawXNORGate(g, x, y, size); break;
                default:
                    g.setColor(new Color(200, 200, 200));
                    g.fillRect(x - size/2, y - size/2, size, size);
                    g.setColor(Color.BLACK);
                    g.drawRect(x - size/2, y - size/2, size, size);
                    g.setFont(new Font("Arial", Font.BOLD, 10));
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(type, x - fm.stringWidth(type)/2, y + 5);
            }
        }
        
        private void drawANDGate(Graphics2D g, int x, int y, int size) {
            g.setColor(new Color(100, 180, 100));
            g.fillRect(x - size/2, y - size/2, size/2, size);
            g.fillArc(x - size/2 + size/2 - 2, y - size/2, size/2 + 4, size, -90, 180);
            
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2.5f));
            g.drawRect(x - size/2, y - size/2, size/2, size);
            g.drawArc(x - size/2 + size/2 - 2, y - size/2, size/2 + 4, size, -90, 180);
            
            g.setFont(new Font("Arial", Font.BOLD, 9));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("AND", x - fm.stringWidth("AND")/2, y + 3);
        }
        
        private void drawORGate(Graphics2D g, int x, int y, int size) {
            g.setColor(new Color(130, 130, 220));
            g.fillArc(x - size/2 - 4, y - size/2, size/2 - 2, size, -90, 180);
            g.fillArc(x - size/2 + 4, y - size/2 - 2, size/2, size + 4, -90, 180);
            g.fillRect(x - size/4, y - size/2, size/2, size);
            
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2.5f));
            g.drawArc(x - size/2 - 4, y - size/2, size/2 - 2, size, -90, 180);
            g.drawArc(x - size/2 + 4, y - size/2 - 2, size/2, size + 4, -90, 180);
            g.drawRect(x - size/4, y - size/2, size/2, size);
            
            g.setFont(new Font("Arial", Font.BOLD, 9));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("OR", x - fm.stringWidth("OR")/2, y + 3);
        }
        
        private void drawNOTGate(Graphics2D g, int x, int y, int size) {
            int[] xPts = {x - size/2, x + size/2 - 8, x - size/2};
            int[] yPts = {y - size/2, y, y + size/2};
            
            g.setColor(new Color(230, 200, 80));
            g.fillPolygon(xPts, yPts, 3);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2.5f));
            g.drawPolygon(xPts, yPts, 3);
            
            // Inversion bubble
            g.setColor(new Color(230, 200, 80));
            g.fillOval(x + size/2 - 10, y - 5, 10, 10);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2f));
            g.drawOval(x + size/2 - 10, y - 5, 10, 10);
            
            g.setFont(new Font("Arial", Font.BOLD, 8));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("NOT", x - fm.stringWidth("NOT")/2 - 3, y + 3);
        }
        
        private void drawXORGate(Graphics2D g, int x, int y, int size) {
            g.setColor(new Color(230, 150, 60));
            g.fillArc(x - size/2 - 4, y - size/2, size/2 - 2, size, -90, 180);
            g.fillArc(x - size/2 + 4, y - size/2 - 2, size/2, size + 4, -90, 180);
            g.fillRect(x - size/4, y - size/2, size/2, size);
            
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2.5f));
            g.drawArc(x - size/2 - 8, y - size/2, 8, size, -90, 180);
            g.drawArc(x - size/2 - 4, y - size/2, size/2 - 2, size, -90, 180);
            g.drawArc(x - size/2 + 4, y - size/2 - 2, size/2, size + 4, -90, 180);
            g.drawRect(x - size/4, y - size/2, size/2, size);
            
            g.setFont(new Font("Arial", Font.BOLD, 8));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("XOR", x - fm.stringWidth("XOR")/2, y + 3);
        }
        
        private void drawNANDGate(Graphics2D g, int x, int y, int size) {
            g.setColor(new Color(80, 190, 170));
            g.fillRect(x - size/2, y - size/2, size/2, size);
            g.fillArc(x - size/2 + size/2 - 2, y - size/2, size/2 + 4, size, -90, 180);
            
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2.5f));
            g.drawRect(x - size/2, y - size/2, size/2, size);
            g.drawArc(x - size/2 + size/2 - 2, y - size/2, size/2 + 4, size, -90, 180);
            
            g.setColor(new Color(80, 190, 170));
            g.fillOval(x + size/2 - 8, y - 5, 10, 10);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2f));
            g.drawOval(x + size/2 - 8, y - 5, 10, 10);
            
            g.setFont(new Font("Arial", Font.BOLD, 7));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("NAND", x - fm.stringWidth("NAND")/2 - 2, y + 3);
        }
        
        private void drawNORGate(Graphics2D g, int x, int y, int size) {
            g.setColor(new Color(170, 100, 200));
            g.fillArc(x - size/2 - 4, y - size/2, size/2 - 2, size, -90, 180);
            g.fillArc(x - size/2 + 4, y - size/2 - 2, size/2, size + 4, -90, 180);
            g.fillRect(x - size/4, y - size/2, size/2, size);
            
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2.5f));
            g.drawArc(x - size/2 - 4, y - size/2, size/2 - 2, size, -90, 180);
            g.drawArc(x - size/2 + 4, y - size/2 - 2, size/2, size + 4, -90, 180);
            g.drawRect(x - size/4, y - size/2, size/2, size);
            
            g.setColor(new Color(170, 100, 200));
            g.fillOval(x + size/2 - 8, y - 5, 10, 10);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2f));
            g.drawOval(x + size/2 - 8, y - 5, 10, 10);
            
            g.setFont(new Font("Arial", Font.BOLD, 8));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("NOR", x - fm.stringWidth("NOR")/2 - 2, y + 3);
        }
        
        private void drawXNORGate(Graphics2D g, int x, int y, int size) {
            g.setColor(new Color(60, 180, 150));
            g.fillArc(x - size/2 - 4, y - size/2, size/2 - 2, size, -90, 180);
            g.fillArc(x - size/2 + 4, y - size/2 - 2, size/2, size + 4, -90, 180);
            g.fillRect(x - size/4, y - size/2, size/2, size);
            
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2.5f));
            g.drawArc(x - size/2 - 8, y - size/2, 8, size, -90, 180);
            g.drawArc(x - size/2 - 4, y - size/2, size/2 - 2, size, -90, 180);
            g.drawArc(x - size/2 + 4, y - size/2 - 2, size/2, size + 4, -90, 180);
            g.drawRect(x - size/4, y - size/2, size/2, size);
            
            g.setColor(new Color(60, 180, 150));
            g.fillOval(x + size/2 - 8, y - 5, 10, 10);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2f));
            g.drawOval(x + size/2 - 8, y - 5, 10, 10);
            
            g.setFont(new Font("Arial", Font.BOLD, 7));
            FontMetrics fm = g.getFontMetrics();
            g.drawString("XNOR", x - fm.stringWidth("XNOR")/2 - 2, y + 3);
        }
    }
}
