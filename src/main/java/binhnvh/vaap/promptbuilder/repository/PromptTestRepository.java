package binhnvh.vaap.promptbuilder.repository;

import binhnvh.vaap.promptbuilder.entity.PromptTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromptTestRepository extends JpaRepository<PromptTest, Long> {

    @EntityGraph(attributePaths = {"promptA", "promptB", "results"})
    Optional<PromptTest> findByIdWithDetails(Long id);

    @Query("SELECT pt FROM PromptTest pt WHERE pt.status = :status ORDER BY pt.createdAt DESC")
    List<PromptTest> findAllByStatus(@Param("status") PromptTest.TestStatus status);

    @Query("SELECT pt FROM PromptTest pt WHERE pt.createdBy = :userId ORDER BY pt.createdAt DESC")
    List<PromptTest> findAllByUser(@Param("userId") String userId);

    @Query("SELECT pt FROM PromptTest pt WHERE (pt.promptA.id = :promptId OR pt.promptB.id = :promptId)")
    List<PromptTest> findAllTestsForPrompt(@Param("promptId") Long promptId);

    @Query("SELECT COUNT(pt) FROM PromptTest pt WHERE pt.status = :status")
    long countByStatus(@Param("status") PromptTest.TestStatus status);
}
