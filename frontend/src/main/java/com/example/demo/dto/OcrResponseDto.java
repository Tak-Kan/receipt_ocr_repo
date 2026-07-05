package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class OcrResponseDto {
    @JsonProperty("merchant_name")
    private String merchantName;
    
    private String date;
    private String time;
    
    @JsonProperty("total_amount")
    private String totalAmount; // Python側で .content (文字列) を取得しているためStringで受ける
    
    private List<OcrItemDto> items;

    @Data
    public static class OcrItemDto {
        private String name;
        private String price;
    }
}