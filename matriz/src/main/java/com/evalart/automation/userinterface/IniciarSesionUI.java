package com.evalart.automation.userinterface;

import net.serenitybdd.screenplay.targets.Target;
public class IniciarSesionUI {

    public static final Target INPUT_USER_NAME = Target.the("Usuario")
            .locatedBy("//input[@name='username']");
    public static final Target INPUT_PASSWORD = Target.the("Contraseña")
            .locatedBy("//input[@name='password']");
    public static final Target BTN_LOGIN = Target.the("Botón Iniciar Sesión")
            .locatedBy("//button[@type='submit']");

    public static final String MENSAJE_ID_INCORRECTO_XPATH = "//p[text()='Id incorrecto, intenta otra vez']";

    public static final Target MENSAJE_ID_INCORRECTO = Target.the("Id incorrecto")
            .locatedBy(MENSAJE_ID_INCORRECTO_XPATH);

    public static final Target MENSAJE_ERROR_PAGINA = Target.the("Mensaje de error en la página")
            .locatedBy("//a[text()=' Regresar']");
}
