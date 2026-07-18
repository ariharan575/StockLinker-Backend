package com.backend.StockLinker.ProfileService.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private String id;
    private String name;
    private String slug;
    private String icon;
    private String imageName;
    private List<SubCategoryDTO> subcategories;
}