package com.configcat;

import java.util.Map;

/**
 * Describes a data source builder.
 */
public class OverrideDataSourceBuilder {
    private String filePath;
    private boolean isResource;
    private boolean autoReload;
    private Map<String, Object> map;

    private OverrideDataSourceBuilder(String filePath, boolean isResource, boolean autoReload) {
        this.filePath = filePath;
        this.isResource = isResource;
        this.autoReload = autoReload;
    }

    private OverrideDataSourceBuilder(Map<String, Object> map) {
        this.map = map;
    }

    OverrideDataSource build(ConfigCatLogger logger) {
        if (this.map != null) {
            return new LocalMapDataSource(this.map);
        }

        if (this.filePath != null && !this.filePath.isEmpty()) {
            return new LocalFileDataSource(this.filePath, this.isResource, logger, this.autoReload);
        }

        return new OverrideDataSource();
    }

    /**
     * Creates an override data source builder that describes a local file data source.
     *
     * @param filePath   path to the file.
     * @param autoReload when it's true, the file will be reloaded when it gets modified.
     * @return the builder.
     */
    public static OverrideDataSourceBuilder localFile(String filePath, boolean autoReload) {
        return new OverrideDataSourceBuilder(filePath, false, autoReload);
    }

    /**
     * Creates an override data source builder that describes a classpath resource data source.
     *
     * @param resourceName name of the classpath resource.
     * @return the builder.
     */
    public static OverrideDataSourceBuilder classPathResource(String resourceName) {
        return new OverrideDataSourceBuilder(resourceName, true, false);
    }

    /**
     * Creates an override data source builder that describes a map data source.
     *
     * @param map map that contains the overrides.
     * @return the builder.
     */
    public static OverrideDataSourceBuilder map(Map<String, Object> map) {
        return new OverrideDataSourceBuilder(map);
    }
}
