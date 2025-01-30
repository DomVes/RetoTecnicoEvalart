# Reto Técnico Evalart - Automatización de Pruebas

## Descripción del Proyecto
Este proyecto implementa una solución automatizada para el reto técnico de Evalart, que consiste en la resolución de una matriz dinámica de botones a través de múltiples ciclos. La automatización utiliza el patrón de diseño Screenplay y está construida con Serenity BDD y Cucumber.

## Características Principales
- Automatización completa del flujo de resolución de la matriz
- Manejo dinámico de coordenadas y cálculos
- Validación de resultados en cada ciclo
- Captura y verificación del hash final
- Implementación de mejores prácticas de automatización
- Manejo robusto de errores y reintentos

## Tecnologías Utilizadas
- Java 8+
- Serenity BDD
- Cucumber
- Selenium WebDriver
- Gradle
- Patrón de Diseño Screenplay

## Estructura del Proyecto
```
src/
├── main/
│   └── java/
│       └── com/
│           └── evalart/
│               └── automation/
│                   ├── drivers/         # Configuración de WebDriver
│                   ├── tasks/           # Tareas de automatización
│                   ├── userinterface/   # Mapeo de elementos UI
│                   └── utils/           # Utilidades generales
└── test/
    ├── java/
    │   └── com/
    │       └── evalart/
    │           └── automation/
    │               └── stepdefinitions/ # Definiciones de pasos
    └── resources/
        └── features/                    # Archivos feature de Cucumber
```

## Características Técnicas Destacadas

### 1. Patrón Screenplay
- Implementación clara de tareas y acciones
- Separación de responsabilidades
- Código mantenible y escalable

### 2. Manejo de Estados
- Control preciso de ciclos de ejecución
- Almacenamiento y recuperación de estados
- Validación continua de resultados

### 3. Robustez
- Manejo de excepciones
- Reintentos automáticos
- Tiempos de espera dinámicos
- Validaciones exhaustivas

### 4. Reportes
- Generación automática de reportes Serenity
- Logs detallados de ejecución
- Capturas de pantalla en puntos clave

## Requisitos Previos
- Java JDK 8 o superior
- Gradle 7.x o superior
- Chrome Browser (versión compatible con ChromeDriver)
- Acceso a Internet

## Configuración del Proyecto

1. Clonar el repositorio:
```bash
git clone [URL_DEL_REPOSITORIO]
```

2. Instalar dependencias:
```bash
gradle clean build
```

3. Configurar credenciales:
- Crear archivo `Data.xlsx` en la carpeta de recursos
- Agregar credenciales en la hoja "Dominios"

## Ejecución de Pruebas

### Ejecución completa:
```bash
gradle clean test aggregate
```

### Ejecución por tags:
```bash
gradle clean test -Dcucumber.options="--tags @tag"
```

## Estructura de Automatización

### 1. Features
Los escenarios están escritos en Gherkin, siguiendo el formato:
```gherkin
Característica: Automatizar el proceso de selección de botones en la matriz de Evalart
  Como tester de automatización
  Quiero resolver la matriz dinámica de botones
  Para obtener el hash de éxito después de completar todos los ciclos

  Escenario: Resolver matriz dinámica y obtener hash
    Dado que estoy logueado en la pagina de la matriz
    Cuando selecciono el boton correcto en la matriz y proceso la respuesta
    Entonces deberia ver el hash de exito
```

### 2. Step Definitions
- Implementación clara y concisa de los pasos
- Manejo de estados entre pasos
- Validaciones específicas

### 3. Tasks
- Modularización de acciones complejas
- Reutilización de código
- Mantenibilidad mejorada

## Mejores Prácticas Implementadas

1. **Clean Code**
   - Nombres descriptivos
   - Métodos concisos
   - Responsabilidad única

2. **Manejo de Datos**
   - Externalización de credenciales
   - Parametrización de valores
   - Gestión segura de información sensible

3. **Control de Calidad**
   - Validaciones exhaustivas
   - Manejo de casos borde
   - Logs detallados

4. **Mantenibilidad**
   - Código documentado
   - Estructura organizada
   - Patrones de diseño

## Resultados y Reportes
Los reportes se generan en:
```
target/site/serenity/index.html
```

Incluyen:
- Resumen de ejecución
- Pasos detallados
- Evidencias visuales
- Métricas de ejecución

## Contacto
Desarrollado por [Tu Nombre]
- Email: [tu.email@dominio.com]
- LinkedIn: [URL_LinkedIn]

## Licencia
Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE.md](LICENSE.md) para más detalles.
