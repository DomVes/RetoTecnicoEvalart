# Reto técnico Evalart

Este repositorio tiene dos proyectos independientes. Cada uno automatiza una prueba distinta del sitio de Evalart, con su propio Gradle, sus credenciales y su reporte.

| Proyecto | Qué automatiza | Documentación |
| --- | --- | --- |
| `formulario` | Diez ciclos de un formulario con dos operaciones, una letra repetida y una fecha. | [formulario/README.md](formulario/README.md) |
| `matriz` | Diez ciclos de una matriz de botones localizada por coordenadas, más la pregunta extra. | [matriz/README.md](matriz/README.md) |

Los dos comparten el mismo stack: Java 11, Gradle 7.4, Serenity BDD 2.0.80, Cucumber a través de `serenity-cucumber` 1.9.45, Selenium 3.141.59 y WebDriverManager 5.9.2. El detalle de versiones, la estructura y la forma de ejecutarlos está en el README de cada carpeta.

La URL base está en la hoja `Dominios` de cada `Data.xlsx`:

```text
https://tasks.evalartapp.com/automatization/
```

Las credenciales no son las mismas. Cada `IniciarSesion.java` tiene las de su prueba, y el sitio abre el formulario o la matriz según ese acceso.

## Resultados de la última ejecución

| Prueba | Hash |
| --- | --- |
| Formulario | `c2fe0e21ce445033Mi00LTcxNTczNDE=df63afbf4da3d4b1` |
| Matriz | `c2fe0e21ce445033My0zLTcxNTczNDE=df63afbf4da3d4b1` |

Cada hash también quedó como evidencia en `target/site/serenity/index.html` dentro de su proyecto.

## Cómo ejecutar

Cada comando se lanza dentro de su carpeta, porque `Data.xlsx` se lee con una ruta relativa.

```bat
cd formulario
gradlew.bat test aggregate
```

```bat
cd matriz
gradlew.bat test aggregate
```

Hace falta JDK 11, Google Chrome y conexión a internet. ChromeDriver lo resuelve WebDriverManager.
