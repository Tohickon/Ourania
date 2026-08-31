package com.zodiacomputing.ourania.gui;
public class TestService3 {
    public static void main(String[] args) {
        InterpretationService svc = InterpretationService.getInstance();
        System.out.println("Ceres Trine Sun: " + svc.getAspect("Ceres", "Sun", "Trine"));
        System.out.println("Vesta Trine Sun: " + svc.getAspect("Vesta", "Sun", "Trine"));
        System.out.println("North Node Conjunct Sun: " + svc.getAspect("North Node", "Sun", "Conjunction"));
        System.out.println("Part of Fortune Conjunct Sun: " + svc.getAspect("Part of Fortune", "Sun", "Conjunction"));
    }
}
