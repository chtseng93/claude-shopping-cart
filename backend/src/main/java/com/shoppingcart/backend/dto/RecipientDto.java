package com.shoppingcart.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 收件人資料 DTO，對應 api.md §5.1 結帳請求中的 recipient 欄位。
 * 使用 Bean Validation 確保所有必填欄位符合格式規定。
 */
public class RecipientDto {

    /** 收件人姓名，不可為空白 */
    @NotBlank(message = "姓名不可為空")
    private String name;

    /** 收件人手機號碼，需符合台灣手機格式（09 開頭共 10 碼） */
    @Pattern(regexp = "^09\\d{8}$", message = "手機號碼格式不正確，需符合 09XXXXXXXX 格式")
    private String phone;

    /** 收件人電子郵件地址，需符合 Email 格式 */
    @Email(message = "Email 格式不正確")
    private String email;

    /** 收件地址，不可為空白 */
    @NotBlank(message = "地址不可為空")
    private String address;

    // ── 建構子 ──────────────────────────────────────────────────────────

    /** Jackson 反序列化用無參數建構子 */
    public RecipientDto() {}

    /**
     * 完整建構子。
     *
     * @param name    收件人姓名
     * @param phone   手機號碼（格式：09XXXXXXXX）
     * @param email   電子郵件地址
     * @param address 收件地址
     */
    public RecipientDto(String name, String phone, String email, String address) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    /** 取得收件人姓名 */
    public String getName() { return name; }

    /** 設定收件人姓名 */
    public void setName(String name) { this.name = name; }

    /** 取得手機號碼 */
    public String getPhone() { return phone; }

    /** 設定手機號碼 */
    public void setPhone(String phone) { this.phone = phone; }

    /** 取得電子郵件 */
    public String getEmail() { return email; }

    /** 設定電子郵件 */
    public void setEmail(String email) { this.email = email; }

    /** 取得收件地址 */
    public String getAddress() { return address; }

    /** 設定收件地址 */
    public void setAddress(String address) { this.address = address; }
}
