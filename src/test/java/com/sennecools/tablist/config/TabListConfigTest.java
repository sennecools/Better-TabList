package com.sennecools.tablist.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class TabListConfigTest {

    @TempDir
    Path directory;

    @Test
    void freshConfigPreservesFtbDefaultAndOriginalAppearance() throws Exception {
        Path config = directory.resolve("tablist.toml");
        TabListConfig.load(config);

        assertEquals(NameFormattingProvider.FTB, TabListConfig.nameFormattingProvider);
        assertEquals("{name} &7#AFK", TabListConfig.displayNameFormat);
        assertFalse(TabListConfig.useSparkMetrics);
    }

    @Test
    void legacyConfigMigratesWithoutOverwritingNameFormat() throws Exception {
        Path config = directory.resolve("tablist.toml");
        Files.writeString(config, """
                [appearance]
                display_name_format = "&6{name} &7#AFK"
                [ftbranks]
                enable_ftbranks_formatting = false
                """);
        TabListConfig.load(config);

        assertEquals(NameFormattingProvider.NONE, TabListConfig.nameFormattingProvider);
        assertEquals("&6{name} &7#AFK", TabListConfig.displayNameFormat);
        assertTrue(Files.readString(config).contains("enable_ftbranks_formatting = false"));
        TabListConfig.load(config);
        assertEquals(NameFormattingProvider.NONE, TabListConfig.nameFormattingProvider);
    }

    @Test
    void explicitLuckPermsProviderOverridesLegacySettingAndSupportsReload() throws Exception {
        Path config = directory.resolve("tablist.toml");
        Files.writeString(config, """
                [appearance]
                name_formatting_provider = "lp"
                display_name_format = "{prefix}{name}{suffix}"
                [ftbranks]
                enable_ftbranks_formatting = true
                [performance]
                metrics_provider = "spark"
                """);
        TabListConfig.load(config);
        assertEquals(NameFormattingProvider.LP, TabListConfig.nameFormattingProvider);
        assertTrue(TabListConfig.useSparkMetrics);

        Files.writeString(config, """
                [appearance]
                name_formatting_provider = "NONE"
                """);
        TabListConfig.load(config);
        assertEquals(NameFormattingProvider.NONE, TabListConfig.nameFormattingProvider);
        assertFalse(TabListConfig.useSparkMetrics);
    }

    @Test
    void invalidProviderFallsBackAndParsingIsLocaleIndependent() throws Exception {
        Path config = directory.resolve("tablist.toml");
        Files.writeString(config, """
                [appearance]
                name_formatting_provider = "unknown"
                [performance]
                metrics_provider = "unknown"
                """);
        TabListConfig.load(config);
        assertEquals(NameFormattingProvider.NONE, TabListConfig.nameFormattingProvider);
        assertFalse(TabListConfig.useSparkMetrics);

        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(NameFormattingProvider.LP, NameFormattingProvider.fromString(" lp "));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void generatedConfigExplainsUnitsProvidersAndAnimationWithMultilineFrames() throws Exception {
        Path config = directory.resolve("tablist.toml");
        TabListConfig.load(config);
        String generated = Files.readString(config);

        assertTrue(generated.contains("# Better TabList - config/tablist.toml"));
        assertTrue(generated.contains("/tablist reload"));
        assertTrue(generated.contains("MILLISECONDS"));
        assertTrue(generated.contains("SECONDS"));
        assertTrue(generated.contains("frames change every 2 seconds"));
        assertTrue(generated.contains("ftbranks.name_format overrides"));
        assertTrue(generated.contains("header = [\n") || generated.contains("header = [\r\n"));

        TabListConfig.load(config);
        assertEquals(generated, Files.readString(config), "Reloading a complete config must not rewrite it");
    }

    @Test
    void addsMissingGuidanceWhilePreservingCustomCommentsValuesAndExtraOptions() throws Exception {
        Path config = directory.resolve("tablist.toml");
        Files.writeString(config, """
                # My custom appearance notes
                [appearance]
                # Keep our branding
                server_name = "Community Server"
                header = ["&aFirst #NSecond", "&bAnother frame"]
                display_name_format = "{prefix}{name}{suffix}"
                name_formatting_provider = "LP"
                custom_option = "keep me"
                """);
        TabListConfig.load(config);

        assertEquals("Community Server", TabListConfig.serverName);
        assertEquals(java.util.List.of("&aFirst #NSecond", "&bAnother frame"), TabListConfig.headerFrames);
        assertEquals("{prefix}{name}{suffix}", TabListConfig.displayNameFormat);
        assertEquals(NameFormattingProvider.LP, TabListConfig.nameFormattingProvider);
        try (CommentedFileConfig loaded = CommentedFileConfig.builder(config).sync().build()) {
            loaded.load();
            assertEquals(" My custom appearance notes", loaded.getComment("appearance"));
            assertEquals(" Keep our branding", loaded.getComment("appearance.server_name"));
            assertEquals("keep me", loaded.get("appearance.custom_option"));
            assertTrue(loaded.getComment("appearance.header").contains("one animation frame"));
        }
    }
}
