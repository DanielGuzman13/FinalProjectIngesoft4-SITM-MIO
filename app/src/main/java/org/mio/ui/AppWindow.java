package org.mio.ui;

import org.mio.graph.Graph;
import org.mio.graph.GraphBuilder;
import org.mio.model.Stop;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class AppWindow extends JFrame {

    private final MapPanel mapPanel;
    private final InfoPanel infoPanel;
    private final ControlPanel controlPanel;


    public AppWindow() {
        setTitle("Visualizador SITM-MIO Cali");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);

        GraphBuilder graphBuilder = new GraphBuilder();
        Graph graph = graphBuilder.build();
        List<Stop> stops = new ArrayList<>(graphBuilder.getStops());

        infoPanel = new InfoPanel();
        mapPanel = new MapPanel(graph, stops, infoPanel::setStop); // Pass stop selection handler
        controlPanel = new ControlPanel(graph, mapPanel::setLineFilter, mapPanel::setOrientationFilter);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(mapPanel, BorderLayout.CENTER);
        contentPane.add(controlPanel, BorderLayout.WEST);
        contentPane.add(infoPanel, BorderLayout.EAST);
    }
}
