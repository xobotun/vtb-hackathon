package com.xobotun.vtb;

import java.util.Map;

public class HardcodedPriceService {
    private static String NO_PRICE = "Я не знаю её цену";

    private static Map<String, String> PRICES = Map.ofEntries(
            Map.entry("Mazda 6", "2262492"),
            Map.entry("Mazda 3", NO_PRICE),
            Map.entry("Cadillac ESCALADE", "2733100"),
            Map.entry("Jaguar F-PACE", "3798000"),
            Map.entry("BMW 5", "3800000"),
            Map.entry("KIA Sportage", "1724900"),
            Map.entry("Chevrolet Tahoe", "4778100"),
            Map.entry("KIA K5", "1974900"),
            Map.entry("Hyundai Genesis", NO_PRICE),
            Map.entry("Toyota Camry", NO_PRICE),
            Map.entry("Mercedes A", NO_PRICE),
            Map.entry("Land Rover RANGE ROVER VELAR", "5433000"),
            Map.entry("BMW 3", "2929300"),
            Map.entry("KIA Optima", "1844922")
    );

    public static String get(String mark) {
        String price = PRICES.get(mark);

        return price == NO_PRICE ? NO_PRICE : "Такая машина стоит от " + price + " рублей. И они у нас есть в наличии [тут](https://developer.hackathon.vtb.ru/vtb/hackathon/)!";
    }
}
