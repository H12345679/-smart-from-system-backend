package com.edu.smartfarm.controller;

import com.edu.smartfarm.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "数据分析", description = "统计分析接口")
@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "死亡原因统计")
    @GetMapping("/mortality-stats")
    public Result<?> getMortalityStats() {
        List<Map<String, Object>> stats = jdbcTemplate.queryForList(
                "SELECT death_cause as name, SUM(death_count) as value FROM t_mortality_record GROUP BY death_cause ORDER BY value DESC");
        return Result.success(stats);
    }

    @Operation(summary = "近7天每日投喂量统计")
    @GetMapping("/feed-daily")
    public Result<?> getDailyFeedStats() {
        List<Map<String, Object>> stats = jdbcTemplate.queryForList(
                "SELECT DATE(feed_time) as date, SUM(feed_weight_kg) as total_kg " +
                "FROM t_feed_record WHERE feed_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                "GROUP BY DATE(feed_time) ORDER BY date");
        return Result.success(stats);
    }

    @Operation(summary = "各批次FCR排行")
    @GetMapping("/fcr-ranking")
    public Result<?> getFcrRanking() {
        List<Map<String, Object>> stats = jdbcTemplate.queryForList(
                "SELECT batch_id, species_name, fcr, total_feed_kg, harvest_weight_kg " +
                "FROM t_breeding_batch WHERE fcr IS NOT NULL ORDER BY fcr ASC LIMIT 10");
        return Result.success(stats);
    }

    @Operation(summary = "各批次存活率")
    @GetMapping("/survival-rate")
    public Result<?> getSurvivalRate() {
        List<Map<String, Object>> stats = jdbcTemplate.queryForList(
                "SELECT batch_id, species_name, initial_count, current_count, " +
                "ROUND(current_count * 100.0 / initial_count, 1) as survival_rate " +
                "FROM t_breeding_batch WHERE status = 'ACTIVE' AND initial_count > 0 ORDER BY survival_rate");
        return Result.success(stats);
    }

    @Operation(summary = "报警趋势(近7天每日报警数)")
    @GetMapping("/alert-trend")
    public Result<?> getAlertTrend() {
        List<Map<String, Object>> stats = jdbcTemplate.queryForList(
                "SELECT DATE(create_time) as date, COUNT(*) as count, alert_level " +
                "FROM t_alert_history WHERE create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                "GROUP BY DATE(create_time), alert_level ORDER BY date");
        return Result.success(stats);
    }
}
