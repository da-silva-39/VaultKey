package mz.ac.ucm.feng.ti.vaultkey.presentation.credentials;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.presentation.MainActivity;

/** Lista de credenciais com pesquisa, filtro de favoritas e criação. */
public class CredentialListFragment extends Fragment {

    private static final String ARG_QUERY = "query";

    public static CredentialListFragment newInstance(String query) {
        CredentialListFragment f = new CredentialListFragment();
        Bundle b = new Bundle();
        b.putString(ARG_QUERY, query);
        f.setArguments(b);
        return f;
    }

    private CredentialViewModel viewModel;
    private CredentialAdapter adapter;
    private TextView tvEmpty;
    private boolean favoritesOnly;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_credential_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CredentialViewModel.class);
        tvEmpty = view.findViewById(R.id.tv_empty);
        TextInputEditText etSearch = view.findViewById(R.id.et_search);
        RecyclerView rv = view.findViewById(R.id.rv_credentials);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        MainActivity activity = (MainActivity) requireActivity();
        adapter = new CredentialAdapter(cred ->
                activity.showFragment(CredentialDetailFragment.newInstance(cred.getId()),
                        "cred_detail", true, false, true));
        rv.setAdapter(adapter);

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            if (favoritesOnly) {
                return;
            }
            adapter.submit(items);
            tvEmpty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getFavorites().observe(getViewLifecycleOwner(), items -> {
            if (!favoritesOnly) {
                return;
            }
            adapter.submit(items);
            tvEmpty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refresh(String.valueOf(s));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        view.findViewById(R.id.chip_all).setOnClickListener(v -> {
            favoritesOnly = false;
            refresh(etSearch.getText() == null ? "" : etSearch.getText().toString());
        });
        view.findViewById(R.id.chip_fav).setOnClickListener(v -> {
            favoritesOnly = true;
            refresh(etSearch.getText() == null ? "" : etSearch.getText().toString());
        });
        view.findViewById(R.id.fab_add).setOnClickListener(v ->
                activity.showFragment(new CredentialFormFragment(),
                        "cred_form", true, false, true));

        if (getArguments() != null && getArguments().getString(ARG_QUERY) != null) {
            etSearch.setText(getArguments().getString(ARG_QUERY));
        }
        refresh(etSearch.getText() == null ? "" : etSearch.getText().toString());
    }

    private void refresh(String query) {
        if (favoritesOnly && (query == null || query.isEmpty())) {
            viewModel.loadFavorites();
        } else if (query == null || query.isEmpty()) {
            viewModel.load(true);
        } else {
            viewModel.search(query);
        }
    }
}
