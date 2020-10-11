package com.xobotun.vtb;

import java.util.Map;

public class HardcodedPriceService {
    private static String NO_PRICE = "Я не знаю её цену";

    private static Map<String, Integer> PRICES = Map.ofEntries(
            Map.entry("Mazda 6", 2262492),
            Map.entry("Mazda 3", null),
            Map.entry("Cadillac ESCALADE", 2733100),
            Map.entry("Jaguar F-PACE", 3798000),
            Map.entry("BMW 5", 3800000),
            Map.entry("KIA Sportage", 1724900),
            Map.entry("Chevrolet Tahoe", 4778100),
            Map.entry("KIA K5", 1974900),
            Map.entry("Hyundai Genesis", null),
            Map.entry("Toyota Camry", null),
            Map.entry("Mercedes A", null),
            Map.entry("Land Rover RANGE ROVER VELAR", 5433000),
            Map.entry("BMW 3", 2929300),
            Map.entry("KIA Optima", 1844922)
    );

    public static String get(String mark) {
        Integer price = PRICES.get(mark);

        return price == null ? NO_PRICE : "Такая машина стоит от " + price.toString() + " рублей";
    }
}
