package com.evalart.automation.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.JavaScriptClick;
import net.serenitybdd.screenplay.targets.Target;
import net.serenitybdd.screenplay.waits.WaitUntil;

import static com.evalart.automation.userinterface.AutomatizarCoordenadasUI.*;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

public class ExtraerMatriz implements Task {
    private final int coordenadaX;
    private final int coordenadaY;

    public ExtraerMatriz(int coordenadaX, int coordenadaY) {
        this.coordenadaX = coordenadaX;
        this.coordenadaY = coordenadaY;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        int tamanoMatriz = actor.recall("tamanoMatriz");

        System.out.println("==============================================");
        System.out.println("Calculando índice para coordenadas (" + coordenadaX + "," + coordenadaY + ")");
        System.out.println("Tamaño de matriz: " + tamanoMatriz + "x" + tamanoMatriz);
        
        int indice = ((coordenadaY * tamanoMatriz) + coordenadaX) + 1;
        System.out.println("Índice calculado: " + indice);
        System.out.println("==============================================");

        Target botonAclickear = BTN_MATRIZ.of(String.valueOf(indice));
        actor.attemptsTo(
            JavaScriptClick.on(botonAclickear),
            WaitUntil.the(INPUT_ANSWER, isVisible()).forNoMoreThan(10).seconds()
        );

        actor.attemptsTo(
            Answers.sumarColumnaX(coordenadaX)
        );
    }

    public static ExtraerMatriz desdeLaPaginaConCoordenadas(int coordenadaX, int coordenadaY) {
        return new ExtraerMatriz(coordenadaX, coordenadaY);
    }
}
