package mz.ac.ucm.feng.ti.vaultkey.presentation.credentials;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.R;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.Credential;

/** Adaptador da lista de credenciais (sem exibir senhas). */
public class CredentialAdapter extends RecyclerView.Adapter<CredentialAdapter.Holder> {

    public interface Listener {
        void onOpen(Credential credential);
    }

    private final Listener listener;
    private final List<Credential> data = new ArrayList<>();

    public CredentialAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Credential> items) {
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
                .inflate(R.layout.item_credential, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Credential c = data.get(position);
        holder.tvService.setText(c.getServiceName());
        String sub = c.getUsername().isEmpty() ? c.getCategory() : c.getUsername();
        holder.tvSub.setText(sub);
        holder.ivFav.setVisibility(c.isFavorite() ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onOpen(c));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView tvService;
        final TextView tvSub;
        final ImageView ivFav;

        Holder(@NonNull View itemView) {
            super(itemView);
            tvService = itemView.findViewById(R.id.tv_service);
            tvSub = itemView.findViewById(R.id.tv_sub);
            ivFav = itemView.findViewById(R.id.iv_fav);
        }
    }
}
