package com.backend.StockLinker.onboarding.mapper;

import com.backend.StockLinker.onboarding.dto.request.BusinessIdentityRequest;
import com.backend.StockLinker.onboarding.dto.request.BusinessLocationRequest;
import com.backend.StockLinker.onboarding.dto.request.LanguageSelectionRequest;
import com.backend.StockLinker.onboarding.dto.response.CommonBusinessProfileResponse;
import com.backend.StockLinker.onboarding.entity.CommonBusinessProfile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = GlobalMapperConfig.class)
public interface CommonBusinessProfileMapper {

    CommonBusinessProfileResponse toResponse(
            CommonBusinessProfile entity
    );

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    void updateLanguageSelection(
            LanguageSelectionRequest request,
            @MappingTarget CommonBusinessProfile entity
    );

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    void updateBusinessIdentity(
            BusinessIdentityRequest request,
            @MappingTarget CommonBusinessProfile entity
    );

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    void updateBusinessLocation(
            BusinessLocationRequest request,
            @MappingTarget CommonBusinessProfile entity
    );
}