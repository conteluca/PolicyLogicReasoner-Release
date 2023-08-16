package special.reasoner;

import org.semanticweb.owlapi.model.*;
import special.model.*;
import special.model.tree.ANDNODE;
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

    private static final OWLOntology ontology = OntologyLoader.load(OntologyLoader.PATH_ONTOLOGY);
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

    private static void realisticTest() {
        // TEST 1
        System.out.println("Realistic Policy VS History SIZE 10-2 (compliant)");
        String policy = Benchmark.Realistic.Policy.Compliant.SIZE_10_2;
        String history = Benchmark.Realistic.History.SIZE_10_2;
        String output = "test-results/realistic/compliant/compliant_size_10_2.csv";
        complianceCheckTest(policy, history, output);


        // TEST 2
        System.out.println("Realistic Policy VS History SIZE 50-10 (compliant)");
        policy = Benchmark.Realistic.Policy.Compliant.SIZE_50_10;
        history = Benchmark.Realistic.History.SIZE_50_10;
        output = "test-results/realistic/compliant/compliant_size_50_10.csv";
        complianceCheckTest(policy, history, output);

        // TEST 3
        System.out.println("Realistic Policy VS History SIZE 100-20 (compliant)");
        policy = Benchmark.Realistic.Policy.Compliant.SIZE_100_20;
        history = Benchmark.Realistic.History.SIZE_100_20;
        output = "test-results/realistic/compliant/compliant_size_100_20.csv";
        complianceCheckTest(policy, history, output);


        // non-compliant tests
        // TEST 1
        System.out.println("Realistic Policy VS History SIZE 10-2 (non compliant)");
        policy = Benchmark.Realistic.Policy.NonCompliant.SIZE_10_2;
        history = Benchmark.Realistic.History.SIZE_10_2;
        output = "test-results/realistic/non-compliant/non_compliant_size_10_2.csv";
        complianceCheckTest(policy, history, output);


        // TEST 2
        System.out.println("Realistic Policy VS History SIZE 50-10 (non compliant)");
        policy = Benchmark.Realistic.Policy.NonCompliant.SIZE_50_10;
        history = Benchmark.Realistic.History.SIZE_50_10;
        output = "test-results/realistic/non-compliant/non_compliant_size_50_10.csv";
        complianceCheckTest(policy, history, output);

        // TEST 3
        System.out.println("Realistic Policy VS History SIZE 100-20 (non compliant)");
        policy = Benchmark.Realistic.Policy.NonCompliant.SIZE_100_20;
        history = Benchmark.Realistic.History.SIZE_100_20;
        output = "test-results/realistic/non-compliant/non_compliant_size_100_20.csv";
        complianceCheckTest(policy, history, output);

    }

    private static void stressTest() {
        // TEST 1
        System.out.println("Stress Policy VS History SIZE 10 ovrd 2 (compliant)");
        String policy = Benchmark.Stress.Policy.Compliant.SIZE_10_OVRD_2;
        String history = Benchmark.Stress.History.Compliant.SIZE_10_OVRD_2;
        String output = "test-results/stress/compliant/compliant_size_10_OVRD_2.csv";
        complianceCheckTest(policy, history, output);

        // TEST 2
        System.out.println("Stress Policy VS History SIZE 10 ovrd 4 (compliant)");
        policy = Benchmark.Stress.Policy.Compliant.SIZE_10_OVRD_4;
        history = Benchmark.Stress.History.Compliant.SIZE_10_OVRD_4;
        output = "test-results/stress/compliant/compliant_size_10_OVRD_4.csv";
        complianceCheckTest(policy, history, output);

        // TEST 3
        System.out.println("Stress Policy VS History SIZE 50 ovrd 2 (compliant)");
        policy = Benchmark.Stress.Policy.Compliant.SIZE_50_OVRD_2;
        history = Benchmark.Stress.History.Compliant.SIZE_50_OVRD_2;
        output = "test-results/stress/compliant/compliant_size_50_OVRD_2.csv";
        complianceCheckTest(policy, history, output);

        // TEST 4
        System.out.println("Stress Policy VS History SIZE 50 ovrd 4 (compliant)");
        policy = Benchmark.Stress.Policy.Compliant.SIZE_50_OVRD_4;
        history = Benchmark.Stress.History.Compliant.SIZE_50_OVRD_4;
        output = "test-results/stress/compliant/compliant_size_50_OVRD_4.csv";
        complianceCheckTest(policy, history, output);

        // TEST 5
        System.out.println("Stress Policy VS History SIZE 100 ovrd 2 (compliant)");
        policy = Benchmark.Stress.Policy.Compliant.SIZE_100_OVRD_2;
        history = Benchmark.Stress.History.Compliant.SIZE_100_OVRD_2;
        output = "test-results/stress/compliant/compliant_size_100_OVRD_2.csv";
        complianceCheckTest(policy, history, output);

        // TEST 6
        System.out.println("Stress Policy VS History SIZE 100 ovrd 4 (compliant)");
        policy = Benchmark.Stress.Policy.Compliant.SIZE_100_OVRD_4;
        history = Benchmark.Stress.History.Compliant.SIZE_100_OVRD_4;
        output = "test-results/stress/compliant/compliant_size_100_OVRD_4.csv";
        complianceCheckTest(policy, history, output);


        // non.compliant
        // TEST 1
        System.out.println("Stress Policy VS History SIZE 10 ovrd 3 (non-compliant)");
        policy = Benchmark.Stress.History.NonCompliant.SIZE_10_OVRD_3;
        history = Benchmark.Stress.History.NonCompliant.SIZE_10_OVRD_3;
        output = "test-results/stress/non-compliant/non_compliant_size_10_OVRD_3.csv";
        complianceCheckTest(policy, history, output);

// TEST 2
        System.out.println("Stress Policy VS History SIZE 10 ovrd 5 (non-compliant)");
        policy = Benchmark.Stress.History.NonCompliant.SIZE_10_OVRD_5;
        history = Benchmark.Stress.History.NonCompliant.SIZE_10_OVRD_5;
        output = "test-results/stress/non-compliant/non_compliant_size_10_OVRD_5.csv";
        complianceCheckTest(policy, history, output);

// TEST 3
        System.out.println("Stress Policy VS History SIZE 50 ovrd 3 (non-compliant)");
        policy = Benchmark.Stress.History.NonCompliant.SIZE_50_OVRD_3;
        history = Benchmark.Stress.History.NonCompliant.SIZE_50_OVRD_3;
        output = "test-results/stress/non-compliant/non_compliant_size_50_OVRD_3.csv";
        complianceCheckTest(policy, history, output);

// TEST 4
        System.out.println("Stress Policy VS History SIZE 50 ovrd 5 (non-compliant)");
        policy = Benchmark.Stress.History.NonCompliant.SIZE_50_OVRD_5;
        history = Benchmark.Stress.History.NonCompliant.SIZE_50_OVRD_5;
        output = "test-results/stress/non-compliant/non_compliant_size_50_OVRD_5.csv";
        complianceCheckTest(policy, history, output);

// TEST 5
        System.out.println("Stress Policy VS History SIZE 100 ovrd 3 (non-compliant)");
        policy = Benchmark.Stress.History.NonCompliant.SIZE_100_OVRD_3;
        history = Benchmark.Stress.History.NonCompliant.SIZE_100_OVRD_3;
        output = "test-results/stress/non-compliant/non_compliant_size_100_OVRD_3.csv";
        complianceCheckTest(policy, history, output);

// TEST 6
        System.out.println("Stress Policy VS History SIZE 100 ovrd 5 (non-compliant)");
        policy = Benchmark.Stress.History.NonCompliant.SIZE_100_OVRD_5;
        history = Benchmark.Stress.History.NonCompliant.SIZE_100_OVRD_5;
        output = "test-results/stress/non-compliant/non_compliant_size_100_OVRD_5.csv";
        complianceCheckTest(policy, history, output);


    }

    public static void main(String[] args) {
        realisticTest();
        stressTest();

    }

    static void complianceCheckTest(String policyPath, String historyPath, String outputPath) {

        String csvFile = outputPath;
        final PolicyIterator jsonIterator = new PolicyIterator(ontology, policyPath, true);

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
            final CsvWriter csvWriter = new CsvWriter(csvFile);
            csvWriter.writeHeader(headers);
            for (PrivacyPolicy privacyPolicy : policyComparison) {
                csvWriter.writeRow(privacyPolicy);
            }
            System.out.println(csvFile + " wrote");

            csvWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}