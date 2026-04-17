# Aspectos técnicos - The Ring Private

## 1. Identificación del proyecto
- **Nombre:** The Ring Private
- **Tipo:** Aplicación Android para gestión de socios, acceso mediante QR, notificaciones y configuración personal
- **Desarrollador:** Promotora de negocios Darias
- **Entorno de desarrollo:** Android Studio

## 2. Objetivo de la aplicación
La aplicación permite registrar usuarios, iniciar sesión, consultar el código QR identificativo, recibir notificaciones del club, revisar normas y acceder a información legal, ayuda y ajustes de cuenta.

## 3. Tecnologías utilizadas
- **Lenguaje principal de la app:** Java
- **Lenguaje de construcción y configuración:** Kotlin DSL en Gradle
- **Interfaz de usuario:** Android XML + AndroidX + Material Components
- **Base de datos:** SQL
- **Autenticación:** Autenticación via SQL
- **Lectura y generación de QR:** ZXing (`com.journeyapps:zxing-android-embedded`)
- **Traducción automática de textos:** ML Kit Translate
- **Guardado de credenciales:** Android Credential Manager
- **Soporte biométrico:** AndroidX Biometric

## 4. Versiones relevantes
- **compileSdk:** 36
- **targetSdk:** 35
- **minSdk:** 24
- **Java:** 11
- **Kotlin:** 2.0.21 en configuración de proyecto
- **Firebase BoM:** 33.7.0
- **Firebase Auth:** 24.0.1
- **ML Kit Translate:** 17.0.3
- **ZXing Android Embedded:** 4.3.0

## 5. Arquitectura funcional
La app está organizada por pantallas y fragmentos:
- `MainActivity`: inicio de sesión, cambio de idioma y acceso al manual.
- `RegisterActivity`: registro de usuario, validación de DNI, contraseña segura y aceptación de términos.
- `HomeActivity`: pantalla principal con QR, notificaciones y accesos rápidos.
- `ProfileFragment`: consulta de datos del usuario y cambio de contraseña.
- `SettingsFragment`: ajustes, idioma, apariencia, información legal, ayuda, FAQ y borrado de cuenta.
- `NormasFragment`: normas internas del club.
- `TranslationHelper`: traducción dinámica de textos legales y de interfaz.

## 6. Funciones principales
1. Registro de usuario con validación de datos.
2. Inicio de sesión con correo o DNI.
3. Recuperación de contraseña por correo.
4. Generación de QR dinámico por sesión.
5. Gestión de notificaciones por usuario.
6. Consulta de perfil y cambio de contraseña.
7. Cambio de idioma y modo claro/oscuro.
8. Consulta de tarifas, términos, aviso legal, ayuda, FAQ y manual.
9. Eliminación de cuenta con borrado de datos asociados.

## 7. Modelo de datos base de datos
La información se organiza principalmente en:
- `Usuarios/{emailSafe}/perfil`
- `MapeoDNI/{dni}`
- `TokensQR/{emailSafe}`
- `NotificacionesGlobal/{id}`

## 8. Compatibilidad y despliegue
- La app está pensada para dispositivos Android de múltiples marcas.
- **Compatibilidad mínima recomendada:** Android 7.0 (API 24) o superior.
- **Compatibilidad objetivo actual:** Android 15 (API 35) y posteriores, revisando dependencias nativas para cumplir requisitos de 16 KB.

## 9. Requisitos de hardware mínimos
- **RAM:** 2 GB como mínimo; recomendable 3 GB o más.
- **Almacenamiento libre:** al menos 200 MB para instalación, datos y caché.
- **Procesador:** ARM de 64 bits o equivalente, con rendimiento básico estable.
- **Pantalla:** cualquier tamaño típico de móvil Android, con soporte para orientación y distintos densidades.
- **Conectividad:** Internet activa para autenticación, base de datos, notificaciones y traducción.

## 10. Puntos clave para mantenimiento
- Revisar `MainActivity`, `RegisterActivity`, `HomeActivity`, `SettingsFragment` y `ProfileFragment` ante cambios funcionales.
- Mantener sincronizados los textos de `strings.xml` en español e inglés.
- Verificar el impacto de `TranslationHelper` cuando se añadan nuevos textos traducibles.
- Controlar los nodos Firebase al borrar usuario para evitar datos huérfanos.
- Revisar las librerías nativas y la compatibilidad con Android 15+ antes de publicar.

## 11. Casos de uso principales
### CU-01: Registro de usuario
El usuario rellena sus datos, acepta las condiciones y crea una cuenta válida en Firebase.

### CU-02: Inicio de sesión
El usuario entra con correo o DNI y contraseña, accediendo a la pantalla principal.

### CU-03: QR dinámico y notificaciones
El usuario abre su QR, lo muestra en recepción y gestiona sus notificaciones de forma individual.

## 12. Observaciones
La solución actual combina Java en la lógica principal con configuración Gradle en Kotlin DSL. El proyecto no depende de Kotlin para las pantallas principales, pero sí en la configuración de compilación y en los tests del proyecto.

