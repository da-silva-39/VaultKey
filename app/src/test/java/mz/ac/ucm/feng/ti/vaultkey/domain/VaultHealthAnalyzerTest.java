package mz.ac.ucm.feng.ti.vaultkey.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mz.ac.ucm.feng.ti.vaultkey.domain.health.VaultHealthAnalyzer;
import mz.ac.ucm.feng.ti.vaultkey.domain.model.Credential;

public class VaultHealthAnalyzerTest {

    private static final long NOW = 1_800_000_000_000L;
    private static final long DAY = 24L * 60L * 60L * 1000L;

    @Test
    public void emptyVaultIsHealthy() {
        VaultHealthAnalyzer.Report report =
                VaultHealthAnalyzer.analyze(new ArrayList<>(), NOW);
        assertEquals(0, report.getTotal());
        assertEquals(100, report.getScore());
        assertTrue(report.isHealthy());
        assertEquals(0, report.getIssueCount());
    }

    @Test
    public void detectsWeakPasswords() {
        List<Credential> list = Arrays.asList(
                credential(1, "Antigo", "123456", NOW - DAY, "a"),
                credential(2, "Forte", "Tr7$kQ9!mZx2@vBn4#Qw", NOW - DAY, "b"));
        VaultHealthAnalyzer.Report report = VaultHealthAnalyzer.analyze(list, NOW);
        assertEquals(1, report.getWeak().size());
        assertEquals("Antigo", report.getWeak().get(0).getServiceName());
        assertTrue(report.getScore() < 100);
    }

    @Test
    public void detectsReusedPasswords() {
        String repeated = "Tr7$kQ9!mZx2@vBn4#Qw";
        List<Credential> list = Arrays.asList(
                credential(1, "GitHub", repeated, NOW - DAY, "a"),
                credential(2, "GitLab", repeated, NOW - DAY, "b"),
                credential(3, "Banco", "Outra-S3nh4!Forte#2026", NOW - DAY, "c"));
        VaultHealthAnalyzer.Report report = VaultHealthAnalyzer.analyze(list, NOW);
        assertEquals(2, report.getReused().size());
        assertEquals(2, report.getIssueCount());
    }

    @Test
    public void detectsOldCredentials() {
        List<Credential> list = Arrays.asList(
                credential(1, "Antigo", "Tr7$kQ9!mZx2@vBn4#Qw", NOW - 400 * DAY, "a"),
                credential(2, "Recente", "Outra-S3nh4!Forte#2026", NOW - 10 * DAY, "b"));
        VaultHealthAnalyzer.Report report = VaultHealthAnalyzer.analyze(list, NOW);
        assertEquals(1, report.getOld().size());
        assertEquals("Antigo", report.getOld().get(0).getServiceName());
    }

    @Test
    public void scoreNeverExceedsBounds() {
        List<Credential> list = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            list.add(credential(i, "Serviço " + i, "123456", NOW - 500 * DAY, "x"));
        }
        VaultHealthAnalyzer.Report report = VaultHealthAnalyzer.analyze(list, NOW);
        assertTrue(report.getScore() >= 0);
        assertTrue(report.getScore() <= 100);
        assertFalse(report.isHealthy());
    }

    private static Credential credential(long id, String service, String password,
                                         long updatedAt, String url) {
        Credential c = new Credential();
        c.setId(id);
        c.setServiceName(service);
        c.setPassword(password);
        c.setUrl(url);
        c.setCreatedAt(updatedAt);
        c.setUpdatedAt(updatedAt);
        return c;
    }
}
