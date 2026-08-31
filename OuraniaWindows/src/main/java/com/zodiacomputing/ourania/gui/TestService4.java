package com.zodiacomputing.ourania.gui;
public class TestService4 {
    public static void main(String[] args) {
        InterpretationService svc = InterpretationService.getInstance();
        System.out.println("Vesta Trine Ascendant: " + svc.getAspect("Vesta", "Ascendant", "Trine"));
        System.out.println("Ceres Square MC: " + svc.getAspect("Ceres", "MC", "Square"));
        System.out.println("North Node Conjunct IC: " + svc.getAspect("North Node", "IC", "Conjunction"));
    }
}
