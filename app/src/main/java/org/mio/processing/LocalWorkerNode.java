package org.mio.processing;

import org.mio.model.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LocalWorkerNode implements Runnable {
    private final int workerId;
    private final Queue<Datagram> workQueue;
    private final org.mio.graph.Graph graph;
    private final Map<String, ArcSpeed> results;
    private final Map<String, List<Datagram>> busHistory;
    private volatile boolean running;

    public LocalWorkerNode(int workerId, Queue<Datagram> workQueue, org.mio.graph.Graph graph) {
        this.workerId = workerId;
        this.workQueue = workQueue;
        this.graph = graph;
        this.results = new ConcurrentHashMap<>();
        this.busHistory = new ConcurrentHashMap<>();
        this.running = true;
    }

    @Override
    public void run() {
        System.out.println("Worker " + workerId + " (thread local) iniciado");

        while (running || !workQueue.isEmpty()) {
            Datagram datagram = workQueue.poll();
            if (datagram != null) {
                processDatagram(datagram);
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        System.out.println("Worker " + workerId + " finalizado. Procesó " + results.size() + " arcos");
    }

    private void processDatagram(Datagram datagram) {
        if (!"GPS_POSITION".equals(datagram.getEventType())) {
            return;
        }

        String busKey = datagram.getBusId();
        busHistory.computeIfAbsent(busKey, k -> new ArrayList<>()).add(datagram);

        List<Datagram> history = busHistory.get(busKey);
        if (history.size() < 2) {
            return;
        }

        Datagram previous = history.get(history.size() - 2);
        double speed = SpeedCalculator.calculateSpeed(previous, datagram);

        if (speed > 0 && speed < 100) {
            Arc arc = SpeedCalculator.findArcForDatagram(graph, datagram, history);
            if (arc != null) {
                String arcKey = arc.getFrom().getStopId() + "-" + arc.getTo().getStopId() + "-" + arc.getLineId();

                results.computeIfAbsent(arcKey, k -> new ArcSpeed(arc)).addSpeedSample(speed);
            }
        }

        if (history.size() > 100) {
            history.remove(0);
        }
    }

    public Map<String, ArcSpeed> getResults() {
        return new HashMap<>(results);
    }

    public void stop() {
        running = false;
    }

    public int getProcessedCount() {
        return results.size();
    }
}
