package org.mio.ui;

import org.mio.graph.Graph;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class ControlPanel extends JPanel {

    private final JComboBox<String> lineComboBox;
    private final JComboBox<String> orientationComboBox;
    private final Consumer<Integer> onLineFilterChanged;
    private final Consumer<Integer> onOrientationFilterChanged;


    public ControlPanel(Graph graph, Consumer<Integer> onLineFilterChanged, Consumer<Integer> onOrientationFilterChanged) {
        this.onLineFilterChanged = onLineFilterChanged;
        this.onOrientationFilterChanged = onOrientationFilterChanged;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(200, 0));
        setBackground(Color.LIGHT_GRAY);

        add(createTitle("Controles"));
        add(Box.createRigidArea(new Dimension(0, 10)));

        add(new JLabel("Línea:"));
        lineComboBox = createLineComboBox(graph);
        lineComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        lineComboBox.addActionListener(e -> handleLineFilterChange());
        add(lineComboBox);

        add(Box.createRigidArea(new Dimension(0, 15)));

        add(new JLabel("Orientación:"));
        orientationComboBox = createOrientationComboBox();
        orientationComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        orientationComboBox.addActionListener(e -> handleOrientationFilterChange());
        add(orientationComboBox);

        add(Box.createVerticalGlue());
    }


    private JLabel createTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        return title;
    }


    private JComboBox<String> createLineComboBox(Graph graph) {
        List<String> lineIds = graph.getArcs().stream()
                .map(arc -> String.valueOf(arc.getLineId()))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        lineIds.add(0, "Todas las líneas");
        return new JComboBox<>(lineIds.toArray(new String[0]));
    }


    private JComboBox<String> createOrientationComboBox() {
        return new JComboBox<>(new String[]{"Ambas", "0", "1"});
    }


    private void handleLineFilterChange() {
        String selected = (String) lineComboBox.getSelectedItem();
        if ("Todas las líneas".equals(selected) || selected == null) {
            onLineFilterChanged.accept(null);
        } else {
            try {
                onLineFilterChanged.accept(Integer.parseInt(selected));
            } catch (NumberFormatException ex) {
                onLineFilterChanged.accept(null);
            }
        }
    }


    private void handleOrientationFilterChange() {
        String selected = (String) orientationComboBox.getSelectedItem();
        if (selected == null || "Ambas".equals(selected)) {
            onOrientationFilterChanged.accept(null);
        } else {
            onOrientationFilterChanged.accept(Integer.parseInt(selected));
        }
    }
}
