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
import com.sambath.admincafe.report.dto.TopProductResponse;
import com.sambath.admincafe.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public byte[] exportCsv(ReportRange range) {
        ReportSummaryResponse summary = summary(range);
        ReportKpisResponse kpis = kpis(range);
        StringBuilder sb = new StringBuilder();
        sb.append("Metric,Value\n");
        sb.append("Range,").append(summary.range()).append('\n');
        sb.append("Total Revenue,").append(summary.totalRevenue()).append('\n');
        sb.append("Revenue Growth %,").append(summary.revenueGrowthPct()).append('\n');
        sb.append("Total Orders,").append(summary.totalOrders()).append('\n');
        sb.append("Orders Growth %,").append(summary.ordersGrowthPct()).append('\n');
        sb.append("Active Orders,").append(summary.activeOrders()).append('\n');
        sb.append("Top Selling Product,").append(csvSafe(summary.topSellingProduct().name())).append('\n');
        sb.append("Top Product Units,").append(summary.topSellingProduct().unitsSold()).append('\n');
        sb.append("Top Category,").append(csvSafe(summary.topCategory().name())).append('\n');
        sb.append("Top Category Share %,").append(summary.topCategory().sharePct()).append('\n');
        sb.append("Avg Order Value,").append(kpis.avgOrderValue()).append('\n');
        sb.append("AOV Growth %,").append(kpis.avgOrderValueGrowthPct()).append('\n');
        sb.append("New Customers,").append(kpis.newCustomers()).append('\n');
        sb.append("New Customers Growth %,").append(kpis.newCustomersGrowthPct()).append('\n');
        sb.append("Staff Efficiency (min),").append(kpis.staffEfficiencyMinutes()).append('\n');
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
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
