package special.reasoner;

import org.json.JSONArray;
import org.semanticweb.owlapi.model.*;
import special.model.*;
import special.model.tree.ANDNODE;
import special.model.tree.ORNODE;
import special.reasoner.factory.ReasonerBuilder;
import special.reasoner.utility.*;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * @author Luca Conte
 */

class Main {

    private static final OWLOntology ontology = OntologyLoader.load(new File(OntologyLoader.PATH_ONTOLOGY));
    private static final PLReasoner plReasoner = ReasonerBuilder.buildReasoner(ontology);
    private static final String[] headers;

    static {
        headers = new String[]{
                "Policy",
                "History",
                "Compliant",
                "STS count",
                "Time 1(ms)",
                "Time 2(ms)",
                "Time 3(ms)",
                "Time 4(ms)",
                "Time 5(ms)",
                "Time 6(ms)",
                "Time 7(ms)",
                "Time 8(ms)",
                "Time 9(ms)",
                "Time 10(ms)"
        };
    }

    private static final List<PrivacyPolicy> policyComparison = new ArrayList<>();

    private static void realisticTestCompliant() {
        /*
        TESTING ALL COMPLIANT WITH HISTORIES
         */
        String[] p = new String[]{
                Benchmark.Realistic.Policy.Compliant.SIZE_10_2,
                Benchmark.Realistic.Policy.Compliant.SIZE_50_10,
                Benchmark.Realistic.Policy.Compliant.SIZE_100_20,
        };
        String[] h = new String[]{
                Benchmark.Realistic.History.SIZE_10_2,
                Benchmark.Realistic.History.SIZE_50_10,
                Benchmark.Realistic.History.SIZE_100_20,
        };
        String[] o = new String[]{
                "test-results/realistic/compliant/compliant_SIZE_10_2.csv",
                "test-results/realistic/compliant/compliant_SIZE_50_10.csv",
                "test-results/realistic/compliant/compliant_SIZE_100_20.csv"
        };
        for (int i = 0; i < p.length; i++) {
            complianceCheckTest(p[i], h[i], o[i]);
        }
    }

    private static void realisticTestNonCompliant() {
       /*
        TESTING ALL NON COMPLIANT WITH HISTORIES
         */

        String[] p = new String[]{
                Benchmark.Realistic.Policy.NonCompliant.SIZE_10_2,
                Benchmark.Realistic.Policy.NonCompliant.SIZE_50_10,
                Benchmark.Realistic.Policy.NonCompliant.SIZE_100_20,
        };
        String[] h = new String[]{
                Benchmark.Realistic.History.SIZE_10_2,
                Benchmark.Realistic.History.SIZE_50_10,
                Benchmark.Realistic.History.SIZE_100_20,
        };
        String[] o = new String[]{
                "test-results/realistic/non-compliant/non_compliant_SIZE_10_2.csv",
                "test-results/realistic/non-compliant/non_compliant_SIZE_50_10.csv",
                "test-results/realistic/non-compliant/non_compliant_SIZE_100_20.csv"
        };
        for (int i = 0; i < p.length; i++) {
            complianceCheckTest(p[i], h[i], o[i]);
        }
    }

    private static void stressTestCompliant() {
        String[] p = new String[]{
                Benchmark.Stress.Policy.Compliant.SIZE_10_OVRD_2,
                Benchmark.Stress.Policy.Compliant.SIZE_10_OVRD_4,
                Benchmark.Stress.Policy.Compliant.SIZE_50_OVRD_2,
                Benchmark.Stress.Policy.Compliant.SIZE_50_OVRD_4,
                Benchmark.Stress.Policy.Compliant.SIZE_100_OVRD_2,
                Benchmark.Stress.Policy.Compliant.SIZE_100_OVRD_4,
        };
        String[] h = new String[]{
                Benchmark.Stress.History.Compliant.SIZE_10_OVRD_2,
                Benchmark.Stress.History.Compliant.SIZE_10_OVRD_4,
                Benchmark.Stress.History.Compliant.SIZE_50_OVRD_2,
                Benchmark.Stress.History.Compliant.SIZE_50_OVRD_4,
                Benchmark.Stress.History.Compliant.SIZE_100_OVRD_2,
                Benchmark.Stress.History.Compliant.SIZE_100_OVRD_4,
        };
        String[] o = new String[]{
                "test-results/stress/compliant/compliant_size_10_OVRD_2.csv",
                "test-results/stress/compliant/compliant_size_10_OVRD_4.csv",
                "test-results/stress/compliant/compliant_size_50_OVRD_2.csv",
                "test-results/stress/compliant/compliant_size_50_OVRD_4.csv",
                "test-results/stress/compliant/compliant_size_100_OVRD_2.csv",
                "test-results/stress/compliant/compliant_size_100_OVRD_4.csv",
        };
        for (int i = 0; i < p.length; i++) {
            complianceCheckTest(p[i], h[i], o[i]);
        }
    }

    private static void stressTestNonCompliant() {
        String[] p = new String[]{
                Benchmark.Stress.Policy.NonCompliant.SIZE_10_OVRD_3,
                Benchmark.Stress.Policy.NonCompliant.SIZE_10_OVRD_5,
                Benchmark.Stress.Policy.NonCompliant.SIZE_50_OVRD_3,
                Benchmark.Stress.Policy.NonCompliant.SIZE_50_OVRD_5,
                Benchmark.Stress.Policy.NonCompliant.SIZE_100_OVRD_3,
                Benchmark.Stress.Policy.NonCompliant.SIZE_100_OVRD_5,
        };
        String[] h = new String[]{
                Benchmark.Stress.History.NonCompliant.SIZE_10_OVRD_3,
                Benchmark.Stress.History.NonCompliant.SIZE_10_OVRD_5,
                Benchmark.Stress.History.NonCompliant.SIZE_50_OVRD_3,
                Benchmark.Stress.History.NonCompliant.SIZE_50_OVRD_5,
                Benchmark.Stress.History.NonCompliant.SIZE_100_OVRD_3,
                Benchmark.Stress.History.NonCompliant.SIZE_100_OVRD_5,
        };
        String[] o = new String[]{
                "test-results/stress/non-compliant/non_compliant_size_10_OVRD_3.csv",
                "test-results/stress/non-compliant/non_compliant_size_10_OVRD_5.csv",
                "test-results/stress/non-compliant/non_compliant_size_50_OVRD_3.csv",
                "test-results/stress/non-compliant/non_compliant_size_50_OVRD_5.csv",
                "test-results/stress/non-compliant/non_compliant_size_100_OVRD_3.csv",
                "test-results/stress/non-compliant/non_compliant_size_100_OVRD_5.csv",
        };
        for (int i = 0; i < p.length; i++) {
            complianceCheckTest(p[i], h[i], o[i]);
        }
    }

    static void test2() {
        String[] strings = {
                "http://www.w3id.org/dpv/dpv-gdpr",
                "http://www.w3id.org/dpv/dpv-legal",
                "http://www.w3id.org/dpv/dpv-owl/dpv-pd",
                "https://w3id.org/dpv/dpv-owl"
        };
        System.out.println(strings);
    }


    public static void main(String[] args) {
        final IRI ontologyIRI = IRI.create("http://www.w3id.org/dpv/dpv-gdpr");
        final OWLOntology ontology = OntologyLoader.load(ontologyIRI);

    }

    static void complianceCheckTest(String policyPath, String historyPath, String outputPath) {
        policyComparison.clear();
        final JSONPolicyIterator jsonIterator = new JSONPolicyIterator(ontology, policyPath, true);
        System.out.println(policyPath);


        while (jsonIterator.hasNext()) {

            PolicyLogic<OWLClassExpression> owl = jsonIterator.toOwl();

            JSONHistoryIterator historyIterator =
                    new JSONHistoryIterator(ontology, historyPath, true);


            while (historyIterator.hasNext()) {
                History history = historyIterator.next();

                PrivacyPolicy privacyPolicy = new PrivacyPolicy(owl, history);

                for (int i = 0; i < 10; i++) {
                    long startTime = System.nanoTime();
                    boolean entailed = plReasoner.isEntailed(owl, history);
                    long endTime = System.nanoTime();
                    long executionTime = endTime - startTime;
                    double executionTimeInMillis = executionTime / 1_000_000.0;

                    privacyPolicy.setCompliant(entailed);
                    privacyPolicy.setExecutionTime(executionTimeInMillis);
                    int stsCount = plReasoner.getStsCount();
                    privacyPolicy.setStsCount(stsCount);

                }
                policyComparison.add(privacyPolicy);
            }
        }

        try {
            CsvWriter csvWriter = new CsvWriter(outputPath);

            csvWriter.writeHeader(headers);
            for (PrivacyPolicy privacyPolicy : policyComparison) {
                csvWriter.writeRow(privacyPolicy);
            }
            System.out.println(outputPath + " wrote");

            csvWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    static void test3() {
        String[] p = new String[]{
                Benchmark.Stress.Policy.NonCompliant.SIZE_10_OVRD_3,
                Benchmark.Stress.Policy.NonCompliant.SIZE_10_OVRD_5
        };
        String[] h = new String[]{
                Benchmark.Stress.History.NonCompliant.SIZE_10_OVRD_3,
                Benchmark.Stress.History.NonCompliant.SIZE_10_OVRD_5
        };
        TranslatorEngine translatorEngine = new TranslatorEngine(new OntologyAxioms(ontology));
        for (String s : p) {
            File x = new File(s);
            for (File left : Objects.requireNonNull(x.listFiles())) {
                ANDNODE c = translatorEngine.parseJSONPolicy(left);
                for (String history : h) {
                    File y = new File(history);
                    for (File right : Objects.requireNonNull(y.listFiles())) {
                        SignedPolicy<ANDNODE>[] d = translatorEngine.parseJSONHistory(right);
                        boolean entailedHistory = plReasoner.isEntailedHistory(left, right);
                        boolean entailed = plReasoner.isEntailed(c, d);
                        System.out.println(left.getName() + " vs " + right.getName() + ": [" + entailedHistory + "," + entailed + "]");
                    }
                }
            }
        }
    }

    static void test4() {
        String c = "BenchmarkArchive/dataset/pilot/non-compliance/PROXIMUS1/policies/Policies/DataControllerPolicies/";
        String p1 = "BenchmarkArchive/dataset/pilot/non-compliance/PROXIMUS1/policies/Policies/DataSubjectPolicies/";
        OWLPolicyIterator i = new OWLPolicyIterator(p1);
        OWLPolicyIterator ic = new OWLPolicyIterator(c);

        PolicyLogic<OWLClassExpression> policy = ic.next();
        while (i.hasNext()) {
            PolicyLogic<OWLClassExpression> next = i.next();
            boolean entailed = plReasoner.isEntailed(policy.expression(), next.expression());
            System.out.println(entailed);
        }
    }
}