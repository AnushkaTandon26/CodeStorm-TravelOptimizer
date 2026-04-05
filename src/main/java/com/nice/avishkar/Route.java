package com.nice.avishkar;

public class Route {

    private String source;
    private String destination;
    private String mode;
    private String departureTime;
    private String arrivalTime;
    private int cost;

    public Route(String source, String destination, String mode,
                 String departureTime, String arrivalTime, int cost) {
        this.source = source;
        this.destination = destination;
        this.mode = mode;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.cost = cost;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public String getMode() {
        return mode;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public int getCost() {
        return cost;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }
}