package com.nice.avishkar;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class TestRunner {
    public static void main(String[] args) throws IOException {
        boolean genTripSummary = false;
        if (args.length > 0) {
            genTripSummary = Boolean.parseBoolean(args[0]);
        }

        System.out.println("Travel Optimizer - Test Runner");
        System.out.println("=".repeat(70));
        System.out.println("Mode: " + (genTripSummary ? "WITH AI Trip Summaries" : "WITHOUT Trip Summaries"));
        System.out.println("=".repeat(70));

        for (int testCase = 1; testCase <= 6; testCase++) {
            runTestCase(testCase, genTripSummary);
        }
    }

    private static void runTestCase(int caseNumber, boolean genTripSummary) throws IOException {
        System.out.println("\n▶ Running TestCase-" + caseNumber);
        String baseDir = "src/main/resources/TestCase-" + caseNumber;
        Path schedulesPath = Paths.get(baseDir + "/Schedules.csv");
        Path requestsPath = Paths.get(baseDir + "/CustomerRequests.csv");

        try {
            ResourceInfo resourceInfo = new ResourceInfo(schedulesPath, requestsPath);
            TravelOptimizerImpl optimizer = new TravelOptimizerImpl(genTripSummary);
            
            // Use new JSON method
            Map<String, Map<String, Object>> results = optimizer.getOptimalTravelOptionsJson(resourceInfo, genTripSummary);

            System.out.println("✓ Test Case " + caseNumber + " passed");
            System.out.println("  Results found for " + results.size() + " requests");
            
            // Print each result as simple JSON-like format
            for (Map.Entry<String, Map<String, Object>> entry : results.entrySet()) {
                Map<String, Object> schedule = entry.getValue();
                System.out.println("\n  Request \"" + entry.getKey() + "\": {");
                System.out.println("    \"schedule\": " + schedule.get("schedule") + ",");
                System.out.println("    \"criteria\": \"" + schedule.get("criteria") + "\",");
                System.out.println("    \"value\": " + schedule.get("value") + ",");
                System.out.println("    \"travelSummary\": \"" + schedule.get("travelSummary") + "\"");
                System.out.println("  }");
            }
            
        } catch (Exception e) {
            System.out.println("✗ Test Case " + caseNumber + " failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
