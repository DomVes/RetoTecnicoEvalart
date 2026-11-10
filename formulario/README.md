# Reto Evalart — Formulario dinámico

Automatización del primer reto de Evalart. Después de iniciar sesión, la página muestra un formulario de cuatro cuadros que cambia en cada ciclo. La prueba exige completar **10 ciclos seguidos**. Si un envío no hace avanzar el contador, el progreso vuelve a cero. Al terminar aparece un hash de éxito.

La última ejecución completó los diez ciclos y registró este hash en el reporte de Serenity:

```text
c2fe0e21ce445033Mi00LTcxNTczNDE=df63afbf4da3d4b1
```

## Qué hace la página

El indicador de progreso es un párrafo con el texto `Se encuentra en el ciclo N de 10`. En cada ciclo los valores cambian, pero siempre hay cuatro cuadros:

1. **Operación matemática.** Una expresión con sumas, restas y multiplicaciones, por ejemplo `155+142-256*143-232+24+158-99=?`. La respuesta se marca en el checkbox (o en un radio, si el cuadro llega así) cuyo valor coincide con el resultado.
2. **Repetición de una letra.** El texto pide escribir una letra una cantidad de veces, por ejemplo `Escriba 80 veces la letra "W"`. Esa cadena se escribe en el área de texto.
3. **Fecha.** El texto pide una fecha contando días hacia adelante o hacia atrás desde una fecha dada, por ejemplo `56 días contados desde el 10/11/2026`. El resultado se carga en el campo de tipo fecha.
4. **Segunda operación matemática.** Misma clase de expresión que la primera. La respuesta se elige en un `select`.

Al pulsar **Enviar**, la página valida las cuatro respuestas. Si son correctas, el párrafo pasa al ciclo siguiente. Si no, el contador se reinicia.

## Cómo lo resuelve la automatización

El escenario de Cucumber tiene tres pasos:

1. Abre la URL guardada en `Data.xlsx` e inicia sesión.
2. Resuelve los diez ciclos.
3. Comprueba que la página mostró el hash y lo deja en el reporte.

`CompletarFormulario` recorre la página en un bucle de hasta 40 intentos:

- Lee el número de ciclo antes de responder.
- Localiza los cuatro cuadros por su contenido y por el control que contienen: radio o checkbox, área de texto, campo de fecha y lista desplegable.
- Calcula cada respuesta y llena el control correspondiente.
- Envía el formulario y espera a que cambie la página.
- Compara el ciclo nuevo con el anterior. Solo cuenta un avance cuando el mensaje pasa de `N` a `N + 1`.
- Si el ciclo no aumenta, vuelve a la URL de la prueba y el contador interno de avances regresa a cero.
- Si la página muestra `Error 500`, pulsa **Regresar** o **Back** y también reinicia el conteo.
- Cuando el texto contiene `Felicidades` o `has terminado la prueba`, guarda el hash en la memoria del actor.

### Cálculos

- **Operaciones.** La multiplicación se resuelve antes que la suma y la resta. El resultado se busca entre las opciones del checkbox, del radio o del `select`, comparando el texto visible y el `value`.
- **Letra.** Se leen la cantidad y el carácter del enunciado y se repite esa letra exactamente esa cantidad de veces.
- **Fecha.** La fecha de partida se interpreta como `dd/MM/yyyy`. Si el enunciado dice «antes», se restan los días; en caso contrario se suman. Un `input type="date"` recibe el valor en formato `yyyy-MM-dd`.

### Inicio de sesión

`IniciarSesion` escribe el usuario y la contraseña del reto del formulario y pulsa el botón de envío. Si aparece `Id incorrecto, intenta otra vez`, recarga la página y lo intenta de nuevo, hasta 10 veces. El usuario y la contraseña están en `IniciarSesion.java`. La URL no está en el código: se lee de Excel.

## Tecnologías y versiones

El código compila con Java 11. La ejecución de referencia usó Amazon Corretto 11.0.32.1, Google Chrome 156 y ChromeDriver 156.0.8078.80.

| Pieza | Versión que usa el proyecto |
| --- | --- |
| Java | 11 (`sourceCompatibility`) |
| Gradle | 7.4, mediante el wrapper |
| JUnit | 4.13.2 |
| Serenity BDD (`serenity-junit`, `serenity-core`, `serenity-screenplay`, `serenity-ensure`) | 2.0.80 |
| Serenity Cucumber | 1.9.45, con la API `cucumber.api` |
| Plugin de Gradle de Serenity | 2.0.89 |
| Selenium | 3.141.59, dependencia transitiva de Serenity |
| WebDriverManager | 5.9.2 |
| Apache POI y POI OOXML | 3.17 |
| Log4j | 2.13.3 |
| SLF4J Simple | 1.7.7 |

`build.gradle` declara las variables `serenityVersion` y `serenityCucumberVersion` en 3.3.4, pero el bloque `dependencies` no las usa. Las librerías que realmente entran al classpath son las de la tabla.

El diseño sigue el patrón Screenplay de Serenity: un actor abre el navegador, las tareas hacen el trabajo y Cucumber describe el escenario en español.

## Requisitos

- JDK 11.
- Google Chrome instalado. WebDriverManager descarga el ChromeDriver compatible.
- Conexión a internet la primera vez que Gradle resuelve dependencias y cada vez que se ejecuta la prueba, porque el sitio es `https://tasks.evalartapp.com/automatization/`.
- Ejecutar Gradle desde la carpeta `formulario`. `Excel` abre `Data.xlsx` con una ruta relativa.

## Estructura

```text
formulario/
├── build.gradle
├── settings.gradle                  # nombre del proyecto: reto-formulario
├── Data.xlsx                        # hoja Dominios, celda A2: URL de la prueba
├── gradlew / gradlew.bat
├── gradle/wrapper/                  # Gradle 7.4
└── src/
    ├── main/java/com/evalart/automation/
    │   ├── drivers/ChromeDriver.java
    │   ├── tasks/
    │   │   ├── IniciarSesion.java
    │   │   └── CompletarFormulario.java
    │   ├── userinterface/IniciarSesionUI.java
    │   └── utils/Excel.java
    └── test/
        ├── java/com/evalart/automation/
        │   ├── runners/CompletarFormularioRunner.java
        │   └── stepdefinitions/CompletarFormularioStepDefinitions.java
        └── resources/features/completar_formulario.feature
```

| Archivo | Responsabilidad |
| --- | --- |
| `completar_formulario.feature` | Escenario en Gherkin: login, diez ciclos y hash. |
| `CompletarFormularioRunner` | Une Cucumber con Serenity y apunta al feature y a los step definitions. |
| `CompletarFormularioStepDefinitions` | Crea el actor `Esteban`, abre Chrome y, al final, publica el hash con `Serenity.recordReportData`. |
| `IniciarSesion` | Login con reintentos ante el mensaje de id incorrecto. |
| `CompletarFormulario` | Mapea los cuatro cuadros, calcula las respuestas y valida que el ciclo aumente. |
| `IniciarSesionUI` | Localizadores del usuario, la contraseña, el botón y el mensaje de error. |
| `ChromeDriver` | Chrome maximizado, en incógnito, con el driver resuelto por WebDriverManager. |
| `Excel` | Lee una celda de `Data.xlsx`. La prueba usa la hoja `Dominios`, fila 2, columna A. |

## Configuración

`Data.xlsx` tiene una hoja llamada `Dominios`. La primera fila es el título `Url` y la segunda fila contiene:

```text
https://tasks.evalartapp.com/automatization/
```

El usuario y la contraseña de este reto están en `src/main/java/com/evalart/automation/tasks/IniciarSesion.java`. Son distintas de las del proyecto de la matriz: el sitio decide qué prueba abrir según la credencial.

## Ejecución

Desde la carpeta `formulario`:

```bat
gradlew.bat test aggregate
```

Si las dependencias ya están en la caché de Gradle:

```bat
gradlew.bat test aggregate --offline
```

`test` ejecuta `CompletarFormularioRunner`. `aggregate` genera el reporte HTML de Serenity. La prueba abre una ventana visible de Chrome y tarda unos minutos, porque recorre los diez ciclos en el sitio real.

## Reportes

- Reporte Serenity: `target/site/serenity/index.html`. El paso `deberia ver el hash de exito` incluye una evidencia llamada **Hash de éxito**.
- Reporte de Gradle: `build/reports/tests/test/index.html`.
- La consola imprime cada pregunta, el resultado calculado y la línea `El ciclo aumentó a "Se encuentra en el ciclo N de 10"`. Al final imprime `HASH FINAL`.

## Qué ocurre si algo falla

- **Id incorrecto en el login.** Se recarga la página y se reintenta hasta 10 veces.
- **Error 500.** Se pulsa el enlace de regreso y el conteo de ciclos válidos vuelve a cero.
- **El ciclo no aumenta.** Se considera una respuesta incorrecta, se abre de nuevo la URL de la prueba y se empieza desde el ciclo 1.
- **No se reconocen los cuatro cuadros.** La tarea imprime el texto de cada tarjeta y detiene la prueba, para no enviar un formulario a medias.
- **Se agotan los 40 intentos.** La prueba falla con el mensaje de que no se completaron los 10 ciclos.
