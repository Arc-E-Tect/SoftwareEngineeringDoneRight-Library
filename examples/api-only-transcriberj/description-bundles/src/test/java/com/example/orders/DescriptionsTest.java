package com.example.orders;

import com.example.contract.ContractDescriptions;
import com.example.contract.ContractField;
import com.example.contract.OrderV1;
import com.example.contract.ProblemV1;
import com.example.contract.restdocs.OrderV1Docs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.FieldDescriptor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The orders contract's documentation, in English and in Dutch, from one generated tree.
 */
class DescriptionsTest {

    private static final Locale ENGLISH = Locale.ENGLISH;
    private static final Locale DUTCH = Locale.of("nl");

    /** The locale the build asked for, which a test that sets its own gives back. */
    private final String runLocale = System.getProperty(ContractDescriptions.LOCALE_PROPERTY);

    @AfterEach
    void restoreTheLocale() {
        if (runLocale == null) {
            System.clearProperty(ContractDescriptions.LOCALE_PROPERTY);
        } else {
            System.setProperty(ContractDescriptions.LOCALE_PROPERTY, runLocale);
        }
    }

    /** Every field of a body, as path and description. */
    private static Map<String, String> described(List<ContractField> fields) {
        return fields.stream().collect(Collectors.toMap(ContractField::path, ContractField::description));
    }

    @Test
    void theBundleDescribesWhatItCarriesAndTheContractDescribesTheRest() {
        Map<String, String> english = described(OrderV1.fields("", ENGLISH));

        // In the bundle: the project's own words.
        assertThat(OrderV1.description(ENGLISH))
                .isEqualTo("An order, as the shop's customer service team explains it.");
        assertThat(english.get("customer.address.street"))
                .isEqualTo("The street and house number, exactly as the customer typed them.");
        assertThat(english.get("lines[].sku")).isEqualTo("The product code printed on the shelf label.");

        // Not in the bundle: the contract's words.
        assertThat(english.get("id")).isEqualTo("The order's identifier.");
        assertThat(english.get("customer.address.city")).isEqualTo("The city.");
    }

    @Test
    void theSameTreeDocumentsTheContractInDutch() {
        Map<String, String> english = described(OrderV1.fields("", ENGLISH));
        Map<String, String> dutch = described(OrderV1.fields("", DUTCH));

        assertThat(OrderV1.description(DUTCH))
                .isEqualTo("Een bestelling, zoals de klantenservice van de winkel die uitlegt.")
                .isNotEqualTo(OrderV1.description(ENGLISH));
        assertThat(dutch.get("customer.address.street"))
                .isEqualTo("De straat en het huisnummer, precies zoals de klant ze intypte.")
                .isNotEqualTo(english.get("customer.address.street"));
        assertThat(dutch.get("lines[].quantity")).isEqualTo("Hoeveel stuks er besteld zijn.");

        // Not translated: the English bundle's text, and after that the contract's.
        assertThat(dutch.get("lines[].sku")).isEqualTo("The product code printed on the shelf label.");
        assertThat(dutch.get("customer.address.city")).isEqualTo("The city.");
    }

    @Test
    void anErrorSchemaIsDescribedTheSameWay() {
        Map<String, String> dutch = described(ProblemV1.fields("", DUTCH));

        assertThat(dutch.get("errors[].message"))
                .isEqualTo("Een zin die de client kan tonen aan wie het formulier invulde.");
        assertThat(dutch.get("title")).isEqualTo("A short summary of the problem.");
    }

    @Test
    void restDocsRendersEachLanguageTheRunAsksFor() {
        System.setProperty(ContractDescriptions.LOCALE_PROPERTY, "en");
        Map<String, Object> english = restDocs(OrderV1Docs.fields());

        System.setProperty(ContractDescriptions.LOCALE_PROPERTY, "nl");
        Map<String, Object> dutch = restDocs(OrderV1Docs.fields());

        assertThat(english.get("customer.name")).isEqualTo("The customer's name.");
        assertThat(dutch.get("customer.name")).isEqualTo("De naam van de klant.");
    }

    @Test
    void aClassWithoutALocaleDocumentsInTheLanguageTheBuildAsksFor() {
        Map<String, String> order = Map.of(
                "en", "An order, as the shop's customer service team explains it.",
                "nl", "Een bestelling, zoals de klantenservice van de winkel die uitlegt.");

        assertThat(OrderV1.description()).isEqualTo(order.get(ContractDescriptions.locale().getLanguage()));
    }

    @Test
    void whatTheBundlesDoNotCoverCanBeListed() {
        // A reader's question: what falls back to the contract's own text.
        assertThat(ContractDescriptions.missing(ENGLISH))
                .contains("OrderV1.id", "OrderV1.customer.address.city", "ProblemV1.title")
                .doesNotContain("OrderV1", "OrderV1.lines[].sku");

        // A translator's question: what Dutch has no text of its own for.
        assertThat(ContractDescriptions.untranslated(DUTCH))
                .contains("OrderV1.lines[].sku", "OrderV1.customer.address.city", "ProblemV1.title")
                .doesNotContain("OrderV1", "OrderV1.customer.name", "OrderV1.lines[].quantity");
    }

    @Test
    void everyKeyInTheBundlesNamesSomethingTheContractDescribes() throws Exception {
        // A renamed schema or a removed field leaves its keys behind, and nothing uses
        // them again. This is how a project finds them: against the keys the tree asks for.
        Map<String, Set<String>> orphans = new TreeMap<>();
        for (String file : List.of("docs/Descriptions.properties", "docs/Descriptions_nl.properties")) {
            java.util.Properties bundle = new java.util.Properties();
            try (var in = getClass().getClassLoader().getResourceAsStream(file);
                 var reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
                bundle.load(reader);
            }
            Set<String> unused = new TreeSet<>(bundle.stringPropertyNames());
            unused.removeAll(ContractDescriptions.keys());
            if (!unused.isEmpty()) {
                orphans.put(file, unused);
            }
        }
        assertThat(orphans).as("keys the contract no longer describes, per file").isEmpty();
    }

    /** REST Docs field descriptors, as path and description. */
    private static Map<String, Object> restDocs(FieldDescriptor[] descriptors) {
        return Arrays.stream(descriptors).collect(Collectors.toMap(FieldDescriptor::getPath,
                FieldDescriptor::getDescription));
    }
}
