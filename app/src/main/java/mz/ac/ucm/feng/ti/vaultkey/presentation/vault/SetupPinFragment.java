package mz.ac.ucm.feng.ti.vaultkey.presentation.vault;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.presentation.MainActivity;

/** Ecrã de configuração inicial: criação do cofre + definição do PIN. */
public class SetupPinFragment extends Fragment {

    private VaultViewModel viewModel;
    private TextInputEditText etPin;
    private TextInputEditText etPinConfirm;
    private TextView tvError;
    private MaterialButton btnCreate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup_pin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(VaultViewModel.class);
        etPin = view.findViewById(R.id.et_pin);
        etPinConfirm = view.findViewById(R.id.et_pin_confirm);
        tvError = view.findViewById(R.id.tv_error);
        btnCreate = view.findViewById(R.id.btn_create);

        btnCreate.setOnClickListener(v -> {
            tvError.setVisibility(View.GONE);
            btnCreate.setEnabled(false);
            String pin = etPin.getText() == null ? "" : etPin.getText().toString();
            String confirm = etPinConfirm.getText() == null ? "" : etPinConfirm.getText().toString();
            viewModel.createVault(pin, confirm);
        });

        viewModel.getCreateResult().observe(getViewLifecycleOwner(), result -> {
            btnCreate.setEnabled(true);
            if (!isAdded()) {
                return;
            }
            if (result.isSuccess()) {
                ((MainActivity) requireActivity()).showHome();
            } else {
                tvError.setText(result.getError());
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }
}
