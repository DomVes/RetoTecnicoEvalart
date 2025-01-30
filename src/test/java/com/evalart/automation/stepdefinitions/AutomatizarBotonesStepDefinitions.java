package com.evalart.automation.stepdefinitions;

import com.evalart.automation.tasks.ExtraerMatriz;
import com.evalart.automation.tasks.IniciarSesion;
import com.evalart.automation.utils.Excel;
import cucumber.api.java.Before;
import cucumber.api.java.es.Cuando;
import cucumber.api.java.es.Dado;
import cucumber.api.java.es.Entonces;
import cucumber.api.java.es.Y;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.actors.OnlineCast;
import net.serenitybdd.screenplay.ensure.Ensure;
import static com.evalart.automation.userinterface.AutomatizarCoordenadasUI.*;
import static net.serenitybdd.screenplay.actors.OnStage.theActorInTheSpotlight;

import java.io.IOException;

public class AutomatizarBotonesStepDefinitions {

    private Excel excel = new Excel();
    private static final String MENSAJE_EXITO_ESPERADO = "Felicidades, has terminado la prueba exitosamente";

    @Before
    public void setStage() {
        OnStage.setTheStage(new OnlineCast());
    }

    @Dado("que estoy logueado en la pagina de la matriz")
    public void queEstoyLogueadoEnLaPaginaDeLaMatriz() throws IOException {
        OnStage.theActorCalled("Esteban").can(BrowseTheWeb.with(com.evalart.automation.drivers.ChromeDriver.ChromeWebDriver().on(excel.leerDatoExcel("Dominios", "Data.xlsx", 1, 0))));
        OnStage.theActorInTheSpotlight().attemptsTo(IniciarSesion.onPage());
    }

    @Cuando("selecciono el boton correcto en la matriz y proceso la respuesta")
    public void seleccionoElBotonCorrectoYProcesoRespuesta() {
        int coordenadaX = theActorInTheSpotlight().recall("coordenadaX");
        int coordenadaY = theActorInTheSpotlight().recall("coordenadaY");
        theActorInTheSpotlight().attemptsTo(
            ExtraerMatriz.desdeLaPaginaConCoordenadas(coordenadaX, coordenadaY)
        );
    }

    @Entonces("deberia ver el hash de exito")
    public void deberiaVerElHashDeExito() {

        Boolean pruebaCompletada = theActorInTheSpotlight().recall("pruebaCompletada");

        if (pruebaCompletada != null && pruebaCompletada) {
            if (HASH_EXITO.resolveFor(theActorInTheSpotlight()).isPresent() &&
                MENSAJE_EXITO.resolveFor(theActorInTheSpotlight()).isPresent()) {

                String mensajeObtenido = MENSAJE_EXITO.resolveFor(theActorInTheSpotlight()).getText().trim();
                String hashObtenido = HASH_EXITO.resolveFor(theActorInTheSpotlight()).getText().trim();

                theActorInTheSpotlight().attemptsTo(
                    Ensure.that(mensajeObtenido).isEqualTo(MENSAJE_EXITO_ESPERADO)
                );
                System.out.println("\n==============================================");
                System.out.println("¡PRUEBA COMPLETADA EXITOSAMENTE!");
                System.out.println("----------------------------------------------");
                System.out.println("Mensaje de éxito esperado: " + MENSAJE_EXITO_ESPERADO);
                System.out.println("Mensaje de éxito obtenido: " + mensajeObtenido);
                System.out.println("----------------------------------------------");
                System.out.println("HASH FINAL: " + hashObtenido);
                System.out.println("==============================================\n");
            } else {
                throw new AssertionError("La prueba se completó pero no se encontraron los elementos de éxito");
            }
        } else {
            throw new AssertionError("La prueba no se ha completado correctamente");
        }
    }
}
