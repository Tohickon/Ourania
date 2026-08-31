package com.zodiacomputing.ourania.gui;
public class TestService11 {
    public static void main(String[] args) {
        InterpretationService svc = InterpretationService.getInstance();
        System.out.println("Sun Trine IC: " + svc.getAngleAspectInterpretation("Sun", "Trine", "IC"));
        System.out.println("Moon Trine IC: " + svc.getAngleAspectInterpretation("Moon", "Trine", "IC"));
        System.out.println("Mars Opposition IC: " + svc.getAngleAspectInterpretation("Mars", "Opposition", "IC"));
    }
}
