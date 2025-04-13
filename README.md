# UNIGO - Aula Open Data de la UPV/EHU

- [UNIGO - Aula Open Data de la UPV/EHU](#unigo---aula-open-data-de-la-upvehu)
  - [Descripción](#descripción)
  - [Características principales](#características-principales)
    - [Planificación de rutas](#planificación-de-rutas)
    - [Gestión de usuarios](#gestión-de-usuarios)
    - [Funcionalidades adicionales](#funcionalidades-adicionales)
  - [Instrucciones](#instrucciones)
  - [Tecnologías utilizadas](#tecnologías-utilizadas)
  - [Autores](#autores)
  - [Licencia](#licencia)

## Descripción

Aplicación Android para acceder al campus de Álava desde cualquier punto de la ciudad de Vitoria.

## Características principales

### Planificación de rutas

- Navegación multimodal:
  - A pie
  - En bicicleta
  - Tranvía
  - Autobús urbano
- Rutas optimizadas:
  - Más rápida
  - Más corta
  - Más ecológica
- Tiempo estimado de llegada
- Cálculo de calorías y emisiones de CO2

### Gestión de usuarios

- Registro e inicio de sesión con Firebase Authentication
- Perfil de usuario personalizado
- Historial de rutas
- Preferencias de transporte

### Funcionalidades adicionales

- Modo offline con mapas descargables

## Instrucciones

Las instrucciones de la aplicación se encuentran en el directorio `docs/Guia`

## Tecnologías utilizadas

- Android Nativo (Java)
- Firebase (Authentication, Realtime Database)
- Google Maps API
- API de Open Data del Gobierno Vasco
- Patrón de diseño MVVM (Model-View-ViewModel)
- Persistencia de datos con Room

## Autores

- ehunzango formado por:
  - Dueñas Fernández, Iñigo
  - Etxaniz Monge, Eneko
  - Gabiña Barañano, Xabier
  - Palacios Orueta, Irune

## Licencia

Este proyecto utiliza dos licencias diferentes según el tipo de contenido:

- **Código fuente** (`/app`): Licenciado bajo la [GNU General Public License v3.0 (GPLv3)](/LICENSE).
- **Documentación** (`/docs`): Licenciada bajo [Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)](/LICENSE-CC-BY-NC.md).

Si deseas reutilizar cualquier parte del proyecto, asegúrate de cumplir con las condiciones específicas de cada licencia.
