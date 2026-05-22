package com.example.theringprivate;

// Utilidad centralizada para traducir textos dinámicos sin duplicar lógica en cada pantalla.
import android.content.Context;
import android.util.Log;
import android.widget.TextView;

// Modo manual: no hay traducción automática en tiempo de ejecución.

// Clase auxiliar que gestiona traducciones de texto y el uso de caches de traductores ML Kit.
public class TranslationHelper {
    // Etiqueta de log para localizar fácilmente mensajes de error de traducción.
    private static final String TAG = "TranslationHelper";
    // Nota: hemos eliminado la traducción automática por ML Kit y usamos
    // exclusivamente recursos localizados de la app. Esto evita
    // dependencias de red/descarga de modelos y discrepancias entre
    // emulador y dispositivos reales (Play Store).

    // Contrato simple para notificar si una traducción termina bien o si falla.
    public interface TranslationCallback {
        // Se llama cuando el texto ya está traducido o se decide conservar el original.
        void onTranslationComplete(String translatedText);
        // Se llama cuando falla la operación.
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

        // En modo manual devolvemos el texto dinámico tal cual (sin ML Kit).
        callback.onTranslationComplete(text);
    }

    /**
     * Lógica interna de traducción para un bloque de texto simple (sin saltos de línea).
     */
    // translateInternal eliminado: ya no usamos ML Kit ni traducciones automáticas.

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
     * Sin traducción automática: usa directamente el locale ya aplicado por la app.
     */
    public static void translateTextView(TextView textView, int resId) {
        try {
            textView.setText(resId);
        } catch (Exception e) {
            Log.e(TAG, "Error setting text resource: " + e.getMessage());
            textView.setText(resId);
        }
    }
}
