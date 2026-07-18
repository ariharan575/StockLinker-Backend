package com.backend.StockLinker.ProfileService.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BusinessInfoRequestDto {

    @NotBlank(message = "Owner name is required")
    private String ownerName;

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid mobile number format")
    private String mobile;

    private String alternateMobile;

    @NotBlank(message = "Business email is required")
    @Email(message = "Invalid email format")
    private String businessEmail;

    private String gstNumber;
}