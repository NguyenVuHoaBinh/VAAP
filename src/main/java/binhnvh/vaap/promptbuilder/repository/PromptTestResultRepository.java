package binhnvh.vaap.promptbuilder.repository;

import binhnvh.vaap.promptbuilder.dto.TestResultSummaryDTO;
import binhnvh.vaap.promptbuilder.entity.PromptTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PromptTestResultRepository extends JpaRepository<PromptTestResult, Long> {

    @Query("SELECT ptr FROM PromptTestResult ptr WHERE ptr.promptTest.id = :testId")
    List<PromptTestResult> findAllByTestId(@Param("testId") Long testId);

    @Query("SELECT ptr FROM PromptTestResult ptr WHERE ptr.prompt.id = :promptId")
    List<PromptTestResult> findAllByPromptId(@Param("promptId") Long promptId);

    @Query("SELECT new binhnvh.vaap.promptbuilder.dto.TestResultSummaryDTO(" +
            "ptr.prompt.id, " +
            "AVG(ptr.executionTimeMs), " +
            "AVG(ptr.tokenCount), " +
            "AVG(ptr.cost), " +
            "AVG(ptr.successRate), " +
            "AVG(ptr.averageScore)) " +
            "FROM PromptTestResult ptr " +
            "WHERE ptr.promptTest.id = :testId " +
            "GROUP BY ptr.prompt.id")
    List<TestResultSummaryDTO> getTestSummary(@Param("testId") Long testId);
}