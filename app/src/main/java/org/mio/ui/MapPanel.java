package org.mio.ui;

import org.mio.geo.CoordinateMapper;
import org.mio.graph.Graph;
import org.mio.model.Stop;
import org.mio.visual.MapRenderer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;


public class MapPanel extends JPanel {

    private static final double ZOOM_FACTOR = 1.1;
    private static final int CLICK_RADIUS = 10;

    private final Graph graph;
    private final List<Stop> stops;
    private final CoordinateMapper mapper;
    private final MapRenderer renderer;
    private final Consumer<Stop> onStopSelected;

    private Image baseMap;
    private AffineTransform worldTransform = new AffineTransform();
    private Point dragStartScreen;

    private Integer filterLineId = null;
    private Integer filterOrientation = null;
    private Stop selectedStop = null;


    public MapPanel(Graph graph, List<Stop> stops, Consumer<Stop> onStopSelected) {
        this.graph = graph;
        this.stops = stops;
        this.onStopSelected = onStopSelected;
        this.renderer = new MapRenderer();

        loadMapImage();

        int mapWidth = (baseMap != null) ? baseMap.getWidth(null) : 800;
        int mapHeight = (baseMap != null) ? baseMap.getHeight(null) : 600;
        this.mapper = new CoordinateMapper(stops, mapWidth, mapHeight);

        setupMouseListeners();
    }


    private void loadMapImage() {
        try {
            URL mapUrl = getClass().getResource("/map/cali-map.png");
            if (mapUrl == null) {
                System.err.println("Error: Map image not found at /map/cali-map.png");
                baseMap = null;
            } else {
                baseMap = ImageIO.read(mapUrl);
            }
        } catch (IOException e) {
            System.err.println("Error loading map image: " + e.getMessage());
            baseMap = null;
        }
    }


    private void setupMouseListeners() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartScreen = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                pan(e.getPoint());
                dragStartScreen = e.getPoint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoom(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    selectStopAt(e.getPoint());
                }
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        addMouseWheelListener(mouseAdapter);
    }


    private void pan(Point dragEndScreen) {
        int dx = dragEndScreen.x - dragStartScreen.x;
        int dy = dragEndScreen.y - dragStartScreen.y;
        worldTransform.preConcatenate(AffineTransform.getTranslateInstance(dx, dy));
        repaint();
    }


    private void zoom(MouseWheelEvent e) {
        double scale = (e.getWheelRotation() < 0) ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;
        Point2D p = e.getPoint();

        AffineTransform at = new AffineTransform();
        at.translate(p.getX(), p.getY());
        at.scale(scale, scale);
        at.translate(-p.getX(), -p.getY());
        
        worldTransform.preConcatenate(at);
        repaint();
    }


    private void selectStopAt(Point clickPoint) {
        try {
            Point2D worldPoint = worldTransform.inverseTransform(clickPoint, null);
            Stop closestStop = null;
            double minDistanceSq = CLICK_RADIUS * CLICK_RADIUS;

            for (Stop stop : stops) {
                int stopX = mapper.toX(stop.getLon());
                int stopY = mapper.toY(stop.getLat());
                double distSq = worldPoint.distanceSq(stopX, stopY);

                if (distSq < minDistanceSq) {
                    minDistanceSq = distSq;
                    closestStop = stop;
                }
            }

            this.selectedStop = closestStop;
            onStopSelected.accept(selectedStop);
            repaint();

        } catch (NoninvertibleTransformException ex) {
            System.err.println("Error handling click: " + ex.getMessage());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.transform(worldTransform);

        if (baseMap != null) {
            g2d.drawImage(baseMap, 0, 0, null);
        } else {
            g2d.setColor(Color.GRAY);
            g2d.fillRect(0, 0, 800, 600); // Default size if no map
            g2d.setColor(Color.WHITE);
            g2d.drawString("Map image not found", 100, 100);
        }

        renderer.render(g2d, graph, mapper, selectedStop, filterLineId, filterOrientation);

        g2d.dispose();
    }


    public void setLineFilter(Integer lineId) {
        this.filterLineId = lineId;
        repaint();
    }

    public void setOrientationFilter(Integer orientation) {
        this.filterOrientation = orientation;
        repaint();
    }
}
