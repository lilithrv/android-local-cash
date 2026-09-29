package com.example.localcash.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

// Encapsula el uso de BiometricPrompt para el acceso a la app
public class BiometriaHelper {

    private static final int AUTENTICADORES =
            BiometricManager.Authenticators.BIOMETRIC_STRONG;

    // Resultado de la autenticación que la Activity implementa
    public interface ResultadoBiometria {
        void onExito();
        void onError(String mensaje);
    }

    // Indica si el dispositivo puede autenticar con biometría o credencial
    public static boolean hayBiometriaDisponible(Context context) {
        BiometricManager manager = BiometricManager.from(context);
        int estado = manager.canAuthenticate(AUTENTICADORES);
        return estado == BiometricManager.BIOMETRIC_SUCCESS;
    }

    // Diálogo del sistema para desbloquear la app
    public static void autenticar(@NonNull FragmentActivity activity,
                                  @NonNull ResultadoBiometria resultado) {
        BiometricPrompt prompt = new BiometricPrompt(activity,
                ContextCompat.getMainExecutor(activity),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        resultado.onExito();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        resultado.onError(errString.toString());
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("LocalCash")
                .setSubtitle("Desbloquea con tu huella")
                .setNegativeButtonText("Usar PIN de respaldo")
                .setAllowedAuthenticators(AUTENTICADORES)
                .build();

        prompt.authenticate(info);
    }
}
