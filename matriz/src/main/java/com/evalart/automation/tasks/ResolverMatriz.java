package com.evalart.automation.tasks;

import net.serenitybdd.core.steps.Instrumented;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResolverMatriz implements Task {

    private static final int MAX_ENVIOS = 40;
    private static final int TOTAL_CICLOS = 10;
    private static final Pattern CICLO = Pattern.compile("Se encuentra en el ciclo\\s+(\\d+)\\s+de\\s+(\\d+)");
    private static final Pattern COORDENADA = Pattern.compile("\\((-?\\d+),(-?\\d+)\\)");
    private static final Pattern COLUMNAS = Pattern.compile("grid-cols-(\\d+)");
    private static final Pattern PREGUNTA_HTML = Pattern.compile("<h2[^>]*>(.*?)</h2>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final By ENLACE_VOLVER = By.xpath(
            "//a[contains(normalize-space(.),'Back') or contains(normalize-space(.),'Regresar')]");

    @Override
    public <T extends Actor> void performAs(T actor) {
        WebDriver driver = BrowseTheWeb.as(actor).getDriver();
        String urlInicio = null;
        int avances = 0;

        for (int envio = 1; envio <= MAX_ENVIOS; envio++) {
            try {
                if (esPaginaIncorrecta(driver)) {
                    System.out.println("La respuesta fue incorrecta. Se vuelve a empezar.");
                    avances = 0;
                    volverAlInicio(driver, urlInicio);
                    continue;
                }
                if (!esperarMatriz(driver)) {
                    System.out.println("No apareció la matriz en el intento " + envio + ".");
                    continue;
                }
                if (esExito(driver)) {
                    guardarExito(actor, driver);
                    return;
                }
                if (urlInicio == null) {
                    urlInicio = driver.getCurrentUrl();
                }

                int cicloAntes = numeroCiclo(driver);
                System.out.println("==============================================");
                System.out.println("Mensaje de ciclo: \"" + textoCiclo(driver) + "\"");
                int[] posicion = sumarCoordenadas(textoCoordenadas(driver));
                WebElement grilla = grilla(driver);
                int columnas = columnas(grilla);
                List<WebElement> botones = grilla.findElements(By.cssSelector("button[name='button_option']"));
                int filas = botones.size() / columnas;
                int[][] matriz = leerMatriz(botones, filas, columnas);
                int indice = posicion[1] * columnas + posicion[0];
                System.out.println("Matriz " + columnas + "x" + filas + ". Botón en (" + posicion[0] + "," + posicion[1] + "), índice " + indice);
                if (posicion[0] < 0 || posicion[1] < 0 || posicion[0] >= columnas || posicion[1] >= filas || indice >= botones.size()) {
                    System.out.println("La coordenada quedó fuera de la matriz. Se vuelve a empezar.");
                    avances = 0;
                    driver.get(urlInicio);
                    continue;
                }

                WebElement boton = botones.get(indice);
                System.out.println("Valor del botón: " + boton.getAttribute("value"));
                clickBotonMatriz(driver, boton);
                responderPreguntaExtra(driver, matriz, posicion[0], posicion[1]);

                if (esExito(driver)) {
                    actor.remember("ciclosValidados", avances + 1);
                    guardarExito(actor, driver);
                    return;
                }
                if (esPaginaIncorrecta(driver)) {
                    System.out.println("La respuesta fue incorrecta. Se vuelve a empezar.");
                    avances = 0;
                    volverAlInicio(driver, urlInicio);
                    continue;
                }

                int cicloDespues = numeroCiclo(driver);
                if (cicloDespues == cicloAntes + 1) {
                    avances++;
                    System.out.println("El ciclo aumentó a \"" + textoCiclo(driver) + "\" (" + avances + " de " + TOTAL_CICLOS + ").");
                } else {
                    System.out.println("El ciclo no aumentó. Mensaje actual: \"" + textoCiclo(driver) + "\". Se vuelve a empezar.");
                    avances = 0;
                    driver.get(urlInicio);
                }
            } catch (org.openqa.selenium.WebDriverException e) {
                if (!ventanaAbierta(driver)) {
                    throw e;
                }
                System.out.println("El navegador interrumpió el ciclo: " + e.getMessage() + ". Se vuelve a empezar.");
                avances = 0;
                volverAlInicio(driver, urlInicio);
            }
        }

        throw new AssertionError("No se completaron los " + TOTAL_CICLOS + " ciclos de la matriz.");
    }

    private int[] sumarCoordenadas(String texto) {
        Matcher matcher = COORDENADA.matcher(texto);
        int sumaX = 0;
        int sumaY = 0;
        int cantidad = 0;
        while (matcher.find()) {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            sumaX += x;
            sumaY += y;
            cantidad++;
            System.out.println("Coordenada: (" + x + "," + y + ")");
        }
        if (cantidad == 0) {
            throw new IllegalStateException("No se encontraron coordenadas en la página");
        }
        System.out.println("Suma de coordenadas: (" + sumaX + "," + sumaY + ")");
        return new int[]{sumaX, sumaY};
    }

    private WebElement grilla(WebDriver driver) {
        List<WebElement> grillas = driver.findElements(By.cssSelector("form div.grid"));
        if (grillas.isEmpty()) {
            throw new IllegalStateException("No se encontró la grilla de botones dentro del formulario");
        }
        return grillas.get(0);
    }

    private int columnas(WebElement grilla) {
        String clase = grilla.getAttribute("class");
        Matcher matcher = COLUMNAS.matcher(clase == null ? "" : clase);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        int botones = grilla.findElements(By.cssSelector("button[name='button_option']")).size();
        int lado = (int) Math.sqrt(botones);
        if (lado * lado != botones) {
            throw new IllegalStateException("No se pudo determinar el número de columnas");
        }
        return lado;
    }

    private boolean esperarMatriz(WebDriver driver) {
        long limite = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < limite) {
            if (esExito(driver) || !driver.findElements(By.cssSelector("form button[name='button_option']")).isEmpty()) {
                return true;
            }
            pausa();
        }
        return esExito(driver);
    }

    private int[][] leerMatriz(List<WebElement> botones, int filas, int columnas) {
        int[][] matriz = new int[filas][columnas];
        for (int fila = 0; fila < filas; fila++) {
            for (int columna = 0; columna < columnas; columna++) {
                String valor = botones.get(fila * columnas + columna).getAttribute("value");
                matriz[fila][columna] = Integer.parseInt(valor.trim());
            }
        }
        return matriz;
    }

    private void responderPreguntaExtra(WebDriver driver, int[][] matriz, int columna, int fila) {
        WebElement respuesta = esperarCampoRespuesta(driver);
        if (respuesta == null) {
            System.out.println("No apareció la pregunta extra. " + diagnosticoClick(driver));
            return;
        }

        String pregunta = preguntaVisible(driver);
        long resultado = operar(pregunta, matriz, columna, fila);
        System.out.println("Pregunta extra: " + pregunta);
        System.out.println("Resultado a enviar: " + resultado);

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].focus(); arguments[0].value = arguments[1];"
                        + "arguments[0].dispatchEvent(new Event('input', {bubbles:true}));"
                        + "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                respuesta, String.valueOf(resultado));
        WebElement enviar = botonEnviar(driver);
        clickSeguro(driver, enviar);
        if (campoVisible(driver) != null) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].closest('form').submit();", respuesta);
        }
        esperarResultado(driver);
    }

    private long operar(String pregunta, int[][] matriz, int columna, int fila) {
        String texto = normalizar(pregunta);
        boolean porFila = texto.contains("fila");
        boolean porColumna = texto.contains("columna");
        if (!porFila && !porColumna) {
            throw new IllegalStateException("La pregunta extra no indica fila ni columna: " + pregunta);
        }

        List<Integer> valores = new ArrayList<>();
        if (porFila) {
            for (int c = 0; c < matriz[fila].length; c++) {
                valores.add(matriz[fila][c]);
                System.out.println("Fila " + fila + ", columna " + c + ": " + matriz[fila][c]);
            }
        }
        if (porColumna) {
            for (int f = 0; f < matriz.length; f++) {
                if (porFila && f == fila) {
                    continue;
                }
                valores.add(matriz[f][columna]);
                System.out.println("Columna " + columna + ", fila " + f + ": " + matriz[f][columna]);
            }
        }

        if (texto.contains("multiplic")) {
            long producto = 1;
            for (int valor : valores) {
                producto *= valor;
            }
            return producto;
        }
        if (texto.contains("rest")) {
            long resta = valores.get(0);
            for (int i = 1; i < valores.size(); i++) {
                resta -= valores.get(i);
            }
            return resta;
        }
        long suma = 0;
        for (int valor : valores) {
            suma += valor;
        }
        return suma;
    }

    private String preguntaVisible(WebDriver driver) {
        for (WebElement titulo : driver.findElements(By.cssSelector(".bootbox h2, .modal-body h2"))) {
            String texto = titulo.getText().replaceAll("\\s+", " ").trim();
            if (!texto.isEmpty()) {
                return texto;
            }
        }
        Matcher matcher = PREGUNTA_HTML.matcher(driver.getPageSource());
        if (matcher.find()) {
            return matcher.group(1).replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
        }
        return "";
    }

    private WebElement botonEnviar(WebDriver driver) {
        List<WebElement> enviar = driver.findElements(By.cssSelector(
                ".bootbox button[type='submit'], form[action*='answer'] button[type='submit']"));
        for (WebElement boton : enviar) {
            if (boton.isDisplayed()) {
                return boton;
            }
        }
        throw new IllegalStateException("No se encontró el botón para enviar la pregunta extra");
    }

    private String textoCoordenadas(WebDriver driver) {
        List<WebElement> ecuaciones = driver.findElements(By.cssSelector("p.text-center.text-xl.font-bold"));
        for (WebElement ecuacion : ecuaciones) {
            String texto = ecuacion.getText();
            if (texto.contains("(")) {
                return texto;
            }
        }
        return textoPagina(driver);
    }

    private boolean esPaginaIncorrecta(WebDriver driver) {
        try {
            String url = driver.getCurrentUrl().toLowerCase(Locale.ROOT);
            if (url.contains("/wrong") || url.contains("incorrect")) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        for (WebElement enlace : driver.findElements(ENLACE_VOLVER)) {
            try {
                if (enlace.isDisplayed()) {
                    return true;
                }
            } catch (Exception ignored) {
                // El enlace desapareció mientras se revisaba.
            }
        }
        return false;
    }

    private void volverAlInicio(WebDriver driver, String urlInicio) {
        if (urlInicio != null && !urlInicio.isBlank()) {
            driver.get(urlInicio);
            return;
        }
        for (WebElement enlace : driver.findElements(ENLACE_VOLVER)) {
            if (enlace.isDisplayed()) {
                enlace.click();
                return;
            }
        }
        driver.navigate().refresh();
    }

    private boolean ventanaAbierta(WebDriver driver) {
        try {
            return !driver.getWindowHandles().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private WebElement esperarCampoRespuesta(WebDriver driver) {
        long limite = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < limite) {
            WebElement visible = campoVisible(driver);
            if (visible != null || esExito(driver)) {
                return visible;
            }
            pausa();
        }
        return campoVisible(driver);
    }

    private WebElement campoVisible(WebDriver driver) {
        for (WebElement campo : driver.findElements(By.cssSelector("input[name='modal_answer'], input[type='number'], input[type='text']"))) {
            try {
                if (campo.isDisplayed() && !"username".equals(campo.getAttribute("name"))
                        && !"password".equals(campo.getAttribute("name"))) {
                    return campo;
                }
            } catch (Exception ignored) {
                // El elemento desapareció mientras se revisaba.
            }
        }
        return null;
    }

    private void esperarResultado(WebDriver driver) {
        long limite = System.currentTimeMillis() + 8000;
        String inicial = textoCiclo(driver);
        while (System.currentTimeMillis() < limite) {
            if (esExito(driver) || !textoCiclo(driver).equals(inicial) || hayError(driver)) {
                return;
            }
            pausa();
        }
    }

    private boolean hayError(WebDriver driver) {
        for (WebElement error : driver.findElements(By.cssSelector("p.text-red-500"))) {
            if (!error.getText().isBlank()) {
                System.out.println("Mensaje de error: " + error.getText());
                return true;
            }
        }
        return false;
    }

    private String textoCercano(WebDriver driver, WebElement campo) {
        try {
            return (String) ((JavascriptExecutor) driver).executeScript(
                    "var nodo = arguments[0]; while (nodo && nodo.innerText && nodo.innerText.length < 20) nodo = nodo.parentElement; return nodo ? nodo.innerText : '';",
                    campo);
        } catch (Exception e) {
            return "";
        }
    }

    private String diagnosticoClick(WebDriver driver) {
        try {
            Object funcion = ((JavascriptExecutor) driver).executeScript(
                    "return (typeof click === 'function') ? click.toString() : 'sin funcion click';");
            return String.valueOf(funcion);
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private boolean esExito(WebDriver driver) {
        String texto = normalizar(textoPagina(driver));
        if (texto.contains("felicidades") || texto.contains("has terminado la prueba")) {
            return true;
        }
        return textoCiclo(driver).isEmpty() && texto.contains("hash") && pareceHash(extraerHash(driver));
    }

    private String textoCiclo(WebDriver driver) {
        List<WebElement> mensajes = driver.findElements(By.xpath(
                "//*[self::span or self::p][contains(.,'Se encuentra en el ciclo')]"));
        if (mensajes.isEmpty()) {
            return "";
        }
        for (WebElement mensaje : mensajes) {
            if ("span".equalsIgnoreCase(mensaje.getTagName())) {
                return mensaje.getText().replaceAll("\\s+", " ").trim();
            }
        }
        return mensajes.get(0).getText().replaceAll("\\s+", " ").trim();
    }

    private int numeroCiclo(WebDriver driver) {
        Matcher matcher = CICLO.matcher(textoCiclo(driver));
        if (!matcher.find()) {
            return -1;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private String extraerHash(WebDriver driver) {
        for (WebElement candidato : driver.findElements(By.cssSelector(
                "p.text-white, p.break-all, code, pre"))) {
            String texto = candidato.getText().replaceAll("\\s+", "").trim();
            if (pareceHash(texto)) {
                return texto;
            }
        }
        String mejor = "";
        Matcher matcher = Pattern.compile("[A-Za-z0-9+/=_\\-]{16,}").matcher(textoPagina(driver));
        while (matcher.find()) {
            if (matcher.group().length() > mejor.length()) {
                mejor = matcher.group();
            }
        }
        return mejor;
    }

    private boolean pareceHash(String texto) {
        return texto != null && texto.matches("[A-Za-z0-9+/=_\\-]{16,}");
    }

    private void clickBotonMatriz(WebDriver driver, WebElement elemento) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});"
                        + "if (typeof button_click === 'function') { button_click(arguments[0]); }"
                        + "else { arguments[0].click(); }",
                elemento);
    }

    private void clickSeguro(WebDriver driver, WebElement elemento) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", elemento);
            elemento.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", elemento);
        }
    }

    private String textoPagina(WebDriver driver) {
        try {
            return driver.findElement(By.tagName("body")).getText();
        } catch (Exception e) {
            return "";
        }
    }

    private void pausa() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private <T extends Actor> void guardarExito(T actor, WebDriver driver) {
        String pagina = textoPagina(driver);
        String hash = extraerHash(driver);
        actor.remember("pruebaCompletada", true);
        actor.remember("hashExito", hash);
        System.out.println("\n==============================================");
        System.out.println("MATRIZ COMPLETADA");
        System.out.println(pagina);
        System.out.println("HASH FINAL: " + hash);
        System.out.println("==============================================\n");
    }

    public static ResolverMatriz duranteDiezCiclos() {
        return Instrumented.instanceOf(ResolverMatriz.class).withProperties();
    }
}
