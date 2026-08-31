package com.zodiacomputing.ourania.gui;
public class TestService {
    public static void main(String[] args) {
        InterpretationService svc = InterpretationService.getInstance();
        System.out.println("Aspect count: " + svc.getAspect("Ceres", "Sun", "Conjunction"));
        System.out.println("Testing aspect ceres_conjunction_sun: " + svc.getAspect("ceres", "sun", "conjunction"));
    }
}
