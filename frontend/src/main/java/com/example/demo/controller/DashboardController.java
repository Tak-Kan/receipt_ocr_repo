package com.example.demo.controller;

import com.example.demo.dto.CategorySummaryDto;
import com.example.demo.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AccountRepository accountRepository; // ※本来はService層を挟むのがベストです

    @GetMapping
    public String index(
            @RequestParam(value = "month", required = false) 
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth targetMonth, 
            Authentication auth, Model model) {
        
        String userName = (auth != null) ? auth.getName() : "anonymous";
        // パラメータがない場合は現在の月を設定
        if (targetMonth == null) {
            targetMonth = YearMonth.now();
        }

        // DBから集計データを取得
        List<CategorySummaryDto> summaryList = accountRepository.findMonthlySummaryByCategory(
                userName, targetMonth.getYear(), targetMonth.getMonthValue());

        // 全体合計を計算
        long totalExpense = summaryList.stream()
                .mapToLong(CategorySummaryDto::getTotalAmount)
                .sum();

        // Chart.js 用にラベル配列とデータ配列を生成
        List<String> chartLabels = summaryList.stream()
                .map(CategorySummaryDto::getCategoryName)
                .collect(Collectors.toList());
        List<Long> chartData = summaryList.stream()
                .map(CategorySummaryDto::getTotalAmount)
                .collect(Collectors.toList());

        model.addAttribute("targetMonth", targetMonth);
        model.addAttribute("summaryList", summaryList);
        model.addAttribute("totalExpense", totalExpense);
        
        // グラフ用データ
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);

        return "dashboard/index";
    }
}