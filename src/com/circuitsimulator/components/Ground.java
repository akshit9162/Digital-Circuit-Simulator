package com.circuitsimulator.components;

import com.circuitsimulator.circuit.Node;

/**
 * Represents a Ground (GND) connection.
 * Always outputs LOW (logic 0).
 */
public class Ground extends Component {
    private static final long serialVersionUID = 1L;

    public Ground(String id, String name) {
        super(id, name);
        componentTypeId = 4;
        // Ground has one output node - always LOW
        Node outNode = new Node(getNextNodeId());
        outNode.setLabel("GND");
        outputNodes.add(outNode);
        evaluate();
    }

    @Override
    public void evaluate() {
        if (!outputNodes.isEmpty()) {
            outputNodes.get(0).setState(false); // Always LOW
        }
    }

    @Override
    public String getComponentType() {
        return "GND";
    }

    @Override
    public String toString() {
        return super.toString() + " [LOW]";
    }
}
