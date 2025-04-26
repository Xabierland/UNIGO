# Lista de tareas

## Prerrequisitos

- [X] Elegir un nombre para el grupo
- [X] Definir el alcance del proyecto
- [X] Mandar el correo al profesorado
- [X] Configurar el repositorio del proyecto

## Requisitos funcionales

### Actividades

- [ ] BaseActivity
  - [ ] Definir metodos y atributos comunes a todas las actividades de la aplicación.
- [ ] SplashScreen
  - [ ] Una pantalla de carga que muestre el logo de la aplicación ademas de los mensajes de las diferentes entidades que colaboran en el desarrollo de la misma. Una vez cargada la aplicación se añade un "swipe" para que el usuario pase a la pantalla de inicio de sesión.
- [ ] LoginActivity (Firebase)
  - [ ] Login y Registro mediante correo electrónico y contraseña.
    - [ ] Correo para confirmar el registro.
    - [ ] Correo para recuperar la contraseña.
  - [ ] Login y Registro mediante cuenta de Google.
  - [ ] Login y Registro de forma anónima.
- [ ] MainActivity
  - [ ] Actividad con un BottomNavigationView que contenga los siguientes elementos botones que carguen sus respectivos fragmentos:
    - [ ] Mapa
    - [ ] Perfil
    - [ ] Ajustes

### Fragmentos

- [ ] MapFragment
  - [ ] Buscador de lugares en la parte superior
  - [ ] Mapa de Google Maps ocupando la mayor parte de la pantalla
- [ ] SettingsFragment
  - [ ] Idioma
  - [ ] Tema
- [ ] ProfileFragment
  - [ ] Ver y modificar los datos del usuario.

### Servicios

- [ ] FirebaseService
- [ ] LocationService
