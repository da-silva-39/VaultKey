package mz.ac.ucm.feng.ti.vaultkey.security.clipboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * Cópia temporária com limpeza automática. Limpa apenas se o conteúdo actual
 * do clipboard ainda for o que a app copiou (não apaga conteúdo posterior
 * de outras apps). Não promete apagar históricos externos.
 */
public final class ClipboardHelper {

    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * @param clearAfterSeconds 0 desactiva a limpeza automática
     */
    public void copy(final Context context, String label, final String text,
                     int clearAfterSeconds) {
        final ClipboardManager cm =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) {
            return;
        }
        cm.setPrimaryClip(ClipData.newPlainText(label, text));
        if (clearAfterSeconds > 0) {
            final String snapshot = text;
            handler.postDelayed(() -> {
                try {
                    ClipData current = cm.getPrimaryClip();
                    if (current != null && current.getItemCount() > 0) {
                        CharSequence cs = current.getItemAt(0).coerceToText(context);
                        if (cs != null && snapshot.contentEquals(cs)) {
                            cm.setPrimaryClip(ClipData.newPlainText("", ""));
                        }
                    }
                } catch (Exception ignored) {
                    // Limpeza é melhor-esforço; nunca deve falhar a UI.
                }
            }, clearAfterSeconds * 1000L);
        }
    }

    public void clearNow(Context context) {
        ClipboardManager cm =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("", ""));
        }
    }
}
