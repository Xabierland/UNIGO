# Lista de tareas

## Prerrequisitos

- [X] Elegir un nombre para el grupo
- [X] Definir el alcance del proyecto
- [X] Mandar el correo al profesorado
- [X] Configurar el repositorio del proyecto

## Requisitos funcionales

### Actividades

- [ ] BaseActivity
  - [ ] Actividad abstracta que contiene la lógica común a todas las actividades de la aplicación. Esta actividad se encarga de inicializar el sistema de navegación y de gestionar el ciclo de vida de la aplicación.
  - [ ] Debe contener tambien la logica para gestionar los mensajes, notificaciones y errores que se produzcan en la aplicación.
- [ ] SplashScreen
  - [ ] Una pantalla de carga que muestre el logo de la aplicación ademas de los mensajes de las diferentes entidades que colaboran en el desarrollo de la misma. Una vez cargada la aplicación se añade un "swipe" para que el usuario pase a la pantalla de inicio de sesión.
- [ ] LoginActivity
  - [ ] La idea detras de esta actividad es la de permitir al usuario iniciar sesión con su cuenta de Google, correo electrónico y contraseña, numero de teléfono o de forma anónima.
- [ ] MainActivity
  - [ ] Esta actividad tendra un menu abajo para navegar entre los diferentes fragmentos de la aplicación.

### Fragmentos

- [ ] MapFragment
- [ ] SettingsFragment
- [ ] ProfileFragment

### Servicios

- [ ] FirebaseService
- [ ] LocationService
