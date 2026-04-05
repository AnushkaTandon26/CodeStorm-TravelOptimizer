package com.nice.avishkar;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class TravelOptimizerImpl implements ITravelOptimizer {

    private boolean generateSummary;

    TravelOptimizerImpl(boolean generateSummary) {
        this.generateSummary = generateSummary;
    }

    static class Request {
        String id;
        String source;
        String destination;
        String criteria;
    }

    static class State {
        String node;
        int time;
        int cost;
        int hops;
        LocalTime currTime;
        List<Route> path;

        State(String node) {
            this.node = node;
            this.time = 0;
            this.cost = 0;
            this.hops = 0;
            this.currTime = null;
            this.path = new ArrayList<>();
        }
    }

    @Override
    public Map<String, Map<String, Object>> getOptimalTravelOptionsJson(ResourceInfo resourceInfo, boolean genTripSummary) throws IOException {
        List<Route> routes = loadSchedules(resourceInfo.getTransportSchedulePath());
        List<Request> requests = loadRequests(resourceInfo.getCustomerRequestPath());
        Map<String, List<Route>> graph = buildGraph(routes);
        Map<String, Map<String, Object>> result = new HashMap<>();

        for (Request req : requests) {
            State best = findBestPath(req.source, req.destination, req.criteria, graph);
            Map<String, Object> scheduleObj = new HashMap<>();

            if (best == null) {
                scheduleObj.put("schedule", new ArrayList<>());
                scheduleObj.put("criteria", req.criteria);
                scheduleObj.put("value", 0);
                scheduleObj.put("travelSummary", genTripSummary ? "No routes available" : "Not generated");
            } else {
                List<Map<String, Object>> scheduleList = new ArrayList<>();
                for (Route r : best.path) {
                    Map<String, Object> routeMap = new HashMap<>();
                    routeMap.put("source", r.getSource());
                    routeMap.put("destination", r.getDestination());
                    routeMap.put("mode", r.getMode());
                    routeMap.put("departureTime", r.getDepartureTime());
                    routeMap.put("arrivalTime", r.getArrivalTime());
                    routeMap.put("cost", r.getCost());
                    scheduleList.add(routeMap);
                }

                scheduleObj.put("schedule", scheduleList);
                scheduleObj.put("criteria", req.criteria);

                if (req.criteria.equalsIgnoreCase("Time")) {
                    scheduleObj.put("value", best.time);
                } else if (req.criteria.equalsIgnoreCase("Cost")) {
                    scheduleObj.put("value", best.cost);
                } else {
                    scheduleObj.put("value", best.hops);
                }

                if (genTripSummary) {
                    scheduleObj.put("travelSummary", generateOptimalRouteSummary(best));
                } else {
                    scheduleObj.put("travelSummary", "Not generated");
                }
            }

            result.put(req.id, scheduleObj);
        }

        return result;
    }

    @Override
    public Map<String, OptimalTravelSchedule> getOptimalTravelOptions(ResourceInfo resourceInfo) throws IOException {

        List<Route> routes = loadSchedules(resourceInfo.getTransportSchedulePath());
        List<Request> requests = loadRequests(resourceInfo.getCustomerRequestPath());

        Map<String, List<Route>> graph = buildGraph(routes);

        Map<String, OptimalTravelSchedule> result = new HashMap<>();

        for (Request req : requests) {

            State best = findBestPath(req.source, req.destination, req.criteria, graph);

            OptimalTravelSchedule ots = new OptimalTravelSchedule();

            if (best == null) {
                ots.setRoutes(new ArrayList<>());
                ots.setCriteria(req.criteria);
                ots.setValue(0);
                ots.setSummary(generateSummary ? "No routes available" : "Not generated");
            } else {
                ots.setRoutes(best.path);
                ots.setCriteria(req.criteria);

                if (req.criteria.equalsIgnoreCase("Time")) {
                    ots.setValue(best.time);
                } else if (req.criteria.equalsIgnoreCase("Cost")) {
                    ots.setValue(best.cost);
                } else {
                    ots.setValue(best.hops);
                }

            if (generateSummary) {
                    ots.setSummary(generateOptimalRouteSummary(best));
                } else {
                    ots.setSummary("Not generated");
                }
            }

            result.put(req.id, ots);
        }

        return result;
    }

    private List<Route> loadSchedules(Path path) throws IOException {
        List<Route> routes = new ArrayList<>();
        List<String> lines = Files.readAllLines(path);
        for (int i = 1; i < lines.size(); i++) {
            String[] p = lines.get(i).split(",");

            routes.add(new Route(
                    p[0],
                    p[1],
                    p[2],
                    p[3],
                    p[4],
                    Integer.parseInt(p[5])
            ));
        }

        return routes;
    }

    private List<Request> loadRequests(Path path) throws IOException {
        List<Request> list = new ArrayList<>();
        List<String> lines = Files.readAllLines(path);
        for (int i = 1; i < lines.size(); i++) {
            String[] p = lines.get(i).split(",");

            Request r = new Request();
            r.id = p[0];
            r.source = p[2];
            r.destination = p[3];
            r.criteria = p[4];

            list.add(r);
        }

        return list;
    }

    private Map<String, List<Route>> buildGraph(List<Route> routes) {
        Map<String, List<Route>> graph = new HashMap<>();

        for (Route r : routes) {
            graph.computeIfAbsent(r.getSource(), k -> new ArrayList<>()).add(r);
        }

        return graph;
    }

   
    private int getMinutes(String t1, String t2) {
        LocalTime a = LocalTime.parse(t1);
        LocalTime b = LocalTime.parse(t2);
        long minutes = Duration.between(a, b).toMinutes();
        if (minutes < 0) {
            minutes += 24 * 60;
        }
        return (int) minutes;
    }

    private String generateOptimalRouteSummary(State best) {
    try {
        if (best == null || best.path == null || best.path.isEmpty()) {
            return "No routes available";
        }

        Route first = best.path.get(0);
        Route last = best.path.get(best.path.size() - 1);

        StringBuilder prompt = new StringBuilder();
        prompt.append("Write a natural travel summary in plain English using no more than 60 words. ");
        prompt.append("Mention source, destination, total travel time, total cost, number of transfers, ");
        prompt.append("Make it sound helpful and user-friendly. ");

        prompt.append("Trip from ").append(first.getSource())
              .append(" to ").append(last.getDestination()).append(". ");

        prompt.append("Total travel time: ").append(formatDuration(best.time)).append(". ");
        prompt.append("Total cost: ").append(best.cost).append(". ");
        prompt.append("Transfers: ").append(Math.max(0, best.hops - 1)).append(". ");

        if (best.path.size() > 1) {
            Route r1 = best.path.get(0);
            Route r2 = best.path.get(1);
            int layover = getMinutes(r1.getArrivalTime(), r2.getDepartureTime());
            prompt.append("Layover: ").append(layover)
                  .append(" minutes at ").append(r1.getDestination()).append(". ");
        } else {
            prompt.append("This is a direct route. ");
        }

        return callHuggingFaceAPI(prompt.toString(), best);

    } catch (Exception e) {
        return createBasicSummary(best);
    }
}

    private String callHuggingFaceAPI(String inputText, State best) {
        try {
        String apiUrl = "https://api-inference.huggingface.co/models/facebook/bart-large-cnn";
        String apiKey = System.getenv("HF_TOKEN");

        if (apiKey == null || apiKey.isEmpty()) {
            return createBasicSummary(best);
        }

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        String safeInput = inputText
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ");

        String payload = "{\"inputs\": \"" + safeInput + "\"}";

        try (java.io.OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            StringBuilder response = new StringBuilder();
            try (java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream())) {
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
            }

            String jsonResponse = response.toString();
            if (jsonResponse.contains("summary_text")) {
                int startIdx = jsonResponse.indexOf("\"summary_text\":\"") + 16;
                int endIdx = jsonResponse.indexOf("\"", startIdx);
                if (endIdx > startIdx) {
                    String summary = jsonResponse.substring(startIdx, endIdx)
                            .replace("\\n", " ")
                            .replace("\\\"", "\"")
                            .trim();
                    return truncateTo60Words(summary);
                }
            }
        }

        return createBasicSummary(best);

    } catch (Exception e) {
        return createBasicSummary(best);
    }
}

    private String truncateTo60Words(String text) {
        String[] words = text.split("\\s+");
        if (words.length <= 60) {
            return text;
        }
        StringBuilder truncated = new StringBuilder();
        for (int i = 0; i < 60 && i < words.length; i++) {
            truncated.append(words[i]).append(" ");
        }
        return truncated.toString().trim() + ".";
    }

    private String createBasicSummary(State best) {
        if (best == null || best.path == null || best.path.isEmpty()) {
        return "No routes available";
    }

    Route first = best.path.get(0);
    Route last = best.path.get(best.path.size() - 1);

    StringBuilder summary = new StringBuilder();
    summary.append("This route travels from ")
           .append(first.getSource())
           .append(" to ")
           .append(last.getDestination())
           .append(" in ")
           .append(formatDuration(best.time))
           .append(" with a total cost of ")
           .append(best.cost)
           .append(". ");

    if (best.hops > 1) {
        summary.append("It includes ")
               .append(best.hops - 1)
               .append(best.hops - 1 == 1 ? " transfer. " : " transfers. ");
    } else {
        summary.append("This is a direct journey. ");
    }

    if (best.path.size() > 1) {
        Route r1 = best.path.get(0);
        Route r2 = best.path.get(1);
        int layover = getMinutes(r1.getArrivalTime(), r2.getDepartureTime());

        summary.append("There is a ")
               .append(layover)
               .append("-minute layover at ")
               .append(r1.getDestination())
               .append(". ");
    }

    summary.append("This option is optimal based on the selected travel criteria.");

    return truncateTo60Words(summary.toString().trim());
}

    private State findBestPath(String src, String dest, String criteria,
                               Map<String, List<Route>> graph) {
        Comparator<State> comp;

        if (criteria.equalsIgnoreCase("Time")) {
            comp = Comparator.comparingInt((State s) -> s.time)
                    .thenComparingInt(s -> s.cost)
                    .thenComparingInt(s -> s.hops);
        } else if (criteria.equalsIgnoreCase("Cost")) {
            comp = Comparator.comparingInt((State s) -> s.cost)
                    .thenComparingInt(s -> s.time)
                    .thenComparingInt(s -> s.hops);
        } else {
            comp = Comparator.comparingInt((State s) -> s.hops)
                    .thenComparingInt(s -> s.time)
                    .thenComparingInt(s -> s.cost);
        }

        PriorityQueue<State> pq = new PriorityQueue<>(comp);
        Map<String, Integer> best = new HashMap<>();
        State start = new State(src);
        pq.add(start);
        best.put(src + "_START", 0);
        int maxHops = 20;
        int maxIterations = 50000;
        int iterations = 0;

        while (!pq.isEmpty() && iterations < maxIterations) {
            iterations++;
            State curr = pq.poll();
            if (curr.node.equals(dest)) {
                return curr;
            }

            if (curr.hops >= maxHops) {
                continue;
            }

            if (!graph.containsKey(curr.node)) continue;

            for (Route r : graph.get(curr.node)) {
                State next = new State(r.getDestination());

                next.path = new ArrayList<>(curr.path);
                next.path.add(r);
                int travel = getMinutes(r.getDepartureTime(), r.getArrivalTime());
                int wait = 0;
                if (curr.currTime != null) {
                    LocalTime dep = LocalTime.parse(r.getDepartureTime());
                    LocalTime currTime = curr.currTime;
                    if (dep.isAfter(currTime)) {
                        wait = (int) Duration.between(currTime, dep).toMinutes();
                    } else if (dep.isBefore(currTime)) {
                        wait = (int) Duration.between(currTime, LocalTime.MAX).toMinutes() + 1;
                        wait += (int) Duration.between(LocalTime.MIN, dep).toMinutes();
                    } else {
                        wait = 0;
                    }
                }

                next.time = curr.time + travel + wait;
                next.cost = curr.cost + r.getCost();
                next.hops = curr.hops + 1;
                next.currTime = LocalTime.parse(r.getArrivalTime());

                String stateKey = next.node + "_" + next.currTime;
                Integer prevBest = best.get(stateKey);
                boolean isBetter = false;
                if (prevBest == null) {
                    isBetter = true;
                } else {
                    if (criteria.equalsIgnoreCase("Time")) {
                        if (next.time < prevBest) {
                            isBetter = true;
                        } else if (next.time == prevBest && next.cost < best.getOrDefault(stateKey + "_COST", Integer.MAX_VALUE)) {
                            isBetter = true;
                        }
                    } else if (criteria.equalsIgnoreCase("Cost")) {
                        if (next.cost < prevBest) {
                            isBetter = true;
                        } else if (next.cost == prevBest && next.time < best.getOrDefault(stateKey + "_TIME", Integer.MAX_VALUE)) {
                            isBetter = true;
                        }
                    } else {
                        if (next.hops < prevBest) {
                            isBetter = true;
                        } else if (next.hops == prevBest && next.time < best.getOrDefault(stateKey + "_TIME", Integer.MAX_VALUE)) {
                            isBetter = true;
                        }
                    }
                }

                if (isBetter) {
                    pq.add(next);
                    if (criteria.equalsIgnoreCase("Time")) {
                        best.put(stateKey, next.time);
                        best.put(stateKey + "_COST", next.cost);
                    } else if (criteria.equalsIgnoreCase("Cost")) {
                        best.put(stateKey, next.cost);
                        best.put(stateKey + "_TIME", next.time);
                    } else {
                        best.put(stateKey, next.hops);
                        best.put(stateKey + "_TIME", next.time);
                    }
                }
            }
        }
        return null;
    }

    private String formatDuration(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (hours > 0 && minutes > 0) {
        return hours + " hours " + minutes + " minutes";
    } else if (hours > 0) {
        return hours + " hours";
    } else {
        return minutes + " minutes";
    }
}
}
