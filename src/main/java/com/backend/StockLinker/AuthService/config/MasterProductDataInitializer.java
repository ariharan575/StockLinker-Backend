package com.backend.StockLinker.AuthService.config;

import com.backend.StockLinker.ProfileService.model.MasterProduct;
import com.backend.StockLinker.ProfileService.model.ProductCategory;
import com.backend.StockLinker.ProfileService.model.ProductSubCategory;
import com.backend.StockLinker.ProfileService.repository.postgres.ProductCategoryRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.ProductSubCategoryRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.MasterProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class MasterProductDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MasterProductDataInitializer.class);

    private final MasterProductRepository masterProductRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductSubCategoryRepository productSubCategoryRepository;

    public MasterProductDataInitializer(MasterProductRepository masterProductRepository,
                                        ProductCategoryRepository productCategoryRepository,
                                        ProductSubCategoryRepository productSubCategoryRepository) {
        this.masterProductRepository = masterProductRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productSubCategoryRepository = productSubCategoryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (masterProductRepository.count() > 0) {
            logger.info("Master Products already initialized. Skipping insertion.");
            return;
        }

        logger.info("Starting Master Product Initialization...");

        // Load all existing Categories and SubCategories
        List<ProductCategory> allCategories = productCategoryRepository.findAll();
        List<ProductSubCategory> allSubCategories = productSubCategoryRepository.findAll();

        if (allCategories.isEmpty() || allSubCategories.isEmpty()) {
            logger.warn("Categories or SubCategories are missing. Please ensure ProductDataInitializer runs first.");
            return;
        }

        logger.info("Loaded {} Categories and {} Sub Categories", allCategories.size(), allSubCategories.size());

        // Create a fast-lookup map: Key = "CategoryName|SubCategoryName", Value = ProductSubCategory
        Map<String, ProductSubCategory> subCategoryMap = allSubCategories.stream()
                .collect(Collectors.toMap(
                        sc -> sc.getProductCategory().getName() + "|" + sc.getName(),
                        sc -> sc,
                        (existing, replacement) -> existing
                ));

        Map<String, List<String>> catalogData = buildMasterCatalogData();
        List<MasterProduct> masterProductsToSave = new ArrayList<>();
        Set<String> usedSlugs = new HashSet<>();

        logger.info("Preparing Master Products...");

        for (Map.Entry<String, List<String>> entry : catalogData.entrySet()) {
            String categorySubCategoryKey = entry.getKey();
            List<String> productNames = entry.getValue();

            ProductSubCategory subCategory = subCategoryMap.get(categorySubCategoryKey);

            if (subCategory == null) {
                logger.warn("SubCategory not found for key: {}. Skipping its products.", categorySubCategoryKey);
                continue;
            }

            ProductCategory category = subCategory.getProductCategory();

            for (String productName : productNames) {
                MasterProduct masterProduct = createMasterProduct(productName, subCategory, usedSlugs);
                masterProductsToSave.add(masterProduct);
            }
        }

        // Batch save for performance
        masterProductRepository.saveAll(masterProductsToSave);
        logger.info("Inserted {} Master Products", masterProductsToSave.size());
        logger.info("Master Product Initialization Completed Successfully.");
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private MasterProduct createMasterProduct(String name, ProductSubCategory subCategory, Set<String> usedSlugs) {
        MasterProduct product = new MasterProduct();
        product.setProductName(name);
        product.setProductSubCategory(subCategory);

        String slug = generateUniqueSlug(name, usedSlugs);
        product.setSlug(slug);

        return product;
    }

    private String generateSlug(String name) {
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

    // ==========================================
    // CATALOG DATA GENERATION
    // ==========================================

    private Map<String, List<String>> buildMasterCatalogData() {
        Map<String, List<String>> catalog = new HashMap<>();

        // 1. ELECTRONICS
        catalog.put("Electronics|Televisions", List.of(
                "Samsung 55 Inch 4K Smart TV", "LG OLED 65 Inch TV", "Sony Bravia 50 Inch 4K TV",
                "TCL 43 Inch Smart LED TV", "Hisense 55 Inch ULED TV", "Panasonic 40 Inch Full HD TV",
                "Mi 43 Inch Smart Android TV", "Vu 50 Inch Premium 4K TV"
        ));
        catalog.put("Electronics|Wireless Earbuds", List.of(
                "Boat Airdopes 141", "Apple AirPods Pro Gen 2", "Samsung Galaxy Buds 2 Pro",
                "Sony WF-1000XM5", "Noise Buds VS102", "OnePlus Buds Pro 2", "JBL Wave 200", "Jabra Elite 4 Active"
        ));
        catalog.put("Electronics|Digital Cameras", List.of(
                "Sony Alpha ILCE-6100Y", "Canon EOS 1500D", "Nikon D7500", "Panasonic LUMIX G7",
                "Fujifilm X-T4", "Olympus OM-D E-M10 Mark IV"
        ));
        catalog.put("Electronics|Smart Watches", List.of(
                "Apple Watch Series 9", "Samsung Galaxy Watch 6", "Garmin Forerunner 265",
                "Noise ColorFit Pro 4", "Boat Xtend Smartwatch", "Amazfit GTR 4", "Fitbit Versa 4"
        ));
        catalog.put("Electronics|Bluetooth Speakers", List.of(
                "JBL Flip 6", "Bose SoundLink Flex", "Sony SRS-XB13", "Ultimate Ears WONDERBOOM 3",
                "Boat Stone 1000", "Marshall Emberton II", "Tribit XSound Go"
        ));

        // 2. GROCERY & GOURMET
        catalog.put("Grocery & Gourmet|Rice & Grains", List.of(
                "India Gate Basmati Rice 5kg", "Daawat Rozana Gold Basmati 1kg", "Fortune Everyday Basmati Rice 5kg",
                "Kohinoor Super Silver Basmati 1kg", "Ponni Boiled Rice 5kg", "Sona Masoori Raw Rice 10kg",
                "Aashirvaad Whole Wheat Atta 5kg", "Pillsbury Chakki Fresh Atta 5kg"
        ));
        catalog.put("Grocery & Gourmet|Pulses & Lentils", List.of(
                "Tata Sampann Toor Dal 1kg", "Organic Tattva Moong Dal 500g", "Fortune Chana Dal 1kg",
                "Rajdhani Urad Dal 1kg", "Pro Nature Kabuli Chana 500g", "24 Mantra Organic Masoor Dal 1kg"
        ));
        catalog.put("Grocery & Gourmet|Edible Oils", List.of(
                "Fortune Sunlite Sunflower Oil 1L", "Saffola Gold Blended Oil 1L", "Dhara Mustard Oil 1L",
                "Idhayam Sesame Oil 500ml", "Figaro Olive Oil 250ml", "Patanjali Cow Ghee 1L", "Amul Pure Ghee 500ml"
        ));
        catalog.put("Grocery & Gourmet|Spices & Masalas", List.of(
                "Everest Garam Masala 100g", "MDH Chana Masala 100g", "Catch Turmeric Powder 200g",
                "Aashirvaad Chilli Powder 200g", "MTR Sambar Powder 200g", "Tata Sampann Coriander Powder 100g"
        ));
        catalog.put("Grocery & Gourmet|Tea & Coffee", List.of(
                "Brooke Bond Red Label Tea 500g", "Tata Tea Premium 1kg", "Taj Mahal Tea 500g",
                "Nescafe Classic Instant Coffee 100g", "Bru Gold Coffee 50g", "Tetley Green Tea 100 Bags"
        ));
        catalog.put("Grocery & Gourmet|Packaged Snacks", List.of(
                "Lay's India's Magic Masala 50g", "Kurkure Masala Munch 90g", "Haldiram's Bhujia Sev 400g",
                "Bikano Aloo Bhujia 200g", "Bingo Mad Angles 90g", "Britannia Good Day Cookies 250g", "Parle-G 800g"
        ));

        // 3. HOME APPLIANCES
        catalog.put("Home Appliances|Refrigerators", List.of(
                "Samsung 236L Frost Free Double Door", "LG 260L Double Door Refrigerator", "Whirlpool 190L Direct Cool Single Door",
                "Godrej 180L Single Door Refrigerator", "Haier 325L Bottom Mounted Refrigerator", "Bosch 358L Double Door Refrigerator"
        ));
        catalog.put("Home Appliances|Washing Machines", List.of(
                "LG 7 Kg Fully Automatic Top Load", "Samsung 6.5 Kg Fully Automatic Top Load", "IFB 8 Kg Fully Automatic Front Load",
                "Bosch 7 Kg Fully Automatic Front Load", "Whirlpool 7.5 Kg Semi-Automatic", "Godrej 6.2 Kg Top Load"
        ));
        catalog.put("Home Appliances|Air Conditioners", List.of(
                "Voltas 1.5 Ton 3 Star Split Inverter AC", "LG 1.5 Ton 5 Star AI Dual Inverter Split AC", "Daikin 1.5 Ton 4 Star Inverter Split AC",
                "Hitachi 1.5 Ton 3 Star Split AC", "Blue Star 1 Ton 3 Star Split AC", "Lloyd 1.5 Ton 3 Star Split AC"
        ));
        catalog.put("Home Appliances|Microwave Ovens", List.of(
                "Samsung 28L Convection Microwave", "LG 21L Convection Microwave", "Panasonic 20L Solo Microwave",
                "IFB 23L Convection Microwave", "Bajaj 17L Solo Microwave Oven", "Morphy Richards 30L Convection"
        ));
        catalog.put("Home Appliances|Mixer Grinders", List.of(
                "Preethi Zodiac MG 218", "Bajaj Rex 500W Mixer Grinder", "Philips HL7756/00 750W",
                "Sujata Dynamix 900W Mixer", "Butterfly Smart 750W", "Bosch Pro 1000W Mixer Grinder"
        ));

        // 4. HEALTHCARE & MEDICAL
        catalog.put("Healthcare & Medical|First Aid Supplies", List.of(
                "Dettol Antiseptic Liquid 500ml", "Savlon Antiseptic Liquid 200ml", "Hansaplast Washproof Band-Aids 100s",
                "Johnson & Johnson Cotton Roll 500g", "Betadine Ointment 20g", "3M Micropore Surgical Tape"
        ));
        catalog.put("Healthcare & Medical|Health Monitors", List.of(
                "Omron HEM 7120 BP Monitor", "Dr. Trust Fully Automatic BP Monitor", "Accu-Chek Active Blood Glucose Meter",
                "OneTouch Select Plus Simple Glucometer", "Dr. Morepen Pulse Oximeter", "Beurer Digital Thermometer"
        ));
        catalog.put("Healthcare & Medical|Nutritional Supplements", List.of(
                "Horlicks Classic Malt 500g", "Bournvita Health Drink 500g", "Ensure Vanilla Nutrition Powder 400g",
                "Revital H Daily Health Supplement 30 Caps", "Centrum Adult Multivitamin 50 Tabs", "MuscleBlaze Whey Protein 1kg"
        ));

        // 5. BEAUTY & PERSONAL CARE
        catalog.put("Beauty & Personal Care|Skincare", List.of(
                "Nivea Soft Light Moisturizer 200ml", "Pond's Super Light Gel 100g", "Cetaphil Gentle Skin Cleanser 250ml",
                "Garnier Vitamin C Serum 30ml", "Lakme Sun Expert SPF 50 50g", "Minimalist 10% Niacinamide Serum 30ml"
        ));
        catalog.put("Beauty & Personal Care|Haircare", List.of(
                "Dove Intense Repair Shampoo 650ml", "L'Oreal Paris Total Repair 5 Shampoo 704ml", "TRESemme Keratin Smooth Shampoo 580ml",
                "Head & Shoulders Anti Dandruff Shampoo 650ml", "Parachute Advanced Coconut Hair Oil 300ml", "Bajaj Almond Drops Hair Oil 200ml"
        ));
        catalog.put("Beauty & Personal Care|Bath & Body", List.of(
                "Pears Pure & Gentle Soap 125g (Pack of 3)", "Dove Cream Beauty Bathing Bar 100g", "Dettol Original Soap 125g",
                "Lux Orchid Scent Soap 100g", "Fiama Di Wills Shower Gel 250ml", "Nivea Body Wash 250ml"
        ));

        // 6. CONSTRUCTION & BUILDING
        catalog.put("Construction & Building|Cement & Concrete", List.of(
                "UltraTech Cement OPC 53 Grade 50kg", "Ambuja Plus Cement 50kg", "ACC Gold Water Shield Cement 50kg",
                "Shree Jung Rodhak Cement 50kg", "Ramco Supergrade Cement 50kg", "Dalmia DSP Cement 50kg"
        ));
        catalog.put("Construction & Building|Paints & Finishes", List.of(
                "Asian Paints Tractor Emulsion 20L", "Berger Bison Emulsion 20L", "Nerolac Beauty Smooth Emulsion 20L",
                "Dulux Promise Interior Emulsion 20L", "Asian Paints Royale Play 1L", "Dr. Fixit 301 Pidicrete URP 1L"
        ));
        catalog.put("Construction & Building|Pipes & Fittings", List.of(
                "Supreme PVC Pipe 1 Inch 3m", "Ashirvad CPVC Pipe 0.5 Inch 3m", "Astral UPVC Pipe 1 Inch",
                "Prince PVC Ball Valve 1 Inch", "Finolex PVC Elbow 1 Inch", "Plasto Water Tank 1000L"
        ));

        // 7. ELECTRICAL & LIGHTING
        catalog.put("Electrical & Lighting|LED Lights", List.of(
                "Philips 9W LED Bulb (Pack of 4)", "Syska 9W LED Bulb", "Crompton 20W LED Tube Light",
                "Wipro 9W Smart LED Bulb", "Panasonic 12W LED Panel Light", "Havells 15W LED Recessed Downlight"
        ));
        catalog.put("Electrical & Lighting|Wires & Cables", List.of(
                "Polycab 1.5 sq mm FR Wire 90m", "Havells Life Line 1 sq mm Wire 90m", "Finolex 2.5 sq mm FR Wire 90m",
                "RR Kabel 1.5 sq mm Wire 90m", "V-Guard 1 sq mm Wire 90m", "Anchor 2.5 sq mm FR Wire 90m"
        ));
        catalog.put("Electrical & Lighting|Switches & Sockets", List.of(
                "Anchor Roma 10A Switch", "Legrand Mylinc 6A Switch", "Havells Crabtree 16A Socket",
                "GM Modular 6A 1 Way Switch", "Schneider Electric Livia 6A Socket", "Cona Original 6A Switch"
        ));

        // 8. MOBILE & ACCESSORIES
        catalog.put("Mobile & Accessories|Smartphones", List.of(
                "Samsung Galaxy S24 Ultra 256GB", "Apple iPhone 15 Pro Max 256GB", "Google Pixel 8 Pro 128GB",
                "OnePlus 12 5G 256GB", "Xiaomi 14 512GB", "Vivo X100 Pro 256GB", "iQOO 12 5G 256GB", "Motorola Edge 50 Pro"
        ));
        catalog.put("Mobile & Accessories|Mobile Cases & Covers", List.of(
                "Spigen Liquid Air Case for iPhone 15", "Ringke Fusion X for Galaxy S24", "Spigen Rugged Armor for Pixel 8",
                "Nillkin CamShield Pro for OnePlus 12", "Urban Armor Gear Case", "Supcase Unicorn Beetle Pro"
        ));
        catalog.put("Mobile & Accessories|Power Banks", List.of( // Duplicate subcategory name handled correctly by prefix key
                "Mi Power Bank 3i 20000mAh", "Ambrane 10000mAh Power Bank", "Samsung 10000mAh Battery Pack",
                "Anker PowerCore 10000 PD", "URBN 20000mAh 22.5W Power Bank", "Syska 10000mAh Power Bank"
        ));
        catalog.put("Mobile & Accessories|Chargers & Cables", List.of(
                "Apple 20W USB-C Power Adapter", "Samsung 25W Travel Adapter", "Mi 33W SonicCharge 2.0",
                "Anker 313 30W USB-C Charger", "AmazonBasics USB-C to Lightning Cable", "Boat Rugged Type-C Cable 1.5m"
        ));

        return catalog;
    }
}