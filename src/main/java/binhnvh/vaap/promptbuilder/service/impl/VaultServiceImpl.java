package binhnvh.vaap.promptbuilder.service.impl;

import binhnvh.vaap.promptbuilder.service.VaultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponseSupport;
import java.util.HashMap;
import java.util.Map;

@Service
public class VaultServiceImpl implements VaultService {

    @Autowired
    private VaultTemplate vaultTemplate;

    @Value("${vault.secret.path:secret/data/llm-providers}")
    private String baseSecretPath;

    @Override
    public void storeApiKey(String vaultPath, String apiKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("api_key", apiKey);
        data.put("created_at", System.currentTimeMillis());

        vaultTemplate.write(getFullPath(vaultPath), data);
    }

    @Override
    public String retrieveApiKey(String vaultPath) {
        VaultResponseSupport<Map<String, Object>> response =
                vaultTemplate.read(getFullPath(vaultPath));

        if (response == null || response.getData() == null) {
            throw new IllegalArgumentException("API key not found in Vault");
        }

        Map<String, Object> data = response.getData();
        Object apiKey = data.get("api_key");

        if (apiKey == null) {
            throw new IllegalArgumentException("API key is null in Vault");
        }

        return apiKey.toString();
    }

    @Override
    public void updateApiKey(String vaultPath, String newApiKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("api_key", newApiKey);
        data.put("updated_at", System.currentTimeMillis());

        vaultTemplate.write(getFullPath(vaultPath), data);
    }

    @Override
    public void deleteApiKey(String vaultPath) {
        vaultTemplate.delete(getFullPath(vaultPath));
    }

    @Override
    public boolean validateVaultConnection() {
        try {
            vaultTemplate.read("sys/health");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getFullPath(String vaultPath) {
        return baseSecretPath + "/" + vaultPath;
    }
}