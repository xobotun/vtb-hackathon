package com.xobotun.vtb;

import lombok.SneakyThrows;
import lombok.val;

import java.util.Properties;

public class PropertiesHelper {
    public static String HTTP_PORT = "http.port";
    public static String PROXY_HOST = "proxy.host";
    public static String PROXY_PORT = "proxy.port";
    public static String PROXY_LOGIN = "proxy.login";
    public static String PROXY_PASSWORD = "proxy.password";
    public static String TELEGRAM_APIKEY = "telegram.apikey";
    public static String VTB_APIKEY = "vtb.apikey";

    private static Properties props;

    static {
        init();
    }

    @SneakyThrows
    private static void init() {
        props = new Properties();

        val resource = PropertiesHelper.class.getClassLoader().getResourceAsStream("application.properties");

        props.load(resource);
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}
