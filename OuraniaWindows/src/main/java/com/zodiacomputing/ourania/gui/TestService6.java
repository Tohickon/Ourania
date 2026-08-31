package com.zodiacomputing.ourania.gui;
public class TestService6 {
    public static void main(String[] args) {
        InterpretationService svc = InterpretationService.getInstance();
        System.out.println("Ceres Trine Sun: " + svc.getAspect("Ceres", "Sun", "Trine"));
        System.out.println("Vesta Trine Ceres: " + svc.getAspect("Vesta", "Ceres", "Trine"));
        System.out.println("Black Moon Lilith Trine Sun: " + svc.getAspect("Black Moon Lilith", "Sun", "Trine"));
    }
}
