# Manual de Empleados - The Ring Private

## 1. Finalidad del sistema
The Ring Private es una aplicación Android destinada a la gestión de socios del club. Permite registrar usuarios, autenticar accesos, mostrar un QR identificativo, gestionar notificaciones, consultar información legal y mantener la cuenta del usuario bajo control.

## 2. Responsable y desarrollo
- **Empresa desarrolladora:** Promotora de negocios Darias
- **Entorno de desarrollo:** Android Studio
- **Tecnología principal de la app:** Java
- **Configuración del proyecto:** Gradle con Kotlin DSL

## 3. Tecnologías y servicios usados
- Firebase Authentication para inicio de sesión y creación de cuentas.
- Firebase Realtime Database para guardar perfiles, DNI mapeado, tokens QR y notificaciones.
- ZXing para generar y mostrar códigos QR.
- ML Kit Translate para textos traducibles.
- AndroidX, Material Components, Credential Manager y Biometric para la interfaz y el soporte de sistema.

## 4. Funciones relevantes para mantenimiento
### 4.1 Acceso de usuario
- `MainActivity`: pantalla de inicio de sesión.
- `RegisterActivity`: alta de usuario y validación de campos.
- Recuperación de contraseña por correo.

### 4.2 Funciones de perfil y cuenta
- `ProfileFragment`: consulta de datos del usuario y cambio de contraseña.
- `SettingsFragment`: idioma, apariencia, manuales, información legal y eliminación de cuenta.
- Eliminación de cuenta con borrado de datos en Firebase y limpieza de mapeos.

### 4.3 Funciones de home
- `HomeActivity`: pantalla principal.
- QR dinámico por sesión.
- Gestión de notificaciones individuales por usuario.
- Acceso a WhatsApp de soporte.

### 4.4 Utilidades compartidas
- `TranslationHelper`: traducción automática de textos de recursos.
- `Notificacion`: modelo de notificación.
- `NormasFragment` y `NotificationsFragment`: visualización de secciones de contenido.

## 5. Flujo técnico principal
1. El usuario entra en la pantalla de inicio de sesión.
2. Se valida contra Firebase Authentication.
3. Si accede correctamente, se abre `HomeActivity`.
4. En Home se cargan notificaciones y QR del usuario.
5. En ajustes se puede cambiar idioma, apariencia, consultar manuales o eliminar cuenta.

## 6. Modelo de datos
La base de datos de Firebase se organiza por nodo de usuario:
- `Usuarios/{emailSafe}/perfil`
- `MapeoDNI/{dni}`
- `TokensQR/{emailSafe}`
- `NotificacionesGlobal/{id}`

Al eliminar una cuenta, se limpian también los datos asociados para evitar registros huérfanos.

## 7. Qué sistema operativo y equipos soporta
- Funciona en dispositivos Android de distintas marcas y gamas.
- Compatibilidad mínima recomendada: Android 7.0 (API 24).
- Compromiso actual de compatibilidad: Android 15+ con librerías actualizadas y revisión de páginas de 16 KB.

## 8. Requisitos mínimos de hardware
- **RAM:** 2 GB mínimo.
- **Almacenamiento libre:** 200 MB recomendado.
- **Procesador:** gama media o equivalente, con soporte ARM de 64 bits.
- **Conectividad:** Internet obligatoria para login, Firebase, QR, notificaciones y traducción.

## 9. Software de desarrollo
- Android Studio.
- Gradle.
- Firebase Console.
- Herramientas de compilación Android SDK.

## 10. Recomendaciones de mantenimiento
- Revisar `strings.xml` en español e inglés cuando se añadan textos nuevos.
- Mantener la estructura Firebase consistente entre registro, inicio de sesión y eliminación.
- Comprobar los permisos y cambios de autenticación si se modifican los diálogos de cuenta.
- Verificar que el QR sigue siendo dinámico al cambiar cualquier dato del perfil.
- Revisar compatibilidad de librerías nativas antes de publicar nuevas versiones.

## 11. Observaciones para recepción y soporte
- El usuario final no ve la complejidad interna: solo debe poder registrarse, iniciar sesión, consultar su QR y gestionar su cuenta.
- En soporte conviene comprobar primero autenticación, datos de perfil y existencia de nodos en Realtime Database.
- Si se actualizan datos desde administración, debe cuidarse la coherencia entre autenticación y base de datos.

