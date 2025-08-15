package org.oagi.score.gateway.http.api.bie_management.model.bie_package;

public record BieMetadata(String uuid,
                          String versionId,
                          String priorVersionUuidInPackage,
                          String den,
                          String propertyTerm,
                          boolean notInPriorPackageVersion,
                          boolean addedElementsFromPriorPackageVersion,
                          boolean removedElementsFromPriorPackageVersion,
                          boolean valueDomainChangeFromPriorPackageVersion,
                          boolean addedElementsReplaceExtensionFromPriorPackageVersion) {
}
