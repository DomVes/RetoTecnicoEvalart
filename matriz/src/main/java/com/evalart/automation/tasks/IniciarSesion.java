package com.evalart.automation.tasks;

import net.serenitybdd.core.steps.Instrumented;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

import static com.evalart.automation.userinterface.IniciarSesionUI.BTN_LOGIN;
import static com.evalart.automation.userinterface.IniciarSesionUI.INPUT_PASSWORD;
import static com.evalart.automation.userinterface.IniciarSesionUI.INPUT_USER_NAME;
import static com.evalart.automation.userinterface.IniciarSesionUI.MENSAJE_ID_INCORRECTO_XPATH;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.isVisible;

public class IniciarSesion implements Task {

    private static final int MAX_INTENTOS_LOGIN = 10;
    private static final By MENSAJE_ID_INCORRECTO_BY = By.xpath(MENSAJE_ID_INCORRECTO_XPATH);
    private static final By CAMPO_USUARIO_BY = By.xpath("//input[@name='username']");
    private static final By ENLACE_VOLVER_BY = By.xpath(
            "//a[contains(normalize-space(.),'Back') or contains(normalize-space(.),'Regresar')]");

    @Override
    public <T extends Actor> void performAs(T actor) {
        String userName = "1437517";
        String password = "10df2f32286b7120My0zLTcxNTczNDE=30e0c83e6c29f1c3";
        iniciarSesionConReintentos(actor, userName, password);
    }

    private <T extends Actor> void iniciarSesionConReintentos(T actor, String userName, String password) {
        WebDriver driver = BrowseTheWeb.as(actor).getDriver();
        String urlInicio = driver.getCurrentUrl();
        Throwable ultimoError = null;

        for (int intento = 1; intento <= MAX_INTENTOS_LOGIN; intento++) {
            try {
                if (intento > 1) {
                    System.out.println("Reintentando el formulario de login (" + intento + " de " + MAX_INTENTOS_LOGIN + ")");
                    driver.get(urlInicio);
                }

                actor.attemptsTo(
                        WaitUntil.the(INPUT_USER_NAME, isVisible()).forNoMoreThan(10).seconds(),
                        Enter.theValue(userName).into(INPUT_USER_NAME),
                        Enter.theValue(password).into(INPUT_PASSWORD),
                        Click.on(BTN_LOGIN)
                );

                if (aparecioIdIncorrecto(driver)) {
                    ultimoError = new AssertionError("Id incorrecto, intenta otra vez");
                    System.out.println("Apareció el mensaje de id incorrecto. Intento " + intento + " de " + MAX_INTENTOS_LOGIN);
                    continue;
                }

                volverSiHayErrorDePagina(driver);
                new WebDriverWait(driver, 10).until(navegador ->
                        !navegador.findElements(By.cssSelector("form button[name='button_option']")).isEmpty());
                return;
            } catch (Exception | AssertionError e) {
                ultimoError = e;
                System.out.println("Falló el intento " + intento + " de inicio de sesión: " + e.getMessage());
            }
        }

        if (ultimoError instanceof RuntimeException) {
            throw (RuntimeException) ultimoError;
        }
        if (ultimoError instanceof Error) {
            throw (Error) ultimoError;
        }
        throw new RuntimeException(
                "No se pudo iniciar sesión después de " + MAX_INTENTOS_LOGIN + " intentos",
                ultimoError
        );
    }

    private boolean aparecioIdIncorrecto(WebDriver driver) {
        WebDriverWait espera = new WebDriverWait(driver, 10);
        try {
            String resultado = espera.until(navegador -> {
                if (estaVisible(navegador, MENSAJE_ID_INCORRECTO_BY)) {
                    return "incorrecto";
                }
                if (!estaVisible(navegador, CAMPO_USUARIO_BY)) {
                    return "ok";
                }
                return null;
            });
            return "incorrecto".equals(resultado);
        } catch (TimeoutException e) {
            return estaVisible(driver, MENSAJE_ID_INCORRECTO_BY);
        }
    }

    private void volverSiHayErrorDePagina(WebDriver driver) {
        List<WebElement> enlaces = driver.findElements(ENLACE_VOLVER_BY);
        for (WebElement enlace : enlaces) {
            if (enlace.isDisplayed()) {
                System.out.println("La página mostró un error. Se vuelve a la prueba.");
                enlace.click();
                return;
            }
        }
    }

    private boolean estaVisible(WebDriver driver, By selector) {
        for (WebElement elemento : driver.findElements(selector)) {
            try {
                if (elemento.isDisplayed()) {
                    return true;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public static IniciarSesion onPage() {
        return Instrumented.instanceOf(IniciarSesion.class).withProperties();
    }
}
