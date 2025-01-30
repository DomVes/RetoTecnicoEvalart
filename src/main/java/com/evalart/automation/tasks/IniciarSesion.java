package com.evalart.automation.tasks;

import net.serenitybdd.core.steps.Instrumented;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;
import net.serenitybdd.core.pages.WebElementFacade;
import java.util.List;

import static com.evalart.automation.userinterface.AutomatizarCoordenadasUI.*;
import static com.evalart.automation.userinterface.IniciarSesionUI.*;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

public class IniciarSesion implements Task {

    @Override
    public <T extends Actor> void performAs(T actor) {
        String UserName = "994073";
        String Password = "10df2f32286b7120My0zLTM3MDQ5OQ==30e0c83e6c29f1c3";
        
        actor.attemptsTo(
            WaitUntil.the(INPUT_USER_NAME, isVisible()).forNoMoreThan(10).seconds(),
            Enter.theValue(UserName).into(INPUT_USER_NAME),
            Enter.theValue(Password).into(INPUT_PASSWORD),
            Click.on(BTN_LOGIN),
            WaitUntil.the(FORM_MATRIZ, isVisible()).forNoMoreThan(10).seconds()
        );

        List<WebElementFacade> botones = BTN_MATRIZ_CONTAR.resolveAllFor(actor);
        double totalBotones = botones.size();
        int tamanoMatriz = (int) Math.sqrt(totalBotones);

        System.out.println("==============================================");
        System.out.println("Tamaño inicial de la matriz: " + tamanoMatriz + "x" + tamanoMatriz);
        System.out.println("Total de botones encontrados: " + totalBotones);
        System.out.println("==============================================");
        actor.remember("tamanoMatriz", tamanoMatriz);

        int[] coordenadas = actor.asksFor(ResolverCoordenadas.enLaPagina());

        actor.remember("coordenadaX", coordenadas[0]);
        actor.remember("coordenadaY", coordenadas[1]);

        System.out.println("==============================================");
        System.out.println("Coordenadas obtenidas: (" + coordenadas[0] + "," + coordenadas[1] + ")");
        System.out.println("==============================================");
    }

    public static IniciarSesion onPage() {
        return Instrumented.instanceOf(IniciarSesion.class).withProperties();
    }
}
