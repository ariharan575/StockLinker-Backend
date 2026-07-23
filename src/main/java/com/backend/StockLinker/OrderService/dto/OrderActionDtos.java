package com.backend.StockLinker.OrderService.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

public class OrderActionDtos {

    @Data
    public static class RejectOrderDto {
        private String reason;
    }

    @Data
    public static class ScheduleOrderDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate deliveryDate;
    }

    @Data
    public static class UpdateSequenceDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate deliveryDate;
        private List<String> orderedOrderIds;
    }
}
