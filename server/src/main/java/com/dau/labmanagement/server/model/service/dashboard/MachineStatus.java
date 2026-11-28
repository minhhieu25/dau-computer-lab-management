package com.dau.labmanagement.server.model.service.dashboard;

public enum MachineStatus {

    ONLINE("Ổn định", "#16A34A"),
    UNSTABLE("Không ổn định", "#D97706"),
    OFFLINE("Ngoại tuyến", "#DC2626");

    private final String label;
    private final String color;

    MachineStatus(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }
}