package com.circuitsimulator.components;

import com.circuitsimulator.circuit.Node;

/**
 * Represents an interactive switch (SPST)
 */
public class Switch extends Component {
    private static final long serialVersionUID = 1L;
    
    private boolean isOpen;

    public Switch(String id, String name) {
        super(id, name);
        this.isOpen = true;
        componentTypeId = 1;
        // Initialize output node with unique ID
        Node outNode = new Node(getNextNodeId());
        outNode.setLabel("Output");
        outputNodes.add(outNode);
        // Set initial state based on switch position
        evaluate();
    }

    public void addOutputNode(Node node) {
        if (outputNodes.size() > 0) {
            outputNodes.set(0, node);
        } else {
            outputNodes.add(node);
        }
    }

    public void toggle() {
        isOpen = !isOpen;
        evaluate();
    }

    public void setState(boolean state) {
        this.isOpen = !state;
        evaluate();
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean isClosed() {
        return !isOpen;
    }

    @Override
    public void evaluate() {
        if (!outputNodes.isEmpty()) {
            Node output = outputNodes.get(0);
            output.setState(!isOpen);  // Invert: CLOSED (false) outputs HIGH (true), OPEN (true) outputs LOW (false)
        }
    }

    @Override
    public String getComponentType() {
        return "SWITCH";
    }

    @Override
    public String toString() {
        return super.toString() + " [" + (isOpen ? "OPEN" : "CLOSED") + "]";
    }
}
