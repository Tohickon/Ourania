package com.zodiacomputing.ourania.gui;
public class TestService7 {
    public static void main(String[] args) {
        System.out.println("Lilith index: " + com.zodiacomputing.ourania.astro.Bodies.indexOfName("Black Moon Lilith"));
        com.zodiacomputing.ourania.astro.Bodies.Def d = com.zodiacomputing.ourania.astro.Bodies.byName("Black Moon Lilith");
        System.out.println("Lilith Def: " + (d != null ? d.id : "null"));
    }
}
