package com.backend.StockLinker.ProfileService.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressInfoRequestDto {
    @NotBlank(message = "Address line 1 is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "Area is required")
    private String area;

    @NotBlank(message = "City / Town is required")
    private String cityOrTown;

    @NotBlank(message = "District is required")
    private String district;

    @NotBlank(message = "Pincode is required")
    private String pincode;
}