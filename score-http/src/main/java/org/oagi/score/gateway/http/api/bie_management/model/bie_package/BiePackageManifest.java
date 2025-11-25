package org.oagi.score.gateway.http.api.bie_management.model.bie_package;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collection;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record BiePackageManifest(String name,
                                 String versionId,
                                 String versionName,
                                 String priorPackageVersionId,
                                 Collection<BieManifestSummary> newBiesFromPriorPackageVersion,
                                 Collection<BieManifestSummary> removedBiesFromPriorPackageVersion,
                                 Collection<BieManifestSummary> changedBiesFromPriorPackageVersion,
                                 Collection<BieManifestSummary> deprecatedBiesFromPriorPackageVersion,
                                 Collection<LibraryCompatibility> libraryCompatibility,
                                 List<BieManifestEntry> bieList) {
}
