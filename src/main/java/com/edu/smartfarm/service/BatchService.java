package com.edu.smartfarm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.smartfarm.common.BusinessException;
import com.edu.smartfarm.entity.*;
import com.edu.smartfarm.mapper.*;
import com.edu.smartfarm.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final BreedingBatchMapper batchMapper;
    private final FeedRecordMapper feedRecordMapper;
    private final MortalityRecordMapper mortalityRecordMapper;
    private final MedicationRecordMapper medicationRecordMapper;

    public BreedingBatch createBatch(Long tankId, String speciesName, Integer initialCount,
                                     BigDecimal initialAvgWeight, String supplier, String quarantineCert, LocalDate startDate) {
        String batchId = "B-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "-" + System.currentTimeMillis() % 1000;
        BreedingBatch batch = new BreedingBatch();
        batch.setBatchId(batchId);
        batch.setTankId(tankId);
        batch.setSpeciesName(speciesName);
        batch.setInitialCount(initialCount);
        batch.setCurrentCount(initialCount);
        batch.setInitialAvgWeight(initialAvgWeight);
        batch.setTotalFeedKg(BigDecimal.ZERO);
        batch.setSupplier(supplier);
        batch.setQuarantineCert(quarantineCert);
        batch.setStatus("ACTIVE");
        batch.setStartDate(startDate != null ? startDate : LocalDate.now());
        batchMapper.insert(batch);
        return batch;
    }

    public Page<BreedingBatch> listBatches(Integer page, Integer size, String status) {
        LambdaQueryWrapper<BreedingBatch> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(BreedingBatch::getStatus, status);
        }
        wrapper.orderByDesc(BreedingBatch::getCreateTime);
        return batchMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public BreedingBatch getDetail(Long id) {
        BreedingBatch batch = batchMapper.selectById(id);
        if (batch == null) throw new BusinessException("批次不存在");
        return batch;
    }

    // ========== 投喂 ==========
    public void addFeed(Long batchId, BigDecimal feedWeightKg, String feedType, String remark) {
        BreedingBatch batch = batchMapper.selectById(batchId);
        if (batch == null) throw new BusinessException("批次不存在");

        // 写入明细表
        FeedRecord record = new FeedRecord();
        record.setBatchId(batchId);
        record.setFeedWeightKg(feedWeightKg);
        record.setFeedType(feedType);
        record.setOperatorId(UserContext.getUserId());
        record.setFeedTime(LocalDateTime.now());
        record.setRemark(remark);
        feedRecordMapper.insert(record);

        // 更新聚合字段
        batch.setTotalFeedKg(batch.getTotalFeedKg().add(feedWeightKg));
        recalcFcr(batch);
        batchMapper.updateById(batch);
    }

    public List<FeedRecord> listFeedRecords(Long batchId) {
        return feedRecordMapper.selectList(
                new LambdaQueryWrapper<FeedRecord>()
                        .eq(FeedRecord::getBatchId, batchId)
                        .orderByDesc(FeedRecord::getFeedTime));
    }

    // ========== 死亡 ==========
    public void addMortality(Long batchId, Integer deathCount, String deathCause, String remark) {
        BreedingBatch batch = batchMapper.selectById(batchId);
        if (batch == null) throw new BusinessException("批次不存在");
        if (deathCount > batch.getCurrentCount()) {
            throw new BusinessException("死亡数量不能大于当前存活尾数");
        }

        // 写入明细表
        MortalityRecord record = new MortalityRecord();
        record.setBatchId(batchId);
        record.setDeathCount(deathCount);
        record.setDeathCause(deathCause);
        record.setOperatorId(UserContext.getUserId());
        record.setRecordTime(LocalDateTime.now());
        record.setRemark(remark);
        mortalityRecordMapper.insert(record);

        // 更新聚合
        batch.setCurrentCount(batch.getCurrentCount() - deathCount);
        batchMapper.updateById(batch);
    }

    // ========== 用药 ==========
    public void addMedication(Long batchId, String drugName, String dosage, Integer withdrawalDays, String remark) {
        BreedingBatch batch = batchMapper.selectById(batchId);
        if (batch == null) throw new BusinessException("批次不存在");

        MedicationRecord record = new MedicationRecord();
        record.setBatchId(batchId);
        record.setDrugName(drugName);
        record.setDosage(dosage);
        record.setWithdrawalDays(withdrawalDays);
        record.setWithdrawalEndDate(LocalDate.now().plusDays(withdrawalDays));
        record.setOperatorId(UserContext.getUserId());
        record.setMedicationTime(LocalDateTime.now());
        record.setRemark(remark);
        medicationRecordMapper.insert(record);
    }

    // ========== 休药期检查 ==========
    public Map<String, Object> checkWithdrawalStatus(Long batchId) {
        // 查询该批次最近的用药记录中是否有未过期的休药期
        List<MedicationRecord> records = medicationRecordMapper.selectList(
                new LambdaQueryWrapper<MedicationRecord>()
                        .eq(MedicationRecord::getBatchId, batchId)
                        .ge(MedicationRecord::getWithdrawalEndDate, LocalDate.now())
                        .orderByDesc(MedicationRecord::getWithdrawalEndDate));

        Map<String, Object> result = new HashMap<>();
        if (records.isEmpty()) {
            result.put("inWithdrawal", false);
            result.put("canHarvest", true);
            result.put("message", "无休药期限制，可正常出栏");
        } else {
            MedicationRecord latest = records.get(0);
            result.put("inWithdrawal", true);
            result.put("canHarvest", false);
            result.put("withdrawalEndDate", latest.getWithdrawalEndDate().toString());
            result.put("drugName", latest.getDrugName());
            result.put("message", "休药期未结束，禁止出栏。结束日期: " + latest.getWithdrawalEndDate());
        }
        return result;
    }

    // ========== 出栏 ==========
    public void harvest(Long id, BigDecimal harvestWeightKg) {
        BreedingBatch batch = batchMapper.selectById(id);
        if (batch == null) throw new BusinessException("批次不存在");
        if (!"ACTIVE".equals(batch.getStatus())) throw new BusinessException("批次已结束");

        // 检查休药期
        Map<String, Object> withdrawal = checkWithdrawalStatus(id);
        if ((Boolean) withdrawal.get("inWithdrawal")) {
            throw new BusinessException("休药期未结束，禁止出栏: " + withdrawal.get("message"));
        }

        batch.setHarvestWeightKg(harvestWeightKg);
        batch.setStatus("HARVESTED");
        batch.setEndDate(LocalDate.now());
        recalcFcr(batch);
        batchMapper.updateById(batch);
    }

    private void recalcFcr(BreedingBatch batch) {
        if (batch.getHarvestWeightKg() != null && batch.getHarvestWeightKg().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal initialWeight = batch.getInitialAvgWeight() != null
                    ? batch.getInitialAvgWeight().multiply(BigDecimal.valueOf(batch.getInitialCount())).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal weightGain = batch.getHarvestWeightKg().subtract(initialWeight);
            if (weightGain.compareTo(BigDecimal.ZERO) > 0) {
                batch.setFcr(batch.getTotalFeedKg().divide(weightGain, 3, RoundingMode.HALF_UP));
            }
        }
    }

    // ========== 查询明细 ==========
    public List<MortalityRecord> listMortalityRecords(Long batchId) {
        return mortalityRecordMapper.selectList(
                new LambdaQueryWrapper<MortalityRecord>()
                        .eq(MortalityRecord::getBatchId, batchId)
                        .orderByDesc(MortalityRecord::getRecordTime));
    }

    public List<MedicationRecord> listMedicationRecords(Long batchId) {
        return medicationRecordMapper.selectList(
                new LambdaQueryWrapper<MedicationRecord>()
                        .eq(MedicationRecord::getBatchId, batchId)
                        .orderByDesc(MedicationRecord::getMedicationTime));
    }
}
