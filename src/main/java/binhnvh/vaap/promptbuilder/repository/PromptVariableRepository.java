package binhnvh.vaap.promptbuilder.repository;

import binhnvh.vaap.promptbuilder.entity.PromptVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromptVariableRepository extends JpaRepository<PromptVariable, Long> {

    @Query("SELECT pv FROM PromptVariable pv WHERE pv.prompt.id = :promptId")
    List<PromptVariable> findAllByPromptId(@Param("promptId") Long promptId);

    @Query("SELECT pv FROM PromptVariable pv WHERE pv.prompt.id = :promptId AND pv.variableName = :varName")
    Optional<PromptVariable> findByPromptIdAndVariableName(@Param("promptId") Long promptId, @Param("varName") String varName);

    @Query("DELETE FROM PromptVariable pv WHERE pv.prompt.id = :promptId")
    void deleteAllByPromptId(@Param("promptId") Long promptId);

    boolean existsByPromptIdAndVariableName(Long promptId, String variableName);
}