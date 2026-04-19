package com.circuitsimulator.circuit;

import java.awt.Point;
import java.io.Serializable;

/**
 * Represents a wire connecting two nodes in the circuit
 * A wire is an object-oriented representation of a connection for UI purposes
 */
public class Wire implements Serializable {
    private static final long serialVersionUID = 1L;
    private Connection connection;
    private Point startPoint;
    private Point endPoint;
    private boolean selected = false;
    private static int wireCount = 0;
    private int wireId;

    public Wire(Connection connection) {
        this.connection = connection;
        this.wireId = wireCount++;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setStartPoint(Point start) {
        this.startPoint = start;
    }

    public void setEndPoint(Point end) {
        this.endPoint = end;
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public int getWireId() {
        return wireId;
    }

    /**
     * Check if a point lies on or near this wire (for click detection)
     * Uses distance from point to line segment formula
     */
    public boolean contains(Point p, int tolerance) {
        if (startPoint == null || endPoint == null) {
            return false;
        }

        // Get line segment points
        double x1 = startPoint.x;
        double y1 = startPoint.y;
        double x2 = endPoint.x;
        double y2 = endPoint.y;

        // Get the point to test
        double px = p.x;
        double py = p.y;

        // Calculate the distance from point p to the line segment
        double distanceSquared = getDistanceSquaredToLineSegment(px, py, x1, y1, x2, y2);
        double distance = Math.sqrt(distanceSquared);

        return distance <= tolerance;
    }

    /**
     * Calculate squared distance from point (px, py) to line segment (x1,y1)-(x2,y2)
     */
    private double getDistanceSquaredToLineSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)));
        double closestX = x1 + t * dx;
        double closestY = y1 + t * dy;
        double distX = px - closestX;
        double distY = py - closestY;
        return distX * distX + distY * distY;
    }

    @Override
    public String toString() {
        return "Wire #" + wireId + " (" + connection.getFromNode().getLabel() + 
               " → " + connection.getToNode().getLabel() + ")";
    }
}
