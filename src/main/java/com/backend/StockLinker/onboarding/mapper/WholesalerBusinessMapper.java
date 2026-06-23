package com.backend.StockLinker.onboarding.mapper;

import com.backend.StockLinker.onboarding.dto.request.WholesalerSetupRequest;
import com.backend.StockLinker.onboarding.dto.response.WholesalerBusinessResponse;
import com.backend.StockLinker.onboarding.entity.WholesalerBusinessDetails;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = GlobalMapperConfig.class)
public interface WholesalerBusinessMapper {

    WholesalerBusinessResponse toResponse(
            WholesalerBusinessDetails entity
    );

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    void updateWholesalerDetails(
            WholesalerSetupRequest request,
            @MappingTarget WholesalerBusinessDetails entity
    );
}