package com.sambath.admincafe.report;

import com.sambath.admincafe.order.Order;
import com.sambath.admincafe.order.OrderRepository;
import com.sambath.admincafe.order.OrderStatus;
import com.sambath.admincafe.report.dto.CategoryShareResponse;
import com.sambath.admincafe.report.dto.NamedCountResponse;
import com.sambath.admincafe.report.dto.ReportKpisResponse;
import com.sambath.admincafe.report.dto.ReportSummaryResponse;
import com.sambath.admincafe.report.dto.RevenuePointResponse;
import com.sambath.admincafe.report.dto.RevenueSeriesResponse;
import com.sambath.admincafe.report.dto.SalesLineItemResponse;
import com.sambath.admincafe.report.dto.SalesReportResponse;
import com.sambath.admincafe.report.dto.TopProductResponse;
import com.sambath.admincafe.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final List<OrderStatus> ACTIVE_STATUSES =
            List.of(OrderStatus.NEW, OrderStatus.PREPARING, OrderStatus.READY);
    private static final DateTimeFormatter DAY_OF_WEEK = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_OF_MONTH = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;

    public ReportSummaryResponse summary(ReportRange range) {
        Window current = currentWindow(range);
        Window previous = previousWindow(range);

        BigDecimal revenue = scale2(transactionRepository.sumAmountBetween(current.start, current.end));
        BigDecimal prevRevenue = scale2(transactionRepository.sumAmountBetween(previous.start, previous.end));
        long orders = orderRepository.countBetween(current.start, current.end);
        long prevOrders = orderRepository.countBetween(previous.start, previous.end);
        int active = (int) orderRepository.countByStatusIn(ACTIVE_STATUSES);

        NamedCountResponse topProduct = topSellingProduct(current.start, current.end);
        CategoryShareResponse topCategory = topCategoryShare(current.start, current.end);

        return new ReportSummaryResponse(
                range.displayName(),
                revenue,
                growthPct(revenue, prevRevenue),
                orders,
                growthPct(BigDecimal.valueOf(orders), BigDecimal.valueOf(prevOrders)),
                active,
                topProduct,
                topCategory
        );
    }

    public RevenueSeriesResponse revenueSeries(ReportRange range) {
        LocalDate today = LocalDate.now(ZONE);
        List<RevenuePointResponse> points = new ArrayList<>();

        switch (range) {
            case YEARLY -> {
                YearMonth thisMonth = YearMonth.from(today);
                for (int i = 11; i >= 0; i--) {
                    YearMonth ym = thisMonth.minusMonths(i);
                    Instant start = ym.atDay(1).atStartOfDay(ZONE).toInstant();
                    Instant end = ym.plusMonths(1).atDay(1).atStartOfDay(ZONE).toInstant();
                    BigDecimal sum = scale2(transactionRepository.sumAmountBetween(start, end));
                    points.add(new RevenuePointResponse(
                            ym.format(MONTH_LABEL),
                            ym.toString(),
                            sum,
                            false
                    ));
                }
            }
            case MONTHLY -> {
                for (int i = 29; i >= 0; i--) {
                    LocalDate d = today.minusDays(i);
                    points.add(dailyPoint(d, DAY_OF_MONTH));
                }
            }
            case DAILY, WEEKLY -> {
                for (int i = 6; i >= 0; i--) {
                    LocalDate d = today.minusDays(i);
                    points.add(dailyPoint(d, DAY_OF_WEEK));
                }
            }
        }

        return new RevenueSeriesResponse(range.displayName(), markPeak(points));
    }

    public List<TopProductResponse> topProducts(ReportRange range, int limit) {
        Window w = currentWindow(range);
        return orderRepository.findTopProducts(w.start, w.end, limit).stream()
                .map(row -> new TopProductResponse(
                        (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        scale2(asBigDecimal(row[3]))
                ))
                .toList();
    }

    public ReportKpisResponse kpis(ReportRange range) {
        Window current = currentWindow(range);
        Window previous = previousWindow(range);

        BigDecimal aov = scale2(transactionRepository.avgAmountBetween(current.start, current.end));
        BigDecimal prevAov = scale2(transactionRepository.avgAmountBetween(previous.start, previous.end));
        long newCust = orderRepository.countNewCustomersBetween(current.start, current.end);
        long prevNewCust = orderRepository.countNewCustomersBetween(previous.start, previous.end);

        CategoryShareResponse topCat = topCategoryShare(current.start, current.end);
        BigDecimal staffEff = staffEfficiencyMinutes(current.start, current.end);

        return new ReportKpisResponse(
                range.displayName(),
                aov,
                growthPct(aov, prevAov),
                newCust,
                growthPct(BigDecimal.valueOf(newCust), BigDecimal.valueOf(prevNewCust)),
                topCat.name(),
                topCat.sharePct(),
                staffEff
        );
    }

    public SalesReportResponse salesReport(ReportRange range, String salesPerson) {
        Window w = currentWindow(range);
        String filter = (salesPerson == null || salesPerson.isBlank()) ? null : salesPerson;

        List<SalesLineItemResponse> items = orderRepository
                .findSalesLineItems(w.start, w.end, filter)
                .stream()
                .map(row -> new SalesLineItemResponse(
                        row[0] == null ? "-" : "P-" + ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        scale2(asBigDecimal(row[5])),
                        ((Number) row[3]).longValue(),
                        scale2(asBigDecimal(row[4]))
                ))
                .toList();

        BigDecimal salesTotal = items.stream()
                .map(SalesLineItemResponse::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new SalesReportResponse(
                range.displayName(),
                LocalDate.ofInstant(w.start, ZONE).toString(),
                LocalDate.ofInstant(w.end, ZONE).toString(),
                filter == null ? "All Staff" : filter,
                salesTotal,
                items
        );
    }

    public byte[] exportXlsx(ReportRange range) {
        ReportSummaryResponse summary = summary(range);
        ReportKpisResponse kpis = kpis(range);
        RevenueSeriesResponse series = revenueSeries(range);
        List<TopProductResponse> top = topProducts(range, 10);

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle header = headerStyle(wb);
            CellStyle money = moneyStyle(wb);
            CellStyle percent = percentStyle(wb);

            writeSummarySheet(wb.createSheet("Summary"), summary, header, money, percent);
            writeKpisSheet(wb.createSheet("KPIs"), kpis, header, money, percent);
            writeRevenueSheet(wb.createSheet("Revenue Series"), series, header, money);
            writeTopProductsSheet(wb.createSheet("Top Products"), top, header, money);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build xlsx report", e);
        }
    }

    private void writeSummarySheet(Sheet s, ReportSummaryResponse r, CellStyle header, CellStyle money, CellStyle percent) {
        writeHeaderRow(s, 0, header, "Metric", "Value");
        int i = 1;
        writeKv(s, i++, "Range", r.range());
        writeKvMoney(s, i++, "Total Revenue", r.totalRevenue(), money);
        writeKvPercent(s, i++, "Revenue Growth", r.revenueGrowthPct(), percent);
        writeKvNumber(s, i++, "Total Orders", r.totalOrders());
        writeKvPercent(s, i++, "Orders Growth", r.ordersGrowthPct(), percent);
        writeKvNumber(s, i++, "Active Orders", r.activeOrders());
        writeKv(s, i++, "Top Selling Product", r.topSellingProduct().name());
        writeKvNumber(s, i++, "Top Product Units", r.topSellingProduct().unitsSold());
        writeKv(s, i++, "Top Category", r.topCategory().name());
        writeKvPercent(s, i++, "Top Category Share", r.topCategory().sharePct(), percent);
        autoSize(s, 2);
    }

    private void writeKpisSheet(Sheet s, ReportKpisResponse r, CellStyle header, CellStyle money, CellStyle percent) {
        writeHeaderRow(s, 0, header, "Metric", "Value");
        int i = 1;
        writeKv(s, i++, "Range", r.range());
        writeKvMoney(s, i++, "Avg Order Value", r.avgOrderValue(), money);
        writeKvPercent(s, i++, "AOV Growth", r.avgOrderValueGrowthPct(), percent);
        writeKvNumber(s, i++, "New Customers", r.newCustomers());
        writeKvPercent(s, i++, "New Customers Growth", r.newCustomersGrowthPct(), percent);
        writeKv(s, i++, "Top Category", r.topCategory());
        writeKvPercent(s, i++, "Top Category Share", r.topCategorySharePct(), percent);
        writeKvNumber(s, i++, "Staff Efficiency (min)", r.staffEfficiencyMinutes());
        autoSize(s, 2);
    }

    private void writeRevenueSheet(Sheet s, RevenueSeriesResponse r, CellStyle header, CellStyle money) {
        writeHeaderRow(s, 0, header, "Label", "Date", "Revenue", "Peak");
        int i = 1;
        for (RevenuePointResponse p : r.points()) {
            Row row = s.createRow(i++);
            row.createCell(0).setCellValue(p.label());
            row.createCell(1).setCellValue(p.date());
            Cell rev = row.createCell(2);
            rev.setCellValue(p.revenue().doubleValue());
            rev.setCellStyle(money);
            row.createCell(3).setCellValue(p.isPeak());
        }
        autoSize(s, 4);
    }

    private void writeTopProductsSheet(Sheet s, List<TopProductResponse> products, CellStyle header, CellStyle money) {
        writeHeaderRow(s, 0, header, "Product", "Category", "Units Sold", "Revenue");
        int i = 1;
        for (TopProductResponse p : products) {
            Row row = s.createRow(i++);
            row.createCell(0).setCellValue(p.productName());
            row.createCell(1).setCellValue(p.category());
            row.createCell(2).setCellValue(p.unitsSold());
            Cell rev = row.createCell(3);
            rev.setCellValue(p.revenue().doubleValue());
            rev.setCellStyle(money);
        }
        autoSize(s, 4);
    }

    private static void writeHeaderRow(Sheet s, int rowIdx, CellStyle style, String... labels) {
        Row row = s.createRow(rowIdx);
        for (int i = 0; i < labels.length; i++) {
            Cell c = row.createCell(i);
            c.setCellValue(labels[i]);
            c.setCellStyle(style);
        }
    }

    private static void writeKv(Sheet s, int rowIdx, String key, String value) {
        Row row = s.createRow(rowIdx);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value == null ? "" : value);
    }

    private static void writeKvNumber(Sheet s, int rowIdx, String key, Number value) {
        Row row = s.createRow(rowIdx);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value == null ? 0 : value.doubleValue());
    }

    private static void writeKvMoney(Sheet s, int rowIdx, String key, BigDecimal value, CellStyle money) {
        Row row = s.createRow(rowIdx);
        row.createCell(0).setCellValue(key);
        Cell v = row.createCell(1);
        v.setCellValue(value == null ? 0 : value.doubleValue());
        v.setCellStyle(money);
    }

    private static void writeKvPercent(Sheet s, int rowIdx, String key, BigDecimal value, CellStyle percent) {
        Row row = s.createRow(rowIdx);
        row.createCell(0).setCellValue(key);
        Cell v = row.createCell(1);
        v.setCellValue(value == null ? 0 : value.doubleValue() / 100.0);
        v.setCellStyle(percent);
    }

    private static CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private static CellStyle moneyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    private static CellStyle percentStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat("0.0%"));
        return style;
    }

    private static void autoSize(Sheet s, int cols) {
        for (int i = 0; i < cols; i++) {
            s.autoSizeColumn(i);
        }
    }

    // CSV export of the Daily Sales Report (tax-free), mirroring the Excel
    // template: a small header block, the line-item table, then the sales total.
    public byte[] exportCsv(ReportRange range) {
        SalesReportResponse report = salesReport(range, null);
        StringBuilder sb = new StringBuilder();
        sb.append("Daily Sales Report\n");
        sb.append("Range,").append(csvSafe(report.range())).append('\n');
        sb.append("Period,").append(report.periodStart()).append(" to ").append(report.periodEnd()).append('\n');
        sb.append("Sales Person,").append(csvSafe(report.salesPerson())).append('\n');
        sb.append('\n');
        sb.append("ITEM NO,ITEM NAME,ITEM DESCRIPTION,PRICE,QTY,TOTAL\n");
        for (SalesLineItemResponse item : report.lineItems()) {
            sb.append(csvSafe(item.itemNo())).append(',')
              .append(csvSafe(item.itemName())).append(',')
              .append(csvSafe(item.itemDescription())).append(',')
              .append(item.price()).append(',')
              .append(item.quantity()).append(',')
              .append(item.total()).append('\n');
        }
        sb.append('\n');
        sb.append("SALES TOTAL,,,,,").append(report.salesTotal()).append('\n');
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // Professionally-styled single-sheet XLSX of the Daily Sales Report: a brand
    // title banner, a meta block (range / period / sales person), a coloured
    // table header with bordered line items, and a highlighted sales-total row.
    public byte[] exportSalesReportXlsx(ReportRange range) {
        SalesReportResponse report = salesReport(range, null);

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet s = wb.createSheet("Daily Sales Report");
            int[] widths = {14, 32, 24, 13, 9, 15};
            for (int c = 0; c < widths.length; c++) {
                s.setColumnWidth(c, widths[c] * 256);
            }

            CellStyle titleStyle = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle labelStyle = wb.createCellStyle();
            Font labelFont = wb.createFont();
            labelFont.setBold(true);
            labelStyle.setFont(labelFont);

            CellStyle headStyle = wb.createCellStyle();
            Font headFont = wb.createFont();
            headFont.setBold(true);
            headFont.setColor(IndexedColors.WHITE.getIndex());
            headStyle.setFont(headFont);
            headStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            headStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headStyle.setAlignment(HorizontalAlignment.CENTER);
            headStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyThinBorder(headStyle);

            CellStyle textStyle = wb.createCellStyle();
            textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyThinBorder(textStyle);

            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(wb.createDataFormat().getFormat("$#,##0.00"));
            moneyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyThinBorder(moneyStyle);

            CellStyle qtyStyle = wb.createCellStyle();
            qtyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            qtyStyle.setAlignment(HorizontalAlignment.CENTER);
            qtyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyThinBorder(qtyStyle);

            Font totalFont = wb.createFont();
            totalFont.setBold(true);
            CellStyle totalLabelStyle = wb.createCellStyle();
            totalLabelStyle.setFont(totalFont);
            totalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalLabelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            applyThinBorder(totalLabelStyle);

            CellStyle totalMoneyStyle = wb.createCellStyle();
            totalMoneyStyle.setFont(totalFont);
            totalMoneyStyle.setDataFormat(wb.createDataFormat().getFormat("$#,##0.00"));
            totalMoneyStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalMoneyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            applyThinBorder(totalMoneyStyle);

            int r = 0;
            Row titleRow = s.createRow(r);
            titleRow.setHeightInPoints(26);
            for (int c = 0; c <= 5; c++) {
                titleRow.createCell(c).setCellStyle(titleStyle);
            }
            titleRow.getCell(0).setCellValue("DAILY SALES REPORT");
            s.addMergedRegion(new CellRangeAddress(r, r, 0, 5));
            r += 2; // title + spacer

            r = metaRow(s, r, labelStyle, "Range", report.range());
            r = metaRow(s, r, labelStyle, "Period", report.periodStart() + " to " + report.periodEnd());
            r = metaRow(s, r, labelStyle, "Sales Person", report.salesPerson());
            r++; // spacer

            int headerRow = r;
            Row head = s.createRow(r++);
            String[] cols = {"ITEM NO", "ITEM NAME", "ITEM DESCRIPTION", "PRICE", "QTY", "TOTAL"};
            for (int c = 0; c < cols.length; c++) {
                Cell cell = head.createCell(c);
                cell.setCellValue(cols[c]);
                cell.setCellStyle(headStyle);
            }

            for (SalesLineItemResponse item : report.lineItems()) {
                Row row = s.createRow(r++);
                cell(row, 0, item.itemNo(), textStyle);
                cell(row, 1, item.itemName(), textStyle);
                cell(row, 2, item.itemDescription(), textStyle);
                Cell price = row.createCell(3);
                price.setCellValue(item.price() == null ? 0 : item.price().doubleValue());
                price.setCellStyle(moneyStyle);
                Cell qty = row.createCell(4);
                qty.setCellValue(item.quantity());
                qty.setCellStyle(qtyStyle);
                Cell total = row.createCell(5);
                total.setCellValue(item.total() == null ? 0 : item.total().doubleValue());
                total.setCellStyle(moneyStyle);
            }

            Row totalRow = s.createRow(r);
            for (int c = 0; c <= 4; c++) {
                totalRow.createCell(c).setCellStyle(totalLabelStyle);
            }
            totalRow.getCell(0).setCellValue("SALES TOTAL");
            s.addMergedRegion(new CellRangeAddress(r, r, 0, 4));
            Cell totalVal = totalRow.createCell(5);
            totalVal.setCellValue(report.salesTotal() == null ? 0 : report.salesTotal().doubleValue());
            totalVal.setCellStyle(totalMoneyStyle);

            s.createFreezePane(0, headerRow + 1);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to build sales report xlsx", e);
        }
    }

    private static void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? "" : value);
        c.setCellStyle(style);
    }

    private static int metaRow(Sheet s, int rowIdx, CellStyle labelStyle, String label, String value) {
        Row row = s.createRow(rowIdx);
        Cell l = row.createCell(0);
        l.setCellValue(label);
        l.setCellStyle(labelStyle);
        row.createCell(1).setCellValue(value == null ? "" : value);
        return rowIdx + 1;
    }

    private static void applyThinBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private RevenuePointResponse dailyPoint(LocalDate d, DateTimeFormatter labelFmt) {
        Instant start = d.atStartOfDay(ZONE).toInstant();
        Instant end = d.plusDays(1).atStartOfDay(ZONE).toInstant();
        BigDecimal sum = scale2(transactionRepository.sumAmountBetween(start, end));
        return new RevenuePointResponse(d.format(labelFmt), d.toString(), sum, false);
    }

    private NamedCountResponse topSellingProduct(Instant start, Instant end) {
        List<Object[]> rows = orderRepository.findTopProducts(start, end, 1);
        if (rows.isEmpty()) {
            return new NamedCountResponse("—", 0);
        }
        Object[] row = rows.get(0);
        return new NamedCountResponse((String) row[0], ((Number) row[2]).longValue());
    }

    private CategoryShareResponse topCategoryShare(Instant start, Instant end) {
        List<Object[]> rows = orderRepository.findCategoryAggregates(start, end);
        if (rows.isEmpty()) {
            return new CategoryShareResponse("—", BigDecimal.ZERO);
        }
        BigDecimal total = rows.stream()
                .map(r -> asBigDecimal(r[2]))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Object[] top = rows.get(0);
        BigDecimal topRevenue = asBigDecimal(top[2]);
        BigDecimal share = total.signum() == 0
                ? BigDecimal.ZERO
                : topRevenue.multiply(BigDecimal.valueOf(100))
                        .divide(total, 1, RoundingMode.HALF_UP);
        return new CategoryShareResponse((String) top[0], share);
    }

    private BigDecimal staffEfficiencyMinutes(Instant start, Instant end) {
        List<Order> completed = orderRepository.findCompletedBetween(start, end);
        if (completed.isEmpty()) {
            return BigDecimal.ZERO;
        }
        double avgSecs = completed.stream()
                .mapToLong(o -> Duration.between(o.getCreatedAt(), o.getStatusUpdatedAt()).getSeconds())
                .average()
                .orElse(0);
        return BigDecimal.valueOf(avgSecs / 60.0).setScale(1, RoundingMode.HALF_UP);
    }

    private List<RevenuePointResponse> markPeak(List<RevenuePointResponse> points) {
        BigDecimal max = points.stream()
                .map(RevenuePointResponse::revenue)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        if (max.signum() == 0) {
            return points;
        }
        return points.stream()
                .map(p -> p.revenue().compareTo(max) == 0
                        ? new RevenuePointResponse(p.label(), p.date(), p.revenue(), true)
                        : p)
                .toList();
    }

    private Window currentWindow(ReportRange range) {
        Instant end = Instant.now();
        return new Window(end.minus(durationOf(range)), end);
    }

    private Window previousWindow(ReportRange range) {
        Duration d = durationOf(range);
        Instant end = Instant.now().minus(d);
        return new Window(end.minus(d), end);
    }

    private Duration durationOf(ReportRange range) {
        return switch (range) {
            case DAILY -> Duration.ofDays(1);
            case WEEKLY -> Duration.ofDays(7);
            case MONTHLY -> Duration.ofDays(30);
            case YEARLY -> Duration.ofDays(365);
        };
    }

    private static BigDecimal growthPct(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return current != null && current.signum() > 0
                    ? BigDecimal.valueOf(100).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale2(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal asBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(value.toString());
    }

    private static String csvSafe(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private record Window(Instant start, Instant end) {
    }
}
