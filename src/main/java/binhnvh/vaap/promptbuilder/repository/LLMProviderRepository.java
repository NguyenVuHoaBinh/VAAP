package binhnvh.vaap.promptbuilder.repository;

import binhnvh.vaap.promptbuilder.entity.LLMProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LLMProviderRepository extends JpaRepository<LLMProvider, Long> {

    Optional<LLMProvider> findByProviderNameAndActiveTrue(String providerName);

    List<LLMProvider> findAllByActiveTrue();

    boolean existsByProviderNameAndActiveTrue(String providerName);

    @Query("SELECT p FROM LLMProvider p WHERE p.active = true AND p.rateLimit > :minRateLimit")
    List<LLMProvider> findProvidersWithMinRateLimit(@Param("minRateLimit") Integer minRateLimit);
}
