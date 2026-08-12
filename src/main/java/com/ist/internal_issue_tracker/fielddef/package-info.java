/**
 * Owns {@code field_definitions} - the user-defined values behind the eight classification points
 * ({@link com.ist.internal_issue_tracker.shared.port.FieldKind}) that used to be fixed Java enums.
 *
 * <p>No other module may depend on this one directly; that would widen the star-shaped dependency
 * graph {@code shared/package-info.java} describes into a second hub. Every other module reaches
 * this one through {@link com.ist.internal_issue_tracker.shared.port.FieldDefinitionLookup} (read)
 * and {@link com.ist.internal_issue_tracker.shared.port.FieldDefinitionProvisioning} (the one
 * write port in {@code shared.port}), both implemented here as adapters - the same seam {@code
 * ProjectLookup}/{@code ProjectLookupAdapter} already establishes.
 */
@ApplicationModule(allowedDependencies = "shared")
package com.ist.internal_issue_tracker.fielddef;

import org.springframework.modulith.ApplicationModule;
