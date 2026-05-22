package com.example.theringprivate;

// Utilidad centralizada para traducir textos dinámicos sin duplicar lógica en cada pantalla.
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;
import android.widget.TextView;

// Eliminadas dependencias de ML Kit: pasamos a modo "traducción manual" usando
// siempre textos en español desde los recursos.

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// Clase auxiliar que gestiona traducciones de texto y el uso de caches de traductores ML Kit.
public class TranslationHelper {
    // Etiqueta de log para localizar fácilmente mensajes de error de traducción.
    private static final String TAG = "TranslationHelper";
    // Nota: hemos eliminado la traducción automática por ML Kit y forzamos
    // el uso de textos en español almacenados en recursos. Esto evita
    // dependencias de red/descarga de modelos y discrepancias entre
    // emulador y dispositivos reales (Play Store).

    // Contrato simple para notificar si una traducción termina bien o si falla.
    public interface TranslationCallback {
        // Se llama cuando el texto ya está traducido o se decide conservar el original.
        void onTranslationComplete(String translatedText);
        // Se llama cuando ML Kit no puede completar la traducción.
        void onError(Exception e);
    }

    /**
     * Traduce un texto. Si contiene saltos de línea, traduce cada parte por separado
     * para mantener exactamente el mismo formato y espaciado original.
     */
    public static void translate(Context context, String text, TranslationCallback callback) {
        // Si no hay texto, devolvemos el valor tal cual para no romper la interfaz.
        if (text == null || text.isEmpty()) {
            callback.onTranslationComplete(text);
            return;
        }

        // En el nuevo modo manual simplemente devolvemos el texto tal cual.
        // Dado que forzamos el locale a español en la app, los recursos y textos
        // ya deben estar en español; aquí evitamos cualquier traducción dinámica.
        callback.onTranslationComplete(text);
    }

    /**
     * Lógica interna de traducción para un bloque de texto simple (sin saltos de línea).
     */
    // translateInternal eliminado: ya no usamos ML Kit ni traducciones automáticas.

    // Recompone las líneas traducidas manteniendo los saltos originales.
    private static String combineLines(String[] lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(lines[i]);
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    // Traduce un texto y lo pinta directamente en un TextView.
    public static void translateTextView(TextView textView, String originalText) {
        if (originalText == null || originalText.isEmpty()) return;
        
        translate(textView.getContext(), originalText, new TranslationCallback() {
            @Override
            public void onTranslationComplete(String translatedText) {
                textView.setText(translatedText);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Translation error: " + e.getMessage());
                textView.setText(originalText); // Fallback to original.
            }
        });
    }

    /**
     * Traduce un recurso de texto usando primero la versión localizada actual.
     * Si falla la carga localizada, aplica traducción automática como respaldo.
     */
    public static void translateTextView(TextView textView, int resId) {
        try {
            Context context = textView.getContext();
            // Forzamos siempre la versión en español desde los recursos base.
            Configuration conf = new Configuration(context.getResources().getConfiguration());
            conf.setLocale(new Locale("es"));
            Context spanishContext = context.createConfigurationContext(conf);
            String spanishText = spanishContext.getString(resId);
            textView.setText(spanishText);
        } catch (Exception e) {
            Log.e(TAG, "Error setting text resource: " + e.getMessage());
            textView.setText(resId);
        }
    }
}
