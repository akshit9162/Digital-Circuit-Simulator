package com.circuitsimulator.validator;

import com.circuitsimulator.circuit.Circuit;
import com.circuitsimulator.components.Switch;
import com.circuitsimulator.components.LED;
import com.circuitsimulator.components.Component;
import java.util.*;

/**
 * Generates truth tables for the circuit with all possible input combinations.
 * Uses switch names as input headers and LED names as output headers.
 */
public class TruthTableGenerator {
    private Circuit circuit;

    public TruthTableGenerator(Circuit circuit) {
        this.circuit = circuit;
    }

    /**
     * Generates truth table with all possible input combinations
     */
    public String[][] generateTruthTable() {
        List<Switch> switches = getSwitches();
        List<LED> leds = getLEDs();
        
        if (switches.isEmpty()) {
            return new String[][] {{"No switches in circuit"}};
        }
        if (leds.isEmpty()) {
            return new String[][] {{"No LEDs in circuit"}};
        }

        int inputCount = switches.size();
        int outputCount = leds.size();
        int rowCount = (int) Math.pow(2, inputCount);
        
        // Limit to prevent excessive computation (max 1024 rows = 10 inputs)
        if (inputCount > 10) {
            return new String[][] {{"Too many inputs (" + inputCount + "). Max 10 supported."}};
        }
        
        String[][] truthTable = new String[rowCount + 1][inputCount + outputCount];

        // Header row - use component names
        int col = 0;
        for (int i = 0; i < inputCount; i++) {
            String name = switches.get(i).getName();
            // Truncate long names
            if (name.length() > 10) name = name.substring(0, 10);
            truthTable[0][col++] = name;
        }
        for (int i = 0; i < outputCount; i++) {
            String name = leds.get(i).getName();
            if (name.length() > 10) name = name.substring(0, 10);
            truthTable[0][col++] = name;
        }

        // Save original switch states to restore after
        boolean[] originalStates = new boolean[inputCount];
        for (int i = 0; i < inputCount; i++) {
            originalStates[i] = switches.get(i).isClosed();
        }

        // Truth table rows
        for (int row = 1; row <= rowCount; row++) {
            col = 0;
            
            // Set switch states based on binary representation
            for (int i = 0; i < inputCount; i++) {
                boolean bit = ((row - 1) & (1 << (inputCount - 1 - i))) != 0;
                switches.get(i).setState(bit);
                truthTable[row][col++] = bit ? "1" : "0";
            }

            // Simulate the circuit
            circuit.simulate();

            // Get output values from LEDs
            for (LED led : leds) {
                truthTable[row][col++] = led.isLit() ? "1" : "0";
            }
        }

        // Restore original switch states
        for (int i = 0; i < inputCount; i++) {
            switches.get(i).setState(originalStates[i]);
        }
        circuit.simulate(); // Re-simulate with original states

        return truthTable;
    }

    /**
     * Returns truth table as formatted string
     */
    public String getTruthTableAsString() {
        String[][] table = generateTruthTable();
        StringBuilder sb = new StringBuilder();

        for (String[] row : table) {
            for (String cell : row) {
                sb.append(String.format("%12s", cell != null ? cell : ""));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Returns truth table as formatted text with borders
     */
    public String getTruthTableFormatted() {
        String[][] table = generateTruthTable();
        
        if (table.length <= 1 && table[0].length == 1) {
            return table[0][0]; // Error message
        }
        
        StringBuilder sb = new StringBuilder();

        int cols = table[0].length;
        int[] colWidths = new int[cols];

        // Calculate column widths
        for (String[] row : table) {
            for (int i = 0; i < row.length; i++) {
                if (row[i] != null) {
                    colWidths[i] = Math.max(colWidths[i], row[i].length());
                }
            }
        }
        // Minimum width of 5
        for (int i = 0; i < colWidths.length; i++) {
            colWidths[i] = Math.max(colWidths[i], 5);
        }

        // Title
        sb.append("TRUTH TABLE\n");
        sb.append("Circuit: ").append(circuit.getCircuitName()).append("\n");
        sb.append("Inputs: ").append(getSwitches().size());
        sb.append(" | Outputs: ").append(getLEDs().size()).append("\n\n");

        // Print header
        printSeparator(sb, colWidths);
        printRow(sb, table[0], colWidths);
        printSeparator(sb, colWidths);

        // Print data rows
        for (int i = 1; i < table.length; i++) {
            printRow(sb, table[i], colWidths);
        }
        printSeparator(sb, colWidths);
        
        sb.append("\nTotal rows: ").append(table.length - 1);
        sb.append(" (2^").append(getSwitches().size()).append(" combinations)\n");

        return sb.toString();
    }

    private void printSeparator(StringBuilder sb, int[] colWidths) {
        for (int width : colWidths) {
            sb.append("+");
            for (int i = 0; i < width + 2; i++) {
                sb.append("-");
            }
        }
        sb.append("+\n");
    }

    private void printRow(StringBuilder sb, String[] row, int[] colWidths) {
        for (int i = 0; i < row.length; i++) {
            sb.append("| ");
            String cell = row[i] != null ? row[i] : "";
            // Center the content
            int padding = colWidths[i] - cell.length();
            int leftPad = padding / 2;
            int rightPad = padding - leftPad;
            for (int j = 0; j < leftPad; j++) sb.append(" ");
            sb.append(cell);
            for (int j = 0; j < rightPad; j++) sb.append(" ");
            sb.append(" ");
        }
        sb.append("|\n");
    }

    private List<Switch> getSwitches() {
        List<Switch> switches = new ArrayList<>();
        for (Component comp : circuit.getComponents()) {
            if (comp instanceof Switch) {
                switches.add((Switch) comp);
            }
        }
        return switches;
    }

    private List<LED> getLEDs() {
        List<LED> leds = new ArrayList<>();
        for (Component comp : circuit.getComponents()) {
            if (comp instanceof LED) {
                leds.add((LED) comp);
            }
        }
        return leds;
    }
}
