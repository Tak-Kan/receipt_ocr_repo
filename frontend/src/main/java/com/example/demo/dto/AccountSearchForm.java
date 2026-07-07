package com.example.demo.dto;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import lombok.Data;

@Data
public class AccountSearchForm {
    
    // HTMLの <input type="date"> から値を受け取るためのフォーマット指定
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    // 店舗名やメモなどを対象としたキーワード
    private String keyword;
}