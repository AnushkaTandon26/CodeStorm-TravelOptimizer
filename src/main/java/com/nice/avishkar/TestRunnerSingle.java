package com.nice.avishkar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class TestRunnerSingle {
    public static void main(String[] args) throws IOException {
        int testCase = 1;
        boolean genTripSummary = true;
        
        if (args.length > 0) {
            testCase = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            genTripSummary = Boolean.parseBoolean(args[1]);
        }

        System.out.println("Travel Optimizer - Single TestCase JSON Output");
        System.out.println("=".repeat(70));
        System.out.println("TestCase: " + testCase);
        System.out.println("Mode: " + (genTripSummary ? "WITH AI Summaries" : "WITHOUT Summaries"));
        System.out.println("=".repeat(70));

        try {
            String baseDir = "src/main/resources/TestCase-" + testCase;
            Path schedulesPath = Paths.get(baseDir + "/Schedules.csv");
            Path requestsPath = Paths.get(baseDir + "/CustomerRequests.csv");

            ResourceInfo resourceInfo = new ResourceInfo(schedulesPath, requestsPath);
            TravelOptimizerImpl optimizer = new TravelOptimizerImpl(genTripSummary);
            
            Map<String, Map<String, Object>> results = optimizer.getOptimalTravelOptionsJson(resourceInfo, genTripSummary);

            // Print as formatted JSON
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(results);
            
            System.out.println("\n✅ Success! " + results.size() + " requests processed\n");
            System.out.println("JSON OUTPUT (All requests):");
            System.out.println("-".repeat(70));
            System.out.println(jsonOutput);
            System.out.println("-".repeat(70));
            
        } catch (Exception e) {
            System.out.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
