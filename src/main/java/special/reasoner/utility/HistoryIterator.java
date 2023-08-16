package special.reasoner.utility;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import special.model.*;
import special.model.tree.ANDNODE;
import special.model.tree.IntRange;
import special.model.tree.ORNODE;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;

public class HistoryIterator implements Iterator<History> {
    private final List<History> histories = new LinkedList<>();
    private final boolean enableKnowledgeBaseCheck;
    private final TranslatorEngine translatorEngine;

    private int currentIndex = 0;

    public HistoryIterator(final @Nonnull OWLOntology ontology,
                           final @Nonnull String historyPathDirectory,
                           final boolean enableKnowledgeBaseCheck) {
        this.enableKnowledgeBaseCheck = enableKnowledgeBaseCheck;
        File directory = new File(historyPathDirectory);
        translatorEngine = new TranslatorEngine(new OntologyAxioms(ontology));

        if (directory.isDirectory()) {
            List<File> jsonFiles = Arrays.stream(Objects.requireNonNull(directory.listFiles())).filter(x -> x.getName().endsWith(".json")).toList();

            for (File fi : jsonFiles) {
                JSONArray history = this.translatorEngine.getJsonArray(fi);
                SignedPolicy<ANDNODE>[] signedPolicies = convertHistory(history);
                String id = fi.getName().substring(0, fi.getName().length() - ".json".length());
                histories.add(new History(id, signedPolicies));
            }
        } else {
            System.err.println(historyPathDirectory + " is not a directory");
            System.exit(1);
        }
    }



    @Override
    public boolean hasNext() {
        return currentIndex < this.histories.size();
    }

    @Override
    public History next() {
        if(!hasNext()){
            throw new NoSuchElementException();
        }
        return this.histories.get(currentIndex++);
    }


    private SignedPolicy<ANDNODE>[] convertHistory(JSONArray policyLogic) {
        SignedPolicy<ANDNODE>[] signedPolicies = new SignedPolicy[policyLogic.length()];
        int index = 0;
        for (Object o : policyLogic) {
            ANDNODE andnode = new ANDNODE();
            boolean[] action = new boolean[1];

            if (this.enableKnowledgeBaseCheck) {
                this.translatorEngine.convertHistory((JSONObject) o, andnode, action);
            } else {
                this.translatorEngine.convertWithNoCheckHistory((JSONObject) o, andnode, action);
            }

            signedPolicies[index++] = new SignedPolicy<>(action[0], andnode);
        }
        return signedPolicies;
    }

}
