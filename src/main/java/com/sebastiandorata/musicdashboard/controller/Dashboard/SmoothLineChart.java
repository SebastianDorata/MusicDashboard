package com.sebastiandorata.musicdashboard.controller.Dashboard;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;

import java.util.ArrayList;
import java.util.List;

//Ref: https://hawkesy.blogspot.com/2010/05/catmull-rom-spline-curve-implementation.html
// https://dl.acm.org/doi/epdf/10.1145/378456.378511
//https://stackoverflow.com/questions/9489736/catmull-rom-curve-with-no-cusps-and-no-self-intersections
public class SmoothLineChart extends LineChart<String, Number> {

    private static final double TENSION = 0.9;

    private static final boolean DEBUG = false;

    public SmoothLineChart(CategoryAxis xAxis, NumberAxis yAxis) {
        super(xAxis, yAxis);
        if (DEBUG) System.err.println("[SmoothLineChart] Constructor called — new class is loaded");
    }

    /**
     * Time Complexity: O(s * n) where s = number of series, n = data points per series
     * Space Complexity: O(n) per series for the point list
     */
    @Override
    protected void layoutPlotChildren() {
        super.layoutPlotChildren();

        if (DEBUG) System.err.println("[SmoothLineChart] layoutPlotChildren — series count: " + getData().size());

        for (Series<String, Number> series : getData()) {
            smoothSeriesPath(series);
        }
    }

    private void smoothSeriesPath(Series<String, Number> series) {
        Node seriesNode = series.getNode();

        if (DEBUG) System.err.println("[SmoothLineChart] seriesNode type: " +
                (seriesNode == null ? "NULL" : seriesNode.getClass().getSimpleName()));

        Path linePath = null;

        // Handle different possible node structures
        if (seriesNode instanceof Path) {
            linePath = (Path) seriesNode;
            if (DEBUG) System.err.println("[SmoothLineChart] Series node is directly a Path");
        } else if (seriesNode instanceof Group) {
            linePath = extractLinePath((Group) seriesNode);
            if (DEBUG) System.err.println("[SmoothLineChart] Extracted Path from Group");
        } else {
            if (DEBUG) System.err.println("[SmoothLineChart] EXIT — unexpected node type: " +
                    (seriesNode == null ? "NULL" : seriesNode.getClass().getSimpleName()));
            return;
        }

        if (linePath == null) {
            if (DEBUG) System.err.println("[SmoothLineChart] EXIT — no Path found");
            return;
        }

        List<double[]> points = collectPointsFromPath(linePath);

        if (DEBUG) System.err.println("[SmoothLineChart] points collected: " + points.size());

        if (points.size() < 2) {
            if (DEBUG) System.err.println("[SmoothLineChart] EXIT — fewer than 2 points");
            return;
        }

        rebuildWithCubicBezier(linePath, points);

        if (DEBUG) System.err.println("[SmoothLineChart] DONE — path elements after: " +
                linePath.getElements().size());
    }

    private Path extractLinePath(Group group) {
        for (Node child : group.getChildren()) {
            if (child instanceof Path) {
                return (Path) child;
            }
        }
        return null;
    }

    /**
     * Time Complexity: O(n) where n = path elements
     * Space Complexity: O(n)
     */
    private List<double[]> collectPointsFromPath(Path path) {
        List<double[]> points = new ArrayList<>();
        for (PathElement element : path.getElements()) {
            if (element instanceof MoveTo) {
                MoveTo mt = (MoveTo) element;
                points.add(new double[]{mt.getX(), mt.getY()});
            } else if (element instanceof LineTo) {
                LineTo lt = (LineTo) element;
                points.add(new double[]{lt.getX(), lt.getY()});
            }
        }
        return points;
    }

    /**
     *   Catmull-Rom tangent at P_i:  T_i = (P_{i+1} - P_{i-1}) / 2
     *
     *   For segment P1 to P2 with neighbours P0 and P3:
     *     cp1 = P1 + TENSION * (P2 - P0) / 6
     *     cp2 = P2 - TENSION * (P3 - P1) / 6
     *
     *   The /6 factor comes from the Hermite-to-Bezier conversion:
     *   tangent scale = 1/3 of segment in Bezier form,
     *   combined with the 1/2 from Catmull-Rom's tangent formula.
     *
     *   TENSION < 1 shortens the tangent vectors, pulling the Bezier
     *   handles closer to the data point and preventing overshooting
     *   at peaks and valleys.
     *
     * Time Complexity: O(n). One CubicCurveTo per segment, no inner loop
     * Space Complexity: O(1). Path modified in-place
     */
    private void rebuildWithCubicBezier(Path path, List<double[]> points) {
        path.getElements().clear();
        path.getElements().add(new MoveTo(points.get(0)[0], points.get(0)[1]));

        int last = points.size() - 1;

        for (int i = 0; i < last; i++) {
            // Zero tangent contribution at the first and last point,which prevents the curve from curving away from the edge data.
            double[] p0 = (i == 0)        ? points.get(0)    : points.get(i - 1);
            double[] p1 = points.get(i);
            double[] p2 = points.get(i + 1);
            double[] p3 = (i + 1 == last) ? points.get(last) : points.get(i + 2);

            // Bezier control points derived from tension-scaled Catmull-Rom tangents
            double cp1x = p1[0] + TENSION * (p2[0] - p0[0]) / 6.0;
            double cp1y = p1[1] + TENSION * (p2[1] - p0[1]) / 6.0;

            double cp2x = p2[0] - TENSION * (p3[0] - p1[0]) / 6.0;
            double cp2y = p2[1] - TENSION * (p3[1] - p1[1]) / 6.0;

            path.getElements().add(new CubicCurveTo(cp1x, cp1y, cp2x, cp2y, p2[0], p2[1]));
        }
    }
}

  /**   Troubleshooting Summary
        Issue: The SmoothLineChart class wasn't applying smooth curves to line charts.
        The code expected each series node to be a Group containing a Path, but in JavaFX 25 with this configuration, series nodes are directly Path objects.

        Correction: Modified the smoothSeriesPath() method to:
            - Check if seriesNode is a Path directly.
            - If it's a Group, extract the Path from children.
            - Log appropriate debug messages for each scenario

        Charts now successfully render with smooth Catmull-Rom spline interpolation using cubic Bezier curves.
*/
