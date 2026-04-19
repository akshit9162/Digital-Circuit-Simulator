# Digital Circuit Simulator v2.0 - Quick Start Guide

## Prerequisites
- **Java Development Kit (JDK)** 8 or higher installed
- `javac` and `java` commands available in your terminal (added to system PATH)

## Compile & Run (Easiest Way)
**Double-click `compile.bat`** — it compiles everything and launches the app.

Or from a terminal:
```
cd C:\Users\Kabir Garg\Downloads\oops_prj_trial2\oops_prj_trial
compile.bat
```

## Manual Compile & Run
```bash
# Create output directory
mkdir bin

# Compile all Java files
javac -d bin -sourcepath src src/com/circuitsimulator/gui/MainWindow.java

# Run the application
java -cp bin com.circuitsimulator.gui.MainWindow
```

## How to Use the Simulator

### 1. Add Components
- Click any component in the **left sidebar** (Switch, LED, AND, OR, NOT, XOR, NAND, NOR, XNOR, VCC, Ground)
- Enter a name when prompted
- Component is placed automatically (no overlap)
- A dialog shows the component's **node numbers** — note these for connections

### 2. Connect Components (Two Methods)

**Method A — By Node Number (recommended):**
1. Click **"Connect Nodes"** button in the toolbar
2. See the list of all available nodes with their numbers
3. Enter the **Output node #** (source) and **Input node #** (destination)
4. Click OK — a wire is drawn between the nodes

**Method B — By Dragging:**
1. **Right-click** on an output port (red circle on right side of component)
2. **Drag** to an input port (blue circle on left side of another component)
3. Release — connection is made

### 3. Simulate
- **Click a Switch** to toggle it ON/OFF — circuit auto-simulates
- Click **"Simulate"** button or press **F5** to manually simulate
- LEDs turn **bright red** when ON, **gray** when OFF
- Wire colors change: **red** = carrying HIGH signal, **blue** = LOW

### 4. Generate Truth Table
- Click **"Truth Table"** button in the toolbar
- Shows all possible input combinations and their outputs
- Uses component names as column headers

### 5. Validate & Check Short Circuits
- **Circuit → Validate** — checks for errors and warnings
- **Circuit → Check Short Circuits** — detects VCC-to-GND paths and conflicting signals
- Status bar shows short circuit count (green = safe, red = detected)

### 6. Save & Load
- **Ctrl+S** or File → Save — saves as `.cir` file
- **Ctrl+O** or File → Open — loads a saved circuit
- File → Export as Text — exports human-readable circuit description

## Keyboard Shortcuts
| Key | Action |
|-----|--------|
| Ctrl+N | New Circuit |
| Ctrl+O | Open Circuit |
| Ctrl+S | Save Circuit |
| F5 | Simulate |
| Delete | Delete selected component or wire |

## Understanding the Display
- **Blue circles** (left of components) = Input ports
- **Red circles** (right of components) = Output ports  
- **Numbers above ports** = Node numbers (use these for connections)
- **0/1 inside ports** = Current logic state
- Wire colors: **Red** = HIGH (1), **Blue** = LOW (0)

## Auto-Connection Rules
- Unconnected LED inputs are automatically connected to **Ground** (stays OFF)
- Unconnected gate inputs default to **Ground** (LOW)
- Switch outputs when closed = **VCC** (HIGH)

## Available Components
| Component | Inputs | Outputs | Description |
|-----------|--------|---------|-------------|
| Switch | 0 | 1 | Interactive toggle (click to switch ON/OFF) |
| LED | 1 | 0 | Visual indicator (red=ON, gray=OFF) |
| VCC | 0 | 1 | Always outputs HIGH (1) |
| Ground | 0 | 1 | Always outputs LOW (0) |
| AND | 2 | 1 | Output = 1 only when ALL inputs = 1 |
| OR | 2 | 1 | Output = 1 when ANY input = 1 |
| NOT | 1 | 1 | Output = inverse of input |
| XOR | 2 | 1 | Output = 1 when inputs differ |
| NAND | 2 | 1 | NOT(AND) — universal gate |
| NOR | 2 | 1 | NOT(OR) — universal gate |
| XNOR | 2 | 1 | NOT(XOR) — outputs 1 when inputs are same |
