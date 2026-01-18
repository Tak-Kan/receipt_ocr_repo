package com.example.demo.model;

/**
 * 定義するロール名（DB には ENUM の文字列として保存される）
 * コード中では ROLE プレフィックスは不要にしておき、GrantedAuthority 作成時に付与します。
 */
public enum RoleName {
    USER,
    ADMIN
}