package org.oagi.score.gateway.http.api.bie_management.model.bie_package;

import java.util.Collection;
import java.util.List;

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
