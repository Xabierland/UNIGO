# Contribución a UNIGO

## Introducción

Gracias por tu interés en contribuir a UNIGO, la aplicación Android para acceder al campus de Álava. Este documento proporciona pautas para contribuir al proyecto.

## Código de Conducta

Antes de comenzar, por favor lee nuestro [Código de Conducta](/CODE_OF_CONDUCT.md). Esperamos que todos los colaboradores respeten estos principios.

## Cómo Contribuir

### Informes de Errores

Si encuentras un error en la aplicación:

1. Comprueba que no haya sido ya reportado en los [Issues](https://github.com/Xabierland/UNIGO/issues)
2. Abre un nuevo issue con la siguiente información:
   - Descripción clara del error
   - Versión de la aplicación
   - Versión de Android
   - Pasos para reproducir el error
   - Capturas de pantalla (si es posible)

### Solicitudes de Funcionalidades

Para proponer nuevas funcionalidades:

1. Abre un issue describiendo:
   - La funcionalidad propuesta
   - Motivación y contexto
   - Comportamiento esperado
   - Alternativas consideradas

### Proceso de Contribución

1. Haz un fork del repositorio
2. Crea una rama para tu contribución
   ```
   git checkout -b feature/nombre-de-la-funcionalidad
   ```
3. Realiza tus cambios
4. Asegúrate de seguir las guías de estilo de código
5. Realiza commits con mensajes claros y concisos
6. Abre un Pull Request (PR) con:
   - Descripción detallada de los cambios
   - Referencia a issues relacionados
   - Capturas de pantalla de los cambios (si aplica)

### Guías de Estilo

#### Código Android

- Sigue las [guías de estilo de Android](https://developer.android.com/guide/topics/ui/look-and-feel)
- Utiliza Android Studio para formatear el código
- Mantén una estructura de proyecto consistente
- Comenta el código cuando sea necesario para mejorar su comprensión

#### Control de Versiones

- Utiliza commits atómicos y con mensajes descriptivos
- Formato recomendado para mensajes de commit:
  ```
  tipo(alcance): descripción breve

  Descripción más detallada si es necesario

  Refs #número-de-issue
  ```
  Tipos: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

### Configuración del Entorno de Desarrollo

1. Instala Android Studio
2. Clona el repositorio
3. Abre el proyecto con Android Studio
4. Instala las dependencias necesarias
5. Configura tu entorno de desarrollo

### Pruebas

- Asegúrate de que todos los tests existentes pasen
- Añade tests para nuevas funcionalidades
- Realiza pruebas en diferentes dispositivos y versiones de Android

## Proceso de Revisión

- Todos los PRs serán revisados por los mantenedores del proyecto
- Se pueden solicitar cambios o mejoras
- La revisión busca mantener la calidad y consistencia del código

## Reconocimientos

Los contribuyentes serán reconocidos en el README o en una sección de contribuidores.

## Preguntas

Si tienes dudas, puedes abrir un issue o contactar con los mantenedores del proyecto.

## Licencia

Al contribuir, aceptas que tus contribuciones se publiquen bajo la [Licencia GNU General Public License v3.0](LICENSE).

¡Gracias por tu ayuda en mejorar UNIGO!