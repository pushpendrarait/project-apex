package com.projectapex.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Configures the primary HikariDataSource for Neon Serverless PostgreSQL
 * with automatic fallback to embedded H2 in PostgreSQL compatibility mode.
 */
@Configuration
public class NeonDataSourceConfig {
    private static final Logger log = LoggerFactory.getLogger(NeonDataSourceConfig.class);

    public static final String H2_POSTGRES_MODE_URL =
            "jdbc:h2:mem:apexdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    public static final String ENGINE_NEON_POSTGRES = "Neon PostgreSQL";
    public static final String ENGINE_H2_POSTGRES_MODE = "H2 PostgreSQL-Mode";

    private static final Pattern SSL_MODE_PATTERN =
            Pattern.compile("(?:^|&)sslmode=", Pattern.CASE_INSENSITIVE);

    private volatile String databaseEngine = ENGINE_H2_POSTGRES_MODE;
    private volatile boolean postgreSql = false;
    private volatile String sanitizedJdbcUrl = H2_POSTGRES_MODE_URL;

    public record ResolvedDataSourceSpec(
            String jdbcUrl,
            String username,
            String password,
            String driverClassName,
            String databaseEngine,
            boolean postgreSql
    ) {}

    @Bean
    public DataSource dataSource(Environment env) {
        String rawUrl = firstNonBlank(
                System.getenv("NEON_DATABASE_URL"),
                env.getProperty("NEON_DATABASE_URL"),
                System.getenv("DATABASE_URL"),
                env.getProperty("DATABASE_URL"),
                env.getProperty("spring.datasource.url")
        );

        String fallbackUser = firstNonBlank(
                System.getenv("PGUSER"),
                env.getProperty("PGUSER"),
                env.getProperty("spring.datasource.username")
        );
        String fallbackPass = firstNonBlank(
                System.getenv("PGPASSWORD"),
                env.getProperty("PGPASSWORD"),
                env.getProperty("spring.datasource.password")
        );

        ResolvedDataSourceSpec spec = resolveSpec(rawUrl, fallbackUser, fallbackPass);

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(spec.jdbcUrl());
        hikari.setDriverClassName(spec.driverClassName());
        if (spec.username() != null) {
            hikari.setUsername(spec.username());
        }
        if (spec.password() != null) {
            hikari.setPassword(spec.password());
        }

        // Neon Free Tier friendly HikariCP settings
        hikari.setMaximumPoolSize(5);
        hikari.setMinimumIdle(1);
        hikari.setIdleTimeout(300_000L);
        hikari.setMaxLifetime(600_000L);
        hikari.setConnectionTimeout(30_000L);
        hikari.setPoolName(spec.postgreSql() ? "ApexNeonHikariPool" : "ApexH2FallbackPool");

        this.databaseEngine = spec.databaseEngine();
        this.postgreSql = spec.postgreSql();
        this.sanitizedJdbcUrl = sanitizeUrlForLogging(spec.jdbcUrl());

        log.info("Configured DataSource engine='{}', pool='{}', url='{}'",
                this.databaseEngine, hikari.getPoolName(), this.sanitizedJdbcUrl);

        return new HikariDataSource(hikari);
    }

    public static ResolvedDataSourceSpec resolveSpec(String rawUrl, String fallbackUser, String fallbackPass) {
        if (!hasText(rawUrl)) {
            return h2FallbackSpec();
        }

        String trimmed = rawUrl.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        if (lower.startsWith("postgresql://") || lower.startsWith("postgres://")) {
            return parseNeonPostgresUri(trimmed, fallbackUser, fallbackPass);
        }

        if (lower.startsWith("jdbc:postgresql://")) {
            String normalized = "jdbc:postgresql://" + trimmed.substring("jdbc:postgresql://".length());
            return new ResolvedDataSourceSpec(
                    normalized,
                    hasText(fallbackUser) ? fallbackUser.trim() : null,
                    hasText(fallbackPass) ? fallbackPass : null,
                    "org.postgresql.Driver",
                    ENGINE_NEON_POSTGRES,
                    true
            );
        }

        if (lower.startsWith("jdbc:h2:")) {
            return new ResolvedDataSourceSpec(
                    trimmed,
                    hasText(fallbackUser) ? fallbackUser.trim() : "sa",
                    fallbackPass != null ? fallbackPass : "",
                    "org.h2.Driver",
                    ENGINE_H2_POSTGRES_MODE,
                    false
            );
        }

        log.warn("Unrecognized database URL scheme; falling back to embedded H2 PostgreSQL-Mode");
        return h2FallbackSpec();
    }

    private static ResolvedDataSourceSpec parseNeonPostgresUri(String rawUrl, String fallbackUser, String fallbackPass) {
        try {
            URI uri = new URI(rawUrl);
            String host = uri.getHost();
            if (!hasText(host)) {
                throw new IllegalArgumentException("Missing host in PostgreSQL URI");
            }

            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath();
            if (!hasText(path) || "/".equals(path)) {
                path = "/neondb";
            }

            String username = hasText(fallbackUser) ? fallbackUser.trim() : "";
            String password = hasText(fallbackPass) ? fallbackPass : "";

            String rawUserInfo = uri.getRawUserInfo();
            if (hasText(rawUserInfo)) {
                int colonIdx = rawUserInfo.indexOf(':');
                if (colonIdx >= 0) {
                    username = decodeUriComponent(rawUserInfo.substring(0, colonIdx));
                    password = decodeUriComponent(rawUserInfo.substring(colonIdx + 1));
                } else {
                    username = decodeUriComponent(rawUserInfo);
                }
            }

            String rawQuery = uri.getRawQuery();
            String query;
            if (!hasText(rawQuery)) {
                query = "sslmode=require";
            } else if (!SSL_MODE_PATTERN.matcher(rawQuery).find()) {
                query = rawQuery + "&sslmode=require";
            } else {
                query = rawQuery;
            }

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + "?" + query;
            return new ResolvedDataSourceSpec(
                    jdbcUrl,
                    username,
                    password,
                    "org.postgresql.Driver",
                    ENGINE_NEON_POSTGRES,
                    true
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse PostgreSQL connection URI: " + e.getMessage(), e);
        }
    }

    private static ResolvedDataSourceSpec h2FallbackSpec() {
        return new ResolvedDataSourceSpec(
                H2_POSTGRES_MODE_URL,
                "sa",
                "",
                "org.h2.Driver",
                ENGINE_H2_POSTGRES_MODE,
                false
        );
    }

    private static String decodeUriComponent(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static String sanitizeUrlForLogging(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "";
        }
        int qIdx = jdbcUrl.indexOf('?');
        return qIdx >= 0 ? jdbcUrl.substring(0, qIdx) + "?..." : jdbcUrl;
    }

    private static String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (hasText(c)) {
                return c.trim();
            }
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public String getDatabaseEngine() {
        return databaseEngine;
    }

    public boolean isPostgreSql() {
        return postgreSql;
    }

    public String getSanitizedJdbcUrl() {
        return sanitizedJdbcUrl;
    }
}
