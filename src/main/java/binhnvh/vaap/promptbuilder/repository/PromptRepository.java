package binhnvh.vaap.promptbuilder.repository;

import binhnvh.vaap.promptbuilder.entity.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromptRepository extends JpaRepository<Prompt, Long> {

    @EntityGraph(attributePaths = {"llmProvider", "variables"})
    Optional<Prompt> findByIdAndActiveTrue(Long id);

    @EntityGraph(attributePaths = {"llmProvider"})
    Optional<Prompt> findByNameAndActiveTrue(String name);

    @Query("SELECT p FROM Prompt p WHERE p.active = true AND p.createdBy = :userId")
    List<Prompt> findAllActiveByUser(@Param("userId") String userId);

    @Query("SELECT p FROM Prompt p WHERE p.active = true AND p.llmProvider.id = :providerId")
    List<Prompt> findAllByProvider(@Param("providerId") Long providerId);

    boolean existsByNameAndActiveTrue(String name);

    @Query("SELECT COUNT(p) FROM Prompt p WHERE p.active = true")
    long countActivePrompts();
}
