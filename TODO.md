# Lista de tareas

## Prerrequisitos

- [X] Elegir un nombre para el grupo
- [X] Definir el alcance del proyecto
- [X] Mandar el correo al profesorado
- [X] Configurar el repositorio del proyecto

## Requisitos funcionales

### Actividades

- [X] BaseActivity
  - [X] Definir metodos y atributos comunes a todas las actividades de la aplicación.
- [X] SplashScreen
  - [X] Una pantalla de carga que muestre el logo de la aplicación ademas de los mensajes de las diferentes entidades que colaboran en el desarrollo de la misma. Una vez cargada la aplicación se añade un "swipe" para que el usuario pase a la pantalla de inicio de sesión.
- [X] LoginActivity
  - [X] Login y Registro mediante correo electrónico y contraseña.
    - [X] Correo para confirmar el registro.
    - [ ] Correo para recuperar la contraseña.
  - [X] Login y Registro mediante cuenta de Google. (Firebase)
  - [X] Login y Registro de forma anónima.
- [ ] MainActivity
  - [X] Actividad con un BottomNavigationView que contenga los siguientes elementos botones que carguen sus respectivos fragmentos:
    - [ ] Mapa
      - [ ] Persistencia de datos
      - [ ] Calcular ruta
        - [ ] A pie
        - [ ] En bicicleta
        - [ ] Bus
      - [ ] Consumo de calorías
      - [ ] Emisiones de CO2
      - [ ] Dibujar rutas
    - [ ] Perfil
      - [ ] Foto de perfil
      - [ ] Metodo de transporte predeterminado
      - [ ] Centro educativo
    - [X] Ajustes
      - [X] Idioma
      - [X] Tema
