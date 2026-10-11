package mz.ac.ucm.feng.ti.vaultkey.presentation.dashboard;

import android.os.Bundle;
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
import mz.ac.ucm.feng.ti.vaultkey.di.ServiceLocator;
import mz.ac.ucm.feng.ti.vaultkey.presentation.MainActivity;
import mz.ac.ucm.feng.ti.vaultkey.presentation.authenticator.AuthenticatorFormFragment;
import mz.ac.ucm.feng.ti.vaultkey.presentation.credentials.CredentialDetailFragment;
import mz.ac.ucm.feng.ti.vaultkey.presentation.credentials.CredentialFormFragment;
import mz.ac.ucm.feng.ti.vaultkey.presentation.credentials.CredentialListFragment;
import mz.ac.ucm.feng.ti.vaultkey.presentation.credentials.CredentialViewModel;
import mz.ac.ucm.feng.ti.vaultkey.presentation.credentials.CredentialAdapter;
import mz.ac.ucm.feng.ti.vaultkey.presentation.passwordgenerator.PasswordGeneratorFragment;

/**
 * Dashboard: contagens, acessos rápidos, favoritos e pesquisa.
 * Nunca apresenta senhas ou segredos sem acção explícita.
 */
public class DashboardFragment extends Fragment {

    private CredentialViewModel credentialViewModel;
    private TextView tvCredCount;
    private TextView tvAuthCount;
    private TextView tvEmpty;
    private CredentialAdapter favoritesAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        credentialViewModel = new ViewModelProvider(this).get(CredentialViewModel.class);

        tvCredCount = view.findViewById(R.id.tv_cred_count);
        tvAuthCount = view.findViewById(R.id.tv_auth_count);
        tvEmpty = view.findViewById(R.id.tv_empty);
        TextInputEditText etSearch = view.findViewById(R.id.et_search);
        RecyclerView rvFavorites = view.findViewById(R.id.rv_favorites);
        rvFavorites.setLayoutManager(new LinearLayoutManager(requireContext()));

        MainActivity activity = (MainActivity) requireActivity();
        favoritesAdapter = new CredentialAdapter(cred ->
                activity.showFragment(CredentialDetailFragment.newInstance(cred.getId()),
                        "cred_detail", true, false, true));
        rvFavorites.setAdapter(favoritesAdapter);

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String q = etSearch.getText() == null ? "" : etSearch.getText().toString();
            activity.showFragment(CredentialListFragment.newInstance(q),
                    "cred_list", true, true, false);
            return true;
        });

        view.findViewById(R.id.btn_lock).setOnClickListener(v -> {
            ServiceLocator.get().vaultRepository().lock();
            activity.showUnlock();
        });
        view.findViewById(R.id.btn_new_credential).setOnClickListener(v ->
                activity.showFragment(new CredentialFormFragment(),
                        "cred_form", true, false, true));
        view.findViewById(R.id.btn_generate).setOnClickListener(v ->
                activity.showFragment(new PasswordGeneratorFragment(),
                        "gen", true, false, false));
        view.findViewById(R.id.btn_new_2fa).setOnClickListener(v ->
                activity.showFragment(new AuthenticatorFormFragment(),
                        "auth_form", true, false, true));

        credentialViewModel.getCounts().observe(getViewLifecycleOwner(), counts -> {
            if (counts != null && counts.length == 2) {
                tvCredCount.setText(String.valueOf(counts[0]));
                tvAuthCount.setText(String.valueOf(counts[1]));
            }
        });
        credentialViewModel.getFavorites().observe(getViewLifecycleOwner(), favs -> {
            favoritesAdapter.submit(favs);
            tvEmpty.setVisibility(favs == null || favs.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        credentialViewModel.loadCounts();
        credentialViewModel.loadFavorites();
    }
}
