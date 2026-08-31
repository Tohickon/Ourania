package com.zodiacomputing.ourania.gui;
public class TestService10 {
    public static void main(String[] args) {
        InterpretationService svc = InterpretationService.getInstance();
        System.out.println("Vesta Trine Moon: " + svc.getAspect("Vesta", "Moon", "Trine"));
        System.out.println("Lilith Trine Vesta: " + svc.getAspect("Black Moon Lilith", "Vesta", "Trine"));
        System.out.println("Lilith Trine Sun: " + svc.getAspect("Black Moon Lilith", "Sun", "Trine"));
        System.out.println("Lilith Square Ascendant: " + svc.getAngleAspectInterpretation("Black Moon Lilith", "Square", "Ascendant"));
    }
}
