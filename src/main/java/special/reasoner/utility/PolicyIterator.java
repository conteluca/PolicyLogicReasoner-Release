package special.reasoner.utility;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import special.model.*;
import special.model.tree.ANDNODE;
import special.model.tree.IntRange;
import special.model.tree.ORNODE;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PolicyIterator implements Iterator<PolicyLogic<JSONArray>> {
    private final List<PolicyLogic<JSONArray>> policies = new LinkedList<>();
    private final OntologyAxioms ontologyAxioms;
    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private final OWLDataFactory factory = manager.getOWLDataFactory();
    private  boolean enableKnowledgeBaseCheck = true;
    private int currentIndex = 0;

    public PolicyIterator(OWLOntology ontology, String path, boolean enableKnowledgeBaseCheck) {
        this.enableKnowledgeBaseCheck = enableKnowledgeBaseCheck;
        File directory = new File(path);
        this.ontologyAxioms = new OntologyAxioms(ontology);
        if (directory.isDirectory()) {
            List<File> jsonFiles = Arrays.stream(Objects.requireNonNull(directory.listFiles())).filter(x -> x.getName().endsWith(".json")).toList();

            for (File fi : jsonFiles) {
                PolicyLogic<JSONArray> policyLogic = getArrayPolicyLogic(fi);
                policies.add(policyLogic);
            }
        } else {
            System.err.println(path + " is not a directory");
            System.exit(1);
        }
    }

    private static PolicyLogic<JSONArray> getArrayPolicyLogic(File fi) {
        byte[] data = new byte[(int) fi.length()];
        FileInputStream stream;
        try {
            stream = new FileInputStream(fi);
            stream.read(data);
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String policy = new String(data, StandardCharsets.UTF_8);
        JSONObject dd = new JSONObject(policy);
        JSONArray objects = (JSONArray) dd.get("@policy_set");
        String id = fi.getName().substring(0, fi.getName().length() - ".json".length());
        return new PolicyLogic<>(id, objects);
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

    public SignedPolicy<ANDNODE>[] toHistory() {
        PolicyLogic<JSONArray> policyLogic = next();
        SignedPolicy<ANDNODE>[] signedPolicies = new SignedPolicy[policyLogic.expression().length()];
        int index = 0;
        for (Object o : policyLogic.expression()) {
            ANDNODE andnode = new ANDNODE();
            boolean[] action = new boolean[1];
            convert((JSONObject) o, andnode, action);
            signedPolicies[index++] = new SignedPolicy<>(action[0],andnode);
        }
        return signedPolicies;
    }

    public PolicyLogic<ORNODE> toTree() {
        PolicyLogic<JSONArray> policyLogic = next();
        ORNODE ornode = new ORNODE();

        for (Object o : policyLogic.expression()) {
            ANDNODE andnode = new ANDNODE();
            if(this.enableKnowledgeBaseCheck){
                convert((JSONObject) o, andnode, new boolean[1]);
            }else{
                convertWithNoCheck((JSONObject) o, andnode, new boolean[1]);
            }
            ornode.add(andnode);
        }
        return new PolicyLogic<>(policyLogic.id(), ornode);
    }

    private void convertWithNoCheck(JSONObject from, ANDNODE to, boolean[] action) {
        for (String key : from.keySet()) {
            Object value = from.get(key);
            if (ontologyAxioms.isProperty(key)) {
                if (value instanceof JSONObject jsonValue) {                        // PROPERTY
                    ANDNODE node = new ANDNODE();
                    convertWithNoCheck(jsonValue, node, action);
                    to.addChild(ontologyAxioms.getProperty(key), node);
                } else {                                                            // value is a string / an ARRAYOBJECT  -- CONCEPT NAME / OR NODE
                    OWLObjectProperty property = ontologyAxioms.getProperty(key);
                    ANDNODE node = new ANDNODE();
                    if (value.toString().contains("instance")) {
                        OWLIndividual individual = manager.getOWLDataFactory().getOWLNamedIndividual(value.toString());
                        node.addIndividualName(individual);
                    } else
                        if (value instanceof JSONArray) {
                        JSONArray isOrNode = (JSONArray) value;
                        ORNODE tmp = new ORNODE();
                        for (Object o : isOrNode) {
                            String v = (String) o;
                            ANDNODE child = new ANDNODE();
                            if (v.contains("instance")) {
                                OWLIndividual individual = manager.getOWLDataFactory().getOWLNamedIndividual(v);
                                child.addIndividualName(individual);
                            } else {
                                OWLClass owlClass = manager.getOWLDataFactory().getOWLClass(v);
                                child.addConceptName(owlClass);
                            }
                            tmp.add(child);
                        }
                        node.addORnode(tmp);
                    } else {
                        OWLClass owlClass = manager.getOWLDataFactory().getOWLClass(value.toString());
                        node.addConceptName(owlClass);
                    }

                    to.addChild(property, node);
                }

            } else if (key.equals("@intersection")) {
                if (value instanceof JSONArray array) {
                    for (Object o : array) {
                        if (o instanceof String conceptName) {
                            OWLClass owlClass = manager.getOWLDataFactory().getOWLClass(conceptName);
                            to.addConceptName(owlClass);
                        } else if (o instanceof JSONObject nominal) {
                            String instance = (String) nominal.get("@instance");
                            OWLIndividual individual = manager.getOWLDataFactory().getOWLNamedIndividual(instance);
                            to.addIndividualName(individual);
                        }
                    }
                }
            } else if (key.equals("@action")) {
                String v = (String) value;
                action[0] = v.equals("permit");
            } else if (key.equals("@class")) {
                OWLClass owlClass = manager.getOWLDataFactory().getOWLClass(value.toString());
                to.addConceptName(owlClass);
            } else if (ontologyAxioms.isDataProperty(key)) {
                if (value instanceof JSONObject dataPropertyValue) {
                    JSONArray arr = (JSONArray) dataPropertyValue.get("@interval");
                    IntRange interval = new IntRange(arr.getInt(0), arr.getInt(1));
                    to.addDataProperty(ontologyAxioms.getDataProperty(key), interval);
                }
            } else if (key.equals("@instance")) {
                OWLIndividual individual = manager.getOWLDataFactory().getOWLNamedIndividual(value.toString());
                to.addIndividualName(individual);
            } else {
                System.err.println("Term " + key + " not in knowledge base");
                System.exit(1);
            }
        }

    }


    private void convert(JSONObject from, ANDNODE to, boolean[] action) {

        for (String key : from.keySet()) {
            Object value = from.get(key);
            if (ontologyAxioms.isProperty(key)) {
                if (value instanceof JSONObject jsonValue) {
                    ANDNODE node = new ANDNODE();
                    convert(jsonValue, node, action);
                    to.addChild(ontologyAxioms.getProperty(key), node);
                } else {      // value is a string
                    OWLObjectProperty property = ontologyAxioms.getProperty(key);
                    ANDNODE node = new ANDNODE();
                    node.addConceptName(ontologyAxioms.getConceptName(value.toString()));
                    to.addChild(property, node);
                }

            } else if (key.equals("@intersection")) {
                if (value instanceof JSONArray array) {
                    for (Object o : array) {
                        if (o instanceof String conceptName && ontologyAxioms.isClass(conceptName)) {
                            to.addConceptName(ontologyAxioms.getConceptName(conceptName));
                        } else if (o instanceof JSONObject nominal) {
                            String instance = (String) nominal.get("@instance");
                            if (ontologyAxioms.isIndividual(instance)) {
                                to.addIndividualName(ontologyAxioms.getIndividual(instance));
                            }
                        }
                    }
                }
            } else if (key.equals("@action")) {
                String v = (String) value;
                action[0] = v.equals("permit");
            } else if (key.equals("@class")) {
                to.addConceptName(ontologyAxioms.getConceptName((String) value));
            } else if (ontologyAxioms.isDataProperty(key)) {
                if (value instanceof JSONObject dataPropertyValue) {
                    JSONArray arr = (JSONArray) dataPropertyValue.get("@interval");
                    IntRange interval = new IntRange(arr.getInt(0), arr.getInt(1));
                    to.addDataProperty(ontologyAxioms.getDataProperty(key), interval);
                }
            } else if (key.equals("@instance")) {
                to.addIndividualName(ontologyAxioms.getIndividual((String) value));
            } else {
                System.err.println("Term " + key + " not in knowledge base");
                System.exit(1);
            }
        }
    }

}
