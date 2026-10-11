package mz.ac.ucm.feng.ti.vaultkey.presentation.credentials;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.Credential;
import mz.ac.ucm.feng.ti.vaultkey.domain.password.PasswordGenerator;
import mz.ac.ucm.feng.ti.vaultkey.domain.password.PasswordOptions;
import mz.ac.ucm.feng.ti.vaultkey.presentation.MainActivity;

/** Formulário de nova credencial / edição (id 0 = nova). */
public class CredentialFormFragment extends Fragment {

    private static final String ARG_ID = "id";

    public static CredentialFormFragment newInstance(long id) {
        CredentialFormFragment f = new CredentialFormFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_ID, id);
        f.setArguments(b);
        return f;
    }

    private CredentialViewModel viewModel;
    private TextInputEditText etService;
    private TextInputEditText etUrl;
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialAutoCompleteTextView actCategory;
    private TextInputEditText etNotes;
    private MaterialCheckBox cbFavorite;
    private TextView tvError;
    private MaterialButton btnSave;
    private long editingId;
    private Credential loaded;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_credential_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CredentialViewModel.class);
        etService = view.findViewById(R.id.et_service);
        etUrl = view.findViewById(R.id.et_url);
        etUsername = view.findViewById(R.id.et_username);
        etPassword = view.findViewById(R.id.et_password);
        actCategory = view.findViewById(R.id.act_category);
        etNotes = view.findViewById(R.id.et_notes);
        cbFavorite = view.findViewById(R.id.cb_favorite);
        tvError = view.findViewById(R.id.tv_error);
        btnSave = view.findViewById(R.id.btn_save);

        editingId = getArguments() != null ? getArguments().getLong(ARG_ID, 0) : 0;
        if (editingId != 0) {
            view.findViewById(R.id.tv_title).setVisibility(View.GONE);
            viewModel.loadDetail(editingId);
        }
        viewModel.loadCategories();

        viewModel.getCategories().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess() && isAdded()) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        new ArrayList<>(result.getValue()));
                actCategory.setAdapter(adapter);
            }
        });

        viewModel.getDetail().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess() && isAdded()) {
                loaded = result.getValue();
                fill(loaded);
            }
        });

        view.findViewById(R.id.btn_suggest).setOnClickListener(v -> {
            PasswordOptions options = new PasswordOptions();
            options.setLength(16);
            String generated = new PasswordGenerator().generate(options);
            etPassword.setText(generated);
            Toast.makeText(requireContext(), R.string.gen_regenerate, Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> {
            tvError.setVisibility(View.GONE);
            btnSave.setEnabled(false);
            Credential c = loaded != null ? loaded : new Credential();
            c.setServiceName(text(etService));
            c.setUrl(text(etUrl));
            c.setUsername(text(etUsername));
            c.setPassword(text(etPassword));
            c.setCategory(actCategory.getText() == null ? "" : actCategory.getText().toString());
            c.setNotes(text(etNotes));
            c.setFavorite(cbFavorite.isChecked());
            if (editingId == 0) {
                viewModel.save(c);
            } else {
                viewModel.update(c);
            }
        });

        viewModel.getOperation().observe(getViewLifecycleOwner(), result -> {
            btnSave.setEnabled(true);
            if (result == null || !isAdded()) {
                return;
            }
            if (result.isSuccess()) {
                Toast.makeText(requireContext(), R.string.cred_saved, Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
                ((MainActivity) requireActivity()).enforceSessionGate();
            } else {
                tvError.setText(result.getError());
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void fill(Credential c) {
        etService.setText(c.getServiceName());
        etUrl.setText(c.getUrl());
        etUsername.setText(c.getUsername());
        etPassword.setText(c.getPassword());
        actCategory.setText(c.getCategory(), false);
        etNotes.setText(c.getNotes());
        cbFavorite.setChecked(c.isFavorite());
    }

    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }
}
