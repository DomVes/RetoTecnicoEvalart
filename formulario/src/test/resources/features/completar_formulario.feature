# language: es

Característica: Automatizar el formulario dinámico de Evalart

  Como tester de automatización
  Quiero completar el formulario durante 10 ciclos
  Para obtener el hash de éxito

  Escenario: Completar los diez ciclos y obtener el hash
    Dado que inicio sesion en la pagina del formulario
    Cuando completo los campos del formulario en cada ciclo
    Entonces deberia ver el hash de exito
