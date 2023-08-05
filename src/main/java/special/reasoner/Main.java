package special.reasoner;

import org.semanticweb.owlapi.model.*;
import special.model.*;
import special.reasoner.factory.ReasonerBuilder;
import special.reasoner.utility.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Luca Conte
 */


class Main {

    private static final OWLOntology ontology = OntologyLoader.load();
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

    private static final String POLICY_FOLDER = Benchmark.Policy.Compliant.SIZE_100_20;
    private static final String HISTORY_FOLDER = Benchmark.History.SIZE_100_20;
    private static final List<PrivacyPolicy> policiesComparison = new ArrayList<>();

    public static void main(String[] args) {

        final PolicyIterator policyIterator =
                new PolicyIterator(ontology, POLICY_FOLDER, false);

        final HistoryIterator historyIterator = new HistoryIterator(ontology, HISTORY_FOLDER, false);
        History history = historyIterator.next();


        do {
            final PolicyLogic<OWLClassExpression> c = policyIterator.toOwl();

            boolean isEntailed = plReasoner.isEntailed(c,history); // COMPLIANCE CHECK

            if(isEntailed){
                System.out.println(c.id()+" entails "+history.id());
            }else{
                System.out.println(c.id()+" doesn't entails "+history.id());
            }
        } while (policyIterator.hasNext());

    }

    static void temp() {
        String csvFile = "compliant_size_100_20.csv";
        final PolicyIterator jsonIterator =
                new PolicyIterator(ontology, POLICY_FOLDER, false);

        while (jsonIterator.hasNext()) {

            PolicyLogic<OWLClassExpression> owl = jsonIterator.toOwl();

            HistoryIterator historyIterator =
                    new HistoryIterator(ontology, HISTORY_FOLDER, false);


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
                policiesComparison.add(privacyPolicy);
            }
        }

        try {
            final CsvWriter csvWriter = new CsvWriter(csvFile);
            csvWriter.writeHeader(headers);
            for (PrivacyPolicy privacyPolicy : policiesComparison) {
                csvWriter.writeRow(privacyPolicy);
            }
            System.out.println(csvFile + " wrote");

            csvWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}