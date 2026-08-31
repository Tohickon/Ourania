package com.zodiacomputing.ourania.gui;
public class TestService2 {
    public static void main(String[] args) {
        InterpretationService svc = InterpretationService.getInstance();
        System.out.println("Ceres in Aries: " + svc.getPlanetInSign("Ceres", "Aries"));
        System.out.println("Ceres in House 1: " + svc.getPlanetInHouse("Ceres", 1));
        System.out.println("Ceres Aspect: " + svc.getAspect("Ceres", "Sun", "Conjunction"));
    }
}
