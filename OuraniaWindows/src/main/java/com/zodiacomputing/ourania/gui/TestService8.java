package com.zodiacomputing.ourania.gui;
public class TestService8 {
    public static void main(String[] args) {
        InterpretationService svc = InterpretationService.getInstance();
        System.out.println("Black Moon Lilith Trine Sun: " + svc.getAspect("Black Moon Lilith", "Sun", "Trine"));
        System.out.println("Part of Fortune Opposition Mars: " + svc.getAspect("Part of Fortune", "Mars", "Opposition"));
    }
}
