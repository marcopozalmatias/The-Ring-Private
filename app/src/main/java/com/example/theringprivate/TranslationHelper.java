package com.example.theringprivate;

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

public class TranslationHelper {
    private static final String TAG = "TranslationHelper";
    private static final Map<String, Translator> translators = new HashMap<>();

    public interface TranslationCallback {
        void onTranslationComplete(String translatedText);
        void onError(Exception e);
    }

    /**
     * Traduce un texto. Si contiene saltos de línea, traduce cada parte por separado
     * para mantener exactamente el mismo formato y espaciado original.
     */
    public static void translate(Context context, String text, TranslationCallback callback) {
        if (text == null || text.isEmpty()) {
            callback.onTranslationComplete(text);
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String targetLangCode = prefs.getString("My_Lang", "es");
        
        String sourceLang = TranslateLanguage.SPANISH;
        String targetLang = targetLangCode.equals("en") ? TranslateLanguage.ENGLISH : TranslateLanguage.SPANISH;

        if (sourceLang.equals(targetLang)) {
            callback.onTranslationComplete(text);
            return;
        }

        // Si el texto tiene saltos de línea, procesamos cada línea individualmente para no perder el formato
        if (text.contains("\n")) {
            String[] lines = text.split("\n", -1);
            String[] translatedLines = new String[lines.length];
            final int[] completedCount = {0};

            for (int i = 0; i < lines.length; i++) {
                final int index = i;
                String currentLine = lines[i];

                // Si la línea está vacía o solo tiene espacios, la mantenemos tal cual
                if (currentLine.trim().isEmpty()) {
                    translatedLines[index] = currentLine;
                    synchronized (completedCount) {
                        completedCount[0]++;
                        if (completedCount[0] == lines.length) {
                            callback.onTranslationComplete(combineLines(translatedLines));
                        }
                    }
                } else {
                    translateInternal(context, currentLine, sourceLang, targetLang, new TranslationCallback() {
                        @Override
                        public void onTranslationComplete(String translatedText) {
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
                            translatedLines[index] = lines[index]; // Mantener original si falla
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
            translateInternal(context, text, sourceLang, targetLang, callback);
        }
    }

    /**
     * Lógica interna de traducción para un bloque de texto simple (sin saltos de línea).
     */
    private static void translateInternal(Context context, String text, String sourceLang, String targetLang, TranslationCallback callback) {
        String key = sourceLang + "_" + targetLang;
        Translator translator = translators.get(key);

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

        finalTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    finalTranslator.translate(text)
                            .addOnSuccessListener(callback::onTranslationComplete)
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

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
                textView.setText(originalText); // Fallback to original
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
            String localizedText = context.getString(resId);
            if (localizedText != null && !localizedText.trim().isEmpty()) {
                textView.setText(localizedText);
                return;
            }

            // Fallback conservador: si el recurso localizado estuviera vacio, traducir desde ES.
            Configuration conf = new Configuration(context.getResources().getConfiguration());
            conf.setLocale(new Locale("es"));
            Context spanishContext = context.createConfigurationContext(conf);
            String spanishText = spanishContext.getString(resId);
            translateTextView(textView, spanishText);
        } catch (Exception e) {
            textView.setText(resId);
        }
    }
}
