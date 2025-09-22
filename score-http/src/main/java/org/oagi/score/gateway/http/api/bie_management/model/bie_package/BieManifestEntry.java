package org.oagi.score.gateway.http.api.bie_management.model.bie_package;

import org.oagi.score.gateway.http.common.model.Guid;

import java.util.Collection;

public record BieManifestEntry(BieManifest bie,
                               Guid priorVersionUuidInPackage,
                               String priorVersionIdInPackage,
                               boolean includedInPriorPackageVersion,
                               Collection<BieComponentChange> addedComponentsFromPriorPackageVersion,
                               Collection<BieComponentChange> removedComponentsFromPriorPackageVersion,
                               Collection<BieComponentChange> changedComponentsFromPriorPackageVersion,
                               Collection<BieComponentChange> deprecatedComponentsFromPriorPackageVersion) {
}
