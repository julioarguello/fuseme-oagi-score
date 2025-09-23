package org.oagi.score.gateway.http.common.util;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SemanticVersion {

    private static final Pattern FULL_SEMVER_PATTERN = Pattern.compile(
            "^(.*?)(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?" +        // major, minor, patch
                    "(?:-([0-9A-Za-z.-]+))?" +                          // pre-release
                    "(?:\\+([0-9A-Za-z.-]+))?$"                         // build metadata
    );

    private final String prefix;
    private int major;
    private Integer minor;
    private Integer patch;
    private String preRelease;
    private String buildMetadata;

    public SemanticVersion(String version) {
        Matcher matcher = FULL_SEMVER_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid semantic version: " + version);
        }

        this.prefix = matcher.group(1) != null ? matcher.group(1) : "";
        this.major = Integer.parseInt(matcher.group(2));
        this.minor = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : null;
        this.patch = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : null;
        this.preRelease = matcher.group(5);
        this.buildMetadata = matcher.group(6);
    }

    public boolean hasPreRelease() {
        return preRelease != null;
    }

    public boolean hasBuildMetadata() {
        return buildMetadata != null;
    }

    /**
     * Increment version.
     *
     * @param promote whether to promote pre-release (alpha → beta → rc → release)
     */
    public SemanticVersion increment(boolean promote) {
        if (preRelease != null) {
            if (promote) {
                this.preRelease = promotePreRelease(preRelease);
            } else {
                this.preRelease = incrementPreRelease(preRelease);
            }
            return this;
        }

        // No pre-release → bump numbers
        if (patch != null) {
            patch++;
        } else if (minor != null) {
            minor++;
        } else {
            major++;
        }

        return this;
    }

    private String promotePreRelease(String pre) {
        if (pre.startsWith("alpha")) {
            return "beta";
        } else if (pre.startsWith("beta")) {
            return "rc";
        } else if (pre.startsWith("rc")) {
            return null; // release → drop pre-release
        }
        return pre; // unknown → leave unchanged
    }

    private String incrementPreRelease(String pre) {
        String[] parts = pre.split("\\.");
        String last = parts[parts.length - 1];

        if (last.matches("\\d+")) {
            int num = Integer.parseInt(last) + 1;
            parts[parts.length - 1] = String.valueOf(num);
        } else {
            // text → append ".1"
            parts = Arrays.copyOf(parts, parts.length + 1);
            parts[parts.length - 1] = "1";
        }

        return String.join(".", parts);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.append(major);
        if (minor != null) {
            sb.append(".").append(minor);
        }
        if (patch != null) {
            sb.append(".").append(patch);
        }
        if (preRelease != null) {
            sb.append("-").append(preRelease);
        }
        if (buildMetadata != null) {
            sb.append("+").append(buildMetadata);
        }
        return sb.toString();
    }

}
