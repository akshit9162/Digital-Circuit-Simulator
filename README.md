# Digital Circuit Simulator

**[→ Visual walkthrough](https://akshit9162.github.io/Digital-Circuit-Simulator/)**

A Java Swing application for designing and simulating digital logic circuits: place switches,
gates and LEDs, wire them together, and watch signals propagate. It also generates truth tables
and flags short circuits. Built as a team object-oriented programming project.

## Features

- **Components:** switches, LEDs, VCC, ground, and AND, OR, NOT, XOR, NAND, NOR, XNOR gates
  (multi-input gates take 2 or more inputs)
- **Simulation:** signals propagate from switches through any depth of combinational logic to the
  LEDs, re-evaluated until the circuit is stable
- **Validation:** short-circuit detection: VCC reaching ground, and two outputs driving
  conflicting values into one input
- **Truth tables** for every input combination
- **Save / load** circuits to files, and export circuit details as text

## Design

```
com.circuitsimulator
├── components/   Component (abstract) → Switch, LED, VCC, Ground, LogicGate (abstract) → 7 gates
├── circuit/      Circuit (graph of components, nodes and connections), Node, Connection, Wire
├── validator/    CircuitValidator, TruthTableGenerator
├── file/         CircuitFileManager (serialisation)
└── gui/          MainWindow, CircuitPanel (Swing, drag-and-drop canvas)
```

- **Inheritance and polymorphism:** every part is a `Component` with its own `evaluate()`; gates
  share their Boolean logic through the abstract `LogicGate`.
- **Simulation core is independent of the GUI,** so it is tested headlessly.
- **Iterative convergence:** `Circuit.simulate()` alternates evaluating sources, propagating signals
  along connections, and evaluating gates, until no node changes. That handles multi-level
  circuits without needing a topological sort.

## Build, run, test

Requires JDK 17 or newer.

```bash
make run     # compile and launch the GUI
make test    # headless tests: 44 checks
```

On Windows without `make`: `compile.bat`, then `build_and_run.bat`.

## Tests

`test/com/circuitsimulator/SimulatorTest.java` runs without a window or any test framework:

- the truth table of every gate
- 3-input gates (AND, XOR parity)
- end-to-end propagation: switches → AND gate → LED, over all input combinations
- a two-level half adder (sum = A XOR B, carry = A AND B)
- short-circuit detection: VCC wired to ground is flagged, and a valid circuit is not

The tests found that multi-input gates ignored their requested input count and always built two
inputs. That is fixed, and the GUI still uses two-input gates by default.
