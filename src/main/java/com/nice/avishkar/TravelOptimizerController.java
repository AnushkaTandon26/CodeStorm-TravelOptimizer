package com.nice.avishkar;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@RestController
@RequestMapping("/api/travel")
public class TravelOptimizerController {

    private final TravelOptimizerImpl travelOptimizer;
    private static final String HF_API_KEY = System.getenv("HF_TOKEN"); // Use environment variable instead of hardcoded key
    private final Gson gson = new Gson();

    public TravelOptimizerController() {
        this.travelOptimizer = new TravelOptimizerImpl(true);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Travel Optimizer API");
        response.put("version", "1.0.0");
        return response;
    }

    @PostMapping("/optimize")
    public Map<String, OptimalTravelSchedule> optimizeTravel(
            @RequestParam String schedulesPath,
            @RequestParam String requestsPath) throws IOException {

        Path schedulesFilePath = Paths.get(schedulesPath);
        Path customerRequestFilePath = Paths.get(requestsPath);

        ResourceInfo resourceInfo = new ResourceInfo(schedulesFilePath, customerRequestFilePath);
        return travelOptimizer.getOptimalTravelOptions(resourceInfo);
    }

    @PostMapping("/optimize-with-summary")
    public Map<String, OptimalTravelSchedule> optimizeWithSummary(
            @RequestParam String schedulesPath,
            @RequestParam String requestsPath) throws IOException {

        Path schedulesFilePath = Paths.get(schedulesPath);
        Path customerRequestFilePath = Paths.get(requestsPath);

        ResourceInfo resourceInfo = new ResourceInfo(schedulesFilePath, customerRequestFilePath);
        Map<String, OptimalTravelSchedule> result = travelOptimizer.getOptimalTravelOptions(resourceInfo);

        // Generate AI summaries using Hugging Face
        // Generate AI summaries using Hugging Face
        for (OptimalTravelSchedule schedule : result.values()) {
            if (schedule.getRoutes() != null && !schedule.getRoutes().isEmpty()) {
                String aiSummary = generateAISummary(schedule);
                schedule.setSummary(aiSummary);
            }
        }

        return result;
    }

    @GetMapping("/test-case/{caseNumber}")
    public Map<String, OptimalTravelSchedule> runTestCase(@PathVariable int caseNumber) throws IOException {
        String baseDir = "src/main/resources/TestCase-" + caseNumber;
        Path schedulesFilePath = Paths.get(baseDir + "/Schedules.csv");
        Path customerRequestFilePath = Paths.get(baseDir + "/CustomerRequests.csv");

        ResourceInfo resourceInfo = new ResourceInfo(schedulesFilePath, customerRequestFilePath);
        return travelOptimizer.getOptimalTravelOptions(resourceInfo);
    }


    private String generateAISummary(OptimalTravelSchedule schedule) {
        try {
            String huggingFaceApiUrl = "https://api-inference.huggingface.co/models/facebook/bart-large-cnn";
            StringBuilder routeText = new StringBuilder();
            int totalDuration = 0;
            if (schedule.getRoutes() != null && !schedule.getRoutes().isEmpty()) {
                Route firstRoute = schedule.getRoutes().get(0);
                Route lastRoute = schedule.getRoutes().get(schedule.getRoutes().size() - 1);
                
                for (Route route : schedule.getRoutes()) {
                    routeText.append(route.getSource()).append(" to ")
                            .append(route.getDestination()).append(" via ")
                            .append(route.getMode()).append(" at ")
                            .append(route.getDepartureTime()).append(". ");
                }
                
                totalDuration = calculateDuration(firstRoute.getDepartureTime(), lastRoute.getArrivalTime());
            }

            String inputText = String.format(
                "Your optimal %s route takes %d hours total. " +
                "Routes: %s Provide a 50-word travel summary mentioning the total time and any alternatives.",
                schedule.getCriteria(),
                totalDuration,
                routeText.toString()
            );
            JsonObject payload = new JsonObject();
            payload.addProperty("inputs", inputText);

            String response = callHuggingFaceAPI(huggingFaceApiUrl, payload.toString());
            return parseHFResponse(response, totalDuration);
            
        } catch (Exception e) {
            return "Your optimal route takes " + calculateTotalDuration(schedule) + " hours total. However there is a earlier option available.";
        }
    }

    private String callHuggingFaceAPI(String apiUrl, String payload) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + getApiKey());
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = payload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        StringBuilder response = new StringBuilder();

        try (Scanner scanner = new Scanner(connection.getInputStream())) {
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
        }

        return response.toString();
    }

    private String parseHFResponse(String response, int duration) {
        try {
            JsonArray jsonArray = gson.fromJson(response, JsonArray.class);
            if (jsonArray != null && jsonArray.size() > 0) {
                JsonObject obj = jsonArray.get(0).getAsJsonObject();
                String summary = obj.get("summary_text").getAsString();
                if (summary != null && !summary.isEmpty()) {
                    return summary;
                }
            }
        } catch (Exception e) {
        }
        return "Your optimal route takes " + duration + " hours total. However there is a earlier option available.";
    }

    private int calculateDuration(String departureTime, String arrivalTime) {
        try {
            String[] depParts = departureTime.split(":");
            String[] arrParts = arrivalTime.split(":");
            int depHour = Integer.parseInt(depParts[0]);
            int depMin = Integer.parseInt(depParts[1]);
            int arrHour = Integer.parseInt(arrParts[0]);
            int arrMin = Integer.parseInt(arrParts[1]);
            
            int depTotalMin = depHour * 60 + depMin;
            int arrTotalMin = arrHour * 60 + arrMin;
            int diffMin = arrTotalMin - depTotalMin;
            return (diffMin + 30) / 60;
        } catch (Exception e) {
            return 2;
        }
    }

    private int calculateTotalDuration(OptimalTravelSchedule schedule) {
        try {
            if (schedule.getRoutes() != null && !schedule.getRoutes().isEmpty()) {
                Route firstRoute = schedule.getRoutes().get(0);
                Route lastRoute = schedule.getRoutes().get(schedule.getRoutes().size() - 1);
                return calculateDuration(firstRoute.getDepartureTime(), lastRoute.getArrivalTime());
            }
        } catch (Exception e) {
        }
        return 2;
    }

    private String getApiKey() {
        String apiKey = System.getenv("HUGGING_FACE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = HF_API_KEY;
        }
        return apiKey;
    }
}
