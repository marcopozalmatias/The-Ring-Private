# Aspectos tecnicos - The Ring Private

## 1. Alcance tecnico del documento
Este documento describe solo la implementacion tecnica de la aplicacion: arquitectura, tecnologias, versiones, modelo de datos, despliegue y mantenimiento.

## 2. Stack tecnico
- **App Android (codigo fuente):** Java
- **Build y configuracion:** Gradle Kotlin DSL (`build.gradle.kts`)
- **UI:** XML + AndroidX + Material Components
- **Backend:** Firebase Authentication + Firebase Realtime Database
- **QR:** ZXing (`com.journeyapps:zxing-android-embedded`)
- **Traduccion:** ML Kit Translate (`com.google.mlkit:translate`)
- **Credenciales del sistema:** Android Credential Manager
- **Funciones backend:** Firebase Functions (`functions/index.js`)

## 3. Versiones y parametros de compilacion
- **compileSdk:** 36
- **targetSdk:** 35
- **minSdk:** 24
- **Java:** 11
- **Kotlin (tooling Gradle):** 2.0.21
- **Firebase BoM:** 33.7.0
- **Firebase Auth:** 24.0.1
- **ML Kit Translate:** 17.0.3
- **ZXing Android Embedded:** 4.3.0

## 4. Arquitectura de aplicacion
- `MainActivity`: login, cambio de idioma y recuperacion de cuenta.
- `RegisterActivity`: alta de usuario, validaciones y guardado inicial.
- `HomeActivity`: QR dinamico, portada y notificaciones.
- `ProfileFragment`: consulta de perfil y cambio de contrasena.
- `SettingsFragment`: idioma, apariencia, legal, cierre y borrado de cuenta.
- `NotificationsFragment`: listado, filtrado y borrado de notificaciones.
- `NormasFragment`: visualizacion de normas en modal fullscreen.
- `TranslationHelper`: capa de traduccion/fallback de textos.

## 5. Modelo de datos en Firebase Realtime Database
Estructura principal:
- `Usuarios/{emailSafe}/perfil`
- `Usuarios/{emailSafe}/notificaciones`
- `Usuarios/{emailSafe}/notificacionesEliminadas`
- `MapeoDNI/{dni}`
- `TokensQR/{emailSafe}`
- `NotificacionesGlobal/{id}`

Convenciones:
- `emailSafe = email.replace(".", "_")`
- El inicio por DNI se resuelve mediante `MapeoDNI`.
- El borrado de cuenta debe limpiar `Usuarios`, `MapeoDNI` y `TokensQR`.

## 6. Comportamientos tecnicos clave
### 6.1 QR dinamico
- Cada apertura de QR crea una nueva sesion (`qrSessionNonce`).
- Se genera token efimero con expiracion corta (`QR_TOKEN_TTL_MS`).
- El token se persiste en `TokensQR/{emailSafe}` y se invalida al cerrar QR.

### 6.2 Notificaciones por usuario
- Las globales se sincronizan al nodo de cada usuario.
- Si un usuario elimina una notificacion, se marca en `notificacionesEliminadas`.
- La eliminacion es independiente entre cuentas.

### 6.3 Integridad de DNI
- Registro valida formato y disponibilidad de DNI.
- Si existe mapeo huérfano, se limpia antes de permitir alta.
- Login acepta correo o DNI y traduce DNI a correo real.

## 7. Seguridad tecnica
- Autenticacion delegada en Firebase Auth.
- Reautenticacion requerida para operaciones sensibles (borrado de cuenta).
- Tokens QR efimeros para reducir reutilizacion de capturas.
- Limpieza de datos en dos capas: app + functions backend.

## 8. Compatibilidad tecnica
- **SO soportado:** Android API 24+
- **Objetivo de publicacion:** API 35
- **Dispositivos:** terminales Android de cualquier fabricante
- **Nota tecnica:** vigilar librerias nativas para compatibilidad 16 KB page size en Google Play.

## 9. Requisitos minimos tecnicos de dispositivo
- **RAM:** 2 GB minimo (3 GB recomendado)
- **Almacenamiento libre:** 200 MB minimo recomendado
- **CPU:** ARM64 o equivalente con rendimiento basico estable
- **Red:** conexion a Internet para Auth/DB/QR/notificaciones/traduccion

## 10. Operacion tecnica y mantenimiento
Checklist tecnico de mantenimiento:
1. Revisar cambios en `MapeoDNI` cuando se toque registro/login/borrado.
2. Validar flujo de borrado completo (DB + Auth + functions).
3. Probar QR dinamico tras cambios de perfil o seguridad.
4. Mantener `strings.xml` y `values-en/strings.xml` sincronizados.
5. Verificar compatibilidad de dependencias antes de publicar.
6. Compilar `:app:assembleDebug` y revisar warnings criticos.
