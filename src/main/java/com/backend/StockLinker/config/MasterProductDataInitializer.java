package com.backend.StockLinker.config;

import com.backend.StockLinker.ProfileService.model.MasterProduct;
import com.backend.StockLinker.ProfileService.model.ProductCategory;
import com.backend.StockLinker.ProfileService.model.ProductSubCategory;
import com.backend.StockLinker.ProfileService.repository.postgres.MasterProductRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.ProductCategoryRepository;
import com.backend.StockLinker.ProfileService.repository.postgres.ProductSubCategoryRepository;
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

        logger.info("Starting Master Product Initialization (Strict Generic Names)...");

        List<ProductCategory> allCategories = productCategoryRepository.findAll();
        List<ProductSubCategory> allSubCategories = productSubCategoryRepository.findAll();

        if (allCategories.isEmpty() || allSubCategories.isEmpty()) {
            logger.warn("SubCategories missing. Ensure ProductDataInitializer runs first.");
            return;
        }

        Map<String, ProductSubCategory> subCategoryMap = allSubCategories.stream()
                .collect(Collectors.toMap(
                        sc -> sc.getProductCategory().getName() + "|" + sc.getName(),
                        sc -> sc,
                        (existing, replacement) -> existing
                ));

        Map<String, List<String>> catalogData = buildCleanMasterCatalogData();
        List<MasterProduct> masterProductsToSave = new ArrayList<>();
        Set<String> usedSlugs = new HashSet<>();

        for (Map.Entry<String, List<String>> entry : catalogData.entrySet()) {
            ProductSubCategory subCategory = subCategoryMap.get(entry.getKey());

            if (subCategory == null) {
                logger.warn("SubCategory not found: {}", entry.getKey());
                continue;
            }

            for (String productName : entry.getValue()) {
                MasterProduct masterProduct = new MasterProduct();
                masterProduct.setProductName(productName);
                masterProduct.setProductSubCategory(subCategory);
                masterProduct.setSlug(generateUniqueSlug(productName, usedSlugs));
                masterProductsToSave.add(masterProduct);
            }
        }

        masterProductRepository.saveAll(masterProductsToSave);
        logger.info("Inserted {} Clean Master Products", masterProductsToSave.size());
    }

    private String generateUniqueSlug(String name, Set<String> usedSlugs) {
        String baseSlug = name.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-").replaceAll("^-|-$", "");
        String uniqueSlug = baseSlug;
        int counter = 1;
        while (usedSlugs.contains(uniqueSlug)) {
            uniqueSlug = baseSlug + "-" + counter++;
        }
        usedSlugs.add(uniqueSlug);
        return uniqueSlug;
    }

    private Map<String, List<String>> buildCleanMasterCatalogData() {
        Map<String, List<String>> catalog = new HashMap<>();

        // 1. ELECTRONICS
        catalog.put("Electronics|Televisions", List.of("LED Smart TV", "OLED Television", "QLED TV", "Plasma TV", "Curved TV", "Full HD TV", "Ultra HD TV", "8K Resolution TV", "Portable TV", "Commercial Display TV"));
        catalog.put("Electronics|Home Theater Systems", List.of("Soundbar System", "Surround Sound Speakers", "Subwoofer Unit", "AV Receiver", "Floorstanding Speakers", "Bookshelf Speakers", "Satellite Speakers", "Wireless Audio Transmitter", "Center Channel Speaker", "Mini Hi-Fi System"));
        catalog.put("Electronics|Digital Cameras", List.of("DSLR Camera", "Mirrorless Camera", "Point and Shoot Camera", "Action Camera", "Cinema Camera", "Medium Format Camera", "Instant Camera", "Rugged Camera", "360 Degree Camera", "Compact Digital Camera"));
        catalog.put("Electronics|Drones & Accessories", List.of("Camera Drone", "Racing Drone", "Mini Drone", "Toy Drone", "Drone Propellers", "Drone Battery", "Drone Landing Pad", "Remote Controller", "Gimbal Stabilizer", "Drone Carrying Case"));
        catalog.put("Electronics|Bluetooth Speakers", List.of("Portable Bluetooth Speaker", "Waterproof Speaker", "Party Speaker", "Smart Speaker", "Neckband Speaker", "Outdoor Speaker", "Desktop Speaker", "Mini Speaker", "Stereo Pairing Speaker", "Karaoke Speaker"));
        catalog.put("Electronics|Wireless Earbuds", List.of("True Wireless Earbuds", "Active Noise Cancelling Earbuds", "Sports Earbuds", "Gaming Earbuds", "Sleep Earbuds", "Bone Conduction Earbuds", "Invisible Earbuds", "Bass Heavy Earbuds", "Audiophile Earbuds", "Open Ear Earbuds"));
        catalog.put("Electronics|Smart Watches", List.of("Fitness Smartwatch", "Cellular Smartwatch", "Hybrid Smartwatch", "Luxury Smartwatch", "Kids Smartwatch", "Rugged Smartwatch", "Swimming Smartwatch", "Health Monitoring Watch", "Fashion Smartwatch", "GPS Smartwatch"));
        catalog.put("Electronics|Projectors", List.of("Home Theater Projector", "Mini Projector", "Business Projector", "Laser Projector", "Short Throw Projector", "Pico Projector", "Outdoor Projector", "Gaming Projector", "Smart Projector", "Holographic Projector"));

        // 2. GROCERY & GOURMET
        catalog.put("Grocery & Gourmet|Rice & Grains", List.of("Basmati Rice", "Sona Masoori Rice", "Ponni Boiled Rice", "Brown Rice", "Jasmine Rice", "Raw Rice", "Whole Wheat Atta", "Multigrain Atta", "Pearl Millet", "Sorghum"));
        catalog.put("Grocery & Gourmet|Pulses & Lentils", List.of("Toor Dal", "Moong Dal", "Chana Dal", "Urad Dal", "Masoor Dal", "Kabuli Chana", "Black Chickpeas", "Rajma", "Green Gram", "Horse Gram"));
        catalog.put("Grocery & Gourmet|Edible Oils", List.of("Sunflower Oil", "Mustard Oil", "Groundnut Oil", "Sesame Oil", "Olive Oil", "Coconut Oil", "Rice Bran Oil", "Canola Oil", "Palm Oil", "Soybean Oil"));
        catalog.put("Grocery & Gourmet|Spices & Masalas", List.of("Garam Masala", "Turmeric Powder", "Red Chilli Powder", "Coriander Powder", "Cumin Powder", "Black Pepper", "Sambar Powder", "Rasam Powder", "Chicken Masala", "Meat Masala"));
        catalog.put("Grocery & Gourmet|Tea & Coffee", List.of("Black Tea Leaves", "Green Tea Bags", "Filter Coffee Powder", "Instant Coffee", "Herbal Tea", "White Tea", "Oolong Tea", "Decaf Coffee", "Espresso Blend", "Masala Chai Mix"));
        catalog.put("Grocery & Gourmet|Packaged Snacks", List.of("Potato Chips", "Corn Puffs", "Potato Crisps", "Roasted Peanuts", "Trail Mix", "Namkeen Sev", "Bhujia", "Cheese Balls", "Popcorn", "Nacho Chips"));
        catalog.put("Grocery & Gourmet|Dry Fruits & Nuts", List.of("Almonds", "Cashew Nuts", "Walnuts", "Pistachios", "Raisins", "Dried Figs", "Dried Dates", "Dried Apricots", "Pine Nuts", "Macadamia Nuts"));
        catalog.put("Grocery & Gourmet|Beverages & Juices", List.of("Apple Juice", "Orange Juice", "Mixed Fruit Juice", "Mango Nectar", "Lemonade", "Carbonated Water", "Cola Drink", "Energy Drink", "Sports Drink", "Aloe Vera Drink"));

        // 3. HOME APPLIANCES
        catalog.put("Home Appliances|Refrigerators", List.of("Single Door Refrigerator", "Double Door Refrigerator", "Side-by-Side Refrigerator", "French Door Refrigerator", "Mini Fridge", "Deep Freezer", "Beverage Cooler", "Wine Cooler", "Chest Freezer", "Frost-Free Refrigerator"));
        catalog.put("Home Appliances|Washing Machines", List.of("Top Load Washer", "Front Load Washer", "Semi-Automatic Washer", "Washer Dryer Combo", "Portable Washer", "Compact Washer", "Steam Washer", "Twin Tub Washer", "Agitator Washer", "Impeller Washer"));
        catalog.put("Home Appliances|Air Conditioners", List.of("Window AC", "Split Inverter AC", "Portable AC", "Tower AC", "Cassette AC", "Central AC Unit", "Smart Air Conditioner", "Evaporative Cooler", "Ductless AC", "Solar AC"));
        catalog.put("Home Appliances|Microwave Ovens", List.of("Solo Microwave", "Convection Microwave", "Grill Microwave", "Over-the-Range Microwave", "Built-in Microwave", "Commercial Microwave", "Smart Microwave", "Compact Microwave", "Retro Microwave", "Inverter Microwave"));
        catalog.put("Home Appliances|Water Purifiers", List.of("RO Water Purifier", "UV Water Purifier", "UF Water Purifier", "Gravity Water Purifier", "Under Sink Purifier", "Alkaline Water Purifier", "Pitcher Water Filter", "Faucet Water Filter", "Countertop Filter", "Whole House Water Filter"));
        catalog.put("Home Appliances|Vacuum Cleaners", List.of("Canister Vacuum", "Upright Vacuum", "Stick Vacuum", "Handheld Vacuum", "Robot Vacuum", "Wet and Dry Vacuum", "Bagless Vacuum", "Cordless Vacuum", "Pet Hair Vacuum", "Industrial Vacuum"));
        catalog.put("Home Appliances|Mixer Grinders", List.of("Juicer Mixer Grinder", "Wet Grinder", "Dry Grinder", "Bullet Blender", "Food Processor", "Hand Blender", "Stand Mixer", "Commercial Grinder", "Spice Grinder", "Meat Grinder"));
        catalog.put("Home Appliances|Room Heaters", List.of("Oil Filled Radiator", "Fan Heater", "Halogen Heater", "Quartz Heater", "Carbon Heater", "PTC Heater", "Convection Heater", "Infrared Heater", "Ceramic Heater", "Baseboard Heater"));

        // 4. FURNITURE & DECOR
        catalog.put("Furniture & Decor|Sofas & Recliners", List.of("Sectional Sofa", "Chesterfield Sofa", "Lawson Sofa", "Tuxedo Sofa", "Camelback Sofa", "Loveseat", "Futon", "Manual Recliner", "Power Recliner", "Massage Recliner"));
        catalog.put("Furniture & Decor|Dining Sets", List.of("Wooden Dining Table", "Glass Dining Table", "Marble Dining Set", "Round Dining Table", "Extendable Dining Table", "Dining Chairs", "Dining Bench", "Bar Stools", "Counter Height Table", "Folding Dining Table"));
        catalog.put("Furniture & Decor|Office Chairs", List.of("Ergonomic Office Chair", "Executive Chair", "Task Chair", "Mesh Chair", "Gaming Chair", "Drafting Chair", "Kneeling Chair", "Conference Chair", "Guest Chair", "Big and Tall Chair"));
        catalog.put("Furniture & Decor|Office Desks", List.of("Standing Desk", "L-Shaped Desk", "Computer Desk", "Executive Desk", "Writing Desk", "Floating Desk", "Corner Desk", "Cubicle Desk", "Treadmill Desk", "Foldable Desk"));
        catalog.put("Furniture & Decor|Wardrobes", List.of("Sliding Door Wardrobe", "Hinged Door Wardrobe", "Walk-in Wardrobe", "Mirrored Wardrobe", "Fitted Wardrobe", "Freestanding Wardrobe", "Wooden Almirah", "Steel Almirah", "Fabric Wardrobe", "Modular Wardrobe"));
        catalog.put("Furniture & Decor|Bed Frames", List.of("Platform Bed", "Panel Bed", "Sleigh Bed", "Four Poster Bed", "Canopy Bed", "Bunk Bed", "Trundle Bed", "Daybed", "Storage Bed", "Adjustable Bed"));
        catalog.put("Furniture & Decor|Mattresses", List.of("Memory Foam Mattress", "Innerspring Mattress", "Latex Mattress", "Hybrid Mattress", "Air Mattress", "Waterbed Mattress", "Futon Mattress", "Orthopedic Mattress", "Gel Mattress", "Pillow Top Mattress"));
        catalog.put("Furniture & Decor|Storage Cabinets", List.of("Filing Cabinet", "Display Cabinet", "Shoe Cabinet", "Kitchen Cabinet", "Bathroom Cabinet", "Garage Storage Cabinet", "Media Cabinet", "Accent Cabinet", "Bar Cabinet", "Corner Cabinet"));

        // 5. AGRICULTURE
        catalog.put("Agriculture|Seeds & Saplings", List.of("Vegetable Seeds", "Fruit Seeds", "Flower Seeds", "Herb Seeds", "Grain Seeds", "Legume Seeds", "Tree Saplings", "Shrub Saplings", "Indoor Plant Saplings", "Organic Seeds"));
        catalog.put("Agriculture|Fertilizers", List.of("Nitrogen Fertilizer", "Phosphorus Fertilizer", "Potassium Fertilizer", "NPK Compound", "Organic Compost", "Vermicompost", "Bone Meal", "Blood Meal", "Liquid Fertilizer", "Slow Release Fertilizer"));
        catalog.put("Agriculture|Pesticides", List.of("Insecticide", "Herbicide", "Fungicide", "Rodenticide", "Bactericide", "Larvicide", "Organic Pesticide", "Neem Oil Extract", "Biological Pest Control", "Weed Killer"));
        catalog.put("Agriculture|Irrigation Equipment", List.of("Drip Irrigation Kit", "Sprinkler System", "Soaker Hose", "Rain Barrel", "Water Timer", "Irrigation Pump", "Micro Sprinklers", "Foggers", "Irrigation Tubing", "Valve Box"));
        catalog.put("Agriculture|Farm Tools", List.of("xxxxxShovel", "Spade", "Hoe", "Pitchfork", "Rake", "Trowel", "Pruning Shears", "Sickle", "Wheelbarrow", "Post Hole Digger"));
        catalog.put("Agriculture|Poultry Equipment", List.of("Chicken Incubator", "Poultry Feeder", "Poultry Drinker", "Egg Candler", "Brooder Lamp", "Nesting Box", "Chicken Coop", "Chicken Wire", "Egg Carton", "Plucking Machine"));
        catalog.put("Agriculture|Animal Feed", List.of("Cattle Feed", "Poultry Feed", "Pig Feed", "Horse Feed", "Sheep Feed", "Goat Feed", "Fish Feed", "Rabbit Feed", "Alfalfa Hay", "Mineral Block"));
        catalog.put("Agriculture|Solar Water Pumps", List.of("Submersible Solar Pump", "Surface Solar Pump", "DC Solar Pump", "AC Solar Pump", "Solar Pump Controller", "Solar Panels for Pump", "Floating Solar Pump", "Deep Well Pump", "Agricultural Solar Pump", "Portable Solar Pump"));

        // 6. HEALTHCARE & MEDICAL
        catalog.put("Healthcare & Medical|First Aid Supplies", List.of("Adhesive Bandages", "Gauze Pads", "Medical Tape", "Antiseptic Wipes", "Hydrogen Peroxide", "Rubbing Alcohol", "Burn Ointment", "Cold Compress", "Tweezers", "First Aid Kit Box"));
        catalog.put("Healthcare & Medical|Mobility Aids", List.of("Wheelchair", "Walker", "Rollator", "Crutches", "Walking Cane", "Mobility Scooter", "Transfer Bench", "Stair Lift", "Patient Hoist", "Bed Rails"));
        catalog.put("Healthcare & Medical|Surgical Instruments", List.of("Surgical Scalpel", "Forceps", "Hemostats", "Surgical Scissors", "Retractors", "Needle Holder", "Surgical Probes", "Dilators", "Surgical Clamps", "Sutures"));
        catalog.put("Healthcare & Medical|Hospital Furniture", List.of("Hospital Bed", "Overbed Table", "Bedside Cabinet", "Examination Table", "IV Pole", "Medical Cart", "Stretcher", "Privacy Screen", "Medical Recliner", "Phlebotomy Chair"));
        catalog.put("Healthcare & Medical|Health Monitors", List.of("Blood Pressure Monitor", "Pulse Oximeter", "Digital Thermometer", "Glucometer", "Heart Rate Monitor", "ECG Machine", "Fetal Doppler", "Body Fat Analyzer", "Spirometer", "Cholesterol Meter"));
        catalog.put("Healthcare & Medical|Orthopedic Supports", List.of("Knee Brace", "Ankle Support", "Wrist Brace", "Elbow Splint", "Back Support Belt", "Neck Collar", "Shoulder Immobilizer", "Thumb Spica", "Clavicle Brace", "Posture Corrector"));
        catalog.put("Healthcare & Medical|Dental Supplies", List.of("Dental Mirror", "Dental Explorer", "Periodontal Probe", "Dental Forceps", "Dental Syringe", "Impression Trays", "Dental Cement", "Prophylaxis Paste", "Dental Floss", "Fluoride Varnish"));
        catalog.put("Healthcare & Medical|Nutritional Supplements", List.of("Multivitamin Tablets", "Protein Powder", "Vitamin C Capsules", "Calcium Supplements", "Iron Tablets", "Omega-3 Fish Oil", "Probiotics", "Collagen Peptides", "Zinc Supplements", "Vitamin D Drops"));

        // 7. BEAUTY & PERSONAL CARE
        catalog.put("Beauty & Personal Care|Skincare", List.of("Facial Cleanser", "Skin Toner", "Moisturizing Cream", "Face Serum", "Eye Cream", "Sunscreen Lotion", "Exfoliating Scrub", "Face Mask", "Acne Treatment", "Anti-aging Cream"));
        catalog.put("Beauty & Personal Care|Haircare", List.of("Daily Shampoo", "Hair Conditioner", "Hair Mask", "Hair Oil", "Leave-in Serum", "Dry Shampoo", "Hair Spray", "Heat Protectant", "Hair Styling Gel", "Scalp Treatment"));
        catalog.put("Beauty & Personal Care|Makeup & Cosmetics", List.of("Liquid Foundation", "Concealer", "Face Powder", "Blush", "Bronzer", "Eyeshadow Palette", "Eyeliner", "Mascara", "Lipstick", "Lip Gloss"));
        catalog.put("Beauty & Personal Care|Fragrances & Perfumes", List.of("Eau de Parfum", "Eau de Toilette", "Body Mist", "Cologne", "Solid Perfume", "Roll-on Perfume", "Hair Mist", "Scented Oil", "Attar", "Deodorant Spray"));
        catalog.put("Beauty & Personal Care|Bath & Body", List.of("Body Wash", "Bar Soap", "Bath Bombs", "Bath Salts", "Body Scrub", "Body Butter", "Body Lotion", "Bubble Bath", "Shower Oil", "Loofah Sponge"));
        catalog.put("Beauty & Personal Care|Men's Grooming", List.of("Shaving Cream", "Aftershave Lotion", "Beard Oil", "Beard Balm", "Men's Face Wash", "Hair Pomade", "Moustache Wax", "Safety Razor", "Shaving Brush", "Styling Clay"));
        catalog.put("Beauty & Personal Care|Organic & Herbal Care", List.of("Aloe Vera Gel", "Neem Face Wash", "Turmeric Cream", "Sandalwood Soap", "Rose Water Toner", "Herbal Hair Powder", "Organic Lip Balm", "Natural Deodorant", "Essential Oil Blend", "Ayurvedic Massage Oil"));
        catalog.put("Beauty & Personal Care|Beauty Tools", List.of("Makeup Brushes", "Beauty Sponge", "Eyelash Curler", "Tweezers", "Nail Clippers", "Facial Roller", "Gua Sha Stone", "Pumice Stone", "Cuticle Pusher", "Hairbrush"));

        // 8. CONSTRUCTION & BUILDING
        catalog.put("Construction & Building|Cement & Concrete", List.of("Portland Cement", "White Cement", "Rapid Hardening Cement", "Waterproof Cement", "Ready Mix Concrete", "Concrete Blocks", "Precast Concrete", "Cement Mortar", "Concrete Sealer", "Self-leveling Concrete"));
        catalog.put("Construction & Building|Bricks & Blocks", List.of("Red Clay Bricks", "Fly Ash Bricks", "Concrete Hollow Blocks", "Autoclaved Aerated Blocks", "Interlocking Bricks", "Fire Bricks", "Glass Blocks", "Sand Lime Bricks", "Paving Blocks", "Terracotta Bricks"));
        catalog.put("Construction & Building|Steel & TMT Bars", List.of("TMT Rebars", "Mild Steel Bars", "Carbon Steel Bars", "Stainless Steel Rods", "Steel I-Beams", "Steel Channels", "Steel Angles", "Welded Wire Mesh", "Steel Binding Wire", "Galvanized Steel Pipes"));
        catalog.put("Construction & Building|Tiles & Sanitaryware", List.of("Ceramic Floor Tiles", "Porcelain Wall Tiles", "Vitrified Tiles", "Mosaic Tiles", "Western Commode", "Wash Basin", "Urinal", "Bidet", "Bathtub", "Shower Tray"));
        catalog.put("Construction & Building|Doors & Windows", List.of("Solid Wood Door", "Flush Door", "UPVC Window", "Aluminum Window", "Sliding Glass Door", "Louvered Window", "Steel Door", "Fiberglass Door", "Casement Window", "Skylight Window"));
        catalog.put("Construction & Building|Roofing Materials", List.of("Clay Roof Tiles", "Concrete Roof Tiles", "Asphalt Shingles", "Corrugated Metal Sheets", "Polycarbonate Sheets", "Fiberglass Roofing", "Slate Tiles", "Roofing Felt", "Roof Flashing", "Roof Sealant"));
        catalog.put("Construction & Building|Plywood & Boards", List.of("Commercial Plywood", "Marine Plywood", "BWR Grade Plywood", "MDF Board", "Particle Board", "Block Board", "Laminates", "Veneer Sheets", "Gypsum Board", "Cement Board"));
        catalog.put("Construction & Building|Pipes & Fittings", List.of("PVC Pipes", "CPVC Pipes", "UPVC Pipes", "Galvanized Iron Pipes", "Copper Pipes", "Pipe Elbows", "Pipe Tees", "Pipe Reducers", "Pipe Couplings", "Pipe Valves"));

        // 9. ELECTRICAL & LIGHTING
        catalog.put("Electrical & Lighting|LED Lights", List.of("LED Bulb", "LED Tube Light", "LED Panel Light", "LED Strip Light", "LED Flood Light", "LED Street Light", "LED High Bay Light", "LED Track Light", "LED Downlight", "LED Spotlight"));
        catalog.put("Electrical & Lighting|Wires & Cables", List.of("House Wire", "Submersible Cable", "Coaxial Cable", "Ethernet Cable", "Fiber Optic Cable", "Armoured Cable", "Telephone Cable", "Speaker Wire", "Control Cable", "Instrumentation Cable"));
        catalog.put("Electrical & Lighting|Switches & Sockets", List.of("One Way Switch", "Two Way Switch", "Bell Push Switch", "Dimmer Switch", "Power Socket", "USB Wall Socket", "Data Socket", "TV Socket", "Blanking Plate", "Switch Cover Plate"));
        catalog.put("Electrical & Lighting|MCBs & Distribution Boards", List.of("Single Pole MCB", "Double Pole MCB", "Triple Pole MCB", "Four Pole MCB", "Residual Current Circuit Breaker", "Earth Leakage Circuit Breaker", "Isolator Switch", "Distribution Board Enclosure", "Busbar Chamber", "Surge Protection Device"));
        catalog.put("Electrical & Lighting|Solar Panels", List.of("Monocrystalline Solar Panel", "Polycrystalline Solar Panel", "Thin Film Solar Panel", "Flexible Solar Panel", "Bifacial Solar Panel", "Portable Solar Panel", "Solar Roof Tiles", "Solar Panel Mounting Kit", "Solar Junction Box", "Solar Charge Controller"));
        catalog.put("Electrical & Lighting|Inverters & UPS", List.of("Sine Wave Inverter", "Square Wave Inverter", "Solar Inverter", "Grid Tie Inverter", "Off Grid Inverter", "Line Interactive UPS", "Online UPS", "Offline UPS", "Inverter Battery", "UPS Battery"));
        catalog.put("Electrical & Lighting|Transformers", List.of("Step Up Transformer", "Step Down Transformer", "Isolation Transformer", "Auto Transformer", "Current Transformer", "Voltage Transformer", "Distribution Transformer", "Power Transformer", "Toroidal Transformer", "Audio Transformer"));
        catalog.put("Electrical & Lighting|Smart Home Devices", List.of("Smart Bulb", "Smart Plug", "Smart Switch", "Smart Thermostat", "Smart Door Lock", "Smart Doorbell", "Smart Security Camera", "Smart Motion Sensor", "Smart Smoke Detector", "Smart Hub"));

        // 10. BOOKS & STATIONERY
        catalog.put("Books & Stationery|Textbooks", List.of("Mathematics Textbook", "Science Textbook", "History Textbook", "Geography Textbook", "Literature Anthology", "Language Grammar Book", "Physics Textbook", "Chemistry Textbook", "Biology Textbook", "Economics Textbook"));
        catalog.put("Books & Stationery|Notebooks & Diaries", List.of("Spiral Notebook", "Composition Notebook", "Subject Notebook", "Hardcover Diary", "Leather Bound Journal", "Pocket Notepad", "Sketchbook", "Grid Notebook", "Dotted Notebook", "Executive Planner"));
        catalog.put("Books & Stationery|Art & Craft Supplies", List.of("Acrylic Paint Set", "Watercolor Paint", "Oil Paint", "Paint Brushes", "Canvas Boards", "Drawing Pencils", "Charcoal Sticks", "Pastel Colors", "Craft Glue", "Origami Paper"));
        catalog.put("Books & Stationery|Office Stationery", List.of("Ballpoint Pens", "Rollerball Pens", "Highlighters", "Permanent Markers", "Whiteboard Markers", "Sticky Notes", "Paper Clips", "Stapler", "Hole Punch", "Correction Fluid"));
        catalog.put("Books & Stationery|Children's Books", List.of("Picture Book", "Board Book", "Pop-up Book", "Fairy Tale Book", "Nursery Rhymes", "Alphabet Book", "Counting Book", "Coloring Book", "Activity Book", "Bedtime Story Book"));
        catalog.put("Books & Stationery|Religious Books", List.of("Holy Bible", "Quran", "Bhagavad Gita", "Torah", "Tripitaka", "Vedas", "Upanishads", "Prayer Book", "Hymnal", "Spiritual Devotional"));
        catalog.put("Books & Stationery|Calendars & Planners", List.of("Wall Calendar", "Desk Calendar", "Pocket Calendar", "Daily Planner", "Weekly Planner", "Monthly Planner", "Academic Planner", "Financial Planner", "Undated Planner", "Magnetic Fridge Calendar"));
        catalog.put("Books & Stationery|Filing Supplies", List.of("Manila Folders", "Hanging Folders", "File Labels", "Ring Binders", "Lever Arch Files", "Document Trays", "Magazine Files", "Sheet Protectors", "Index Dividers", "Expanding Files"));

        // 11. MOBILE & ACCESSORIES
        catalog.put("Mobile & Accessories|Smartphones", List.of("Android Smartphone", "iOS Smartphone", "Foldable Smartphone", "Flip Smartphone", "Gaming Smartphone", "Rugged Smartphone", "Camera Centric Phone", "Budget Smartphone", "Mid-range Smartphone", "Flagship Smartphone"));
        catalog.put("Mobile & Accessories|Mobile Cases & Covers", List.of("Silicone Case", "Hard Plastic Case", "Leather Wallet Case", "Rugged Armor Case", "Clear Transparent Case", "Bumper Case", "Battery Case", "Waterproof Pouch", "Flip Cover", "Armband Case"));
        catalog.put("Mobile & Accessories|Screen Protectors", List.of("Tempered Glass Protector", "PET Plastic Protector", "TPU Screen Protector", "Privacy Screen Protector", "Anti-Glare Protector", "Liquid Screen Protector", "Matte Screen Protector", "Hydrogel Protector", "Ceramic Film Protector", "Camera Lens Protector"));
        catalog.put("Mobile & Accessories|Chargers & Cables", List.of("Wall Charger Adapter", "Car Charger", "Wireless Charging Pad", "Wireless Charging Stand", "USB-C Cable", "Lightning Cable", "Micro USB Cable", "Multi-pin Charging Cable", "Magnetic Charging Cable", "Fast Charging Adapter"));
        catalog.put("Mobile & Accessories|Power Banks", List.of("10000mAh Power Bank", "20000mAh Power Bank", "Solar Power Bank", "Wireless Power Bank", "Mini Pocket Power Bank", "Laptop Power Bank", "Rugged Power Bank", "Fast Charging Power Bank", "Magnetic Power Bank", "High Capacity Power Bank"));
        catalog.put("Mobile & Accessories|Bluetooth Headsets", List.of("Mono Bluetooth Headset", "Stereo Bluetooth Headset", "Neckband Bluetooth", "True Wireless Earbuds", "Over-Ear Bluetooth Headphones", "On-Ear Bluetooth Headphones", "Sports Bluetooth Headset", "Noise Cancelling Headset", "Gaming Bluetooth Headset", "Bone Conduction Headset"));
        catalog.put("Mobile & Accessories|Mobile Repair Parts", List.of("Replacement LCD Screen", "Replacement Battery", "Charging Port Flex Cable", "Camera Module", "Earpiece Speaker", "Loudspeaker Module", "Vibration Motor", "Volume Button Flex", "Power Button Flex", "Back Glass Cover"));
        catalog.put("Mobile & Accessories|Selfie Sticks & Tripods", List.of("Extendable Selfie Stick", "Bluetooth Selfie Stick", "Tripod Stand", "Mini Flexible Tripod", "Ring Light Tripod", "Gimbal Stabilizer", "Phone Mount Holder", "Camera Remote Shutter", "Desk Stand Holder", "Car Mount Holder"));

        // 12. SPORTS & FITNESS
        catalog.put("Sports & Fitness|Gym Equipment", List.of("Treadmill", "Elliptical Machine", "Rowing Machine", "Exercise Bike", "Dumbbell Set", "Kettlebells", "Barbell Weights", "Weight Bench", "Squat Rack", "Pull-up Bar"));
        catalog.put("Sports & Fitness|Yoga Accessories", List.of("Yoga Mat", "Yoga Blocks", "Yoga Strap", "Yoga Wheel", "Resistance Bands", "Pilates Ball", "Meditation Cushion", "Stretching Rope", "Foam Roller", "Yoga Towel"));
        catalog.put("Sports & Fitness|Team Sports", List.of("Soccer Ball", "Basketball", "Volleyball", "Rugby Ball", "Baseballs", "Cricket Bat", "Cricket Ball", "Hockey Stick", "Hockey Ball", "Team Pinnies"));
        catalog.put("Sports & Fitness|Racquet Sports", List.of("Tennis Racquet", "Badminton Racquet", "Squash Racquet", "Table Tennis Racquet", "Tennis Balls", "Badminton Shuttlecocks", "Squash Balls", "Table Tennis Balls", "Racquet Grip Tape", "Racquet Bag"));
        catalog.put("Sports & Fitness|Cycling", List.of("Mountain Bike", "Road Bicycle", "Hybrid Bike", "BMX Bike", "Cycling Helmet", "Bicycle Pump", "Bike Lights", "Cycling Gloves", "Bike Lock", "Cycling Water Bottle"));
        catalog.put("Sports & Fitness|Swimming Gear", List.of("Swimming Goggles", "Swim Cap", "Swimsuits", "Swim Trunks", "Kickboard", "Pull Buoy", "Swim Fins", "Nose Clip", "Ear Plugs", "Swimming Towel"));
        catalog.put("Sports & Fitness|Athletic Wear", List.of("Running Shorts", "Track Pants", "Sports T-Shirt", "Athletic Tank Top", "Compression Leggings", "Sports Bra", "Athletic Socks", "Running Shoes", "Training Jacket", "Sweatband"));
        catalog.put("Sports & Fitness|Camping & Hiking", List.of("Camping Tent", "Sleeping Bag", "Hiking Backpack", "Trekking Poles", "Camping Stove", "Portable Lantern", "Pocket Knife", "Compass", "Hydration Pack", "Hiking Boots"));

        return catalog;
    }
}