package com.circuitsimulator.validator;

import com.circuitsimulator.circuit.Circuit;
import com.circuitsimulator.circuit.Connection;
import com.circuitsimulator.circuit.Node;
import com.circuitsimulator.components.*;
import java.util.*;

/**
 * Validates circuit for errors, warnings, and short circuits.
 * Performs comprehensive analysis including:
 * - Component connection validation
 * - Short circuit detection (VCC→GND path, conflicting outputs)
 * - Data flow analysis (floating inputs, undriven nodes)
 */
public class CircuitValidator {
    private Circuit circuit;
    private List<String> errors;
    private List<String> warnings;

    public CircuitValidator(Circuit circuit) {
        this.circuit = circuit;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    /**
     * Performs complete circuit validation
     */
    public boolean validate() {
        errors.clear();
        warnings.clear();

        if (circuit.getTotalComponents() == 0) {
            warnings.add("Circuit is empty - no components added");
            return true;
        }

        validateComponents();
        validateConnections();
        validateShortCircuits();
        validateDataFlow();

        return errors.isEmpty();
    }

    /**
     * Checks if all components have necessary connections
     */
    private void validateComponents() {
        // Track connected nodes
        Set<Integer> connectedAsSource = new HashSet<>();
        Set<Integer> connectedAsDest = new HashSet<>();
        for (Connection conn : circuit.getConnections()) {
            connectedAsSource.add(conn.getFromNode().getNodeNumber());
            connectedAsDest.add(conn.getToNode().getNodeNumber());
        }

        for (Component comp : circuit.getComponents()) {
            String type = comp.getComponentType();

            if (comp instanceof Switch) {
                // Check if switch output is connected to anything
                for (Node outNode : comp.getOutputNodes()) {
                    if (!connectedAsSource.contains(outNode.getNodeNumber())) {
                        warnings.add("Switch '" + comp.getName() + "' output (Node " + 
                                    outNode.getNodeNumber() + ") is not connected to any input. Auto-connected to VCC.");
                    }
                }
            } else if (comp instanceof LED) {
                // Check if LED input is connected to anything
                for (Node inNode : comp.getInputNodes()) {
                    if (!connectedAsDest.contains(inNode.getNodeNumber())) {
                        warnings.add("LED '" + comp.getName() + "' input (Node " + 
                                    inNode.getNodeNumber() + ") is not connected. Auto-connected to Ground.");
                    }
                }
            } else if (comp instanceof VCC) {
                for (Node outNode : comp.getOutputNodes()) {
                    if (!connectedAsSource.contains(outNode.getNodeNumber())) {
                        warnings.add("VCC '" + comp.getName() + "' is not connected to anything");
                    }
                }
            } else if (comp instanceof Ground) {
                for (Node outNode : comp.getOutputNodes()) {
                    if (!connectedAsSource.contains(outNode.getNodeNumber())) {
                        warnings.add("Ground '" + comp.getName() + "' is not connected to anything");
                    }
                }
            } else if (comp instanceof NOTGate) {
                // NOT gate needs 1 input
                boolean inputConnected = false;
                for (Node inNode : comp.getInputNodes()) {
                    if (connectedAsDest.contains(inNode.getNodeNumber())) {
                        inputConnected = true;
                    }
                }
                if (!inputConnected) {
                    warnings.add("NOT Gate '" + comp.getName() + "' input is not connected (defaults to Ground)");
                }
                boolean outputConnected = false;
                for (Node outNode : comp.getOutputNodes()) {
                    if (connectedAsSource.contains(outNode.getNodeNumber())) {
                        outputConnected = true;
                    }
                }
                if (!outputConnected) {
                    warnings.add("NOT Gate '" + comp.getName() + "' output is not connected");
                }
            } else if (comp instanceof LogicGate) {
                // Check all inputs are connected
                int unconnectedInputs = 0;
                for (Node inNode : comp.getInputNodes()) {
                    if (!connectedAsDest.contains(inNode.getNodeNumber())) {
                        unconnectedInputs++;
                    }
                }
                if (unconnectedInputs > 0) {
                    warnings.add(type + " Gate '" + comp.getName() + "' has " + unconnectedInputs + 
                                " unconnected input(s) (default to Ground)");
                }
                boolean outputConnected = false;
                for (Node outNode : comp.getOutputNodes()) {
                    if (connectedAsSource.contains(outNode.getNodeNumber())) {
                        outputConnected = true;
                    }
                }
                if (!outputConnected) {
                    warnings.add(type + " Gate '" + comp.getName() + "' output is not connected");
                }
            }
        }
    }

    /**
     * Validates all connections
     */
    private void validateConnections() {
        for (Connection conn : circuit.getConnections()) {
            if (conn.getFromNode() == null || conn.getToNode() == null) {
                errors.add("Connection has null nodes");
                continue;
            }

            // Check that fromNode is an output and toNode is an input
            boolean fromIsOutput = circuit.isOutputNode(conn.getFromNode());
            boolean toIsInput = circuit.isInputNode(conn.getToNode());

            if (!fromIsOutput) {
                warnings.add("Connection from Node " + conn.getFromNode().getNodeNumber() + 
                            " - source is not a component output");
            }
            if (!toIsInput) {
                warnings.add("Connection to Node " + conn.getToNode().getNodeNumber() + 
                            " - destination is not a component input");
            }
        }
    }

    /**
     * Detects short circuits using Circuit's detection method.
     * Short circuit = 0 resistance path from VCC/HIGH to GND/LOW.
     */
    private void validateShortCircuits() {
        List<String> shortCircuits = circuit.detectShortCircuits();
        for (String sc : shortCircuits) {
            errors.add(sc);
        }
    }

    /**
     * Checks for data flow issues (floating inputs, etc.)
     */
    private void validateDataFlow() {
        // Build set of all driven nodes (nodes that are output of some component)
        Set<Integer> drivenNodes = new HashSet<>();
        for (Component comp : circuit.getComponents()) {
            for (Node node : comp.getOutputNodes()) {
                drivenNodes.add(node.getNodeNumber());
            }
        }

        // Check that all connection source nodes are actually driven
        for (Connection conn : circuit.getConnections()) {
            int fromNodeNum = conn.getFromNode().getNodeNumber();
            if (!drivenNodes.contains(fromNodeNum)) {
                warnings.add("Connection source Node " + fromNodeNum + " is not driven by any component output");
            }
        }

        // Check for feedback loops (output connected to same component's input)
        for (Connection conn : circuit.getConnections()) {
            Component fromComp = circuit.getComponentForNode(conn.getFromNode());
            Component toComp = circuit.getComponentForNode(conn.getToNode());
            if (fromComp != null && fromComp == toComp) {
                warnings.add("Feedback loop detected: " + fromComp.getName() + 
                            " output connected to its own input (not supported in combinational circuits)");
            }
        }
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /**
     * Returns the number of short circuits detected
     */
    public int getShortCircuitCount() {
        int count = 0;
        for (String error : errors) {
            if (error.startsWith("SHORT CIRCUIT")) {
                count++;
            }
        }
        return count;
    }

    public String getValidationReport() {
        StringBuilder report = new StringBuilder();
        
        if (errors.isEmpty() && warnings.isEmpty()) {
            report.append("✓ Circuit is VALID - No errors or warnings\n");
        } else {
            if (!errors.isEmpty()) {
                report.append("✗ ERRORS (").append(errors.size()).append("):\n");
                for (String error : errors) {
                    report.append("  ✗ ").append(error).append("\n");
                }
            }
            if (!warnings.isEmpty()) {
                report.append("⚠ WARNINGS (").append(warnings.size()).append("):\n");
                for (String warning : warnings) {
                    report.append("  ⚠ ").append(warning).append("\n");
                }
            }
        }
        
        // Summary
        report.append("\n--- Summary ---\n");
        report.append("Components: ").append(circuit.getTotalComponents()).append("\n");
        report.append("Connections: ").append(circuit.getConnections().size()).append("\n");
        report.append("Errors: ").append(errors.size()).append("\n");
        report.append("Warnings: ").append(warnings.size()).append("\n");
        
        return report.toString();
    }
}
