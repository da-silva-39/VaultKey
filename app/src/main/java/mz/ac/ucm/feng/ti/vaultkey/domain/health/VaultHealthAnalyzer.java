package mz.ac.ucm.feng.ti.vaultkey.domain.health;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mz.ac.ucm.feng.ti.vaultkey.domain.model.Credential;
import mz.ac.ucm.feng.ti.vaultkey.domain.password.PasswordStrengthEvaluator;

/**
 * Analisa a saúde do cofre sem revelar segredos: identifica senhas fracas,
 * repetidas e credenciais antigas. O resultado é uma estimativa local.
 */
public final class VaultHealthAnalyzer {

    public static final long OLD_DAYS = 180;
    private static final int WEAK_THRESHOLD = 45;

    private VaultHealthAnalyzer() {
    }

    public static Report analyze(List<Credential> credentials, long nowMillis) {
        List<Credential> safe = credentials == null
                ? Collections.emptyList() : credentials;
        List<Credential> weak = new ArrayList<>();
        Map<String, List<Credential>> byPassword = new HashMap<>();

        for (Credential c : safe) {
            String password = c.getPassword() == null ? "" : c.getPassword();
            if (!password.isEmpty()
                    && PasswordStrengthEvaluator.evaluate(password).getScore() < WEAK_THRESHOLD) {
                weak.add(c);
            }
            if (!password.isEmpty()) {
                byPassword.computeIfAbsent(password, k -> new ArrayList<>()).add(c);
            }
        }

        List<Credential> reused = new ArrayList<>();
        Set<Long> reusedIds = new HashSet<>();
        for (List<Credential> group : byPassword.values()) {
            if (group.size() > 1) {
                for (Credential c : group) {
                    if (reusedIds.add(c.getId())) {
                        reused.add(c);
                    }
                }
            }
        }

        List<Credential> old = new ArrayList<>();
        long cutoff = nowMillis - OLD_DAYS * 24L * 60L * 60L * 1000L;
        for (Credential c : safe) {
            long changed = c.getUpdatedAt() > 0 ? c.getUpdatedAt() : c.getCreatedAt();
            if (changed > 0 && changed < cutoff) {
                old.add(c);
            }
        }

        sortByName(weak);
        sortByName(reused);
        sortByName(old);

        int score = 100;
        score -= Math.min(40, weak.size() * 12);
        score -= Math.min(30, reused.size() * 10);
        score -= Math.min(15, old.size() * 5);
        score = Math.max(0, Math.min(100, score));

        return new Report(safe.size(), weak, reused, old, score);
    }

    private static void sortByName(List<Credential> list) {
        list.sort(Comparator.comparing(
                c -> c.getServiceName() == null ? "" : c.getServiceName(),
                String.CASE_INSENSITIVE_ORDER));
    }

    /** Resultado imutável da análise. */
    public static final class Report {

        private final int total;
        private final List<Credential> weak;
        private final List<Credential> reused;
        private final List<Credential> old;
        private final int score;

        Report(int total, List<Credential> weak, List<Credential> reused,
               List<Credential> old, int score) {
            this.total = total;
            this.weak = Collections.unmodifiableList(weak);
            this.reused = Collections.unmodifiableList(reused);
            this.old = Collections.unmodifiableList(old);
            this.score = score;
        }

        public int getTotal() {
            return total;
        }

        public List<Credential> getWeak() {
            return weak;
        }

        public List<Credential> getReused() {
            return reused;
        }

        public List<Credential> getOld() {
            return old;
        }

        public int getScore() {
            return score;
        }

        public int getIssueCount() {
            Set<Long> ids = new HashSet<>();
            for (Credential c : weak) {
                ids.add(c.getId());
            }
            for (Credential c : reused) {
                ids.add(c.getId());
            }
            for (Credential c : old) {
                ids.add(c.getId());
            }
            return ids.size();
        }

        public boolean isHealthy() {
            return weak.isEmpty() && reused.isEmpty() && old.isEmpty();
        }
    }
}
