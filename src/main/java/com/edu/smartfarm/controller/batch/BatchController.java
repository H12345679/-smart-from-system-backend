package com.edu.smartfarm.controller.batch;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.smartfarm.common.Result;
import com.edu.smartfarm.entity.BreedingBatch;
import com.edu.smartfarm.entity.FeedRecord;
import com.edu.smartfarm.service.BatchService;
import com.edu.smartfarm.service.ReportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "养殖台账模块", description = "批次管理、投喂、死亡、用药记录")
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;
    private final ReportExportService reportExportService;

    @Data
    public static class CreateBatchRequest {
        private Long tankId;
        private String speciesName;
        private Integer initialCount;
        private BigDecimal initialAvgWeight;
        private String supplier;
        private String quarantineCert;
        private LocalDate startDate;
    }

    @Data
    public static class FeedRequest {
        private Long batchId;
        private BigDecimal feedWeightKg;
        private String feedType;
        private String remark;
    }

    @Data
    public static class MortalityRequest {
        private Long batchId;
        private Integer deathCount;
        private String deathCause;
        private String remark;
    }

    @Data
    public static class MedicationRequest {
        private Long batchId;
        private String drugName;
        private String dosage;
        private Integer withdrawalDays;
        private String remark;
    }

    @Operation(summary = "创建养殖批次")
    @PostMapping("/create")
    public Result<?> createBatch(@RequestBody CreateBatchRequest request) {
        BreedingBatch batch = batchService.createBatch(request.getTankId(), request.getSpeciesName(),
                request.getInitialCount(), request.getInitialAvgWeight(), request.getSupplier(),
                request.getQuarantineCert(), request.getStartDate());
        return Result.success(batch);
    }

    @Operation(summary = "查询批次列表")
    @GetMapping("/list")
    public Result<?> listBatches(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size,
                                  @RequestParam(required = false) String status) {
        Page<BreedingBatch> result = batchService.listBatches(page, size, status);
        return Result.success(result);
    }

    @Operation(summary = "查询批次详情")
    @GetMapping("/{id}")
    public Result<?> getBatchDetail(@PathVariable Long id) {
        return Result.success(batchService.getDetail(id));
    }

    @Operation(summary = "关闭批次(出栏)")
    @PutMapping("/{id}/harvest")
    public Result<?> harvestBatch(@PathVariable Long id, @RequestParam BigDecimal harvestWeightKg) {
        batchService.harvest(id, harvestWeightKg);
        return Result.success("出栏成功");
    }

    @Operation(summary = "录入投喂记录")
    @PostMapping("/feed")
    public Result<?> addFeedRecord(@RequestBody FeedRequest request) {
        batchService.addFeed(request.getBatchId(), request.getFeedWeightKg(), request.getFeedType(), request.getRemark());
        return Result.success("投喂记录已保存");
    }

    @Operation(summary = "查询投喂历史")
    @GetMapping("/{batchId}/feed-records")
    public Result<?> listFeedRecords(@PathVariable Long batchId) {
        List<FeedRecord> records = batchService.listFeedRecords(batchId);
        return Result.success(records);
    }

    @Operation(summary = "录入死亡捞取记录")
    @PostMapping("/mortality")
    public Result<?> addMortalityRecord(@RequestBody MortalityRequest request) {
        batchService.addMortality(request.getBatchId(), request.getDeathCount(), request.getDeathCause(), request.getRemark());
        return Result.success("死亡记录已保存，存货已扣减");
    }

    @Operation(summary = "录入用药记录")
    @PostMapping("/medication")
    public Result<?> addMedicationRecord(@RequestBody MedicationRequest request) {
        batchService.addMedication(request.getBatchId(), request.getDrugName(), request.getDosage(), request.getWithdrawalDays(), request.getRemark());
        return Result.success("用药记录已保存，休药期已启动");
    }

    @Operation(summary = "检查休药期状态")
    @GetMapping("/{batchId}/withdrawal-status")
    public Result<?> checkWithdrawalStatus(@PathVariable Long batchId) {
        Map<String, Object> result = batchService.checkWithdrawalStatus(batchId);
        return Result.success(result);
    }

    @Operation(summary = "查询死亡记录")
    @GetMapping("/{batchId}/mortality-records")
    public Result<?> listMortalityRecords(@PathVariable Long batchId) {
        return Result.success(batchService.listMortalityRecords(batchId));
    }

    @Operation(summary = "查询用药记录")
    @GetMapping("/{batchId}/medication-records")
    public Result<?> listMedicationRecords(@PathVariable Long batchId) {
        return Result.success(batchService.listMedicationRecords(batchId));
    }

    @Operation(summary = "导出批次全生命周期报表(Excel)")
    @GetMapping("/{batchId}/export")
    public void exportBatchReport(@PathVariable Long batchId, HttpServletResponse response) {
        try {
            reportExportService.exportBatchReport(batchId, response);
        } catch (Exception e) {
            throw new com.edu.smartfarm.common.BusinessException("导出失败: " + e.getMessage());
        }
    }
}
