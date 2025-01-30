# language: es

Característica: Automatizar el proceso de selección de botones en la matriz de Evalart

  Como tester de automatización
  Quiero resolver la matriz dinámica de botones
  Para obtener el hash de éxito después de completar todos los ciclos

  Escenario: Resolver matriz dinámica y obtener hash
    Dado que estoy logueado en la pagina de la matriz
    Cuando selecciono el boton correcto en la matriz y proceso la respuesta
    Entonces deberia ver el hash de exito
