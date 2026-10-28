package ru.antenatal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String publicBaseUrl = "http://localhost:8080";
    private String clinicTimezone = "Europe/Moscow";
    private int linkTokenTtlMinutes = 15;
    private boolean devMode = false;
    private String adminEmail = "";
    private String adminPassword = "";
    private Bot bot = new Bot();
    private Max max = new Max();

    @Getter
    @Setter
    public static class Bot {
        private String token = "";
        private String username = "";
        private String webhookSecret = "";
        private long initDataMaxAgeSeconds = 3600;
    }

    @Getter
    @Setter
    public static class Max {
        private String token = "";
        private String username = "";
        private String webhookSecret = "";
        private String apiBaseUrl = "https://platform-api2.max.ru";
    }
}
