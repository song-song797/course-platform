package com.demo.courseplatform.service;

import com.demo.courseplatform.domain.dto.DemoRequests;
import com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationItemEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.RubricItemEntity;
import java.util.Map;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreCalculatorTest {

    private final ScoreCalculator scoreCalculator = new ScoreCalculator();

    @Test
    void shouldCalculateWeightedRubricTotal() {
        RubricItemEntity item1 = rubricItem(1L, 40);
        RubricItemEntity item2 = rubricItem(2L, 60);

        BigDecimal total = scoreCalculator.calculateWeightedTotal(
            List.of(
                new DemoRequests.ItemScoreRequest(1L, BigDecimal.valueOf(8.0), null),
                new DemoRequests.ItemScoreRequest(2L, BigDecimal.valueOf(9.0), null)
            ),
            List.of(item1, item2)
        );

        assertEquals(BigDecimal.valueOf(86.00).setScale(2), total);
    }

    @Test
    void shouldTrimHighestAndLowestStudentScores() {
        Double peerScore = scoreCalculator.calculatePeerScore(List.of(
            evaluation("STUDENT", 70),
            evaluation("STUDENT", 80),
            evaluation("STUDENT", 90),
            evaluation("STUDENT", 100)
        ));

        assertEquals(85.0, peerScore);
    }

    @Test
    void shouldAverageTeacherScoresWithoutTrim() {
        Double teacherScore = scoreCalculator.calculateTeacherScore(List.of(
            evaluation("TEACHER", 88),
            evaluation("TEACHER", 92)
        ));

        assertEquals(90.0, teacherScore);
    }

    @Test
    void shouldCalculateRealtimeAndClosedFinalScoreSeparately() {
        AssignmentEntity reviewing = assignment("REVIEWING", 40, 60);

        Double realtime = scoreCalculator.calculateRealtimeFinalScore(reviewing, 85.0, 95.0);
        Double finalForReviewing = scoreCalculator.calculateFinalScore(reviewing, 85.0, 95.0);

        assertEquals(91.0, realtime);
        assertEquals(91.0, finalForReviewing);
    }

    @Test
    void shouldIgnoreExcludedEvaluationItemsWhenBuildingDimensionMetrics() {
        RubricItemEntity rubricItem = rubricItem(1L, 100);

        EvaluationEntity validEvaluation = evaluation("STUDENT", 90);
        validEvaluation.id = 11L;
        EvaluationEntity excludedEvaluation = evaluation("STUDENT", 20);
        excludedEvaluation.id = 12L;
        excludedEvaluation.excluded = true;

        EvaluationItemEntity validItem = evaluationItem(11L, 1L, 9.0);
        EvaluationItemEntity excludedItem = evaluationItem(12L, 1L, 1.0);

        List<com.demo.courseplatform.domain.vo.DemoViews.MetricVo> metrics = scoreCalculator.buildDimensionMetrics(
            List.of(rubricItem),
            List.of(validEvaluation, excludedEvaluation),
            List.of(validItem, excludedItem)
        );

        assertEquals(90.0, metrics.get(0).value());
    }

    @Test
    void shouldSkipAbnormalDetectionBeforeThreeActiveStudentReviews() {
        EvaluationEntity first = evaluation("STUDENT", 85);
        first.id = 1L;
        EvaluationEntity second = evaluation("STUDENT", 100);
        second.id = 2L;

        Map<Long, ScoreCalculator.AbnormalDetectionResult> result = scoreCalculator.detectStudentAbnormalities(List.of(first, second));

        assertFalse(result.get(1L).abnormal());
        assertNull(result.get(1L).reason());
        assertFalse(result.get(2L).abnormal());
    }

    @Test
    void shouldDetectMeanAndMedianAbnormalityTogether() {
        EvaluationEntity first = evaluation("STUDENT", 85);
        first.id = 1L;
        EvaluationEntity second = evaluation("STUDENT", 85);
        second.id = 2L;
        EvaluationEntity abnormal = evaluation("STUDENT", 100);
        abnormal.id = 3L;

        Map<Long, ScoreCalculator.AbnormalDetectionResult> result = scoreCalculator.detectStudentAbnormalities(List.of(first, second, abnormal));

        assertFalse(result.get(1L).abnormal());
        assertFalse(result.get(2L).abnormal());
        assertTrue(result.get(3L).abnormal());
        assertTrue(result.get(3L).reason().contains("均值偏差 15.0 分"));
        assertTrue(result.get(3L).reason().contains("中位数偏差 15.0 分"));
    }

    @Test
    void shouldDetectMedianOnlyAbnormality() {
        EvaluationEntity first = evaluation("STUDENT", 50);
        first.id = 1L;
        EvaluationEntity second = evaluation("STUDENT", 50);
        second.id = 2L;
        EvaluationEntity third = evaluation("STUDENT", 95);
        third.id = 3L;
        EvaluationEntity candidate = evaluation("STUDENT", 78);
        candidate.id = 4L;

        Map<Long, ScoreCalculator.AbnormalDetectionResult> result =
            scoreCalculator.detectStudentAbnormalities(List.of(first, second, third, candidate));

        assertTrue(result.get(4L).abnormal());
        assertTrue(result.get(4L).reason().contains("中位数偏差"));
        assertFalse(result.get(4L).reason().contains("均值偏差"));
    }

    private RubricItemEntity rubricItem(Long id, int weight) {
        RubricItemEntity item = new RubricItemEntity();
        item.id = id;
        item.weight = weight;
        return item;
    }

    private EvaluationEntity evaluation(String role, double totalScore) {
        EvaluationEntity entity = new EvaluationEntity();
        entity.evaluatorRole = role;
        entity.totalScore = BigDecimal.valueOf(totalScore);
        return entity;
    }

    private EvaluationItemEntity evaluationItem(Long evaluationId, Long rubricItemId, double score) {
        EvaluationItemEntity entity = new EvaluationItemEntity();
        entity.evaluationId = evaluationId;
        entity.rubricItemId = rubricItemId;
        entity.score = BigDecimal.valueOf(score);
        return entity;
    }

    private AssignmentEntity assignment(String status, int peerWeight, int teacherWeight) {
        AssignmentEntity entity = new AssignmentEntity();
        entity.status = status;
        entity.peerWeight = peerWeight;
        entity.teacherWeight = teacherWeight;
        return entity;
    }
}
