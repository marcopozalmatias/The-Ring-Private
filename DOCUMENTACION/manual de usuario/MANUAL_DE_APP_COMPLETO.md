# Manual completo para empleados - The Ring Private

## 1. De que va la app
The Ring Private es una aplicacion Android para gestionar socios del club. Su objetivo operativo es permitir registro, inicio de sesion, identificacion por QR, consulta de notificaciones y gestion de cuenta desde el movil.

## 2. Quien la ha desarrollado
- **Empresa desarrolladora:** Promotora de negocios Darias

## 3. Como funciona la app (Java, Kotlin o ambas)
- La logica principal de pantallas y flujos de negocio esta implementada en **Java**.
- El sistema de compilacion y configuracion del proyecto usa **Gradle con Kotlin DSL** (`.kts`).
- En pruebas del proyecto hay tambien ficheros **Kotlin**.

Resumen: la app funciona con **Java en funcionalidad principal** y **Kotlin DSL en configuracion de build**.

## 4. Funciones relevantes para mantenimiento futuro
Estas son las funciones que un equipo de mantenimiento debe conocer primero:
- **Registro y validacion de usuario** (nombre, DNI, correo, contrasena segura y terminos).
- **Inicio de sesion por correo o DNI** con resolucion de DNI contra base de datos.
- **Recuperacion y cambio de contrasena**.
- **QR dinamico por sesion** con refresco y expiracion de token.
- **Notificaciones por usuario** (eliminacion independiente por cuenta).
- **Ajustes de idioma y apariencia** (claro/oscuro).
- **Borrado de cuenta con limpieza de datos** asociados en Firebase.

## 5. Software con el que se ha desarrollado
- **Android Studio** (entorno principal).
- **Gradle** (compilacion y dependencias).
- **Firebase Console** (Auth, Realtime Database y Functions).

## 6. En que equipos puede funcionar
La aplicacion esta pensada para **telefonos Android de cualquier marca** (Samsung, Xiaomi, Motorola, Oppo, Vivo, Realme, Pixel, etc.), siempre que cumplan la version minima de Android y requisitos de hardware.

## 7. Hardware minimo recomendado
Para un funcionamiento estable se recomienda:
- **RAM:** 2 GB minimo (recomendado 3 GB o mas).
- **Almacenamiento libre:** minimo 200 MB.
- **Procesador:** ARM64 o equivalente con rendimiento basico estable.
- **Conexion:** Internet activa para autenticacion, QR, notificaciones y sincronizacion.

## 8. Version de Android compatible
- **Version minima compatible actual:** Android 7.0 (API 24).
- **Version objetivo de compilacion/publicacion:** Android 15 (API 35).
