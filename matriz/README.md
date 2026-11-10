# Reto Evalart — Matriz de coordenadas

Automatización del segundo reto de Evalart. Después de iniciar sesión, la página muestra una lista de coordenadas y una matriz de botones. Hay que localizar el botón correcto, responder la pregunta extra que aparece y repetir el proceso durante **10 ciclos**. Si una respuesta es incorrecta, el contador vuelve a `Se encuentra en el ciclo 1 de 10`. Al completar el último ciclo, la página muestra el hash.

La última ejecución completó los diez ciclos sin reinicios y registró este hash en el reporte de Serenity:

```text
c2fe0e21ce445033My0zLTcxNTczNDE=df63afbf4da3d4b1
```

## Qué hace la página

Arriba aparece el texto `Usa las siguientes coordenadas para hallar el botón correcto`, seguido de pares como `(7,0),(-1,0),(-4,6),(2,5),(-1,-6)`. A un lado, un párrafo indica el ciclo: `Se encuentra en el ciclo N de 10`.

La matriz está dentro de un `form`. Es un `div` con las clases `grid` y `grid-cols-N`, y cada celda es un botón:

```html
<button type="button" name="button_option" onclick="button_click(this)" value="33">33</button>
```

El número de columnas sale de la clase `grid-cols-12` (u otro tamaño si la página lo cambia). El origen `(0,0)` es la esquina superior izquierda. La primera cifra del par es la columna y la segunda es la fila.

Al pulsar un botón, la página envía su `value` a `/automatization/buttons/api/answer`.

- Si el botón no es el correcto, redirige a `/automatization/buttons/wrong` y el ciclo se reinicia.
- Si es el correcto, abre un cuadro **Pregunta extra**. En la ejecución observada el enunciado fue `Suma todos los numeros en la fila de la respuesta`, con un campo `Suma total` y un botón **Enviar**. Esa respuesta se envía por POST a `/automatization/buttons/answer`.

## Cómo lo resuelve la automatización

El escenario de Cucumber tiene tres pasos:

1. Abre la URL de `Data.xlsx` e inicia sesión con las credenciales de la matriz.
2. Resuelve los diez ciclos en `ResolverMatriz`.
3. Comprueba el hash y lo publica en el reporte.

En cada ciclo `ResolverMatriz` hace lo siguiente:

1. Lee solo el párrafo de las coordenadas, para no mezclar otros números de la página.
2. Suma todas las `x` y todas las `y`. El ejemplo anterior da `(3,5)`.
3. Cuenta las columnas de la grilla y guarda el valor de cada botón.
4. Calcula el índice `fila * columnas + columna` y pulsa ese botón llamando a `button_click`.
5. Espera el cuadro de la pregunta extra y lee el enunciado.
6. Si el enunciado habla de **fila**, suma los números de la fila del botón elegido. Si habla de **columna**, suma esa columna. Si el texto pide multiplicar o restar, aplica esa operación sobre los mismos valores.
7. Escribe el resultado en `input[name='modal_answer']` y envía el formulario del cuadro.
8. Comprueba que el párrafo haya pasado al ciclo siguiente. Si la URL contiene `/wrong`, si aparece **Regresar** o si el ciclo no aumentó, vuelve a la URL inicial y el conteo de avances regresa a cero.
9. Al ver `Felicidades` o `has terminado la prueba`, guarda el hash.

El bucle permite hasta 40 intentos, de modo que un reinicio no termina la prueba de inmediato.

### Inicio de sesión

`IniciarSesion` usa el usuario y la contraseña propios de la matriz, distintos de los del formulario. Si el sitio responde `Id incorrecto, intenta otra vez`, recarga el login y lo reintenta hasta 10 veces. Después espera a que exista la grilla `button[name='button_option']`. Si la página muestra un error con enlace **Back** o **Regresar**, lo pulsa y vuelve a la prueba.

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

`build.gradle` declara las variables `serenityVersion` y `serenityCucumberVersion` en 3.3.4, pero el bloque `dependencies` no las usa. Las librerías del classpath son las de la tabla.

El flujo activo sigue el patrón Screenplay: el actor abre Chrome, `IniciarSesion` y `ResolverMatriz` son las tareas, y Cucumber describe el escenario en español.

## Requisitos

- JDK 11.
- Google Chrome instalado. WebDriverManager descarga el ChromeDriver de la misma versión.
- Internet para resolver dependencias la primera vez y para abrir `https://tasks.evalartapp.com/automatization/` en cada ejecución.
- Ejecutar los comandos desde la carpeta `matriz`, porque `Data.xlsx` se abre con una ruta relativa.

## Estructura

```text
matriz/
├── build.gradle
├── settings.gradle                  # nombre del proyecto: reto-matriz
├── Data.xlsx                        # hoja Dominios, celda A2: URL de la prueba
├── gradlew / gradlew.bat
├── gradle/wrapper/                  # Gradle 7.4
└── src/
    ├── main/java/com/evalart/automation/
    │   ├── drivers/ChromeDriver.java
    │   ├── tasks/
    │   │   ├── IniciarSesion.java
    │   │   └── ResolverMatriz.java
    │   ├── userinterface/
    │   │   ├── IniciarSesionUI.java
    │   │   └── AutomatizarCoordenadasUI.java
    │   └── utils/Excel.java
    └── test/
        ├── java/com/evalart/automation/
        │   ├── runners/ValidarCoordenadasRunner.java
        │   └── stepdefinitions/AutomatizarBotonesStepDefinitions.java
        └── resources/features/automatizar_botones.feature
```

| Archivo | Responsabilidad |
| --- | --- |
| `automatizar_botones.feature` | Escenario en Gherkin: login, selección del botón y hash. |
| `ValidarCoordenadasRunner` | Ejecuta ese feature con Serenity y Cucumber. |
| `AutomatizarBotonesStepDefinitions` | Crea el actor, lanza las dos tareas y guarda el hash como evidencia del reporte. |
| `IniciarSesion` | Login de la matriz, con reintentos y espera de la grilla. |
| `ResolverMatriz` | Suma las coordenadas, pulsa el botón, responde la pregunta extra y vigila el ciclo. |
| `IniciarSesionUI` | Localizadores del formulario de acceso. |
| `ChromeDriver` | Chrome maximizado, en incógnito, con el driver resuelto por WebDriverManager. |
| `Excel` | Lee la URL de la hoja `Dominios`, fila 2, columna A. |

En `tasks` quedan tres clases de un intento anterior (`ResolverCoordenadas`, `ExtraerMatriz` y `Answers`) y los localizadores de `AutomatizarCoordenadasUI`. El runner no las usa. El flujo que completa los diez ciclos es `ResolverMatriz`.

## Configuración

La hoja `Dominios` de `Data.xlsx` guarda la URL en la celda A2:

```text
https://tasks.evalartapp.com/automatization/
```

Es la misma dirección base que el proyecto del formulario. El usuario y la contraseña, definidos en `IniciarSesion.java`, son los que llevan a la prueba de la matriz.

## Ejecución

Desde la carpeta `matriz`:

```bat
gradlew.bat test aggregate
```

Con dependencias ya descargadas:

```bat
gradlew.bat test aggregate --offline
```

`test` ejecuta `ValidarCoordenadasRunner`. `aggregate` construye el reporte de Serenity. Chrome se abre en una ventana visible. Una pasada correcta de los diez ciclos tarda alrededor de dos minutos.

## Reportes

- Reporte Serenity: `target/site/serenity/index.html`. En el paso `deberia ver el hash de exito` hay una evidencia **Hash de éxito** con el código obtenido.
- Reporte de Gradle: `build/reports/tests/test/index.html`.
- La consola muestra cada coordenada, la suma, el valor del botón, los números de la fila o la columna, el resultado enviado y la confirmación `El ciclo aumentó a "Se encuentra en el ciclo N de 10"`. Al terminar imprime `HASH FINAL`.

## Qué ocurre si algo falla

- **Id incorrecto.** Se reintenta el login hasta 10 veces.
- **Coordenada fuera de la grilla.** Se recarga la prueba y el conteo vuelve a empezar.
- **Botón incorrecto.** La página navega a `/wrong`. La tarea lo detecta, regresa a la URL inicial y reinicia los ciclos.
- **La pregunta extra no menciona fila ni columna.** La tarea se detiene e imprime el enunciado, en lugar de enviar un número al azar.
- **El ciclo no aumenta después de enviar la suma.** Se trata como un fallo y se comienza de nuevo.
- **La ventana de Chrome se cierra.** El error sale de inmediato, porque ya no hay una sesión que se pueda recuperar.
- **40 intentos sin llegar al ciclo 10.** La prueba falla indicando que no se completaron los ciclos.
