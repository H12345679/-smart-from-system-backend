package com.edu.smartfarm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.smartfarm.entity.*;
import com.edu.smartfarm.mapper.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 报表导出服务 — 生成批次全生命周期 Excel
 */
@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final BreedingBatchMapper batchMapper;
    private final FeedRecordMapper feedRecordMapper;
    private final MortalityRecordMapper mortalityRecordMapper;
    private final MedicationRecordMapper medicationRecordMapper;

    public void exportBatchReport(Long batchId, HttpServletResponse response) throws IOException {
        BreedingBatch batch = batchMapper.selectById(batchId);
        if (batch == null) throw new RuntimeException("批次不存在");

        List<FeedRecord> feedRecords = feedRecordMapper.selectList(
                new LambdaQueryWrapper<FeedRecord>().eq(FeedRecord::getBatchId, batchId).orderByAsc(FeedRecord::getFeedTime));
        List<MortalityRecord> mortalityRecords = mortalityRecordMapper.selectList(
                new LambdaQueryWrapper<MortalityRecord>().eq(MortalityRecord::getBatchId, batchId).orderByAsc(MortalityRecord::getRecordTime));
        List<MedicationRecord> medicationRecords = medicationRecordMapper.selectList(
                new LambdaQueryWrapper<MedicationRecord>().eq(MedicationRecord::getBatchId, batchId).orderByAsc(MedicationRecord::getMedicationTime));

        Workbook workbook = new XSSFWorkbook();

        // ===== Sheet 1: 批次概览 =====
        Sheet overviewSheet = workbook.createSheet("批次概览");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);

        Row titleRow = overviewSheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("养殖批次全生命周期报表");
        titleCell.setCellStyle(titleStyle);
        overviewSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        String[][] overviewData = {
                {"批次号", batch.getBatchId()},
                {"品种", batch.getSpeciesName()},
                {"水池ID", String.valueOf(batch.getTankId())},
                {"入池日期", batch.getStartDate().toString()},
                {"出栏日期", batch.getEndDate() != null ? batch.getEndDate().toString() : "进行中"},
                {"初始尾数", String.valueOf(batch.getInitialCount())},
                {"当前尾数", String.valueOf(batch.getCurrentCount())},
                {"累计饲料(kg)", batch.getTotalFeedKg().toString()},
                {"出栏重量(kg)", batch.getHarvestWeightKg() != null ? batch.getHarvestWeightKg().toString() : "-"},
                {"FCR", batch.getFcr() != null ? batch.getFcr().toString() : "-"},
                {"状态", batch.getStatus()},
                {"供应商", batch.getSupplier() != null ? batch.getSupplier() : "-"},
                {"检疫证号", batch.getQuarantineCert() != null ? batch.getQuarantineCert() : "-"},
        };

        for (int i = 0; i < overviewData.length; i++) {
            Row row = overviewSheet.createRow(i + 2);
            Cell keyCell = row.createCell(0);
            keyCell.setCellValue(overviewData[i][0]);
            keyCell.setCellStyle(headerStyle);
            row.createCell(1).setCellValue(overviewData[i][1]);
        }
        overviewSheet.setColumnWidth(0, 5000);
        overviewSheet.setColumnWidth(1, 8000);

        // ===== Sheet 2: 投喂记录 =====
        Sheet feedSheet = workbook.createSheet("投喂记录");
        Row feedHeader = feedSheet.createRow(0);
        String[] feedHeaders = {"序号", "投喂时间", "饲料重量(kg)", "饲料类型", "备注"};
        for (int i = 0; i < feedHeaders.length; i++) {
            Cell cell = feedHeader.createCell(i);
            cell.setCellValue(feedHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < feedRecords.size(); i++) {
            FeedRecord record = feedRecords.get(i);
            Row row = feedSheet.createRow(i + 1);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(record.getFeedTime() != null ? record.getFeedTime().toString() : "");
            row.createCell(2).setCellValue(record.getFeedWeightKg().doubleValue());
            row.createCell(3).setCellValue(record.getFeedType() != null ? record.getFeedType() : "");
            row.createCell(4).setCellValue(record.getRemark() != null ? record.getRemark() : "");
        }

        // ===== Sheet 3: 死亡记录 =====
        Sheet mortalitySheet = workbook.createSheet("死亡记录");
        Row mortalityHeader = mortalitySheet.createRow(0);
        String[] mortalityHeaders = {"序号", "记录时间", "死亡数量", "死因分类", "备注"};
        for (int i = 0; i < mortalityHeaders.length; i++) {
            Cell cell = mortalityHeader.createCell(i);
            cell.setCellValue(mortalityHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < mortalityRecords.size(); i++) {
            MortalityRecord record = mortalityRecords.get(i);
            Row row = mortalitySheet.createRow(i + 1);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(record.getRecordTime() != null ? record.getRecordTime().toString() : "");
            row.createCell(2).setCellValue(record.getDeathCount());
            row.createCell(3).setCellValue(formatDeathCause(record.getDeathCause()));
            row.createCell(4).setCellValue(record.getRemark() != null ? record.getRemark() : "");
        }

        // ===== Sheet 4: 用药记录 =====
        Sheet medSheet = workbook.createSheet("用药记录");
        Row medHeader = medSheet.createRow(0);
        String[] medHeaders = {"序号", "用药时间", "药物名称", "用量", "休药期(天)", "休药结束日期"};
        for (int i = 0; i < medHeaders.length; i++) {
            Cell cell = medHeader.createCell(i);
            cell.setCellValue(medHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < medicationRecords.size(); i++) {
            MedicationRecord record = medicationRecords.get(i);
            Row row = medSheet.createRow(i + 1);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(record.getMedicationTime() != null ? record.getMedicationTime().toString() : "");
            row.createCell(2).setCellValue(record.getDrugName());
            row.createCell(3).setCellValue(record.getDosage() != null ? record.getDosage() : "");
            row.createCell(4).setCellValue(record.getWithdrawalDays());
            row.createCell(5).setCellValue(record.getWithdrawalEndDate() != null ? record.getWithdrawalEndDate().toString() : "");
        }

        // 输出到响应流
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=batch_report_" + batch.getBatchId() + ".xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    private String formatDeathCause(String cause) {
        if (cause == null) return "未知";
        return switch (cause) {
            case "HYPOXIA" -> "缺氧";
            case "MECHANICAL" -> "机械损伤";
            case "DISEASE" -> "病害";
            case "OTHER" -> "其他";
            default -> cause;
        };
    }
}
