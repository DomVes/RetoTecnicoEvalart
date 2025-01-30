package com.evalart.automation.userinterface;

import net.serenitybdd.screenplay.targets.Target;
public class AutomatizarCoordenadasUI {

    public static final Target ECUACION_TEXT = Target.the("Ecuacion")
            .locatedBy("//p[@class='text-center text-xl font-bold']");
    public static final Target BTN_MATRIZ = Target.the("Botón Matriz")
            .locatedBy("(//button[@name='button_option'])[{0}]");
    public static final Target BTN_MATRIZ_CONTAR = Target.the("Botón Matriz")
            .locatedBy("//button[@name='button_option']");
    public static final Target INPUT_ANSWER = Target.the("Respuesta")
            .locatedBy("//input[@name='modal_answer']");

    public static final Target HASH_EXITO = Target.the("Hash de éxito")
            .locatedBy("//p[@class='text-white text-2xl text-center break-all']");

    public static final Target MENSAJE_EXITO = Target.the("Mensaje de éxito")
            .locatedBy("//h1[@class='text-center text-3xl p-5']");

    public static final Target FORM_MATRIZ = Target.the("Formulario de la Matriz")
            .locatedBy("//form[@class='space-y-4 flex flex-col justify-center content-center items-center bg-white p-4 rounded-md']");

    public static final Target ERROR_MESSAGE = Target.the("Mensaje de error")
            .locatedBy("//p[@class='text-2xl text-center text-red-500']");

    public static final Target MENSAJE_CICLOS = Target.the("Mensaje de ciclo")
            .locatedBy("//p[@class='text-xl text-center text-green-500']");
}
