package mz.ac.ucm.feng.ti.vaultkey.presentation.authenticator;

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

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.presentation.MainActivity;

/** Lista de contas 2FA. */
public class AuthenticatorListFragment extends Fragment {

    private AuthenticatorViewModel viewModel;
    private AuthenticatorAdapter adapter;
    private TextView tvEmpty;

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
        RecyclerView rv = view.findViewById(R.id.rv_accounts);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        MainActivity activity = (MainActivity) requireActivity();
        adapter = new AuthenticatorAdapter(account ->
                activity.showFragment(AuthenticatorDetailFragment.newInstance(account.getId()),
                        "auth_detail", true, false, true));
        rv.setAdapter(adapter);

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            adapter.submit(items);
            tvEmpty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
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
}
