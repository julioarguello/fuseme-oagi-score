package org.oagi.score.gateway.http.api.bie_management.model.bie_package;

public record BiePackageMetadata(String name,
                                 String versionId,
                                 String versionName,
                                 String priorPackageVersionId,
                                 boolean newBiesFromPriorPackageVersion,
                                 boolean removedBiesFromPriorPackageVersion,
                                 boolean changedBiesFromPriorPackageVersion,
                                 String connectSpecMajorVersion,
                                 BieManifest bieManifest) {
}
