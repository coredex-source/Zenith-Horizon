package io.canvasmc.horizon.fabric;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.fabricmc.loader.impl.metadata.DependencyOverrides;
import net.fabricmc.loader.impl.metadata.ModMetadataParser;
import net.fabricmc.loader.impl.metadata.ParseMetadataException;
import net.fabricmc.loader.impl.metadata.VersionOverrides;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record PaperModMetadata(Version version, Map<String, CustomValue> customValues) implements ModMetadata {
    public static final String ID = "paper";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static @NonNull PaperModMetadata create(@NonNull String version, @NonNull ObjectNode customValues, @NonNull Path configDirectory) {
        ObjectNode declaration = MAPPER.createObjectNode()
            .put("schemaVersion", 1)
            .put("id", ID)
            .put("version", version);
        declaration.set("custom", customValues);

        try {
            ModMetadata declared = ModMetadataParser.parseMetadata(
                new ByteArrayInputStream(MAPPER.writeValueAsBytes(declaration)), ID, List.of(),
                new VersionOverrides(), new DependencyOverrides(configDirectory), false
            );
            return new PaperModMetadata(Version.parse(version), Map.copyOf(declared.getCustomValues()));
        } catch (IOException | ParseMetadataException | VersionParsingException exception) {
            throw new RuntimeException("Couldn't create the builtin Paper mod", exception);
        }
    }

    @Override
    public String getType() {
        return "builtin";
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Collection<String> getProvides() {
        return List.of();
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public ModEnvironment getEnvironment() {
        return ModEnvironment.UNIVERSAL;
    }

    @Override
    public Collection<ModDependency> getDependencies() {
        return List.of();
    }

    @Override
    public String getName() {
        return "Paper";
    }

    @Override
    public String getDescription() {
        return "The Paper server Horizon runs on";
    }

    @Override
    public Collection<Person> getAuthors() {
        return List.of();
    }

    @Override
    public Collection<Person> getContributors() {
        return List.of();
    }

    @Override
    public ContactInformation getContact() {
        return ContactInformation.EMPTY;
    }

    @Override
    public Collection<String> getLicense() {
        return List.of();
    }

    @Override
    public Optional<String> getIconPath(int size) {
        return Optional.empty();
    }

    @Override
    public boolean containsCustomValue(String key) {
        return customValues.containsKey(key);
    }

    @Override
    public CustomValue getCustomValue(String key) {
        return customValues.get(key);
    }

    @Override
    public Map<String, CustomValue> getCustomValues() {
        return customValues;
    }

    @Override
    @Deprecated
    public boolean containsCustomElement(String key) {
        return containsCustomValue(key);
    }
}
