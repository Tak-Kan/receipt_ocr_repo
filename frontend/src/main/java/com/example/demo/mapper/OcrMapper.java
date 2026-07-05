package com.example.demo.mapper;

import com.example.demo.dto.AccountForm;
import com.example.demo.dto.AccountDetailForm;
import com.example.demo.dto.OcrResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OcrMapper {

    // 1. 親データ（OcrResponseDto -> AccountForm）の変換定義
    @Mapping(target = "storeName", source = "merchantName")
    @Mapping(target = "details", source = "items")
    // ★ポイント1: 独自メソッド combineDateTime を呼び出して購入日時をセット
    @Mapping(target = "purchaseDatetime", expression = "java(combineDateTime(dto.getDate(), dto.getTime()))")
    // ★ポイント2: @Named で指定した独自メソッド parseAmount を適用して数値をセット
    @Mapping(target = "totalAmount", source = "totalAmount", qualifiedByName = "parseAmount")
    @Mapping(target = "receiptImagePath", ignore = true) // 画像パスはControllerで後からセットするため無視
    AccountForm toForm(OcrResponseDto dto);

    // 2. 子データ（OcrItemDto -> AccountDetailForm）の変換定義（リストの中身用）
    @Mapping(target = "itemName", source = "name")
    // こちらも同様に金額の数値化メソッドを適用
    @Mapping(target = "itemAmount", source = "price", qualifiedByName = "parseAmount")
    AccountDetailForm toDetailForm(OcrResponseDto.OcrItemDto itemDto);

    // ==============================================================
    // 以下の default メソッドが、自動マッピング時に呼び出される自作ロジックです
    // ==============================================================

    /**
     * 【自作ルール1】金額文字列から数値のみを抽出する
     */
    @Named("parseAmount") // ← この名前で @Mapping から呼び出せるようになります
    default Integer parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) return 0;
        String numericStr = amountStr.replaceAll("[^0-9]", "");
        return numericStr.isEmpty() ? 0 : Integer.parseInt(numericStr);
    }

    /**
     * 【自作ルール2】日付と時間を結合してHTML(datetime-local)用のフォーマットにする
     */
    default String combineDateTime(String date, String time) {
        if (date == null || date.isEmpty()) {
            return null;
        }
        String timeStr = (time != null && !time.isEmpty()) 
                         ? time.substring(0, 5) 
                         : "00:00";
        return date + "T" + timeStr;
    }
}