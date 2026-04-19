package com.circuitsimulator.circuit;

import com.circuitsimulator.components.*;
import java.io.Serializable;
import java.util.*;

/**
 * Manages the entire circuit: components, nodes, and connections.
 * Supports multi-level combinational circuit simulation with
 * iterative signal propagation and auto-connection of open terminals.
 */
public class Circuit implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String circuitName;
    private List<Component> components;
    private List<Connection> connections;
    private Map<Integer, Node> nodesMap; // Map of node number to Node object

    public Circuit(String name) {
        this.circuitName = name;
        this.components = new ArrayList<>();
        this.connections = new ArrayList<>();
        this.nodesMap = new HashMap<>();
    }

    /**
     * Creates a new node in the circuit
     */
    public Node createNode(int nodeNumber) {
        if (nodesMap.containsKey(nodeNumber)) {
            return nodesMap.get(nodeNumber);
        }
        Node node = new Node(nodeNumber);
        nodesMap.put(nodeNumber, node);
        return node;
    }

    /**
     * Gets a node by its number
     */
    public Node getNode(int nodeNumber) {
        return nodesMap.get(nodeNumber);
    }

    /**
     * Adds a component to the circuit
     */
    public void addComponent(Component component) {
        if (!components.contains(component)) {
            components.add(component);
            // Register all component nodes in the nodesMap
            for (Node n : component.getInputNodes()) {
                nodesMap.put(n.getNodeNumber(), n);
            }
            for (Node n : component.getOutputNodes()) {
                nodesMap.put(n.getNodeNumber(), n);
            }
        }
    }

    /**
     * Removes a component from the circuit
     */
    public void removeComponent(Component component) {
        components.remove(component);
        // Remove connections related to this component
        connections.removeIf(conn -> 
            isNodeBelongingToComponent(conn.getFromNode(), component) ||
            isNodeBelongingToComponent(conn.getToNode(), component)
        );
        // Remove nodes from map
        for (Node n : component.getInputNodes()) {
            nodesMap.remove(n.getNodeNumber());
        }
        for (Node n : component.getOutputNodes()) {
            nodesMap.remove(n.getNodeNumber());
        }
    }

    /**
     * Connects two nodes with a connection
     */
    public void connect(Node fromNode, Node toNode) {
        connect(fromNode, toNode, 0); // default resistance 0
    }

    public void connect(Node fromNode, Node toNode, double resistance) {
        Connection conn = new Connection(fromNode, toNode, resistance);
        if (!connections.contains(conn)) {
            connections.add(conn);
        }
    }

    /**
     * Removes a connection from the circuit
     */
    public void removeConnection(Connection connection) {
        connections.remove(connection);
    }

    /**
     * Auto-connects open terminals as per the specification:
     * - Open LED input connections are automatically connected to ground (LOW)
     * - Open Switch output connections default to VCC (handled by Switch itself)
     * 
     * This is called before simulation to ensure correct behavior.
     */
    private void autoConnectOpenTerminals() {
        // Build set of destination nodes that have connections coming in
        Set<Integer> connectedInputNodes = new HashSet<>();
        for (Connection conn : connections) {
            connectedInputNodes.add(conn.getToNode().getNodeNumber());
        }

        // For LEDs: if input node is not connected to anything, set it to GND (LOW)
        for (Component comp : components) {
            if (comp instanceof LED) {
                for (Node inputNode : comp.getInputNodes()) {
                    if (!connectedInputNodes.contains(inputNode.getNodeNumber())) {
                        inputNode.setState(false); // Auto-connect to ground
                    }
                }
            }
        }

        // For logic gates: unconnected inputs default to LOW (ground)
        for (Component comp : components) {
            if (comp instanceof LogicGate) {
                for (Node inputNode : comp.getInputNodes()) {
                    if (!connectedInputNodes.contains(inputNode.getNodeNumber())) {
                        inputNode.setState(false); // Auto-connect to ground
                    }
                }
            }
        }

        // Switch outputs when open: default to VCC (HIGH) when switch is closed
        // This is already handled by Switch.evaluate() - when closed -> HIGH output
    }

    /**
     * Propagates signal through the circuit using iterative convergence.
     * Handles multi-level combinational circuits by running multiple
     * propagate-evaluate passes until the circuit reaches a stable state.
     */
    public void simulate() {
        // Auto-connect open terminals first
        autoConnectOpenTerminals();

        // Iterative convergence: run propagate→evaluate passes 
        // until circuit stabilizes or max iterations reached.
        // Max iterations = number of components + 2 (sufficient for any combinational circuit)
        int maxIter = components.size() + 2;
        if (maxIter < 5) maxIter = 5;
        
        for (int iter = 0; iter < maxIter; iter++) {
            boolean changed = false;
            
            // Step 1: Evaluate all source components (Switches, VCC, Ground)
            for (Component comp : components) {
                if (comp instanceof Switch || comp instanceof VCC || comp instanceof Ground) {
                    comp.evaluate();
                }
            }
            
            // Step 2: Propagate signals through connections
            for (Connection conn : connections) {
                Node sourceNode = conn.getFromNode();
                Node destNode = conn.getToNode();
                boolean oldState = destNode.getState();
                destNode.setState(sourceNode.getState());
                if (oldState != destNode.getState()) {
                    changed = true;
                }
            }
            
            // Step 3: Evaluate all logic gates
            for (Component comp : components) {
                if (comp instanceof LogicGate) {
                    boolean oldOutput = false;
                    if (!comp.getOutputNodes().isEmpty()) {
                        oldOutput = comp.getOutputNodes().get(0).getState();
                    }
                    comp.evaluate();
                    if (!comp.getOutputNodes().isEmpty() && 
                        oldOutput != comp.getOutputNodes().get(0).getState()) {
                        changed = true;
                    }
                }
            }
            
            // Step 4: Propagate gate outputs through connections
            for (Connection conn : connections) {
                Node sourceNode = conn.getFromNode();
                Node destNode = conn.getToNode();
                boolean oldState = destNode.getState();
                destNode.setState(sourceNode.getState());
                if (oldState != destNode.getState()) {
                    changed = true;
                }
            }
            
            // Step 5: Evaluate outputs (LEDs)
            for (Component comp : components) {
                if (comp instanceof LED) {
                    comp.evaluate();
                }
            }
            
            // If nothing changed, circuit has stabilized
            if (!changed && iter > 0) {
                break;
            }
        }
    }

    /**
     * Checks for short circuits in the circuit.
     * A short circuit occurs when:
     * 1. VCC output is directly connected to GND output (0 resistance path)
     * 2. Two outputs driving conflicting values are connected to the same input
     * 
     * @return list of short circuit descriptions, empty if none found
     */
    public List<String> detectShortCircuits() {
        List<String> shortCircuits = new ArrayList<>();
        
        // Check 1: Direct VCC to GND connections
        Set<Integer> vccNodes = new HashSet<>();
        Set<Integer> gndNodes = new HashSet<>();
        
        for (Component comp : components) {
            if (comp instanceof VCC || (comp instanceof Switch && ((Switch) comp).isClosed())) {
                for (Node n : comp.getOutputNodes()) {
                    vccNodes.add(n.getNodeNumber());
                }
            }
            if (comp instanceof Ground) {
                for (Node n : comp.getOutputNodes()) {
                    gndNodes.add(n.getNodeNumber());
                }
            }
        }
        
        // Trace VCC nodes through connections and check if they reach GND
        Set<Integer> reachableFromVCC = new HashSet<>(vccNodes);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Connection conn : connections) {
                int fromNum = conn.getFromNode().getNodeNumber();
                int toNum = conn.getToNode().getNodeNumber();
                if (reachableFromVCC.contains(fromNum) && !reachableFromVCC.contains(toNum)) {
                    reachableFromVCC.add(toNum);
                    changed = true;
                }
            }
        }
        
        for (Integer gndNode : gndNodes) {
            if (reachableFromVCC.contains(gndNode)) {
                shortCircuits.add("SHORT CIRCUIT: VCC is connected to GND through node " + gndNode + " (0 resistance path)");
            }
        }
        
        // Check 2: Two outputs connected to the same input with conflicting values
        Map<Integer, List<Connection>> inputConnections = new HashMap<>();
        for (Connection conn : connections) {
            int destNum = conn.getToNode().getNodeNumber();
            inputConnections.computeIfAbsent(destNum, k -> new ArrayList<>()).add(conn);
        }
        
        for (Map.Entry<Integer, List<Connection>> entry : inputConnections.entrySet()) {
            List<Connection> conns = entry.getValue();
            if (conns.size() > 1) {
                boolean hasHigh = false, hasLow = false;
                for (Connection conn : conns) {
                    if (conn.getFromNode().getState()) hasHigh = true;
                    else hasLow = true;
                }
                if (hasHigh && hasLow) {
                    shortCircuits.add("SHORT CIRCUIT: Conflicting signals driving node " + entry.getKey() + " (both HIGH and LOW)");
                }
            }
        }
        
        return shortCircuits;
    }

    /**
     * Gets all switches in the circuit
     */
    public List<Switch> getSwitches() {
        List<Switch> switches = new ArrayList<>();
        for (Component comp : components) {
            if (comp instanceof Switch) {
                switches.add((Switch) comp);
            }
        }
        return switches;
    }

    /**
     * Gets all LEDs in the circuit
     */
    public List<LED> getLEDs() {
        List<LED> leds = new ArrayList<>();
        for (Component comp : components) {
            if (comp instanceof LED) {
                leds.add((LED) comp);
            }
        }
        return leds;
    }

    /**
     * Gets all input nodes (from switches and input sources)
     */
    public List<Node> getInputNodes() {
        List<Node> inputs = new ArrayList<>();
        for (Component comp : components) {
            if (comp instanceof Switch) {
                List<Node> outputNodes = comp.getOutputNodes();
                if (!outputNodes.isEmpty()) {
                    inputs.add(outputNodes.get(0));
                }
            }
        }
        return inputs;
    }

    /**
     * Gets all output nodes (LEDs, etc.)
     */
    public List<Node> getOutputNodes() {
        List<Node> outputs = new ArrayList<>();
        for (Component comp : components) {
            if (comp instanceof LED) {
                List<Node> inputNodes = comp.getInputNodes();
                if (!inputNodes.isEmpty()) {
                    outputs.add(inputNodes.get(0));
                }
            }
        }
        return outputs;
    }

    /**
     * Finds a node by its number across all components
     */
    public Node findNodeByNumber(int nodeNumber) {
        // Check nodesMap first
        if (nodesMap.containsKey(nodeNumber)) {
            return nodesMap.get(nodeNumber);
        }
        // Also check all component nodes
        for (Component comp : components) {
            for (Node n : comp.getInputNodes()) {
                if (n.getNodeNumber() == nodeNumber) return n;
            }
            for (Node n : comp.getOutputNodes()) {
                if (n.getNodeNumber() == nodeNumber) return n;
            }
        }
        return null;
    }

    /**
     * Check if a node number exists in any component
     */
    public boolean hasNode(int nodeNumber) {
        return findNodeByNumber(nodeNumber) != null;
    }

    /**
     * Gets the component that owns a given node
     */
    public Component getComponentForNode(Node node) {
        for (Component comp : components) {
            if (comp.getInputNodes().contains(node) || comp.getOutputNodes().contains(node)) {
                return comp;
            }
        }
        return null;
    }

    /**
     * Checks if a node is an output node of its owning component
     */
    public boolean isOutputNode(Node node) {
        for (Component comp : components) {
            if (comp.getOutputNodes().contains(node)) return true;
        }
        return false;
    }

    /**
     * Checks if a node is an input node of its owning component
     */
    public boolean isInputNode(Node node) {
        for (Component comp : components) {
            if (comp.getInputNodes().contains(node)) return true;
        }
        return false;
    }

    public String getCircuitName() {
        return circuitName;
    }

    public void setCircuitName(String name) {
        this.circuitName = name;
    }

    public List<Component> getComponents() {
        return new ArrayList<>(components);
    }

    public List<Connection> getConnections() {
        return new ArrayList<>(connections);
    }

    public Map<Integer, Node> getAllNodes() {
        return new HashMap<>(nodesMap);
    }

    public int getTotalNodes() {
        return nodesMap.size();
    }

    public int getTotalComponents() {
        return components.size();
    }

    public void clear() {
        components.clear();
        connections.clear();
        nodesMap.clear();
    }

    private boolean isNodeBelongingToComponent(Node node, Component comp) {
        return comp.getInputNodes().contains(node) || comp.getOutputNodes().contains(node);
    }

    @Override
    public String toString() {
        return "Circuit[" + circuitName + ": " + components.size() + " components, " + 
               nodesMap.size() + " nodes]";
    }
}
