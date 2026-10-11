package mz.ac.ucm.feng.ti.vaultkey.presentation.authenticator;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.di.ServiceLocator;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.AuthenticatorAccount;
import mz.ac.ucm.feng.ti.vaultkey.presentation.MainActivity;

/**
 * Detalhes TOTP (código + contagem decrescente, actualização automática
 * com um único temporizador) e HOTP (gerar/avançar contador explicitamente).
 */
public class AuthenticatorDetailFragment extends Fragment {

    private static final String ARG_ID = "id";

    public static AuthenticatorDetailFragment newInstance(long id) {
        AuthenticatorDetailFragment f = new AuthenticatorDetailFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_ID, id);
        f.setArguments(b);
        return f;
    }

    private AuthenticatorViewModel viewModel;
    private AuthenticatorAccount current;
    private TextView tvName;
    private TextView tvType;
    private TextView tvCode;
    private TextView tvCounterInfo;
    private LinearProgressIndicator progressPeriod;
    private final Handler ticker = new Handler(Looper.getMainLooper());
    private boolean tickerRunning;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (current == null || !isAdded()) {
                return;
            }
            viewModel.refreshTotp(current.getId());
            ticker.postDelayed(this, 1000);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_authenticator_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthenticatorViewModel.class);
        long id = getArguments() != null ? getArguments().getLong(ARG_ID, 0) : 0;

        tvName = view.findViewById(R.id.tv_name);
        tvType = view.findViewById(R.id.tv_type);
        tvCode = view.findViewById(R.id.tv_code);
        tvCounterInfo = view.findViewById(R.id.tv_counter_info);
        progressPeriod = view.findViewById(R.id.progress_period);
        MainActivity activity = (MainActivity) requireActivity();

        viewModel.getDetail().observe(getViewLifecycleOwner(), result -> {
            if (result == null || !isAdded()) {
                return;
            }
            if (!result.isSuccess()) {
                Toast.makeText(requireContext(), R.string.error_generic,
                        Toast.LENGTH_SHORT).show();
                activity.getSupportFragmentManager().popBackStack();
                return;
            }
            boolean first = current == null;
            current = result.getValue();
            bindHeader();
            if (first) {
                startMode();
            } else if (current.getType() == AuthenticatorAccount.OtpType.HOTP) {
                tvCounterInfo.setText("Contador: " + current.getCounter());
            }
        });

        viewModel.getCodeState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || !isAdded()) {
                return;
            }
            tvCode.setText(state.code);
            if (state.remaining >= 0 && current != null) {
                progressPeriod.setMax(current.getPeriod());
                progressPeriod.setProgress((int) state.remaining);
            }
        });

        view.findViewById(R.id.btn_copy).setOnClickListener(v -> {
            String code = tvCode.getText().toString();
            if (code.contains("•")) {
                return;
            }
            int secs = ServiceLocator.get().vaultRepository().getClipboardClearSeconds();
            ServiceLocator.get().clipboard().copy(requireContext(), "2fa", code, secs);
            Toast.makeText(requireContext(),
                    secs > 0 ? getString(R.string.copied, secs)
                            : getString(R.string.copied_no_clear),
                    Toast.LENGTH_SHORT).show();
        });
        view.findViewById(R.id.btn_next).setOnClickListener(v -> {
            if (current != null) {
                viewModel.advanceHotp(current.getId());
            }
        });
        view.findViewById(R.id.btn_delete).setOnClickListener(v -> {
            if (current == null) {
                return;
            }
            new AlertDialog.Builder(requireContext())
                    .setMessage(R.string.auth_delete_confirm)
                    .setPositiveButton(R.string.action_delete, (d, w) -> {
                        viewModel.delete(current.getId());
                        Toast.makeText(requireContext(), R.string.auth_deleted,
                                Toast.LENGTH_SHORT).show();
                        activity.getSupportFragmentManager().popBackStack();
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        });

        viewModel.getOperation().observe(getViewLifecycleOwner(), result -> {
            if (result != null && !result.isSuccess() && isAdded()) {
                Toast.makeText(requireContext(), result.getError(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.loadDetail(id);
    }

    private void bindHeader() {
        tvName.setText(current.displayName());
        boolean hotp = current.getType() == AuthenticatorAccount.OtpType.HOTP;
        tvType.setText(hotp ? "HOTP • " + current.getHmacAlgorithm().replace("Hmac", "")
                : "TOTP • " + current.getHmacAlgorithm().replace("Hmac", "")
                + " • " + current.getPeriod() + "s");
    }

    private void startMode() {
        View view = getView();
        if (view == null) {
            return;
        }
        boolean hotp = current.getType() == AuthenticatorAccount.OtpType.HOTP;
        view.findViewById(R.id.btn_next).setVisibility(hotp ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.til_sync).setVisibility(hotp ? View.VISIBLE : View.GONE);
        tvCounterInfo.setVisibility(hotp ? View.VISIBLE : View.GONE);
        progressPeriod.setVisibility(hotp ? View.GONE : View.VISIBLE);
        if (hotp) {
            tvCounterInfo.setText("Contador: " + current.getCounter());
            view.findViewById(R.id.btn_next).setOnClickListener(v -> {
                if (current != null) {
                    viewModel.advanceHotp(current.getId());
                }
            });
            TextInputEditText etSync = view.findViewById(R.id.et_sync_counter);
            etSync.setText(String.valueOf(current.getCounter()));
            view.findViewById(R.id.til_sync).setOnClickListener(null);
            // Sincronização manual: avançar para um contador específico.
            view.findViewById(R.id.btn_next).setOnLongClickListener(v -> {
                String s = etSync.getText() == null ? "" : etSync.getText().toString();
                try {
                    long target = Long.parseLong(s.trim());
                    new Thread(() -> ServiceLocator.get().authenticator()
                            .setHotpCounter(current.getId(), target)).start();
                    Toast.makeText(requireContext(), R.string.set_saved,
                            Toast.LENGTH_SHORT).show();
                    viewModel.loadDetail(current.getId());
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), R.string.auth_invalid,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            // Gera pré-visualização sem avançar? Não: HOTP exige avanço explícito.
            tvCode.setText("••••••");
        } else {
            stopTicker();
            tickerRunning = true;
            ticker.post(tick);
        }
    }

    private void stopTicker() {
        tickerRunning = false;
        ticker.removeCallbacks(tick);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTicker();
        tvCode.setText("••••••");
        current = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (current != null && current.getType() == AuthenticatorAccount.OtpType.TOTP
                && !tickerRunning) {
            tickerRunning = true;
            ticker.post(tick);
        }
    }
}
