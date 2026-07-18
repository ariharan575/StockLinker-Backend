package com.backend.StockLinker.AuthService.config;

import com.backend.StockLinker.ProfileService.model.ProductCategory;
import com.backend.StockLinker.ProfileService.model.ProductSubCategory;
import com.backend.StockLinker.ProfileService.repository.postgres.ProductCategoryRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.ProductSubCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ProductDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProductDataInitializer.class);

    private final ProductCategoryRepository productCategoryRepository;
    private final ProductSubCategoryRepository productSubCategoryRepository;

    public ProductDataInitializer(ProductCategoryRepository productCategoryRepository,
                                  ProductSubCategoryRepository productSubCategoryRepository) {
        this.productCategoryRepository = productCategoryRepository;
        this.productSubCategoryRepository = productSubCategoryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // We expect exactly 11 categories as per the new frontend synchronization.
        if (productCategoryRepository.count() == 11) {
            logger.info("Already Initialized. Product categories match frontend configuration. Skipping.");
            return;
        }

        logger.info("Starting Product Initialization...");

        // Completely remove old data to ensure 100% synchronization without merging
        if (productCategoryRepository.count() > 0 || productSubCategoryRepository.count() > 0) {
            logger.info("Removing old deprecated categories (Grocery/FMCG)...");
            productSubCategoryRepository.deleteAll();
            productCategoryRepository.deleteAll();
        }

        // ==========================================
        // 1. DEFINE FRONTEND DATA STRUCTURE
        // ==========================================
        List<CategoryDef> definitions = Arrays.asList(
                new CategoryDef("Electronics", "FiCpu", Arrays.asList(
                        "Televisions", "Home Theater Systems", "Digital Cameras", "Drones & Accessories",
                        "Action Cameras", "Bluetooth Speakers", "Wireless Earbuds", "Power Banks",
                        "Smart Watches", "VR Headsets", "Set-Top Boxes", "Projectors",
                        "Car Electronics", "Surveillance Cameras"
                )),
                new CategoryDef("Grocery & Gourmet", "FiShoppingCart", Arrays.asList(
                        "Rice & Grains", "Pulses & Lentils", "Edible Oils", "Spices & Masalas",
                        "Tea & Coffee", "Packaged Snacks", "Dry Fruits & Nuts", "Bakery Ingredients",
                        "Sauces & Condiments", "Breakfast Cereals", "Frozen Foods", "Organic Foods",
                        "Beverages & Juices", "Sweeteners", "Canned & Preserved Foods"
                )),
                new CategoryDef("Home Appliances", "FiHome", Arrays.asList(
                        "Refrigerators", "Washing Machines", "Air Conditioners", "Microwave Ovens",
                        "Water Purifiers", "Vacuum Cleaners", "Air Fryers", "Mixer Grinders",
                        "Ceiling Fans", "Room Heaters", "Water Heaters", "Induction Cooktops",
                        "Dishwashers", "Air Coolers", "Chimneys & Hoods"
                )),
                new CategoryDef("Furniture & Decor", "FiBox", Arrays.asList(
                        "Sofas & Recliners", "Dining Sets", "Office Chairs", "Office Desks",
                        "Wardrobes", "Bookshelves", "Bed Frames", "Mattresses",
                        "Coffee Tables", "TV Units", "Wall Art & Decor", "Curtains & Blinds",
                        "Rugs & Carpets", "Storage Cabinets", "Outdoor Furniture"
                )),
                new CategoryDef("Agriculture", "FiSun", Arrays.asList(
                        "Seeds & Saplings", "Fertilizers", "Pesticides", "Irrigation Equipment",
                        "Tractors & Implements", "Greenhouse Supplies", "Farm Tools", "Poultry Equipment",
                        "Animal Feed", "Harvesting Machinery", "Sprayers & Dusters", "Solar Water Pumps",
                        "Beekeeping Supplies", "Soil Testing Kits"
                )),
                new CategoryDef("Healthcare & Medical", "FiHeart", Arrays.asList(
                        "First Aid Supplies", "Mobility Aids", "Surgical Instruments",
                        "Personal Protective", "Hospital Furniture", "Health Monitors",
                        "Orthopedic Supports", "Respiratory Equipment", "Dental Supplies",
                        "Ayurvedic Products", "Nutritional Supplements"
                )),
                new CategoryDef("Beauty & Personal Care", "FiDroplet", Arrays.asList(
                        "Skincare", "Haircare", "Makeup & Cosmetics", "Fragrances & Perfumes",
                        "Grooming Tools", "Bath & Body", "Nail Care",
                        "Men's Grooming", "Organic & Herbal Care", "Beauty Tools & Accessories"
                )),
                new CategoryDef("Construction & Building", "FiTool", Arrays.asList(
                        "Cement & Concrete", "Bricks & Blocks", "Steel & TMT Bars", "Tiles & Sanitaryware",
                        "Doors & Windows", "Roofing Materials", "Plywood & Boards", "Waterproofing Solutions",
                        "Scaffolding Equipment", "Construction Tools", "Paints & Finishes", "Pipes & Fittings"
                )),
                new CategoryDef("Electrical & Lighting", "FiZap", Arrays.asList(
                        "LED Lights", "Wires & Cables", "Switches & Sockets", "MCBs & Distribution Boards",
                        "Solar Panels", "Inverters & UPS", "Transformers", "Lighting Fixtures",
                        "Smart Home Devices", "Industrial Lighting", "Street Lighting"
                )),
                new CategoryDef("Books & Stationery", "FiBook", Arrays.asList(
                        "Textbooks", "Notebooks & Diaries", "Art & Craft Supplies", "Office Stationery",
                        "Children's Books", "Religious & Spiritual Books", "Calendars & Planners",
                        "Gift Wrapping Supplies", "Filing Supplies"
                )),
                new CategoryDef("Mobile & Accessories", "FiSmartphone", Arrays.asList(
                        "Smartphones", "Mobile Cases & Covers", "Screen Protectors", "Chargers & Cables",
                        "Power Banks", "Bluetooth Headsets", "Mobile Repair Parts", "Phone Holders & Mounts",
                        "Selfie Sticks & Tripods", "SIM Accessories"
                ))
        );

        // ==========================================
        // 2. INSERT PRODUCT CATEGORIES
        // ==========================================
        List<ProductCategory> categoriesToSave = new ArrayList<>();
        Set<String> categorySlugs = new HashSet<>(); // Guard against duplicate slugs

        for (CategoryDef def : definitions) {
            categoriesToSave.add(createCategory(def.name, def.icon, categorySlugs));
        }

        List<ProductCategory> savedCategories = productCategoryRepository.saveAll(categoriesToSave);
        logger.info("Inserted {} Categories", savedCategories.size());

        // ==========================================
        // 3. INSERT PRODUCT SUB CATEGORIES
        // ==========================================
        List<ProductSubCategory> subCategoriesToSave = new ArrayList<>();
        Set<String> subCategorySlugs = new HashSet<>(); // Ensures global subcategory slug uniqueness

        for (ProductCategory savedCategory : savedCategories) {
            // Find corresponding definition
            CategoryDef matchingDef = definitions.stream()
                    .filter(d -> d.name.equals(savedCategory.getName()))
                    .findFirst()
                    .orElseThrow();

            for (String subName : matchingDef.subcategories) {
                subCategoriesToSave.add(createSubCategory(savedCategory, subName, subCategorySlugs));
            }
        }

        productSubCategoryRepository.saveAll(subCategoriesToSave);
        logger.info("Inserted {} Sub Categories", subCategoriesToSave.size());

        logger.info("Initialization Completed Successfully");
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private ProductCategory createCategory(String name, String icon, Set<String> usedSlugs) {
        ProductCategory category = new ProductCategory();
        category.setName(name);
        category.setIcon(icon);
        category.setActive(true);

        String slug = generateUniqueSlug(name, usedSlugs);
        category.setSlug(slug);
        category.setImageName(generateImageName(name));

        return category;
    }

    private ProductSubCategory createSubCategory(ProductCategory productCategory, String name, Set<String> usedSlugs) {
        ProductSubCategory subCategory = new ProductSubCategory();
        subCategory.setProductCategory(productCategory);
        subCategory.setName(name);

        String slug = generateUniqueSlug(name, usedSlugs);
        subCategory.setSlug(slug);
        subCategory.setImageName(generateImageName(name));

        return subCategory;
    }

    private String generateSlug(String name) {
        // Removes ampersands, normalizes spaces/special chars, formats to lowercase kebab-case
        return name.toLowerCase()
                .replaceAll("&", "")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private String generateUniqueSlug(String name, Set<String> usedSlugs) {
        String baseSlug = generateSlug(name);
        String uniqueSlug = baseSlug;
        int counter = 1;

        while (usedSlugs.contains(uniqueSlug)) {
            uniqueSlug = baseSlug + "-" + counter++;
        }
        usedSlugs.add(uniqueSlug);
        return uniqueSlug;
    }

    private String generateImageName(String name) {
        return generateSlug(name) + ".png";
    }

    // Static nested class to keep definitions clean and manageable
    private static class CategoryDef {
        String name;
        String icon;
        List<String> subcategories;

        CategoryDef(String name, String icon, List<String> subcategories) {
            this.name = name;
            this.icon = icon;
            this.subcategories = subcategories;
        }
    }
}