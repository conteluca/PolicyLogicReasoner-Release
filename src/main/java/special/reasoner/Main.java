package special.reasoner;

import org.semanticweb.owlapi.model.*;
import special.model.*;
import special.reasoner.factory.ReasonerBuilder;
import special.reasoner.utility.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


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
           complianceCheckTest(p[i],h[i],o[i] );
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
              complianceCheckTest(p[i],h[i],o[i] );
        }
    }
    private static void stressTestCompliant(){
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
            complianceCheckTest(p[i],h[i],o[i]);
        }
    }
    private static void stressTestNonCompliant(){
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
            complianceCheckTest(p[i],h[i],o[i]);
        }
    }
    static void test2(){
    OWLOntology load = OntologyLoader.load(new String[]{
            "http://www.w3id.org/dpv/dpv-gdpr",
            "http://www.w3id.org/dpv/dpv-legal",
            "http://www.w3id.org/dpv/dpv-owl/dpv-pd",
            "https://w3id.org/dpv/dpv-owl"
    });
    PLReasoner reasoner = ReasonerBuilder.buildReasoner(load);

    File c = new File("testSET/Contr-pol-compl-no-exc.json");
    File h = new File("testSET/hist-10-2-01.json");
    File d = new File("testSET/Contr-pol-noncompl-exc-003.json");

    boolean entailed2 = reasoner.isEntailedHistory(d,h);

    System.out.println(entailed2);

}
    public static void main(String[] args) {

        stressTestCompliant();
        stressTestNonCompliant();
    }

    static void complianceCheckTest(String policyPath, String historyPath, String outputPath) {
        policyComparison.clear();
        final PolicyIterator jsonIterator = new PolicyIterator(ontology, policyPath, true);
            System.out.println(policyPath);


        while (jsonIterator.hasNext()) {

         PolicyLogic<OWLClassExpression> owl = jsonIterator.toOwl();

            HistoryIterator historyIterator =
                    new HistoryIterator(ontology, historyPath, true);


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

}