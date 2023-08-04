package special.reasoner;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasonerRuntimeException;

import java.io.File;
import java.util.*;

public class DataManager {

    private static final Logger LOG = Logger.getLogger(DataManager.class);

    private static final Map<String, OWLClassExpression> controllerPolicies = new HashMap<>();
    private static final Map<String, OWLOntology> controllerPoliciesOntology = new HashMap<>();
    private static final Map<String, String> controllerPoliciesFile = new HashMap<>();
    private static final Map<String, OWLClassExpression> subjectsPolicies = new HashMap<>();
    private static final Map<String, OWLOntology> subjectsPoliciesOntology = new HashMap<>();
    private static final Map<String, String> subjectsPoliciesFile = new HashMap<>();
    public static final String PATH_NEW_ONTOLOGY = "benchmarkWithNominal/dataset/pilot/imports";
    public static final String PATH_OLD_ONTOLOGY = "BenchmarkArchive/dataset/pilot/imports";
    private static final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private static final Set<OWLOntology> ontologies = new HashSet<>();
    public static final String DATA_SUBJECT_SUBFOLDER = "/policies/Policies/DataSubjectPolicies";

    public static OWLOntology loadOntology(String pathOntology) {
        final File ontologyFolder = new File(pathOntology);
        ontologies.clear();
        if (!ontologyFolder.exists()) {
            LOG.error("Ontologies folder/file not exists! Path: " + pathOntology);
            System.exit(1);
        }
        for (File owlFile : Objects.requireNonNull(ontologyFolder.listFiles())) {
            if (owlFile.isFile() && owlFile.getName().endsWith(".owl")) {
                try {
                    ontologies.add(manager.loadOntologyFromOntologyDocument(new File(owlFile.getAbsolutePath())));
                } catch (OWLOntologyCreationException e) {
                    throw new OWLReasonerRuntimeException();
                }
            }
        }
        manager.clearOntologies();
        try {
            return manager.createOntology(IRI.create("http://base.ontologies.com/base"), ontologies);
        } catch (OWLOntologyCreationException e) {
            throw new OWLReasonerRuntimeException();
        }
    }

    public static void loadOldControllerPolicies(String oldPath) {
        controllerPolicies.clear();
        controllerPoliciesOntology.clear();
        controllerPoliciesFile.clear();
        File folder = new File(oldPath);
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            File x = new File(file.getAbsolutePath() + "/policies/Policies/DataControllerPolicies");
            loadPolicies(x, controllerPolicies, controllerPoliciesOntology, controllerPoliciesFile);
        }
    }


    public static void loadOldDataSubjectsPolicies(String oldPath) {
        subjectsPolicies.clear();
        subjectsPoliciesOntology.clear();
        subjectsPoliciesFile.clear();
        File folder = new File(oldPath + DATA_SUBJECT_SUBFOLDER);   // per caricare le vecchie
        loadPolicies(folder, subjectsPolicies, subjectsPoliciesOntology,subjectsPoliciesFile);
    }


    private static void loadPolicies(File folder, Map<String, OWLClassExpression> map, Map<String, OWLOntology> ontologies,Map<String, String> mapFiles) {
        List<File> files = Arrays.stream(Objects.requireNonNull(folder.listFiles())).filter(x -> x.getName().endsWith(".owl")).toList();
        for (File file : files) {
            try {
                OWLOntology policyOntology = manager.loadOntologyFromOntologyDocument(file);
                List<OWLEquivalentClassesAxiom> owlEquivalentClassesAxioms = policyOntology.axioms(AxiomType.EQUIVALENT_CLASSES).toList();
                for (OWLEquivalentClassesAxiom classesAxiom : owlEquivalentClassesAxioms) {
                    List<OWLClassExpression> operands = classesAxiom.getOperandsAsList();
                    Iterator<OWLClassExpression> iterator = operands.iterator();
                    String left = iterator.next().toString();
                    OWLClassExpression right = iterator.next();
                    String id = left.substring(left.indexOf("_") + 1, left.length() - 1);
                    map.put(id, right);
                    ontologies.put(id, policyOntology);
                    mapFiles.put(id,file.getAbsolutePath());
                }
            } catch (OWLOntologyCreationException e) {
                throw new OWLOntologyResourceAccessException(e);
            }
            manager.clearOntologies();
        }
    }

    public static Map<String, OWLClassExpression> getControllerPolicies() {
        return controllerPolicies;
    }


    public static Map<String, String> getControllerPoliciesFile() {
        return controllerPoliciesFile;
    }

    public static Map<String, OWLClassExpression> getSubjectsPolicies() {
        return subjectsPolicies;
    }

    public static Map<String, String> getSubjectsPoliciesFiles() {
        return subjectsPoliciesFile;
    }

}
