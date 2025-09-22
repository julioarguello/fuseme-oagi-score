package org.oagi.score.gateway.http.api.bie_management.model.bie_package;

import java.util.List;

public record BieComponentChange(String componentName,
                                 String parentComponentPath,
                                 List<String> changes) {
}
