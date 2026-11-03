package mz.ac.ucm.feng.ti.vaultkey.presentation.health;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.domain.health.VaultHealthAnalyzer;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.Credential;
import mz.ac.ucm.feng.ti.vaultkey.presentation.MainActivity;
import mz.ac.ucm.feng.ti.vaultkey.presentation.credentials.CredentialDetailFragment;

/**
 * Relatório local de saúde do cofre: senhas fracas, repetidas e antigas.
 * Não expõe senhas, apenas os serviços afectados.
 */
public class VaultHealthFragment extends Fragment {

    private VaultHealthViewModel viewModel;
    private TextView tvScore;
    private TextView tvSummary;
    private TextView tvTotal;
    private TextView tvAllGood;
    private TextView tvWeakTitle;
    private TextView tvReusedTitle;
    private TextView tvOldTitle;
    private ProgressBar pbScore;
    private LinearLayout llWeak;
    private LinearLayout llReused;
    private LinearLayout llOld;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vault_health, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(VaultHealthViewModel.class);

        tvScore = view.findViewById(R.id.tv_score);
        tvSummary = view.findViewById(R.id.tv_summary);
        tvTotal = view.findViewById(R.id.tv_total);
        tvAllGood = view.findViewById(R.id.tv_all_good);
        tvWeakTitle = view.findViewById(R.id.tv_weak_title);
        tvReusedTitle = view.findViewById(R.id.tv_reused_title);
        tvOldTitle = view.findViewById(R.id.tv_old_title);
        pbScore = view.findViewById(R.id.pb_score);
        llWeak = view.findViewById(R.id.ll_weak);
        llReused = view.findViewById(R.id.ll_reused);
        llOld = view.findViewById(R.id.ll_old);

        view.findViewById(R.id.btn_back).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        viewModel.getReport().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.load();
    }

    private void render(VaultHealthAnalyzer.Report report) {
        if (report == null || !isAdded()) {
            return;
        }
        int score = report.getScore();
        int colorRes;
        if (score >= 80) {
            colorRes = R.color.vault_success;
        } else if (score >= 50) {
            colorRes = R.color.vault_warning;
        } else {
            colorRes = R.color.vault_error;
        }
        int color = ContextCompat.getColor(requireContext(), colorRes);
        tvScore.setText(String.valueOf(score));
        tvScore.setTextColor(color);
        pbScore.setProgress(score);
        pbScore.setProgressTintList(ColorStateList.valueOf(color));

        int issues = report.getIssueCount();
        tvSummary.setText(issues == 0
                ? getString(R.string.health_summary_perfect)
                : getResources().getQuantityString(R.plurals.health_issues, issues, issues));
        tvTotal.setText(getResources().getQuantityString(
                R.plurals.health_total, report.getTotal(), report.getTotal()));
        tvAllGood.setVisibility(report.isHealthy() ? View.VISIBLE : View.GONE);

        bind(tvWeakTitle, llWeak, report.getWeak(), R.string.health_weak_count);
        bind(tvReusedTitle, llReused, report.getReused(), R.string.health_reused_count);
        bind(tvOldTitle, llOld, report.getOld(), R.string.health_old_count);
    }

    private void bind(TextView title, LinearLayout container, List<Credential> items,
                      int titleRes) {
        title.setText(getString(titleRes, items.size()));
        container.removeAllViews();
        if (items.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText(R.string.health_none);
            empty.setTextColor(ContextCompat.getColor(
                    requireContext(), R.color.vault_text_muted));
            empty.setTextSize(13);
            container.addView(empty);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Credential c : items) {
            View row = inflater.inflate(R.layout.item_health_issue, container, false);
            TextView name = row.findViewById(R.id.tv_name);
            TextView sub = row.findViewById(R.id.tv_sub);
            name.setText(c.getServiceName());
            String detail = c.getUsername();
            if (detail == null || detail.isEmpty()) {
                detail = c.getCategory();
            }
            sub.setText(detail);
            row.setOnClickListener(v -> ((MainActivity) requireActivity()).showFragment(
                    CredentialDetailFragment.newInstance(c.getId()),
                    "cred_detail", true, false, true));
            container.addView(row);
        }
    }
}
