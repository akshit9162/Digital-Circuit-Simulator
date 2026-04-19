package com.circuitsimulator.components;

import com.circuitsimulator.circuit.Node;

/**
 * Represents a VCC (power supply) source.
 * Always outputs HIGH (logic 1).
 */
public class VCC extends Component {
    private static final long serialVersionUID = 1L;

    public VCC(String id, String name) {
        super(id, name);
        componentTypeId = 3;
        // VCC has one output node - always HIGH
        Node outNode = new Node(getNextNodeId());
        outNode.setLabel("VCC");
        outputNodes.add(outNode);
        evaluate();
    }

    @Override
    public void evaluate() {
        if (!outputNodes.isEmpty()) {
            outputNodes.get(0).setState(true); // Always HIGH
        }
    }

    @Override
    public String getComponentType() {
        return "VCC";
    }

    @Override
    public String toString() {
        return super.toString() + " [HIGH]";
    }
}
