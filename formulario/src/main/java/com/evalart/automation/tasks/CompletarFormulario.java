package com.evalart.automation.tasks;

import net.serenitybdd.core.steps.Instrumented;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import net.serenitybdd.screenplay.abilities.BrowseTheWeb;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompletarFormulario implements Task {

    private static final int MAX_ENVIOS = 40;
    private static final int TOTAL_CICLOS = 10;
    private static final DateTimeFormatter FECHA_LATINA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern CICLO = Pattern.compile("Se encuentra en el ciclo\\s+(\\d+)\\s+de\\s+(\\d+)");
    private static final Pattern LETRA = Pattern.compile(
            "(?i)escriba\\s+(\\d+)\\s+veces\\s+la\\s+letra\\s+[\"“']([^\"“”']+)[\"”']");
    private static final Pattern FECHA = Pattern.compile(
            "(?i)(\\d+)\\s+dias\\s+contados\\s+desde\\s+(.*?)\\s*:\\s*(\\d{1,2})/(\\d{1,2})/(\\d{4})");
    private static final Pattern OPERACION = Pattern.compile("-?\\d+(?:\\s*[+\\-*]\\s*-?\\d+)+");
    private static final By ENLACE_VOLVER = By.xpath(
            "//a[contains(normalize-space(.),'Back') or contains(normalize-space(.),'Regresar')]");

    @Override
    public <T extends Actor> void performAs(T actor) {
        WebDriver driver = BrowseTheWeb.as(actor).getDriver();
        String urlInicio = null;
        int avances = 0;

        for (int envio = 1; envio <= MAX_ENVIOS; envio++) {
            if (esError500(driver)) {
                System.out.println("La página mostró Error 500. Se vuelve a abrir la prueba.");
                if (!clickVolver(driver)) {
                    driver.navigate().refresh();
                }
                avances = 0;
                continue;
            }

            if (!esperarCiclo(driver)) {
                System.out.println("No apareció el ciclo del formulario en el intento " + envio + ".");
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
            System.out.println("==============================================");

            List<WebElement> tarjetas = tarjetas(driver);
            mapearYResolver(driver, tarjetas);

            String textoAntes = textoPagina(driver);
            enviar(driver);
            esperarCambio(driver, textoAntes);

            if (esExito(driver)) {
                actor.remember("ciclosValidados", avances + 1);
                guardarExito(actor, driver);
                return;
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
        }

        throw new AssertionError("No se completaron los " + TOTAL_CICLOS + " ciclos.");
    }

    private boolean esperarCiclo(WebDriver driver) {
        long limite = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < limite) {
            if (esExito(driver)) {
                return true;
            }
            if (!textoCiclo(driver).isEmpty()) {
                return true;
            }
            if (esError500(driver)) {
                return false;
            }
            pausa();
        }
        return !textoCiclo(driver).isEmpty() || esExito(driver);
    }

    private List<WebElement> tarjetas(WebDriver driver) {
        List<WebElement> tarjetas = new ArrayList<>();
        List<WebElement> divs = driver.findElements(By.xpath("//div[.//textarea or .//select or .//input[not(@type='hidden')]]"));
        for (WebElement div : divs) {
            if (contarPreguntas(div.getText()) != 1) {
                continue;
            }
            boolean hayTarjetaMasPequena = false;
            for (WebElement hijo : div.findElements(By.xpath("./div"))) {
                if (contarPreguntas(hijo.getText()) == 1 && tieneControl(hijo)) {
                    hayTarjetaMasPequena = true;
                    break;
                }
            }
            if (!hayTarjetaMasPequena) {
                tarjetas.add(div);
            }
        }
        return tarjetas;
    }

    private void mapearYResolver(WebDriver driver, List<WebElement> tarjetas) {
        WebElement cuadroCheckbox = null;
        WebElement cuadroLetra = null;
        WebElement cuadroFecha = null;
        WebElement cuadroSelect = null;

        for (WebElement tarjeta : tarjetas) {
            String texto = tarjeta.getText();
            String normalizado = normalizar(texto);
            if (LETRA.matcher(texto).find()) {
                cuadroLetra = tarjeta;
            } else if (FECHA.matcher(normalizado).find()) {
                cuadroFecha = tarjeta;
            } else if (!tarjeta.findElements(By.cssSelector("input[type='checkbox']")).isEmpty()) {
                cuadroCheckbox = tarjeta;
            } else if (cuadroCheckbox == null && !tarjeta.findElements(By.cssSelector("input[type='radio']")).isEmpty()) {
                cuadroCheckbox = tarjeta;
            } else if (!tarjeta.findElements(By.tagName("select")).isEmpty()) {
                cuadroSelect = tarjeta;
            }
        }

        if (cuadroCheckbox == null || cuadroLetra == null || cuadroFecha == null || cuadroSelect == null) {
            System.out.println("No se mapearon los cuatro cuadros. Texto de la página:");
            System.out.println(textoPagina(driver));
            for (WebElement tarjeta : tarjetas) {
                System.out.println("TARJETA checkbox=" + tarjeta.findElements(By.cssSelector("input[type='checkbox']")).size()
                        + " radio=" + tarjeta.findElements(By.cssSelector("input[type='radio']")).size()
                        + " select=" + tarjeta.findElements(By.tagName("select")).size()
                        + " date=" + tarjeta.findElements(By.cssSelector("input[type='date'], [type='date']")).size()
                        + " texto=" + tarjeta.getText().replaceAll("\\s+", " ").trim());
            }
            throw new IllegalStateException("No se mapearon los cuatro cuadros");
        }

        System.out.println("Mapeo: checkbox, letra, fecha y select.");
        resolverTarjeta(driver, cuadroCheckbox);
        resolverTarjeta(driver, cuadroLetra);
        resolverTarjeta(driver, cuadroFecha);
        resolverTarjeta(driver, cuadroSelect);
    }

    private void resolverTarjeta(WebDriver driver, WebElement tarjeta) {
        String texto = tarjeta.getText();
        String normalizado = normalizar(texto);
        System.out.println("Pregunta: " + texto.replaceAll("\\s+", " ").trim());

        Matcher letra = LETRA.matcher(texto);
        if (letra.find()) {
            int veces = Integer.parseInt(letra.group(1));
            String contenido = letra.group(2).repeat(veces);
            escribir(driver, campoDeTexto(tarjeta), contenido);
            System.out.println("Letra repetida " + veces + " veces.");
            return;
        }

        Matcher fecha = FECHA.matcher(normalizado);
        if (fecha.find()) {
            int dias = Integer.parseInt(fecha.group(1));
            boolean haciaAtras = normalizar(fecha.group(2)).contains("antes");
            LocalDate inicio = LocalDate.of(
                    Integer.parseInt(fecha.group(5)),
                    Integer.parseInt(fecha.group(4)),
                    Integer.parseInt(fecha.group(3)));
            LocalDate resultado = haciaAtras ? inicio.minusDays(dias) : inicio.plusDays(dias);
            WebElement campo = campoFecha(tarjeta);
            String valor = "date".equalsIgnoreCase(campo.getAttribute("type"))
                    ? resultado.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    : resultado.format(FECHA_LATINA);
            asignarValor(driver, campo, valor);
            System.out.println("Fecha calculada: " + resultado.format(FECHA_LATINA));
            return;
        }

        Matcher operacion = OPERACION.matcher(texto.replace("=?", ""));
        if (operacion.find() || normalizado.contains("operacion matematica")) {
            String expresion = operacionMasLarga(texto);
            long resultado = evaluar(expresion);
            String respuesta = String.valueOf(resultado);
            System.out.println("Operación " + expresion + " = " + respuesta);
            if (!tarjeta.findElements(By.cssSelector("input[type='checkbox']")).isEmpty()) {
                seleccionarCheckbox(driver, tarjeta, respuesta);
            } else if (!tarjeta.findElements(By.tagName("select")).isEmpty()) {
                seleccionarOpcion(tarjeta.findElement(By.tagName("select")), respuesta);
            } else if (!tarjeta.findElements(By.cssSelector("input[type='radio']")).isEmpty()) {
                seleccionarRadio(driver, tarjeta, respuesta);
            } else {
                escribir(driver, campoDeTexto(tarjeta), respuesta);
            }
            return;
        }

        if (!tarjeta.findElements(By.cssSelector("input[type='checkbox']")).isEmpty()) {
            String expresion = operacionMasLarga(texto);
            seleccionarCheckbox(driver, tarjeta, String.valueOf(evaluar(expresion)));
            return;
        }

        throw new IllegalStateException("No se pudo interpretar la pregunta: " + texto);
    }

    private String operacionMasLarga(String texto) {
        String mejor = "";
        for (String linea : texto.split("\\R")) {
            int corte = linea.indexOf("=?");
            String zona = corte >= 0 ? linea.substring(0, corte) : linea;
            if (!zona.contains("+") && !zona.contains("*")) {
                continue;
            }
            Matcher matcher = OPERACION.matcher(zona);
            while (matcher.find()) {
                if (matcher.group().length() > mejor.length()) {
                    mejor = matcher.group();
                }
            }
        }
        if (mejor.isEmpty()) {
            int corte = texto.indexOf("=?");
            String zona = corte >= 0 ? texto.substring(0, corte) : texto;
            Matcher matcher = OPERACION.matcher(zona);
            if (matcher.find()) {
                mejor = matcher.group();
            }
        }
        if (mejor.isEmpty()) {
            throw new IllegalStateException("No se encontró la operación matemática");
        }
        return mejor.replaceAll("\\s+", "");
    }

    static long evaluar(String expresion) {
        List<Long> numeros = new ArrayList<>();
        List<Character> operadores = new ArrayList<>();
        int indice = 0;
        while (indice < expresion.length()) {
            int fin = indice;
            if (expresion.charAt(fin) == '-' && numeros.isEmpty()) {
                fin++;
            }
            while (fin < expresion.length() && Character.isDigit(expresion.charAt(fin))) {
                fin++;
            }
            numeros.add(Long.parseLong(expresion.substring(indice, fin)));
            indice = fin;
            if (indice < expresion.length()) {
                operadores.add(expresion.charAt(indice));
                indice++;
            }
        }

        for (int i = 0; i < operadores.size();) {
            if (operadores.get(i) == '*') {
                numeros.set(i, numeros.get(i) * numeros.get(i + 1));
                numeros.remove(i + 1);
                operadores.remove(i);
            } else {
                i++;
            }
        }

        long total = numeros.get(0);
        for (int i = 0; i < operadores.size(); i++) {
            if (operadores.get(i) == '+') {
                total += numeros.get(i + 1);
            } else {
                total -= numeros.get(i + 1);
            }
        }
        return total;
    }

    private void seleccionarRadio(WebDriver driver, WebElement tarjeta, String respuesta) {
        for (WebElement radio : tarjeta.findElements(By.cssSelector("input[type='radio']"))) {
            if (coincideNumero(radio.getAttribute("value"), respuesta) || coincideNumero(textoCercano(driver, radio), respuesta)) {
                clickSeguro(driver, radio);
                System.out.println("Radio seleccionado: " + respuesta);
                return;
            }
        }
        throw new IllegalStateException("Ningún radio coincide con " + respuesta);
    }

    private void seleccionarOpcion(WebElement lista, String respuesta) {
        Select select = new Select(lista);
        for (WebElement opcion : select.getOptions()) {
            String valorOpcion = opcion.getAttribute("value");
            if (coincideNumero(opcion.getText(), respuesta) || respuesta.equals(valorOpcion == null ? "" : valorOpcion.trim())) {
                select.selectByVisibleText(opcion.getText());
                System.out.println("Opción seleccionada: " + opcion.getText().trim());
                return;
            }
        }
        throw new IllegalStateException("Ninguna opción coincide con " + respuesta);
    }

    private void seleccionarCheckbox(WebDriver driver, WebElement tarjeta, String respuesta) {
        boolean encontro = false;
        for (WebElement casilla : tarjeta.findElements(By.cssSelector("input[type='checkbox']"))) {
            boolean coincide = coincideNumero(casilla.getAttribute("value"), respuesta)
                    || coincideNumero(textoCercano(driver, casilla), respuesta);
            if (coincide && !casilla.isSelected()) {
                clickSeguro(driver, casilla);
                encontro = true;
            } else if (!coincide && casilla.isSelected()) {
                clickSeguro(driver, casilla);
            } else if (coincide) {
                encontro = true;
            }
        }
        if (!encontro) {
            throw new IllegalStateException("Ningún checkbox coincide con " + respuesta);
        }
        System.out.println("Checkbox marcado: " + respuesta);
    }

    private WebElement campoFecha(WebElement tarjeta) {
        List<WebElement> fechas = tarjeta.findElements(By.cssSelector("input[type='date']"));
        if (!fechas.isEmpty()) {
            return fechas.get(0);
        }
        fechas = tarjeta.findElements(By.cssSelector("[type='date']"));
        if (fechas.isEmpty()) {
            throw new IllegalStateException("No se encontró el campo de fecha");
        }
        WebElement campo = fechas.get(0);
        if (!"input".equalsIgnoreCase(campo.getTagName())) {
            List<WebElement> interno = campo.findElements(By.cssSelector("input"));
            if (!interno.isEmpty()) {
                return interno.get(0);
            }
        }
        return campo;
    }

    private WebElement campoDeTexto(WebElement tarjeta) {
        List<WebElement> areas = tarjeta.findElements(By.tagName("textarea"));
        if (!areas.isEmpty()) {
            return areas.get(0);
        }
        List<WebElement> campos = tarjeta.findElements(By.cssSelector("input[type='text'], input[type='number'], input:not([type])"));
        if (!campos.isEmpty()) {
            return campos.get(0);
        }
        throw new IllegalStateException("La pregunta no tiene un campo de texto");
    }

    private String textoCercano(WebDriver driver, WebElement control) {
        return (String) ((JavascriptExecutor) driver).executeScript(
                "const control = arguments[0];"
                        + "let nodo = control.nextSibling;"
                        + "while (nodo) {"
                        + "  const texto = (nodo.textContent || '').trim();"
                        + "  if (texto) return texto;"
                        + "  nodo = nodo.nextSibling;"
                        + "}"
                        + "if (control.id) {"
                        + "  const etiqueta = document.querySelector('label[for=\"' + CSS.escape(control.id) + '\"]');"
                        + "  if (etiqueta && etiqueta.querySelectorAll('input').length === 0) return etiqueta.innerText.trim();"
                        + "}"
                        + "const padre = control.parentElement;"
                        + "if (padre && padre.tagName === 'LABEL' && padre.querySelectorAll('input').length === 1) {"
                        + "  return padre.innerText.trim();"
                        + "}"
                        + "return control.value || '';",
                control);
    }

    private boolean coincideNumero(String texto, String respuesta) {
        if (texto == null) {
            return false;
        }
        String limpio = texto.trim().replace(" ", "");
        if (limpio.equals(respuesta)) {
            return true;
        }
        Matcher matcher = Pattern.compile("-?\\d+").matcher(texto);
        while (matcher.find()) {
            if (matcher.group().equals(respuesta)) {
                return true;
            }
        }
        return false;
    }

    private void escribir(WebDriver driver, WebElement elemento, String valor) {
        try {
            elemento.click();
            elemento.clear();
            elemento.sendKeys(valor);
        } catch (Exception e) {
            asignarValor(driver, elemento, valor);
        }
    }

    private void asignarValor(WebDriver driver, WebElement elemento, String valor) {
        ((JavascriptExecutor) driver).executeScript(
                "const campo = arguments[0];"
                        + "campo.focus();"
                        + "campo.value = arguments[1];"
                        + "campo.dispatchEvent(new Event('input', {bubbles:true}));"
                        + "campo.dispatchEvent(new Event('change', {bubbles:true}));",
                elemento, valor);
    }

    private void enviar(WebDriver driver) {
        List<WebElement> botones = driver.findElements(By.xpath(
                "//button[normalize-space(.)='Enviar'] | //input[@type='submit' or normalize-space(@value)='Enviar']"));
        if (botones.isEmpty()) {
            throw new IllegalStateException("No se encontró el botón Enviar");
        }
        WebElement boton = botones.get(0);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", boton);
        clickSeguro(driver, boton);
    }

    private void esperarCambio(WebDriver driver, String textoAnterior) {
        try {
            new WebDriverWait(driver, 15).until(navegador ->
                    esExito(navegador) || !textoPagina(navegador).equals(textoAnterior));
        } catch (TimeoutException e) {
            System.out.println("La página no cambió después de enviar.");
        }
    }

    private boolean esExito(WebDriver driver) {
        String texto = normalizar(textoPagina(driver));
        if (texto.contains("felicidades") || texto.contains("has terminado la prueba")) {
            return true;
        }
        return textoCiclo(driver).isEmpty() && pareceHash(extraerHash(driver));
    }

    private boolean esError500(WebDriver driver) {
        return textoPagina(driver).contains("Error 500");
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
        String mejor = "";
        Matcher enPagina = Pattern.compile("[A-Za-z0-9+/=_\\-]{16,}").matcher(textoPagina(driver));
        while (enPagina.find()) {
            if (enPagina.group().length() > mejor.length()) {
                mejor = enPagina.group();
            }
        }
        for (WebElement campo : driver.findElements(By.cssSelector("input, textarea"))) {
            String valor = campo.getAttribute("value");
            if (valor != null && pareceHash(valor.trim()) && valor.trim().length() > mejor.length()) {
                mejor = valor.trim();
            }
        }
        return mejor;
    }

    private boolean pareceHash(String texto) {
        return texto != null && texto.matches("[A-Za-z0-9+/=_\\-]{20,}");
    }

    private int contarPreguntas(String texto) {
        String normalizado = normalizar(texto);
        return ocurrencias(normalizado, "operacion matematica")
                + ocurrencias(normalizado, "veces la letra")
                + ocurrencias(normalizado, "contados desde");
    }

    private int ocurrencias(String texto, String fragmento) {
        int total = 0;
        int indice = 0;
        while ((indice = texto.indexOf(fragmento, indice)) >= 0) {
            total++;
            indice += fragmento.length();
        }
        return total;
    }

    private boolean tieneControl(WebElement elemento) {
        return !elemento.findElements(By.cssSelector("textarea, select, input:not([type='hidden'])")).isEmpty();
    }

    private boolean clickVolver(WebDriver driver) {
        for (WebElement enlace : driver.findElements(ENLACE_VOLVER)) {
            if (enlace.isDisplayed()) {
                clickSeguro(driver, enlace);
                return true;
            }
        }
        return false;
    }

    private void clickSeguro(WebDriver driver, WebElement elemento) {
        try {
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

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
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
        actor.remember("paginaExito", pagina);
        System.out.println("\n==============================================");
        System.out.println("FORMULARIO COMPLETADO");
        System.out.println("PAGINA DE EXITO:");
        System.out.println(pagina);
        System.out.println("HASH FINAL: " + hash);
        System.out.println("==============================================\n");
    }

    public static CompletarFormulario duranteDiezCiclos() {
        return Instrumented.instanceOf(CompletarFormulario.class).withProperties();
    }
}
