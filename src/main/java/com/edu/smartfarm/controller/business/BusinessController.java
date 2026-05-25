package com.edu.smartfarm.controller.business;

import com.edu.smartfarm.common.Result;
import com.edu.smartfarm.interceptor.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * 经营管理模块：成本核算、利润预测、饲料库存、市场行情、投喂策略、出栏预测、追溯码、检疫证
 */
@Tag(name = "经营管理", description = "成本/利润/库存/市场/策略/追溯")
@RestController
@RequestMapping("/business")
@RequiredArgsConstructor
public class BusinessController {

    private final JdbcTemplate jdbc;

    // ==================== 成本核算 ====================

    @Operation(summary = "获取批次成本汇总")
    @GetMapping("/cost/summary/{batchId}")
    public Result<?> getCostSummary(@PathVariable Long batchId) {
        List<Map<String, Object>> costs = jdbc.queryForList(
                "SELECT cost_type, SUM(amount) as total FROM t_cost_record WHERE batch_id = ? GROUP BY cost_type", batchId);
        BigDecimal totalCost = BigDecimal.ZERO;
        Map<String, Object> result = new HashMap<>();
        for (Map<String, Object> c : costs) {
            result.put((String) c.get("cost_type"), c.get("total"));
            totalCost = totalCost.add((BigDecimal) c.get("total"));
        }
        result.put("TOTAL", totalCost);
        // 计算单位成本
        List<Map<String, Object>> batch = jdbc.queryForList("SELECT current_count, harvest_weight_kg FROM t_breeding_batch WHERE id = ?", batchId);
        if (!batch.isEmpty() && batch.get(0).get("harvest_weight_kg") != null) {
            BigDecimal weight = (BigDecimal) batch.get(0).get("harvest_weight_kg");
            if (weight.compareTo(BigDecimal.ZERO) > 0) {
                result.put("costPerKg", totalCost.divide(weight, 2, RoundingMode.HALF_UP));
            }
        }
        return Result.success(result);
    }

    @Operation(summary = "录入成本记录")
    @PostMapping("/cost/add")
    @RequireRole({"ADMIN", "MANAGER"})
    public Result<?> addCostRecord(@RequestBody Map<String, Object> body) {
        jdbc.update("INSERT INTO t_cost_record (batch_id, cost_type, amount, description, record_date) VALUES (?,?,?,?,?)",
                body.get("batchId"), body.get("costType"), body.get("amount"), body.get("description"), LocalDate.now());
        return Result.success("成本记录已添加");
    }

    // ==================== 利润预测 ====================

    @Operation(summary = "利润预测", description = "基于当前成本+市场价+存量预估利润")
    @GetMapping("/profit/predict/{batchId}")
    public Result<?> predictProfit(@PathVariable Long batchId) {
        Map<String, Object> batch = jdbc.queryForMap(
                "SELECT species_name, current_count, initial_avg_weight, total_feed_kg FROM t_breeding_batch WHERE id = ?", batchId);
        // 获取成本
        BigDecimal totalCost = BigDecimal.ZERO;
        try {
            totalCost = jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM t_cost_record WHERE batch_id = ?", BigDecimal.class, batchId);
        } catch (Exception e) { /* 无成本记录 */ }
        // 获取市场价
        String species = (String) batch.get("species_name");
        BigDecimal marketPrice;
        try {
            marketPrice = jdbc.queryForObject("SELECT price_per_kg FROM t_market_price WHERE species_name = ? ORDER BY price_date DESC LIMIT 1", BigDecimal.class, species);
        } catch (Exception e) {
            marketPrice = BigDecimal.valueOf(65); // 默认65元/kg
        }
        // 预测出栏重量 (按FCR=1.3估算)
        BigDecimal feedKg = (BigDecimal) batch.get("total_feed_kg");
        int count = (int) batch.get("current_count");
        BigDecimal avgWeight = batch.get("initial_avg_weight") != null ? (BigDecimal) batch.get("initial_avg_weight") : BigDecimal.valueOf(50);
        BigDecimal estimatedWeightKg = feedKg.divide(BigDecimal.valueOf(1.3), 2, RoundingMode.HALF_UP)
                .add(avgWeight.multiply(BigDecimal.valueOf(count)).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP));
        BigDecimal estimatedRevenue = estimatedWeightKg.multiply(marketPrice);
        BigDecimal estimatedProfit = estimatedRevenue.subtract(totalCost);

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("species", species);
        result.put("currentCount", count);
        result.put("totalCost", totalCost);
        result.put("marketPrice", marketPrice);
        result.put("estimatedWeightKg", estimatedWeightKg);
        result.put("estimatedRevenue", estimatedRevenue);
        result.put("estimatedProfit", estimatedProfit);
        result.put("profitMargin", estimatedRevenue.compareTo(BigDecimal.ZERO) > 0
                ? estimatedProfit.multiply(BigDecimal.valueOf(100)).divide(estimatedRevenue, 1, RoundingMode.HALF_UP) + "%"
                : "0%");
        return Result.success(result);
    }

    // ==================== 饲料库存 ====================

    @Operation(summary = "饲料库存列表")
    @GetMapping("/feed-inventory/list")
    public Result<?> listFeedInventory() {
        return Result.success(jdbc.queryForList("SELECT *, (stock_kg <= warning_threshold) as is_warning FROM t_feed_inventory ORDER BY id"));
    }

    @Operation(summary = "饲料入库")
    @PostMapping("/feed-inventory/in")
    public Result<?> feedIn(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(body.get("inventoryId").toString());
        double qty = Double.parseDouble(body.get("quantityKg").toString());
        jdbc.update("UPDATE t_feed_inventory SET stock_kg = stock_kg + ? WHERE id = ?", qty, id);
        jdbc.update("INSERT INTO t_feed_inventory_log (inventory_id, type, quantity_kg, remark, create_time) VALUES (?, 'IN', ?, ?, NOW())",
                id, qty, body.getOrDefault("remark", "入库"));
        return Result.success("入库成功");
    }

    @Operation(summary = "饲料出库")
    @PostMapping("/feed-inventory/out")
    public Result<?> feedOut(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(body.get("inventoryId").toString());
        double qty = Double.parseDouble(body.get("quantityKg").toString());
        jdbc.update("UPDATE t_feed_inventory SET stock_kg = stock_kg - ? WHERE id = ?", qty, id);
        jdbc.update("INSERT INTO t_feed_inventory_log (inventory_id, type, quantity_kg, batch_id, remark, create_time) VALUES (?, 'OUT', ?, ?, ?, NOW())",
                id, qty, body.get("batchId"), body.getOrDefault("remark", "投喂出库"));
        return Result.success("出库成功");
    }

    // ==================== 市场行情 ====================

    @Operation(summary = "获取市场行情(近7天)")
    @GetMapping("/market/prices")
    public Result<?> getMarketPrices(@RequestParam(defaultValue = "黄条鰤") String species) {
        return Result.success(jdbc.queryForList(
                "SELECT species_name, price_per_kg, market_name, price_date FROM t_market_price WHERE species_name = ? ORDER BY price_date DESC LIMIT 30", species));
    }

    @Operation(summary = "获取所有品种最新价格")
    @GetMapping("/market/latest")
    public Result<?> getLatestPrices() {
        return Result.success(jdbc.queryForList(
                "SELECT mp.* FROM t_market_price mp INNER JOIN (SELECT species_name, MAX(price_date) as max_date FROM t_market_price GROUP BY species_name) latest ON mp.species_name = latest.species_name AND mp.price_date = latest.max_date"));
    }

    // ==================== 投喂策略推荐 ====================

    @Operation(summary = "获取投喂策略推荐", description = "根据水温+鱼体重+存活尾数计算最佳日投喂量")
    @GetMapping("/feed-strategy/{batchId}")
    public Result<?> getFeedStrategy(@PathVariable Long batchId) {
        Map<String, Object> batch = jdbc.queryForMap(
                "SELECT species_name, current_count, initial_avg_weight, total_feed_kg, DATEDIFF(NOW(), start_date) as days FROM t_breeding_batch WHERE id = ?", batchId);

        int count = (int) batch.get("current_count");
        BigDecimal avgWeight = batch.get("initial_avg_weight") != null ? (BigDecimal) batch.get("initial_avg_weight") : BigDecimal.valueOf(50);
        int days = ((Long) batch.get("days")).intValue();

        // 估算当前体重: 初始均重 + 日增重(约3-5g/天 黄条鰤)
        double currentAvgWeightG = avgWeight.doubleValue() + days * 4.0;
        double totalBiomassKg = currentAvgWeightG * count / 1000.0;

        // 投喂率: 水温18-22℃时投喂率约2-3%体重
        double feedRate = 0.025; // 2.5%
        double recommendedDailyKg = totalBiomassKg * feedRate;

        // 分餐建议
        int mealsPerDay = currentAvgWeightG < 200 ? 3 : 2;
        double perMealKg = recommendedDailyKg / mealsPerDay;

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("currentCount", count);
        result.put("estimatedAvgWeightG", Math.round(currentAvgWeightG));
        result.put("totalBiomassKg", Math.round(totalBiomassKg * 10.0) / 10.0);
        result.put("feedRate", "2.5%");
        result.put("recommendedDailyKg", Math.round(recommendedDailyKg * 10.0) / 10.0);
        result.put("mealsPerDay", mealsPerDay);
        result.put("perMealKg", Math.round(perMealKg * 10.0) / 10.0);
        result.put("suggestion", String.format("建议今日投喂 %.1fkg，分%d餐，每餐 %.1fkg", recommendedDailyKg, mealsPerDay, perMealKg));
        return Result.success(result);
    }

    // ==================== 出栏时机预测 ====================

    @Operation(summary = "出栏时机预测", description = "预测达到目标体重的日期+预估利润")
    @GetMapping("/harvest-predict/{batchId}")
    public Result<?> predictHarvest(@PathVariable Long batchId) {
        Map<String, Object> batch = jdbc.queryForMap(
                "SELECT species_name, current_count, initial_avg_weight, total_feed_kg, start_date, DATEDIFF(NOW(), start_date) as days FROM t_breeding_batch WHERE id = ?", batchId);

        BigDecimal avgWeight = batch.get("initial_avg_weight") != null ? (BigDecimal) batch.get("initial_avg_weight") : BigDecimal.valueOf(50);
        int days = ((Long) batch.get("days")).intValue();
        double currentWeightG = avgWeight.doubleValue() + days * 4.0;
        double targetWeightG = 800; // 黄条鰤出栏目标800g
        int remainingDays = (int) ((targetWeightG - currentWeightG) / 4.0);
        if (remainingDays < 0) remainingDays = 0;

        LocalDate predictedDate = LocalDate.now().plusDays(remainingDays);

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("currentAvgWeightG", Math.round(currentWeightG));
        result.put("targetWeightG", (int) targetWeightG);
        result.put("growthRateGPerDay", 4);
        result.put("remainingDays", remainingDays);
        result.put("predictedHarvestDate", predictedDate.toString());
        result.put("ready", remainingDays == 0);
        result.put("message", remainingDays == 0 ? "已达出栏标准，建议尽快出栏" : String.format("预计还需 %d 天达到出栏体重，预计 %s 可出栏", remainingDays, predictedDate));
        return Result.success(result);
    }

    // ==================== 产品追溯码 ====================

    @Operation(summary = "生成产品追溯码", description = "出栏时生成包含批次全信息的追溯二维码内容")
    @GetMapping("/traceability/{batchId}")
    public Result<?> generateTraceability(@PathVariable Long batchId) {
        Map<String, Object> batch = jdbc.queryForMap(
                "SELECT * FROM t_breeding_batch WHERE id = ?", batchId);
        // 查检疫证
        List<Map<String, Object>> certs = jdbc.queryForList(
                "SELECT cert_no, issuer, issue_date FROM t_quarantine_cert WHERE batch_id = ? ORDER BY issue_date DESC", batchId);
        // 查用药记录
        List<Map<String, Object>> meds = jdbc.queryForList(
                "SELECT drug_name, withdrawal_end_date FROM t_medication_record WHERE batch_id = ?", batchId);

        Map<String, Object> traceInfo = new HashMap<>();
        traceInfo.put("traceCode", "TRACE-" + batch.get("batch_id") + "-" + System.currentTimeMillis() % 10000);
        traceInfo.put("batchId", batch.get("batch_id"));
        traceInfo.put("species", batch.get("species_name"));
        traceInfo.put("farmName", "辽宁省大连市智慧养殖示范基地");
        traceInfo.put("startDate", batch.get("start_date"));
        traceInfo.put("endDate", batch.get("end_date"));
        traceInfo.put("harvestWeight", batch.get("harvest_weight_kg"));
        traceInfo.put("fcr", batch.get("fcr"));
        traceInfo.put("supplier", batch.get("supplier"));
        traceInfo.put("quarantineCerts", certs);
        traceInfo.put("medications", meds);
        traceInfo.put("qrContent", "https://trace.smartfarm.com/query?code=TRACE-" + batch.get("batch_id"));
        return Result.success(traceInfo);
    }

    // ==================== 检疫证管理 ====================

    @Operation(summary = "获取检疫证列表")
    @GetMapping("/quarantine/list")
    public Result<?> listQuarantineCerts(@RequestParam(required = false) Long batchId) {
        String sql = "SELECT qc.*, bb.batch_id as batch_code, bb.species_name FROM t_quarantine_cert qc LEFT JOIN t_breeding_batch bb ON qc.batch_id = bb.id";
        if (batchId != null) sql += " WHERE qc.batch_id = " + batchId;
        sql += " ORDER BY qc.create_time DESC";
        return Result.success(jdbc.queryForList(sql));
    }

    @Data
    public static class QuarantineCertRequest {
        private Long batchId;
        private String certNo;
        private String issuer;
        private String issueDate;
        private String validUntil;
        private String certType;
    }

    @Operation(summary = "新增检疫证")
    @PostMapping("/quarantine/add")
    public Result<?> addQuarantineCert(@RequestBody QuarantineCertRequest req) {
        jdbc.update("INSERT INTO t_quarantine_cert (batch_id, cert_no, issuer, issue_date, valid_until, cert_type) VALUES (?,?,?,?,?,?)",
                req.getBatchId(), req.getCertNo(), req.getIssuer(), req.getIssueDate(), req.getValidUntil(), req.getCertType());
        return Result.success("检疫证添加成功");
    }
}
