# Notificaciones globales (direccion)

La app replica automaticamente cualquier entrada nueva en `NotificacionesGlobal/{id}` a `Usuarios/{emailSafe}/notificaciones`.

- Si un usuario elimina una notificacion, queda marcada en `notificacionesEliminadas` y no vuelve a aparecerle.
- Usuarios nuevos tambien reciben las notificaciones globales existentes cuando inician sesion por primera vez.

## Estructura recomendada

```json
{
  "id": "1715000000000_aviso-general",
  "titulo": "Aviso direccion",
  "mensaje": "Texto de la notificacion",
  "tipo": "GENERAL",
  "timestamp": 1715000000000,
  "leida": false
}
```

## Opcion A - Desde Firebase Console (rapida)

1. Abre Realtime Database.
2. Ve al nodo `NotificacionesGlobal`.
3. Crea un hijo con una clave unica (por ejemplo, `1715000000000_aviso-general`).
4. Inserta los campos del JSON anterior.

## Opcion B - Desde script (recomendado para direccion)

Requisitos:
- `GOOGLE_APPLICATION_CREDENTIALS` apuntando a un Service Account JSON con permisos de Realtime Database.
- (Opcional) `FIREBASE_DATABASE_URL`.

Comando:

```powershell
Set-Location "C:\Users\marco\Desktop\TheRingPrivate\functions"
npm run publish:global -- --title "Aviso direccion" --message "Hoy cerramos a las 23:00" --type "GENERAL"
```

## Opcion C - Desde web o app interna de administracion

La web/app de administracion debe escribir en `NotificacionesGlobal/{id}` con la misma estructura.
Se recomienda proteger la escritura con reglas y rol admin (por ejemplo `perfil/rol == 'admin'`).

