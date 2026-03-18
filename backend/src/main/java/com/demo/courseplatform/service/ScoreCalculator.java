package com.demo.courseplatform.service;

import com.demo.courseplatform.domain.dto.DemoRequests;
import com.demo.courseplatform.domain.entity.PersistenceModels.AssignmentEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.EvaluationItemEntity;
import com.demo.courseplatform.domain.entity.PersistenceModels.RubricItemEntity;
import com.demo.courseplatform.domain.vo.DemoViews;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ScoreCalculator {

    public static final int MIN_ACTIVE_STUDENT_REVIEWS_FOR_ABNORMAL = 3;
    public static final double ABNORMAL_SCORE_GAP_THRESHOLD = 15D;

    public BigDecimal calculateWeightedTotal(List<DemoRequests.ItemScoreRequest> itemScores, List<RubricItemEntity> rubricItems) {
        if (itemScores == null || itemScores.isEmpty()) {
            throw new IllegalArgumentException("评分项不能为空");
        }
        if (rubricItems == null || rubricItems.isEmpty()) {
            throw new IllegalStateException("Rubric 未配置");
        }

        Map<Long, BigDecimal> scoreByRubric = itemScores.stream()
            .collect(Collectors.toMap(DemoRequests.ItemScoreRequest::rubricItemId, DemoRequests.ItemScoreRequest::score, (left, right) -> right));

        int totalWeight = rubricItems.stream().mapToInt(item -> item.weight).sum();
        if (totalWeight <= 0) {
            throw new IllegalStateException("Rubric 权重必须大于 0");
        }

        double weighted = 0D;
        for (RubricItemEntity item : rubricItems) {
            BigDecimal score = scoreByRubric.get(item.id);
            if (score == null) {
                throw new IllegalArgumentException("评分项不完整");
            }
            weighted += score.doubleValue() * item.weight;
        }
        return BigDecimal.valueOf(weighted / totalWeight * 10).setScale(2, RoundingMode.HALF_UP);
    }

    public Double calculatePeerScore(List<EvaluationEntity> evaluations) {
        return averageWithTrim(filterByRole(evaluations, "STUDENT"));
    }

    public Double calculateTeacherScore(List<EvaluationEntity> evaluations) {
        return averageWithoutTrim(filterByRole(evaluations, "TEACHER"));
    }

    public Double calculateRealtimeFinalScore(AssignmentEntity assignment, Double peerScore, Double teacherScore) {
        double activeWeight = 0D;
        double total = 0D;
        if (peerScore != null) {
            total += peerScore * assignment.peerWeight;
            activeWeight += assignment.peerWeight;
        }
        if (teacherScore != null) {
            total += teacherScore * assignment.teacherWeight;
            activeWeight += assignment.teacherWeight;
        }
        if (activeWeight <= 0) {
            return 0D;
        }
        return round(total / activeWeight);
    }

    public Double calculateFinalScore(AssignmentEntity assignment, Double peerScore, Double teacherScore) {
        return calculateRealtimeFinalScore(assignment, peerScore, teacherScore);
    }

    public List<DemoViews.MetricVo> buildDimensionMetrics(List<RubricItemEntity> rubricItems, List<EvaluationEntity> evaluations,
                                                          List<EvaluationItemEntity> evaluationItems) {
        List<Long> activeEvaluationIds = filterActiveEvaluationIds(evaluations);
        Map<Long, List<EvaluationItemEntity>> itemGroups = evaluationItems.stream()
            .filter(item -> activeEvaluationIds.contains(item.evaluationId))
            .collect(Collectors.groupingBy(item -> item.rubricItemId));
        List<DemoViews.MetricVo> metrics = new ArrayList<>();
        for (RubricItemEntity rubricItem : rubricItems) {
            List<EvaluationItemEntity> scoped = itemGroups.getOrDefault(rubricItem.id, List.of()).stream().toList();
            double value = scoped.isEmpty()
                ? 0D
                : scoped.stream().map(item -> item.score).mapToDouble(BigDecimal::doubleValue).average().orElse(0D) * 10;
            metrics.add(new DemoViews.MetricVo(rubricItem.itemName, round(value)));
        }
        return metrics;
    }

    public List<DemoViews.CommentVo> buildComments(List<EvaluationEntity> evaluations) {
        return evaluations.stream()
            .filter(item -> !item.excluded)
            .filter(item -> item.comment != null && !item.comment.isBlank())
            .sorted(Comparator.comparing((EvaluationEntity item) -> item.createdAt).reversed())
            .map(item -> new DemoViews.CommentVo(item.evaluatorRole, item.comment))
            .toList();
    }

    // Governance semantics:
    // 1. The system only auto-detects abnormalities on STUDENT peer reviews.
    // 2. `abnormal=true` means "flagged for review", not "excluded from aggregation".
    // 3. `excluded=true` alone controls whether a score participates in score aggregation.
    // 4. Detection starts only after there are at least 3 active student reviews.
    // 5. Mean-gap and median-gap rules run in parallel with a fixed 15-point threshold.
    public Map<Long, AbnormalDetectionResult> detectStudentAbnormalities(List<EvaluationEntity> evaluations) {
        List<EvaluationEntity> studentEvaluations = evaluations == null ? List.of() : evaluations.stream()
            .filter(item -> "STUDENT".equals(item.evaluatorRole))
            .filter(item -> item.id != null)
            .toList();
        if (studentEvaluations.isEmpty()) {
            return Map.of();
        }

        Map<Long, AbnormalDetectionResult> results = new LinkedHashMap<>();
        for (EvaluationEntity evaluation : studentEvaluations) {
            results.put(evaluation.id, new AbnormalDetectionResult(false, null));
        }

        List<EvaluationEntity> activeStudentEvaluations = studentEvaluations.stream()
            .filter(item -> !item.excluded)
            .toList();
        if (activeStudentEvaluations.size() < MIN_ACTIVE_STUDENT_REVIEWS_FOR_ABNORMAL) {
            return results;
        }

        for (EvaluationEntity evaluation : studentEvaluations) {
            List<Double> baselineScores = activeStudentEvaluations.stream()
                .filter(item -> !Objects.equals(item.id, evaluation.id))
                .map(item -> item.totalScore.doubleValue())
                .toList();
            if (baselineScores.size() < MIN_ACTIVE_STUDENT_REVIEWS_FOR_ABNORMAL - 1) {
                continue;
            }

            double candidateScore = evaluation.totalScore.doubleValue();
            double mean = baselineScores.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
            double median = median(baselineScores);
            List<String> reasons = new ArrayList<>();
            double meanGap = Math.abs(candidateScore - mean);
            double medianGap = Math.abs(candidateScore - median);
            if (meanGap >= ABNORMAL_SCORE_GAP_THRESHOLD) {
                reasons.add("与其他学生评分均值偏差 " + round(meanGap) + " 分");
            }
            if (medianGap >= ABNORMAL_SCORE_GAP_THRESHOLD) {
                reasons.add("与其他学生评分中位数偏差 " + round(medianGap) + " 分");
            }
            if (!reasons.isEmpty()) {
                results.put(evaluation.id, new AbnormalDetectionResult(true, String.join("，", reasons)));
            }
        }
        return results;
    }

    private List<EvaluationEntity> filterByRole(List<EvaluationEntity> evaluations, String role) {
        return evaluations == null ? List.of() : evaluations.stream()
            .filter(item -> !item.excluded)
            .filter(item -> role.equals(item.evaluatorRole))
            .toList();
    }

    private List<Long> filterActiveEvaluationIds(List<EvaluationEntity> evaluations) {
        return evaluations == null ? List.of() : evaluations.stream()
            .filter(item -> !item.excluded)
            .map(item -> item.id)
            .filter(Objects::nonNull)
            .toList();
    }

    private Double averageWithTrim(List<EvaluationEntity> evaluations) {
        if (evaluations.isEmpty()) {
            return null;
        }
        List<Double> scores = evaluations.stream()
            .map(item -> item.totalScore)
            .map(BigDecimal::doubleValue)
            .sorted()
            .collect(Collectors.toCollection(ArrayList::new));
        if (scores.size() >= 3) {
            scores.remove(0);
            scores.remove(scores.size() - 1);
        }
        return round(scores.stream().mapToDouble(Double::doubleValue).average().orElse(0D));
    }

    private Double averageWithoutTrim(List<EvaluationEntity> evaluations) {
        if (evaluations.isEmpty()) {
            return null;
        }
        return round(evaluations.stream().map(item -> item.totalScore).mapToDouble(BigDecimal::doubleValue).average().orElse(0D));
    }

    public double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double median(List<Double> values) {
        if (values.isEmpty()) {
            return 0D;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2D;
    }

    public record AbnormalDetectionResult(boolean abnormal, String reason) {
    }
}
