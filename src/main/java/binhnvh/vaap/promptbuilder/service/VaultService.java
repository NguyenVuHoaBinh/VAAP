package binhnvh.vaap.promptbuilder.service;

public interface VaultService {
    void storeApiKey(String vaultPath, String apiKey);
    String retrieveApiKey(String vaultPath);
    void updateApiKey(String vaultPath, String newApiKey);
    void deleteApiKey(String vaultPath);
    boolean validateVaultConnection();
}
