package special.reasoner.utility;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import special.model.*;
import special.model.tree.ANDNODE;
import special.model.tree.ORNODE;

import java.io.*;

import java.util.*;

public class JSONPolicyIterator implements Iterator<PolicyLogic<JSONArray>> {
    private final List<PolicyLogic<JSONArray>> policies = new LinkedList<>();
    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private final OWLDataFactory factory = manager.getOWLDataFactory();
    private final TranslatorEngine translatorEngine;
    private  boolean enableKnowledgeBaseCheck = true;
    private int currentIndex = 0;
    public JSONPolicyIterator(OWLOntology ontology, String path, boolean enableKnowledgeBaseCheck) {
        this.policies.clear();
        this.enableKnowledgeBaseCheck = enableKnowledgeBaseCheck;
        File directory = new File(path);
        translatorEngine = new TranslatorEngine(new OntologyAxioms(ontology));
        if (directory.isDirectory()) {
            List<File> jsonFiles = Arrays.stream(Objects.requireNonNull(directory.listFiles())).filter(x -> x.getName().endsWith(".json")).toList();

            for (File fi : jsonFiles) {

                PolicyLogic<JSONArray> policyLogic = this.translatorEngine.getArrayPolicyLogic(fi);
                policies.add(policyLogic);
            }
        } else {
            System.err.println(path + " is not a directory");
            System.exit(1);
        }
    }

    @Override
    public boolean hasNext() {
        return currentIndex < policies.size();
    }
    @Override
    public PolicyLogic<JSONArray> next() {

        if(!hasNext()){
            throw new NoSuchElementException();
        }
        return policies.get(currentIndex++);
    }
    public PolicyLogic<OWLClassExpression> toOwl() {
        PolicyLogic<ORNODE> tree = toTree();
        OWLClassExpression expression = tree.expression()
                .toOWLClassExpression(factory);

        return new PolicyLogic<>(tree.id(),expression);
    }
    public PolicyLogic<ORNODE> toTree() {
        PolicyLogic<JSONArray> policyLogic = next();
        ORNODE ornode = new ORNODE();

        for (Object o : policyLogic.expression()) {
            ANDNODE andnode = new ANDNODE();
            if(this.enableKnowledgeBaseCheck){
                this.translatorEngine.convert((JSONObject) o, andnode, new boolean[1]);
            }else{
                this.translatorEngine.convertWithNoCheck((JSONObject) o, andnode, new boolean[1]);
            }
            ornode.add(andnode);
        }
        return new PolicyLogic<>(policyLogic.id(), ornode);
    }
    public int size(){
        return this.policies.size();
    }

}
