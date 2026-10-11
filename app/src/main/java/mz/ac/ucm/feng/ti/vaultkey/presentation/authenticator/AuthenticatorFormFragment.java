package mz.ac.ucm.feng.ti.vaultkey.presentation.authenticator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.AuthenticatorAccount;
import mz.ac.ucm.feng.ti.vaultkey.presentation.MainActivity;

/** Cadastro manual de conta 2FA + entrada para leitura de QR Code. */
public class AuthenticatorFormFragment extends Fragment {

    private AuthenticatorViewModel viewModel;
    private TextInputEditText etIssuer;
    private TextInputEditText etAccount;
    private TextInputEditText etSecret;
    private TextInputEditText etDigits;
    private TextInputEditText etPeriod;
    private TextInputEditText etCounter;
    private RadioGroup rgType;
    private RadioGroup rgHash;
    private TextView tvError;
    private MaterialButton btnSave;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_authenticator_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthenticatorViewModel.class);
        etIssuer = view.findViewById(R.id.et_issuer);
        etAccount = view.findViewById(R.id.et_account);
        etSecret = view.findViewById(R.id.et_secret);
        etDigits = view.findViewById(R.id.et_digits);
        etPeriod = view.findViewById(R.id.et_period);
        etCounter = view.findViewById(R.id.et_counter);
        rgType = view.findViewById(R.id.rg_type);
        rgHash = view.findViewById(R.id.rg_hash);
        tvError = view.findViewById(R.id.tv_error);
        btnSave = view.findViewById(R.id.btn_save);

        // Resultado do leitor de QR: pré-preenche o formulário.
        getParentFragmentManager().setFragmentResultListener(QrScanFragment.RESULT_KEY,
                this, (key, bundle) -> {
                    etIssuer.setText(bundle.getString("issuer", ""));
                    etAccount.setText(bundle.getString("account", ""));
                    etSecret.setText(bundle.getString("secret", ""));
                    String type = bundle.getString("type", "TOTP");
                    if ("HOTP".equals(type)) {
                        rgType.check(R.id.rb_hotp);
                    } else {
                        rgType.check(R.id.rb_totp);
                    }
                    String algo = bundle.getString("algorithm", "HmacSHA1");
                    if ("HmacSHA256".equals(algo)) {
                        rgHash.check(R.id.rb_sha256);
                    } else if ("HmacSHA512".equals(algo)) {
                        rgHash.check(R.id.rb_sha512);
                    } else {
                        rgHash.check(R.id.rb_sha1);
                    }
                    etDigits.setText(String.valueOf(bundle.getInt("digits", 6)));
                    etPeriod.setText(String.valueOf(bundle.getInt("period", 30)));
                    etCounter.setText(String.valueOf(bundle.getLong("counter", 0)));
                });

        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean hotp = checkedId == R.id.rb_hotp;
            view.findViewById(R.id.til_period).setVisibility(hotp ? View.GONE : View.VISIBLE);
            view.findViewById(R.id.til_counter).setVisibility(hotp ? View.VISIBLE : View.GONE);
        });

        view.findViewById(R.id.btn_scan).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showFragment(new QrScanFragment(),
                        "qr_scan", true, false, true));

        btnSave.setOnClickListener(v -> {
            tvError.setVisibility(View.GONE);
            AuthenticatorAccount a = collect();
            String error = viewModel.validate(a);
            if (error != null) {
                tvError.setText(error);
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            new AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.auth_confirm_save) + "\n\n" + a.displayName())
                    .setPositiveButton(R.string.action_confirm, (d, w) -> {
                        btnSave.setEnabled(false);
                        viewModel.save(a);
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        });

        viewModel.getOperation().observe(getViewLifecycleOwner(), result -> {
            btnSave.setEnabled(true);
            if (result == null || !isAdded()) {
                return;
            }
            if (result.isSuccess()) {
                Toast.makeText(requireContext(), R.string.auth_saved,
                        Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                tvError.setText(result.getError());
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private AuthenticatorAccount collect() {
        AuthenticatorAccount a = new AuthenticatorAccount();
        a.setIssuer(text(etIssuer).trim());
        a.setAccountName(text(etAccount).trim());
        a.setSecretBase32(text(etSecret));
        a.setType(rgType.getCheckedRadioButtonId() == R.id.rb_hotp
                ? AuthenticatorAccount.OtpType.HOTP
                : AuthenticatorAccount.OtpType.TOTP);
        int hashId = rgHash.getCheckedRadioButtonId();
        if (hashId == R.id.rb_sha256) {
            a.setHmacAlgorithm("HmacSHA256");
        } else if (hashId == R.id.rb_sha512) {
            a.setHmacAlgorithm("HmacSHA512");
        } else {
            a.setHmacAlgorithm("HmacSHA1");
        }
        a.setDigits(parseInt(text(etDigits), 6));
        a.setPeriod(parseInt(text(etPeriod), 30));
        try {
            a.setCounter(Long.parseLong(text(etCounter).trim()));
        } catch (NumberFormatException e) {
            a.setCounter(-1);
        }
        return a;
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }
}
