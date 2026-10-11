package mz.ac.ucm.feng.ti.vaultkey.presentation.passwordgenerator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.slider.Slider;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.di.ServiceLocator;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.PasswordStrength;
import mz.ac.ucm.feng.ti.vaultkey.domain.password.PasswordOptions;
import mz.ac.ucm.feng.ti.vaultkey.domain.usecase.AuthenticatorUseCases;

/**
 * Gerador de senhas: parâmetros, força e cópia temporária.
 * Nada é persistido sem acção explícita do utilizador.
 */
public class PasswordGeneratorFragment extends Fragment {

    private TextView tvPassword;
    private TextView tvStrength;
    private TextView tvLength;
    private LinearProgressIndicator strengthBar;
    private Slider sliderLength;
    private MaterialCheckBox cbUpper;
    private MaterialCheckBox cbLower;
    private MaterialCheckBox cbDigits;
    private MaterialCheckBox cbSymbols;
    private MaterialCheckBox cbNoAmbiguous;
    private String current = "";

    private final AuthenticatorUseCases.GeneratePasswordUseCase generateUseCase =
            new AuthenticatorUseCases.GeneratePasswordUseCase();
    private final AuthenticatorUseCases.EvaluatePasswordStrengthUseCase strengthUseCase =
            new AuthenticatorUseCases.EvaluatePasswordStrengthUseCase();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_password_generator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvPassword = view.findViewById(R.id.tv_password);
        tvStrength = view.findViewById(R.id.tv_strength);
        tvLength = view.findViewById(R.id.tv_length);
        strengthBar = view.findViewById(R.id.strength_bar);
        sliderLength = view.findViewById(R.id.slider_length);
        cbUpper = view.findViewById(R.id.cb_upper);
        cbLower = view.findViewById(R.id.cb_lower);
        cbDigits = view.findViewById(R.id.cb_digits);
        cbSymbols = view.findViewById(R.id.cb_symbols);
        cbNoAmbiguous = view.findViewById(R.id.cb_no_ambiguous);

        sliderLength.addOnChangeListener((slider, value, fromUser) -> {
            tvLength.setText(getString(R.string.gen_length, (int) value));
            regenerate();
        });
        View.OnClickListener regen = v -> regenerate();
        cbUpper.setOnClickListener(regen);
        cbLower.setOnClickListener(regen);
        cbDigits.setOnClickListener(regen);
        cbSymbols.setOnClickListener(regen);
        cbNoAmbiguous.setOnClickListener(regen);

        view.findViewById(R.id.btn_regenerate).setOnClickListener(v -> regenerate());
        MaterialButton btnCopy = view.findViewById(R.id.btn_copy);
        btnCopy.setOnClickListener(v -> {
            if (current.isEmpty()) {
                return;
            }
            int secs = ServiceLocator.get().vaultRepository().getClipboardClearSeconds();
            ServiceLocator.get().clipboard().copy(requireContext(), "senha", current, secs);
            Toast.makeText(requireContext(),
                    secs > 0 ? getString(R.string.copied, secs)
                            : getString(R.string.copied_no_clear),
                    Toast.LENGTH_SHORT).show();
        });

        tvLength.setText(getString(R.string.gen_length, 16));
        regenerate();
    }

    private void regenerate() {
        PasswordOptions options = new PasswordOptions();
        options.setLength((int) sliderLength.getValue());
        options.setUpper(cbUpper.isChecked());
        options.setLower(cbLower.isChecked());
        options.setDigits(cbDigits.isChecked());
        options.setSymbols(cbSymbols.isChecked());
        options.setExcludeAmbiguous(cbNoAmbiguous.isChecked());
        try {
            current = generateUseCase.execute(options);
        } catch (IllegalArgumentException e) {
            Toast.makeText(requireContext(), R.string.gen_need_category,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        tvPassword.setText(current);
        PasswordStrength strength = strengthUseCase.execute(current);
        tvStrength.setText(getString(R.string.gen_strength, label(strength.getLevel()))
                + " — " + strength.getAdvice());
        strengthBar.setProgress(strength.getScore());
    }

    private String label(PasswordStrength.Level level) {
        switch (level) {
            case FAIR:
                return getString(R.string.strength_fair);
            case GOOD:
                return getString(R.string.strength_good);
            case STRONG:
                return getString(R.string.strength_strong);
            case VERY_STRONG:
                return getString(R.string.strength_very_strong);
            case WEAK:
            default:
                return getString(R.string.strength_weak);
        }
    }
}
