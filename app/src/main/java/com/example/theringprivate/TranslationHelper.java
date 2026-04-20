package com.example.theringprivate;

// Utilidad centralizada para traducir textos dinámicos sin duplicar lógica en cada pantalla.
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;
import android.widget.TextView;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// Clase auxiliar que gestiona traducciones de texto y el uso de caches de traductores ML Kit.
public class TranslationHelper {
    // Etiqueta de log para localizar fácilmente mensajes de error de traducción.
    private static final String TAG = "TranslationHelper";
    // Caché en memoria para reutilizar instancias de Translator y evitar recrearlas constantemente.
    private static final Map<String, Translator> translators = new HashMap<>();

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

        // Leemos el idioma objetivo que el usuario ha elegido en la aplicación.
        SharedPreferences prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String targetLangCode = prefs.getString("My_Lang", "es");
        
        // El contenido base del proyecto se considera español.
        String sourceLang = TranslateLanguage.SPANISH;
        // Solo traducimos al inglés cuando la app está configurada en ese idioma.
        String targetLang = targetLangCode.equals("en") ? TranslateLanguage.ENGLISH : TranslateLanguage.SPANISH;

        // Si el idioma de origen y destino coinciden, no necesitamos traducir nada.
        if (sourceLang.equals(targetLang)) {
            callback.onTranslationComplete(text);
            return;
        }

        // Si el texto tiene saltos de línea, procesamos cada línea individualmente para no perder el formato
        if (text.contains("\n")) {
            String[] lines = text.split("\n", -1);
            String[] translatedLines = new String[lines.length];
            final int[] completedCount = {0};

            // Traducimos cada línea por separado para preservar títulos, párrafos y listas.
            for (int i = 0; i < lines.length; i++) {
                final int index = i;
                String currentLine = lines[i];

                // Si la línea está vacía o solo tiene espacios, la mantenemos tal cual.
                if (currentLine.trim().isEmpty()) {
                    translatedLines[index] = currentLine;
                    synchronized (completedCount) {
                        completedCount[0]++;
                        if (completedCount[0] == lines.length) {
                            callback.onTranslationComplete(combineLines(translatedLines));
                        }
                    }
                } else {
                    // Lanzamos una traducción asíncrona para la línea concreta.
                    translateInternal(context, currentLine, sourceLang, targetLang, new TranslationCallback() {
                        @Override
                        public void onTranslationComplete(String translatedText) {
                            // Guardamos el resultado de esa línea en su posición original.
                            translatedLines[index] = translatedText;
                            synchronized (completedCount) {
                                completedCount[0]++;
                                if (completedCount[0] == lines.length) {
                                    callback.onTranslationComplete(combineLines(translatedLines));
                                }
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error translating line: " + e.getMessage());
                            translatedLines[index] = lines[index]; // Mantener original si falla.
                            synchronized (completedCount) {
                                completedCount[0]++;
                                if (completedCount[0] == lines.length) {
                                    callback.onTranslationComplete(combineLines(translatedLines));
                                }
                            }
                        }
                    });
                }
            }
        } else {
            // Si es una sola frase o bloque, traducimos directamente.
            translateInternal(context, text, sourceLang, targetLang, callback);
        }
    }

    /**
     * Lógica interna de traducción para un bloque de texto simple (sin saltos de línea).
     */
    private static void translateInternal(Context context, String text, String sourceLang, String targetLang, TranslationCallback callback) {
        // Creamos una clave única por combinación de idioma para reutilizar el traductor.
        String key = sourceLang + "_" + targetLang;
        Translator translator = translators.get(key);

        // Si no existe traductor en caché, lo construimos y lo guardamos para futuras peticiones.
        if (translator == null) {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLang)
                    .setTargetLanguage(targetLang)
                    .build();
            translator = Translation.getClient(options);
            translators.put(key, translator);
        }

        final Translator finalTranslator = translator;
        DownloadConditions conditions = new DownloadConditions.Builder().build();

        // Descargamos el modelo solo si hace falta y después ejecutamos la traducción.
        finalTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    finalTranslator.translate(text)
                            .addOnSuccessListener(callback::onTranslationComplete)
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

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
            // Primero intentamos usar el texto ya localizado en los recursos de Android.
            String localizedText = context.getString(resId);
            if (localizedText != null && !localizedText.trim().isEmpty()) {
                textView.setText(localizedText);
                return;
            }

            // Fallback conservador: si el recurso localizado estuviera vacío, traducimos desde español.
            Configuration conf = new Configuration(context.getResources().getConfiguration());
            conf.setLocale(new Locale("es"));
            Context spanishContext = context.createConfigurationContext(conf);
            String spanishText = spanishContext.getString(resId);
            translateTextView(textView, spanishText);
        } catch (Exception e) {
            // Último recurso: mostramos el identificador del recurso para no dejar el TextView vacío.
            textView.setText(resId);
        }
    }
}
