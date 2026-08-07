package com.backend.StockLinker.config;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

@Component
@EnableScheduling
public class MemoryLogger {

    @Scheduled(fixedRate = 30000)
    public void logMemory() {

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();

        System.out.println("========== MEMORY ==========");
        System.out.println("Heap Used      : " + heap.getUsed() / 1024 / 1024 + " MB");
        System.out.println("Heap Max       : " + heap.getMax() / 1024 / 1024 + " MB");
        System.out.println("Non Heap Used  : " + nonHeap.getUsed() / 1024 / 1024 + " MB");
        System.out.println("Non Heap Max   : " + nonHeap.getMax() / 1024 / 1024 + " MB");
        System.out.println("============================");
    }
}