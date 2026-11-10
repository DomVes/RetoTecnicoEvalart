package com.evalart.automation.stepdefinitions;

import com.evalart.automation.drivers.ChromeDriver;
import com.evalart.automation.tasks.IniciarSesion;
import com.evalart.automation.tasks.ResolverMatriz;
import com.evalart.automation.utils.Excel;
import cucumber.api.java.Before;
import cucumber.api.java.es.Cuando;
import cucumber.api.java.es.Dado;
import cucumber.api.java.es.Entonces;
import net.serenitybdd.core.Serenity;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.actors.OnlineCast;

import java.io.IOException;

import static net.serenitybdd.screenplay.actors.OnStage.theActorInTheSpotlight;

public class AutomatizarBotonesStepDefinitions {

    private final Excel excel = new Excel();

    @Before
    public void setStage() {
        OnStage.setTheStage(new OnlineCast());
    }

    @Dado("que estoy logueado en la pagina de la matriz")
    public void queEstoyLogueadoEnLaPaginaDeLaMatriz() throws IOException {
        OnStage.theActorCalled("Esteban").can(BrowseTheWeb.with(
                ChromeDriver.ChromeWebDriver().on(excel.leerDatoExcel("Dominios", "Data.xlsx", 1, 0))));
        theActorInTheSpotlight().attemptsTo(IniciarSesion.onPage());
    }

    @Cuando("selecciono el boton correcto en la matriz y proceso la respuesta")
    public void seleccionoElBotonCorrectoYProcesoRespuesta() {
        theActorInTheSpotlight().attemptsTo(ResolverMatriz.duranteDiezCiclos());
    }

    @Entonces("deberia ver el hash de exito")
    public void deberiaVerElHashDeExito() {
        Boolean pruebaCompletada = theActorInTheSpotlight().recall("pruebaCompletada");
        String hashObtenido = theActorInTheSpotlight().recall("hashExito");

        if (pruebaCompletada == null || !pruebaCompletada || hashObtenido == null || hashObtenido.isBlank()) {
            throw new AssertionError("La prueba no mostró el hash de éxito");
        }

        Serenity.recordReportData().withTitle("Hash de éxito").andContents(hashObtenido);
        System.out.println("\n==============================================");
        System.out.println("HASH FINAL: " + hashObtenido);
        System.out.println("==============================================\n");
    }
}
