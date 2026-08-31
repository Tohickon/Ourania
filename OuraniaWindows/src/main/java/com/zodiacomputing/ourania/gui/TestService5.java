package com.zodiacomputing.ourania.gui;
public class TestService5 {
    public static void main(String[] args) {
        InterpretationService svc = InterpretationService.getInstance();
        System.out.println("Moon Quincunx Chiron: " + svc.getAspect("Chiron", "Moon", "Quincunx"));
        System.out.println("Mars Quincunx Pallas: " + svc.getAspect("Pallas", "Mars", "Quincunx"));
        System.out.println("Neptune Sextile Ceres: " + svc.getAspect("Ceres", "Neptune", "Sextile"));
    }
}
