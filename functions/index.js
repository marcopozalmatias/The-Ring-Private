const admin = require('firebase-admin');
const { onValueDeleted } = require('firebase-functions/v2/database');
const { onUserDeleted } = require('firebase-functions/v2/identity');

admin.initializeApp();

async function cleanupUserMappings({ emailSafe, email, dni, apodo }) {
  if (!emailSafe && !email && !dni && !apodo) return;

  const db = admin.database();
  const updates = {};

  if (emailSafe) {
    updates[`Usuarios/${emailSafe}`] = null;
    updates[`TokensQR/${emailSafe}`] = null;
  }
  if (dni) updates[`MapeoDNI/${dni}`] = null;
  if (apodo) updates[`Apodos/${apodo}`] = null;

  // Fallback: borra cualquier DNI mapeado a este correo.
  if (email) {
    const dniSnapshot = await db.ref('MapeoDNI').orderByValue().equalTo(email).get();
    dniSnapshot.forEach((child) => {
      if (child.key) updates[`MapeoDNI/${child.key}`] = null;
    });
  }

  // Fallback: borra cualquier apodo mapeado a este emailSafe.
  if (emailSafe) {
    const apodosSnapshot = await db.ref('Apodos').orderByValue().equalTo(emailSafe).get();
    apodosSnapshot.forEach((child) => {
      if (child.key) updates[`Apodos/${child.key}`] = null;
    });
  }

  await db.ref().update(updates);
}

exports.cleanupOnUserNodeDeleted = onValueDeleted(
  {
    ref: '/Usuarios/{emailSafe}',
    region: 'europe-west1',
  },
  async (event) => {
    const emailSafe = event.params.emailSafe;
    const deletedUser = event.data && event.data.val ? event.data.val() : null;
    const perfil = deletedUser && deletedUser.perfil ? deletedUser.perfil : {};
    const email = perfil && perfil.correo ? perfil.correo : '';
    const dni = perfil && perfil.dni ? perfil.dni : '';
    const apodo = perfil && perfil.apodo ? perfil.apodo : '';

    await cleanupUserMappings({ emailSafe, email, dni, apodo });
  }
);

exports.cleanupOnAuthUserDeleted = onUserDeleted(
  {
    region: 'europe-west1',
  },
  async (event) => {
    const email = event.data.email || '';
    const emailSafe = email.replace(/\./g, '_');
    await cleanupUserMappings({ emailSafe, email });
  }
);

