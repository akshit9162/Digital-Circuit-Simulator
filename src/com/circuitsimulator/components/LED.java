package com.circuitsimulator.components;

import com.circuitsimulator.circuit.Node;
import java.awt.Color;

/**
 * Represents an LED
 */
public class LED extends Component {
    private static final long serialVersionUID = 1L;
    
    private static final Color COLOR_OFF = new Color(100, 100, 100);
    private static final Color COLOR_ON = new Color(255, 0, 0);
    private Color currentColor;
    private boolean isLit;

    public LED(String id, String name) {
        super(id, name);
        this.isLit = false;
        this.currentColor = COLOR_OFF;
        componentTypeId = 2;
        // Initialize input node with unique ID
        Node inNode = new Node(getNextNodeId());
        inNode.setLabel("Input");
        inputNodes.add(inNode);
        // Set initial state
        evaluate();
    }

    public void addInputNode(Node node) {
        if (inputNodes.size() > 0) {
            inputNodes.set(0, node);
        } else {
            inputNodes.add(node);
        }
    }

    @Override
    public void evaluate() {
        if (!inputNodes.isEmpty()) {
            Node input = inputNodes.get(0);
            isLit = input.getState();
            currentColor = isLit ? COLOR_ON : COLOR_OFF;
        } else {
            isLit = false;
            currentColor = COLOR_OFF;
        }
    }

    public boolean isLit() {
        return isLit;
    }

    public Color getCurrentColor() {
        return currentColor;
    }

    @Override
    public String getComponentType() {
        return "LED";
    }

    @Override
    public String toString() {
        return super.toString() + " [" + (isLit ? "ON" : "OFF") + "]";
    }
}
