package com.ist.internal_issue_tracker;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

  ApplicationModules modules = ApplicationModules.of(InternalIssueTrackerApplication.class);

  @Test
  void verifiesModularStructure() {
    modules.verify();
  }

  @Test
  void writeDocumentationSnippets() {
    new Documenter(modules).writeDocumentation().writeIndividualModulesAsPlantUml();
  }
}
