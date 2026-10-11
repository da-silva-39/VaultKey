package mz.ac.ucm.feng.ti.vaultkey.presentation.authenticator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.AuthenticatorAccount;

/** Adaptador da lista de contas 2FA (sem exibir códigos nem segredos). */
public class AuthenticatorAdapter extends RecyclerView.Adapter<AuthenticatorAdapter.Holder> {

    public interface Listener {
        void onOpen(AuthenticatorAccount account);
    }

    private final Listener listener;
    private final List<AuthenticatorAccount> data = new ArrayList<>();

    public AuthenticatorAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<AuthenticatorAccount> items) {
        data.clear();
        if (items != null) {
            data.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_authenticator, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AuthenticatorAccount a = data.get(position);
        holder.tvName.setText(a.displayName());
        holder.tvType.setText(a.getType() == AuthenticatorAccount.OtpType.HOTP ? "HOTP" : "TOTP");
        holder.itemView.setOnClickListener(v -> listener.onOpen(a));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvType;

        Holder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvType = itemView.findViewById(R.id.tv_type);
        }
    }
}
