package mz.ac.ucm.feng.ti.vaultkey.presentation.credentials;

import android.content.Intent;
import android.net.Uri;
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

import com.google.android.material.button.MaterialButton;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.di.ServiceLocator;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.Credential;
import mz.ac.ucm.feng.ti.vaultkey.presentation.MainActivity;

/**
 * Detalhes da credencial. A senha fica oculta por padrão; a visualização é
 * temporária (10 s) e a cópia usa limpeza automática configurada.
 */
public class CredentialDetailFragment extends Fragment {

    private static final String ARG_ID = "id";

    public static CredentialDetailFragment newInstance(long id) {
        CredentialDetailFragment f = new CredentialDetailFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_ID, id);
        f.setArguments(b);
        return f;
    }

    private CredentialViewModel viewModel;
    private Credential current;
    private TextView tvPassword;
    private MaterialButton btnShow;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable hidePassword = () -> {
        if (tvPassword != null && current != null) {
            tvPassword.setText("••••••••");
            if (btnShow != null) {
                btnShow.setText(R.string.cred_show);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_credential_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CredentialViewModel.class);
        long id = getArguments() != null ? getArguments().getLong(ARG_ID, 0) : 0;

        TextView tvService = view.findViewById(R.id.tv_service);
        TextView tvCategory = view.findViewById(R.id.tv_category);
        TextView tvUsername = view.findViewById(R.id.tv_username);
        tvPassword = view.findViewById(R.id.tv_password);
        btnShow = view.findViewById(R.id.btn_show);
        TextView tvUrl = view.findViewById(R.id.tv_url);
        TextView tvNotes = view.findViewById(R.id.tv_notes);
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
            current = result.getValue();
            tvService.setText(current.getServiceName());
            tvCategory.setText(current.getCategory());
            tvUsername.setText(current.getUsername());
            tvUrl.setText(current.getUrl());
            tvUrl.setVisibility(current.getUrl().isEmpty() ? View.GONE : View.VISIBLE);
            view.findViewById(R.id.tv_url_label)
                    .setVisibility(current.getUrl().isEmpty() ? View.GONE : View.VISIBLE);
            tvNotes.setText(current.getNotes());
            tvNotes.setVisibility(current.getNotes().isEmpty() ? View.GONE : View.VISIBLE);
            view.findViewById(R.id.tv_notes_label)
                    .setVisibility(current.getNotes().isEmpty() ? View.GONE : View.VISIBLE);
        });
        viewModel.loadDetail(id);

        btnShow.setOnClickListener(v -> {
            if (current == null) {
                return;
            }
            if ("••••••••".contentEquals(tvPassword.getText())) {
                tvPassword.setText(current.getPassword());
                btnShow.setText(R.string.cred_hide);
                handler.removeCallbacks(hidePassword);
                handler.postDelayed(hidePassword, 10000);
            } else {
                handler.removeCallbacks(hidePassword);
                hidePassword.run();
            }
        });

        view.findViewById(R.id.btn_copy_user).setOnClickListener(v -> {
            if (current == null) {
                return;
            }
            copy("utilizador", current.getUsername());
        });
        view.findViewById(R.id.btn_copy_pass).setOnClickListener(v -> {
            if (current == null) {
                return;
            }
            copy("senha", current.getPassword());
        });
        view.findViewById(R.id.btn_open_url).setOnClickListener(v -> {
            if (current == null || current.getUrl().isEmpty()) {
                return;
            }
            String url = current.getUrl();
            if (!url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
                url = "https://" + url;
            }
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(requireContext(), R.string.error_generic,
                        Toast.LENGTH_SHORT).show();
            }
        });
        view.findViewById(R.id.btn_edit).setOnClickListener(v -> {
            if (current == null) {
                return;
            }
            activity.showFragment(CredentialFormFragment.newInstance(current.getId()),
                    "cred_form", true, false, true);
        });
        view.findViewById(R.id.btn_delete).setOnClickListener(v -> {
            if (current == null) {
                return;
            }
            new AlertDialog.Builder(requireContext())
                    .setMessage(R.string.cred_delete_confirm)
                    .setPositiveButton(R.string.action_delete, (d, w) -> {
                        viewModel.delete(current.getId());
                        Toast.makeText(requireContext(), R.string.cred_deleted,
                                Toast.LENGTH_SHORT).show();
                        activity.getSupportFragmentManager().popBackStack();
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        });
    }

    private void copy(String label, String text) {
        int secs = ServiceLocator.get().vaultRepository().getClipboardClearSeconds();
        ServiceLocator.get().clipboard().copy(requireContext(), label, text, secs);
        if (secs > 0) {
            Toast.makeText(requireContext(), getString(R.string.copied, secs),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), R.string.copied_no_clear,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(hidePassword);
        // Oculta dados sensíveis ao sair do ecrã.
        if (tvPassword != null) {
            tvPassword.setText("••••••••");
        }
        current = null;
    }
}
