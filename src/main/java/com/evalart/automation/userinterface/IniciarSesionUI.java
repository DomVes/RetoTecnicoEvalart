package com.evalart.automation.userinterface;

import net.serenitybdd.screenplay.targets.Target;
public class IniciarSesionUI {

    public static final Target INPUT_USER_NAME = Target.the("Usuario")
            .locatedBy("//input[@name='username']");
    public static final Target INPUT_PASSWORD = Target.the("Contraseña")
            .locatedBy("//input[@name='password']");
    public static final Target BTN_LOGIN = Target.the("Botón Iniciar Sesión")
            .locatedBy("//button[@type='submit']");
}
