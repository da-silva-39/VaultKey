package mz.ac.ucm.feng.ti.vaultkey.presentation.authenticator;

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

import java.util.ArrayList;
import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.AuthenticatorAccount;
import mz.ac.ucm.feng.ti.vaultkey.presentation.MainActivity;

/** Lista de contas 2FA com pesquisa local por serviço ou conta. */
public class AuthenticatorListFragment extends Fragment {

    private AuthenticatorViewModel viewModel;
    private AuthenticatorAdapter adapter;
    private TextView tvEmpty;
    private TextInputEditText etSearch;
    private List<AuthenticatorAccount> allAccounts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_authenticator_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AuthenticatorViewModel.class);
        tvEmpty = view.findViewById(R.id.tv_empty);
        etSearch = view.findViewById(R.id.et_search);
        RecyclerView rv = view.findViewById(R.id.rv_accounts);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        MainActivity activity = (MainActivity) requireActivity();
        adapter = new AuthenticatorAdapter(account ->
                activity.showFragment(AuthenticatorDetailFragment.newInstance(account.getId()),
                        "auth_detail", true, false, true));
        rv.setAdapter(adapter);

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            allAccounts = items == null ? new ArrayList<>() : items;
            applyFilter();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        view.findViewById(R.id.fab_add).setOnClickListener(v ->
                activity.showFragment(new AuthenticatorFormFragment(),
                        "auth_form", true, false, true));
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.load();
    }

    private void applyFilter() {
        String query = etSearch.getText() == null
                ? "" : etSearch.getText().toString().trim().toLowerCase();
        List<AuthenticatorAccount> filtered = new ArrayList<>();
        for (AuthenticatorAccount account : allAccounts) {
            if (query.isEmpty() || matches(account, query)) {
                filtered.add(account);
            }
        }
        adapter.submit(filtered);
        boolean empty = filtered.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        tvEmpty.setText(allAccounts.isEmpty() ? R.string.auth_empty : R.string.auth_no_results);
    }

    private static boolean matches(AuthenticatorAccount account, String query) {
        return contains(account.getIssuer(), query)
                || contains(account.getAccountName(), query);
    }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
