package com.example.demo.mapper;

import com.example.demo.dto.AccountForm;
import com.example.demo.dto.AccountDetailForm;
import com.example.demo.dto.OcrResponseDto;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface OcrMapper {

    // 1. 親データ（OcrResponseDto -> AccountForm）の変換定義
    @Mapping(target = "storeName", source = "merchantName")
    @Mapping(target = "details", source = "items")
    // ★ポイント1: 独自メソッド combineDateTime を呼び出して購入日時をセット
    @Mapping(target = "purchaseDatetime", source = ".", qualifiedByName = "toLocalDateTime")
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

    @Named("toLocalDateTime")
    default LocalDateTime toLocalDateTime(OcrResponseDto dto) {
        // 1. 確実なNULLチェック（ここで弾くことで例外を防ぐ）
        if (dto == null || dto.getDate() == null || dto.getDate().trim().isEmpty()) {
            return null; // OCRで読み取れなかった場合は素直にnullを返す
        }

        String dateStr = dto.getDate();
        String timeStr = dto.getTime();

        // 2. 正規表現で「年」「月」「日」の数字を抽出
        Pattern pattern = Pattern.compile("(\\d{4})年\\s*(\\d{1,2})月\\s*(\\d{1,2})日");
        Matcher matcher = pattern.matcher(dateStr);
        
        if (!matcher.find()) {
            return null; // フォーマットが合わず読み取れなかった場合もnullを返す
        }

        int year = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int day = Integer.parseInt(matcher.group(3));

        // 3. 時刻の抽出（レシートに時刻がない場合は 0時0分 とする）
        int hour = 0;
        int minute = 0;
        if (timeStr != null && !timeStr.trim().isEmpty()) {
            // "21:30:00" などの文字列をコロンで分割
            String[] timeParts = timeStr.trim().split(":");
            if (timeParts.length >= 2) {
                try {
                    hour = Integer.parseInt(timeParts[0]);
                    minute = Integer.parseInt(timeParts[1]);
                } catch (NumberFormatException e) {
                    // 時刻のパースに失敗した場合は 00:00 のまま処理を続行
                }
            }
        }

        // 4. LocalDateTime型として直接組み立てて返却
        return LocalDateTime.of(year, month, day, hour, minute);
    }


    @Named("toDateTimeLocalString")
    default String toDateTimeLocalString(OcrResponseDto dto) {
        if (dto == null || dto.getDate() == null || dto.getTime().isEmpty()) {
            return null;
        }

        String dateStr = dto.getDate(); // 例: "2026年07月06日（金）"
        String timeStr = dto.getTime(); // 例: "21:30:00" または null

        // 1. 正規表現で「年」「月」「日」の数字のみを抽出（OCRの揺らぎ対策）
        String formattedDate = "";
        // \d{4} = 4桁の数字, \d{1,2} = 1〜2桁の数字, \s* = 0個以上の空白（誤検知対策）
        Pattern pattern = Pattern.compile("(\\d{4})年\\s*(\\d{1,2})月\\s*(\\d{1,2})日");
        Matcher matcher = pattern.matcher(dateStr);
        
        if (matcher.find()) {
            String year = matcher.group(1);
            // 月と日が1桁の場合（例: "7月"）を考慮し、必ず2桁にゼロ埋めする
            String month = String.format("%02d", Integer.parseInt(matcher.group(2)));
            String day = String.format("%02d", Integer.parseInt(matcher.group(3)));
            
            formattedDate = year + "-" + month + "-" + day; // "2026-07-06" の形に整形
        } else {
            // パターンに合致しない（完全に読み取り失敗した）場合はnullを返し、画面で手入力させる
            return null;
        }

        // 2. 時刻の整形
        if (timeStr == null || timeStr.trim().isEmpty()) {
            timeStr = "00:00"; // レシートに時刻がない場合のデフォルト値
        } else {
            timeStr = timeStr.trim();
            // "21:30:00" など秒が含まれる場合は hh:mm に切り詰める
            if (timeStr.length() >= 5) {
                timeStr = timeStr.substring(0, 5);
            }
        }

        // 3. datetime-local用の形式 (yyyy-MM-dd'T'HH:mm) で結合して返却
        return formattedDate + "T" + timeStr;
    }
}