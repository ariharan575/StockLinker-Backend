package com.backend.StockLinker.onboarding.mapper;

import com.backend.StockLinker.onboarding.dto.request.ShopkeeperSetupRequest;
import com.backend.StockLinker.onboarding.dto.response.ShopkeeperBusinessResponse;
import com.backend.StockLinker.onboarding.entity.ShopkeeperBusinessDetails;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = GlobalMapperConfig.class)
public interface ShopkeeperBusinessMapper {

    ShopkeeperBusinessResponse toResponse(
            ShopkeeperBusinessDetails entity
    );

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    void updateShopkeeperDetails(
            ShopkeeperSetupRequest request,
            @MappingTarget ShopkeeperBusinessDetails entity
    );
}