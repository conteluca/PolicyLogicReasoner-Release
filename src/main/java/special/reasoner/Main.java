package special.reasoner;

import org.semanticweb.owlapi.model.*;
import special.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Luca Conte
 */



class Main {
    private static final OWLOntology ontology = DataManager.loadOntology(DataManager.PATH_NEW_ONTOLOGY);
    private static final PLReasoner plReasoner = ReasonerBuilder.buildReasoner(ontology);
    private static final String[] headers =
            new String[]{
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

    private static final String TREE_FOLDER = TestbedIDOnlyNotCompliant.SIZE_100_20;
    private static final String HISTORY_FOLDER = TestbedIDOnlyHistory.SIZE_100_20;
    private static final List<PrivacyPolicy> policiesComparison = new ArrayList<>();

    public static void main(String[] args) {
        String csvFile = "not_compliant_size_100_20.csv";

        JSONPolicyIterator jsonIterator =
                new JSONPolicyIterator(ontology, TREE_FOLDER, false);

        while (jsonIterator.hasNext()) {

            PolicyLogic<OWLClassExpression> owl = jsonIterator.toOwl();

            HistoryPolicyIterator historyIterator =
                    new HistoryPolicyIterator(ontology, HISTORY_FOLDER, false);


            while (historyIterator.hasNext()) {
                History history = historyIterator.next();

                PrivacyPolicy privacyPolicy = new PrivacyPolicy(owl, history);

                for (int i = 0; i < 10; i++) {
                    long startTime = System.nanoTime();
                    boolean entailed = plReasoner.isEntailed(owl, history.getSignedPolicy());
                    long endTime = System.nanoTime();
                    long executionTime = endTime - startTime;
                    double executionTimeInMillis = executionTime / 1_000_000.0;

                    privacyPolicy.setCompliant(entailed);
                    privacyPolicy.setExecutionTime(executionTimeInMillis);
                    int stsCount = plReasoner.getStsCount();
                    privacyPolicy.setStsCount(stsCount);

                }
                policiesComparison.add(privacyPolicy);
            }
        }

        try {
            final CsvWriter csvWriter = new CsvWriter(csvFile);
            csvWriter.writeHeader(headers);
            for (PrivacyPolicy privacyPolicy : policiesComparison) {
                csvWriter.writeRow(privacyPolicy);
            }
            System.out.println(csvFile+" wrote");

            csvWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}