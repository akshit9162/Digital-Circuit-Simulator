package com.circuitsimulator;

import com.circuitsimulator.circuit.Circuit;
import com.circuitsimulator.components.*;
import java.util.List;

/**
 * Headless tests for the simulation core (no Swing window). Dependency-free:
 *
 *   make test
 *
 * Covers every gate's truth table, multi-input gates, end-to-end propagation through a circuit
 * (switches -> gates -> LEDs), a two-level half adder, and short-circuit detection.
 */
public class SimulatorTest {
    private static int passed = 0, failed = 0;

    private static void check(boolean ok, String what) {
        if (ok) passed++;
        else { failed++; System.out.println("FAIL: " + what); }
    }

    /** Drive a gate's inputs directly and read its output. */
    private static boolean eval(LogicGate g, boolean... in) {
        for (int i = 0; i < in.length; i++) g.getInputNode(i).setState(in[i]);
        g.evaluate();
        return g.getOutputNode(0).getState();
    }

    private static void truthTable(String name, LogicGate g, boolean f00, boolean f01, boolean f10, boolean f11) {
        check(eval(g, false, false) == f00, name + "(0,0)");
        check(eval(g, false, true) == f01, name + "(0,1)");
        check(eval(g, true, false) == f10, name + "(1,0)");
        check(eval(g, true, true) == f11, name + "(1,1)");
    }

    static void gateTruthTables() {
        truthTable("AND", new ANDGate("g", "AND", 2), false, false, false, true);
        truthTable("OR", new ORGate("g", "OR", 2), false, true, true, true);
        truthTable("XOR", new XORGate("g", "XOR", 2), false, true, true, false);
        truthTable("NAND", new NANDGate("g", "NAND", 2), true, true, true, false);
        truthTable("NOR", new NORGate("g", "NOR", 2), true, false, false, false);
        truthTable("XNOR", new XNORGate("g", "XNOR", 2), true, false, false, true);
        NOTGate not = new NOTGate("g", "NOT");
        check(eval(not, false), "NOT(0)");
        check(!eval(not, true), "NOT(1)");
    }

    static void multiInputGates() {
        ANDGate and3 = new ANDGate("g", "AND3", 3);
        check(eval(and3, true, true, true), "AND3(1,1,1)");
        check(!eval(and3, true, false, true), "AND3(1,0,1)");
        XORGate xor3 = new XORGate("g", "XOR3", 3);  // odd parity
        check(eval(xor3, true, true, true), "XOR3(1,1,1) = 1");
        check(!eval(xor3, true, true, false), "XOR3(1,1,0) = 0");
    }

    /** Two switches -> AND gate -> LED, simulated through the Circuit. */
    static void switchesThroughGateToLed() {
        for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) {
            Circuit c = new Circuit("and");
            Switch sa = new Switch("sa", "A"), sb = new Switch("sb", "B");
            ANDGate and = new ANDGate("g", "AND", 2);
            LED led = new LED("l", "Y");
            for (Component comp : new Component[]{sa, sb, and, led}) c.addComponent(comp);
            c.connect(sa.getOutputNode(0), and.getInputNode(0));
            c.connect(sb.getOutputNode(0), and.getInputNode(1));
            c.connect(and.getOutputNode(0), led.getInputNode(0));
            sa.setState(a == 1);
            sb.setState(b == 1);
            c.simulate();
            check(led.isLit() == (a == 1 && b == 1), "switches " + a + "," + b + " -> AND -> LED");
        }
    }

    /** Half adder: sum = A XOR B, carry = A AND B (two outputs, shared inputs). */
    static void halfAdder() {
        for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) {
            Circuit c = new Circuit("half-adder");
            Switch sa = new Switch("sa", "A"), sb = new Switch("sb", "B");
            XORGate xor = new XORGate("x", "XOR", 2);
            ANDGate and = new ANDGate("n", "AND", 2);
            LED sum = new LED("s", "SUM"), carry = new LED("c", "CARRY");
            for (Component comp : new Component[]{sa, sb, xor, and, sum, carry}) c.addComponent(comp);
            c.connect(sa.getOutputNode(0), xor.getInputNode(0));
            c.connect(sb.getOutputNode(0), xor.getInputNode(1));
            c.connect(sa.getOutputNode(0), and.getInputNode(0));
            c.connect(sb.getOutputNode(0), and.getInputNode(1));
            c.connect(xor.getOutputNode(0), sum.getInputNode(0));
            c.connect(and.getOutputNode(0), carry.getInputNode(0));
            sa.setState(a == 1);
            sb.setState(b == 1);
            c.simulate();
            check(sum.isLit() == ((a ^ b) == 1), "half adder sum " + a + "+" + b);
            check(carry.isLit() == (a == 1 && b == 1), "half adder carry " + a + "+" + b);
        }
    }

    static void shortCircuitDetection() {
        Circuit bad = new Circuit("short");
        VCC vcc = new VCC("v", "VCC");
        Ground gnd = new Ground("g", "GND");
        bad.addComponent(vcc);
        bad.addComponent(gnd);
        bad.connect(vcc.getOutputNode(0), gnd.getOutputNode(0));
        List<String> shorts = bad.detectShortCircuits();
        check(!shorts.isEmpty(), "VCC wired to GND is reported as a short");

        Circuit ok = new Circuit("ok");
        Switch s = new Switch("s", "A");
        LED led = new LED("l", "Y");
        ok.addComponent(s);
        ok.addComponent(led);
        ok.connect(s.getOutputNode(0), led.getInputNode(0));
        check(ok.detectShortCircuits().isEmpty(), "switch -> LED has no short");
    }

    public static void main(String[] args) {
        gateTruthTables();
        multiInputGates();
        switchesThroughGateToLed();
        halfAdder();
        shortCircuitDetection();
        System.out.println(passed + "/" + (passed + failed) + " checks passed");
        if (failed > 0) System.exit(1);
    }
}
