package binhnvh.vaap.promptbuilder.repository;

import binhnvh.vaap.promptbuilder.entity.PromptVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromptVersionRepository extends JpaRepository<PromptVersion, Long> {

    @Query("SELECT pv FROM PromptVersion pv WHERE pv.prompt.id = :promptId ORDER BY pv.versionNumber DESC")
    List<PromptVersion> findAllByPromptIdOrderByVersionDesc(@Param("promptId") Long promptId);

    @Query("SELECT pv FROM PromptVersion pv WHERE pv.prompt.id = :promptId AND pv.versionNumber = :version")
    Optional<PromptVersion> findByPromptIdAndVersion(@Param("promptId") Long promptId, @Param("version") Integer version);

    @Query("SELECT MAX(pv.versionNumber) FROM PromptVersion pv WHERE pv.prompt.id = :promptId")
    Optional<Integer> findMaxVersionByPromptId(@Param("promptId") Long promptId);

    @Query("SELECT COUNT(pv) FROM PromptVersion pv WHERE pv.prompt.id = :promptId")
    long countVersionsByPromptId(@Param("promptId") Long promptId);
}