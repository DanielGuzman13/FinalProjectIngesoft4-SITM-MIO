package org.mio.ui;

import org.mio.model.Stop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;


public class InfoPanel extends JPanel {

    private final JLabel stopIdLabel = new JLabel();
    private final JLabel shortNameLabel = new JLabel();
    private final JLabel longNameLabel = new JLabel();
    private final JLabel latLabel = new JLabel();
    private final JLabel lonLabel = new JLabel();


    public InfoPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(250, 0));
        setBackground(Color.WHITE);

        add(createTitle("Información de la Parada"));
        add(Box.createRigidArea(new Dimension(0, 10)));

        add(createField("ID:", stopIdLabel));
        add(createField("Nombre Corto:", shortNameLabel));
        add(createField("Nombre Largo:", longNameLabel));
        add(createField("Latitud:", latLabel));
        add(createField("Longitud:", lonLabel));

        setStop(null);
    }


    private JLabel createTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        return title;
    }


    private JPanel createField(String labelText, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setPreferredSize(new Dimension(100, 20));
        
        valueLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        
        panel.add(label, BorderLayout.WEST);
        panel.add(valueLabel, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20)); // Constrain height
        return panel;
    }


    public void setStop(Stop stop) {
        if (stop != null) {
            stopIdLabel.setText(String.valueOf(stop.getStopId()));
            shortNameLabel.setText(stop.getShortName());
            longNameLabel.setText("<html>" + stop.getLongName() + "</html>"); // Allow wrapping
            latLabel.setText(String.format("%.6f", stop.getLat()));
            lonLabel.setText(String.format("%.6f", stop.getLon()));
        } else {
            stopIdLabel.setText("-");
            shortNameLabel.setText("-");
            longNameLabel.setText("-");
            latLabel.setText("-");
            lonLabel.setText("-");
        }
    }
}
