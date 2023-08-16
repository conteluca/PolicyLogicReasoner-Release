package special.reasoner.utility;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import special.model.PolicyLogic;
import special.model.SignedPolicy;
import special.model.tree.ANDNODE;
import special.model.tree.IntRange;
import special.model.tree.ORNODE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TranslatorEngine {
    private final OntologyAxioms ontologyAxioms;
    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

    public TranslatorEngine(OntologyAxioms ontologyAxioms) {
        this.ontologyAxioms = ontologyAxioms;
    }

    public JSONArray getJsonArray(File file) {
        byte[] data = new byte[(int) file.length()];
        FileInputStream stream;
        try {
            stream = new FileInputStream(file);
            stream.read(data);
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String policy = new String(data, StandardCharsets.UTF_8);
        JSONObject objects = new JSONObject(policy);
        return (JSONArray) objects.get("@policy_set");
    }

    public PolicyLogic<JSONArray> getArrayPolicyLogic(File fi) {
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

    public void convertWithNoCheck(JSONObject from, ANDNODE to, boolean[] action) {
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
                    } else if (value instanceof JSONArray) {
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

    public void convert(JSONObject from, ANDNODE to, boolean[] action) {

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

    public void convertWithNoCheckHistory(JSONObject from, ANDNODE to, boolean[] action) {
        for (String key : from.keySet()) {
            Object value = from.get(key);
            if (ontologyAxioms.isProperty(key)) {
                if (value instanceof JSONObject jsonValue) {                        // PROPERTY
                    ANDNODE node = new ANDNODE();
                    convertWithNoCheckHistory(jsonValue, node, action);
                    to.addChild(ontologyAxioms.getProperty(key), node);
                } else {                                                            // value is a string / an ARRAYOBJECT  -- CONCEPT NAME / OR NODE
                    OWLObjectProperty property = ontologyAxioms.getProperty(key);
                    ANDNODE node = new ANDNODE();
                    if (value.toString().contains("instance")) {
                        OWLIndividual individual = manager.getOWLDataFactory().getOWLNamedIndividual(value.toString());
                        node.addIndividualName(individual);
                    } else if (value instanceof JSONArray) {
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

    public void convertHistory(JSONObject from, ANDNODE to, boolean[] action) {
        for (String key : from.keySet()) {
            Object value = from.get(key);
            if (ontologyAxioms.isProperty(key)) {
                OWLObjectProperty property = ontologyAxioms.getProperty(key);
                if (value instanceof JSONObject objectValue) {     // recursive call (ANDNODE)
                    ANDNODE child = new ANDNODE();
                    convertHistory(objectValue, child, action);
                    to.addChild(property, child);
                } else if (value instanceof JSONArray arrayValue) { // ORNODE
                    ORNODE disj = new ORNODE();
                    for (Object o : arrayValue) {
                        ANDNODE child = new ANDNODE();
                        String vv = (String) o;
                        // check if is an individual or a Concept Name
                        if (ontologyAxioms.isIndividual(vv)) {
                            child.addIndividualName(ontologyAxioms.getIndividual(vv));
                            disj.add(child);
                        } else if (ontologyAxioms.isClass(vv)) {
                            child.addConceptName(ontologyAxioms.getConceptName(vv));
                            disj.add(child);
                        } else {
                            System.err.println(vv + " not in KB");
                        }
                    }
                    ANDNODE child = new ANDNODE();
                    child.addORnode(disj);
                    to.addChild(property, child);

                } else if (value instanceof String stringValue) {
                    ANDNODE child = new ANDNODE();
                    if (ontologyAxioms.isIndividual(stringValue) && stringValue.contains("instance")) {     // only 1 Nominal
                        OWLIndividual individual = ontologyAxioms.getIndividual(stringValue);
                        child.addIndividualName(individual);
                        to.addChild(property, child);
                    } else if (ontologyAxioms.isClass(stringValue)) {  // only 1 Class
                        OWLClass conceptName = ontologyAxioms.getConceptName(stringValue);
                        child.addConceptName(conceptName);
                        to.addChild(property, child);
                    } else {
                        System.err.println(value + " not in KB");
                    }

                } else {
                    System.out.println(key + " is a property with value: " + value);
                }

            } else if (ontologyAxioms.isDataProperty(key)) {
                OWLDataProperty dataProperty = ontologyAxioms.getDataProperty(key);
                if (from.get(key) instanceof JSONObject dataPropertyValue) {
                    JSONArray arr = (JSONArray) dataPropertyValue.get("@interval");
                    IntRange interval = new IntRange(arr.getInt(0), arr.getInt(1));
                    to.addDataProperty(dataProperty, interval);
                } else {
                    System.err.println("DataProperty (" + key + ") has not object value");
                }
            } else {   // key is a @property
                switch (key) {
                    case "@action": {
                        action[0] = from.get(key).equals("permit");
                    }
                    break;
                    case "@instance": {
                        OWLIndividual individual = ontologyAxioms.getIndividual((String) value);
                        to.addIndividualName(individual);
                    }
                    break;
                    case "@intersection": {
                        JSONArray intersection = (JSONArray) value;
                        for (Object o : intersection) {
                            String vv = (String) o;
                            OWLClass conceptName = ontologyAxioms.getConceptName(vv);
                            to.addConceptName(conceptName);
                        }

                    }
                    break;
                    case "@class": {
                        String vv = (String) value;
                        OWLClass conceptName = ontologyAxioms.getConceptName(vv);
                        to.addConceptName(conceptName);
                    }
                    break;
                    default: {
                        System.out.println(key + " is not a property");
                    }
                }
            }
        }
    }

    public ANDNODE parseJSONPolicy(File json) {
        if (!json.getAbsolutePath().endsWith(".json")) {
            throw new IllegalArgumentException("Il file non ha l'estensione .json");
        }
        PolicyLogic<JSONArray> policyLogic = this.getArrayPolicyLogic(json);
        ORNODE ornode = new ORNODE();

        for (Object o : policyLogic.expression()) {
            ANDNODE andnode = new ANDNODE();
            this.convertWithNoCheck((JSONObject) o, andnode, new boolean[1]);
            ornode.add(andnode);
        }
        ANDNODE andnode = new ANDNODE();
        andnode.addORnode(ornode);
        return andnode;
    }

    public SignedPolicy<ANDNODE>[] parseJSONHistory(File json) {
        if (!json.getAbsolutePath().endsWith(".json")) {
            throw new IllegalArgumentException("Il file non ha l'estensione .json");
        }
        JSONArray jsonArray = this.getJsonArray(json);
        SignedPolicy<ANDNODE>[] signedPolicies = new SignedPolicy[jsonArray.length()];
        int index = 0;
        for (Object o : jsonArray) {
            ANDNODE andnode = new ANDNODE();
            boolean[] action = new boolean[1];

            this.convertWithNoCheckHistory((JSONObject) o, andnode, action);

            signedPolicies[index++] = new SignedPolicy<>(action[0], andnode);
        }
        return signedPolicies;
    }
}
