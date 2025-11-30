package org.mio.processing;

import org.mio.model.*;

import java.time.temporal.ChronoUnit;

public class SpeedCalculator {
    
    private static final double EARTH_RADIUS_KM = 6371.0;

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }

    public static double calculateSpeed(Datagram point1, Datagram point2) {
        if (!point1.getBusId().equals(point2.getBusId())) {
            return 0.0;
        }

        double distance = calculateDistance(
            point1.getLatitude(), point1.getLongitude(),
            point2.getLatitude(), point2.getLongitude()
        );

        long timeDiffMinutes = ChronoUnit.MINUTES.between(point1.getTimestamp(), point2.getTimestamp());
        
        if (timeDiffMinutes <= 0) {
            return 0.0;
        }

        double timeDiffHours = timeDiffMinutes / 60.0;
        return distance / timeDiffHours;
    }

    public static Arc findArcForDatagram(org.mio.graph.Graph graph, Datagram datagram, java.util.List<Datagram> busHistory) {
        for (Arc arc : graph.getArcs()) {
            double distToFrom = calculateDistance(
                datagram.getLatitude(), datagram.getLongitude(),
                arc.getFrom().getLat(), arc.getFrom().getLon()
            );
            
            double distToTo = calculateDistance(
                datagram.getLatitude(), datagram.getLongitude(),
                arc.getTo().getLat(), arc.getTo().getLon()
            );

            if (distToFrom < 0.1 || distToTo < 0.1) {
                if (busHistory.size() > 1) {
                    Datagram previous = busHistory.get(busHistory.size() - 2);
                    double distFromPrev = calculateDistance(
                        previous.getLatitude(), previous.getLongitude(),
                        arc.getFrom().getLat(), arc.getFrom().getLon()
                    );
                    
                    if (distFromPrev < 0.1) {
                        return arc;
                    }
                } else {
                    return arc;
                }
            }
        }
        return null;
    }
}
