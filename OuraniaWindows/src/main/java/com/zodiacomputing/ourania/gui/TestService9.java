package com.zodiacomputing.ourania.gui;
public class TestService9 {
    public static void main(String[] args) {
        InterpretationService svc = InterpretationService.getInstance();
        System.out.println("Lilith Square Ascendant: " + svc.getAspect("Black Moon Lilith", "Ascendant", "Square"));
    }
}
