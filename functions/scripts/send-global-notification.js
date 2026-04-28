#!/usr/bin/env node
'use strict';

/**
 * Publica una notificacion global en Realtime Database para que la app la replique
 * automaticamente en cada usuario (salvo las que cada usuario haya eliminado).
 *
 * Uso:
 * node scripts/send-global-notification.js --title "Titulo" --message "Mensaje" --type "GENERAL"
 *
 * Requisitos:
 * - Variable GOOGLE_APPLICATION_CREDENTIALS apuntando al JSON de Service Account.
 * - Variable FIREBASE_DATABASE_URL (opcional). Si no existe, usa la URL por defecto del proyecto.
 */

const admin = require('firebase-admin');
const fs = require('fs');

function parseArgs(argv) {
  const args = {};
  for (let i = 2; i < argv.length; i += 1) {
    const part = argv[i];
    if (!part.startsWith('--')) continue;
    const key = part.slice(2);
    const next = argv[i + 1];
    if (!next || next.startsWith('--')) {
      args[key] = 'true';
      continue;
    }
    args[key] = next;
    i += 1;
  }
  return args;
}

function slugify(value) {
  return String(value || '')
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 40);
}

async function main() {
  const args = parseArgs(process.argv);
  const title = (args.title || '').trim();
  const message = (args.message || '').trim();
  const type = (args.type || 'GENERAL').trim().toUpperCase();

  if (!title || !message) {
    throw new Error('Debes indicar --title y --message.');
  }

  const credentialsPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  if (!credentialsPath) {
    throw new Error('Falta GOOGLE_APPLICATION_CREDENTIALS con la ruta al JSON de Service Account.');
  }
  if (!fs.existsSync(credentialsPath)) {
    throw new Error(`No existe el archivo de credenciales: ${credentialsPath}`);
  }

  const databaseURL = process.env.FIREBASE_DATABASE_URL || 'https://the-ring-private-default-rtdb.europe-west1.firebasedatabase.app/';

  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    databaseURL,
  });

  const timestamp = Date.now();
  const id = `${timestamp}_${slugify(title)}`;

  const notif = {
    id,
    titulo: title,
    mensaje: message,
    tipo: type,
    timestamp,
    leida: false,
  };

  await admin.database().ref('NotificacionesGlobal').child(id).set(notif);

  process.stdout.write(`OK: notificacion publicada con id ${id}\n`);
}

main().catch((error) => {
  process.stderr.write(`ERROR: ${error.message}\n`);
  process.exit(1);
});

