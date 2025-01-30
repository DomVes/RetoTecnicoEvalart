package com.evalart.automation.tasks;

import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.actions.Click;
import net.serenitybdd.screenplay.actions.Enter;
import net.serenitybdd.screenplay.waits.WaitUntil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.evalart.automation.userinterface.AutomatizarCoordenadasUI.*;
import static com.evalart.automation.userinterface.IniciarSesionUI.BTN_LOGIN;
import static net.serenitybdd.screenplay.matchers.WebElementStateMatchers.*;

public class Answers implements Task {
    private final int coordenadaX;
    private static final int TOTAL_CICLOS = 12;
    private static boolean detenerEjecucion = false;
    private static final String MENSAJE_EXITO_ESPERADO = "Felicidades, has terminado la prueba exitosamente";

    public Answers(int coordenadaX) {
        this.coordenadaX = coordenadaX;
    }

    private int extraerNumeroCiclo(String mensaje) {
        Pattern pattern = Pattern.compile("ciclo (\\d+) de");
        Matcher matcher = pattern.matcher(mensaje);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    private int calcularSumaColumna(Actor actor, int tamanoMatriz) {
        int suma = 0;
        System.out.println("==============================================");
        System.out.println("Calculando suma de la columna vertical X=" + coordenadaX);
        System.out.println("Tamaño de matriz: " + tamanoMatriz + "x" + tamanoMatriz);

        for (int fila = 0; fila < tamanoMatriz; fila++) {
            int indice = ((fila * tamanoMatriz) + coordenadaX) + 1;
            String valorBoton = BTN_MATRIZ.of(String.valueOf(indice)).resolveFor(actor).getAttribute("value");

            if (valorBoton != null && !valorBoton.isEmpty()) {
                try {
                    int valor = Integer.parseInt(valorBoton);
                    System.out.println("Valor en fila " + fila + ": " + valor);
                    suma += valor;
                } catch (NumberFormatException e) {
                    System.out.println("El valor '" + valorBoton + "' no es un número válido. Se omitió.");
                }
            } else {
                System.out.println("Valor vacío en el botón de la posición " + indice + ". Se omitió.");
            }
        }
        
        System.out.println("Suma total de la columna vertical X=" + coordenadaX + ": " + suma);
        System.out.println("==============================================");
        return suma;
    }

    private boolean verificarExito(Actor actor) {
        try {
            if (HASH_EXITO.resolveFor(actor).isPresent() && MENSAJE_EXITO.resolveFor(actor).isPresent()) {
                String mensajeObtenido = MENSAJE_EXITO.resolveFor(actor).getText().trim();
                String hashObtenido = HASH_EXITO.resolveFor(actor).getText().trim();

                System.out.println("==============================================");
                System.out.println("¡CICLO 12 COMPLETADO!");
                System.out.println("Mensaje obtenido: " + mensajeObtenido);
                System.out.println("Hash obtenido: " + hashObtenido);
                System.out.println("==============================================");

                return true;
            }
        } catch (Exception e) {
            System.out.println("Error al verificar éxito: " + e.getMessage());
        }
        return false;
    }

    @Override
    public <T extends Actor> void performAs(T actor) {
        if (detenerEjecucion) {
            return;
        }

        boolean debeReintentar;
        do {
            int tamanoMatriz = actor.recall("tamanoMatriz");
            int suma = calcularSumaColumna(actor, tamanoMatriz);
            
            actor.attemptsTo(
                Enter.theValue(String.valueOf(suma)).into(INPUT_ANSWER),
                Click.on(BTN_LOGIN)
            );

            try {
                Thread.sleep(2000); // Aumentamos el tiempo de espera
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            debeReintentar = ERROR_MESSAGE.resolveFor(actor).isPresent();
            
            if (debeReintentar) {
                System.out.println("Se detectó un error. Recalculando con la nueva matriz...");
                actor.attemptsTo(
                    WaitUntil.the(FORM_MATRIZ, isVisible()).forNoMoreThan(10).seconds()
                );
                tamanoMatriz = (int) Math.sqrt(BTN_MATRIZ_CONTAR.resolveAllFor(actor).size());
                actor.remember("tamanoMatriz", tamanoMatriz);
                System.out.println("Nuevo tamaño de matriz detectado: " + tamanoMatriz + "x" + tamanoMatriz);
            } else {
                // Después de una respuesta correcta, esperamos más tiempo para verificar el éxito
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (verificarExito(actor)) {
                    actor.remember("pruebaCompletada", true);
                    return;
                }
            }
        } while (debeReintentar);

        Integer cicloActual = actor.recall("cicloActual");
        if (cicloActual == null) {
            cicloActual = 1;
        } else {
            cicloActual++;
        }
        actor.remember("cicloActual", cicloActual);

        System.out.println("==============================================");
        System.out.println("Ciclo actual: " + cicloActual + " de " + TOTAL_CICLOS);
        System.out.println("==============================================");

        if (cicloActual >= TOTAL_CICLOS) {
            try {
                Thread.sleep(3000); // Aumentamos el tiempo de espera después del ciclo 12
                if (verificarExito(actor)) {
                    actor.remember("pruebaCompletada", true);
                    return;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }

        int nuevoTamano = (int) Math.sqrt(BTN_MATRIZ_CONTAR.resolveAllFor(actor).size());
        actor.remember("tamanoMatriz", nuevoTamano);
        
        int[] coordenadas = actor.asksFor(ResolverCoordenadas.enLaPagina());
        actor.remember("coordenadaX", coordenadas[0]);
        actor.remember("coordenadaY", coordenadas[1]);

        actor.attemptsTo(
            ExtraerMatriz.desdeLaPaginaConCoordenadas(coordenadas[0], coordenadas[1])
        );
    }

    public static Answers sumarColumnaX(int coordenadaX) {
        return new Answers(coordenadaX);
    }
}
