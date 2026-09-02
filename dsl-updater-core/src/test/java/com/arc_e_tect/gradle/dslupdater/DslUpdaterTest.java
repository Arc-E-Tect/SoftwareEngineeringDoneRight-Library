package com.arc_e_tect.gradle.dslupdater;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DslUpdaterTest {

    private static final DslExtensionSchema TRACKER_LENS_SCHEMA = new DslExtensionSchema("trackerLens", List.of(
            DslPropertySpec.scalar("defaultLens", "\"light-lens\"", "The lens id active on first load."),
            DslPropertySpec.scalar("dashboardName", "\"${project.name} Lens\"", "The dashboard's displayed name."),
            DslPropertySpec.container("trackers", "At least one tracker must be registered.",
                    "// register('bdd-scenarios') {\n"
                            + "//     historyFiles.from(file('gherkin-progress-history.ndjson'))\n"
                            + "// }")
    ));

    @Test
    void missingBlockWithoutGenerateDslIsANoOp() {
        String source = "plugins {\n    id 'com.arc-e-tect.tracker-lens'\n}\n";

        DslUpdater.Outcome outcome = DslUpdater.update(source, TRACKER_LENS_SCHEMA, UpdateDslOptions.defaults());

        assertThat(outcome.source()).isEqualTo(source);
        assertThat(outcome.result().blockFoundBefore()).isFalse();
        assertThat(outcome.result().blockGenerated()).isFalse();
        assertThat(outcome.result().addedProperties()).isEmpty();
        assertThat(outcome.result().changed()).isFalse();
    }

    @Test
    void missingBlockWithGenerateDslIsAppendedAtEndOfFile() {
        String source = "plugins {\n    id 'com.arc-e-tect.tracker-lens'\n}\n";

        DslUpdater.Outcome outcome = DslUpdater.update(source, TRACKER_LENS_SCHEMA,
                new UpdateDslOptions(true, false));

        assertThat(outcome.result().blockFoundBefore()).isFalse();
        assertThat(outcome.result().blockGenerated()).isTrue();
        assertThat(outcome.result().addedProperties()).containsExactly("defaultLens", "dashboardName");
        assertThat(outcome.result().changed()).isTrue();

        String generated = outcome.source();
        assertThat(generated).startsWith(source);
        assertThat(generated).contains("trackerLens {");
        assertThat(generated).contains("// The lens id active on first load.");
        assertThat(generated).contains("defaultLens = \"light-lens\"");
        assertThat(generated).contains("dashboardName = \"${project.name} Lens\"");
        assertThat(generated).contains("trackers {");
        assertThat(generated).contains("register('bdd-scenarios')");
        assertThat(generated).endsWith("}\n");
    }

    @Test
    void generateDslWithCleanupOmitsAllComments() {
        String source = "plugins {\n    id 'com.arc-e-tect.tracker-lens'\n}\n";

        DslUpdater.Outcome outcome = DslUpdater.update(source, TRACKER_LENS_SCHEMA,
                new UpdateDslOptions(true, true));

        String generated = outcome.source();
        assertThat(generated).doesNotContain("//");
        assertThat(generated).contains("defaultLens = \"light-lens\"");
        assertThat(generated).contains("trackers {");
    }

    @Test
    void existingBlockOnlyGetsMissingScalarPropertiesAppended() {
        String source = "trackerLens {\n"
                + "    dashboardName = \"Checkout Service Lens\"\n"
                + "}\n";

        DslUpdater.Outcome outcome = DslUpdater.update(source, TRACKER_LENS_SCHEMA, UpdateDslOptions.defaults());

        assertThat(outcome.result().blockFoundBefore()).isTrue();
        assertThat(outcome.result().blockGenerated()).isFalse();
        assertThat(outcome.result().addedProperties()).containsExactly("defaultLens");
        assertThat(outcome.source()).contains("dashboardName = \"Checkout Service Lens\"");
        assertThat(outcome.source()).contains("defaultLens = \"light-lens\"");
        // trackers container is never auto-populated on an existing block.
        assertThat(outcome.source()).doesNotContain("register(");
    }

    @Test
    void existingAssignmentIsNeverTouchedEvenIfItDiffersFromDefault() {
        String source = "trackerLens {\n"
                + "    defaultLens = \"dark-lens\"\n"
                + "    dashboardName = \"Checkout Service Lens\"\n"
                + "}\n";

        DslUpdater.Outcome outcome = DslUpdater.update(source, TRACKER_LENS_SCHEMA, UpdateDslOptions.defaults());

        assertThat(outcome.result().addedProperties()).isEmpty();
        assertThat(outcome.result().changed()).isFalse();
        assertThat(outcome.source()).isEqualTo(source);
    }

    @Test
    void unrecognizedContentInsideTheBlockIsPreservedVerbatim() {
        String source = "trackerLens {\n"
                + "    dashboardName = \"Checkout Service Lens\"\n"
                + "    if (project.hasProperty('ci')) {\n"
                + "        outputDir = layout.buildDirectory.dir('ci-reports')\n"
                + "    }\n"
                + "}\n";

        DslUpdater.Outcome outcome = DslUpdater.update(source, TRACKER_LENS_SCHEMA, UpdateDslOptions.defaults());

        assertThat(outcome.source()).contains("if (project.hasProperty('ci')) {");
        assertThat(outcome.source()).contains("outputDir = layout.buildDirectory.dir('ci-reports')");
    }

    @Test
    void propertyNameNestedInsideAContainerBlockDoesNotCountAsConfiguredAtTopLevel() {
        String source = "trackerLens {\n"
                + "    trackers {\n"
                + "        register('bdd-scenarios') {\n"
                + "            dashboardName = 'not the real property'\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        DslUpdater.Outcome outcome = DslUpdater.update(source, TRACKER_LENS_SCHEMA, UpdateDslOptions.defaults());

        assertThat(outcome.result().addedProperties()).containsExactly("defaultLens", "dashboardName");
        // The added top-level dashboardName is distinct from the nested one inside trackers { }.
        assertThat(outcome.source()).contains("dashboardName = \"${project.name} Lens\"");
        assertThat(outcome.source()).contains("dashboardName = 'not the real property'");
    }

    @Test
    void propertyNameMentionedOnlyInACommentDoesNotCountAsConfigured() {
        String source = "trackerLens {\n"
                + "    // defaultLens = \"dark-lens\" (left disabled for now)\n"
                + "    dashboardName = \"Checkout Service Lens\"\n"
                + "}\n";

        DslUpdater.Outcome outcome = DslUpdater.update(source, TRACKER_LENS_SCHEMA, UpdateDslOptions.defaults());

        assertThat(outcome.result().addedProperties()).containsExactly("defaultLens");
        assertThat(outcome.source()).contains("defaultLens = \"light-lens\"");
    }

    @Test
    void gStringInterpolationBracesDoNotConfuseBlockMatching() {
        String source = "trackerLens {\n"
                + "    dashboardName = \"${project.name} - custom\"\n"
                + "}\n";

        DslUpdater.Outcome outcome = DslUpdater.update(source, TRACKER_LENS_SCHEMA, UpdateDslOptions.defaults());

        assertThat(outcome.result().addedProperties()).containsExactly("defaultLens");
        assertThat(outcome.source()).contains("dashboardName = \"${project.name} - custom\"");
        assertThat(outcome.source()).endsWith("}\n");
    }

    @Test
    void cleanupDslStripsPreExistingCommentsFromAnExistingBlock() {
        String source = "trackerLens {\n"
                + "    // why this value was chosen\n"
                + "    dashboardName = \"Checkout Service Lens\"\n"
                + "}\n";

        DslUpdater.Outcome outcome = DslUpdater.update(source, TRACKER_LENS_SCHEMA,
                new UpdateDslOptions(false, true));

        assertThat(outcome.result().cleaned()).isTrue();
        assertThat(outcome.source()).doesNotContain("//");
        assertThat(outcome.source()).contains("dashboardName = \"Checkout Service Lens\"");
        assertThat(outcome.source()).contains("defaultLens = \"light-lens\"");
    }

    @Test
    void cleanupDslOnAFullyConfiguredBlockWithNoCommentsIsANoOp() {
        String source = "trackerLens {\n"
                + "    defaultLens = \"dark-lens\"\n"
                + "    dashboardName = \"Checkout Service Lens\"\n"
                + "}\n";

        DslUpdater.Outcome outcome = DslUpdater.update(source, TRACKER_LENS_SCHEMA,
                new UpdateDslOptions(false, true));

        assertThat(outcome.result().cleaned()).isFalse();
        assertThat(outcome.result().changed()).isFalse();
        assertThat(outcome.source()).isEqualTo(source);
    }

    @Test
    void indentationOfInsertedLinesMatchesTheExistingBlock() {
        String source = "trackerLens {\n"
                + "  dashboardName = \"Checkout Service Lens\"\n"
                + "}\n";

        DslUpdater.Outcome outcome = DslUpdater.update(source, TRACKER_LENS_SCHEMA, UpdateDslOptions.defaults());

        assertThat(outcome.source()).contains("\n  defaultLens = \"light-lens\"\n");
    }
}
