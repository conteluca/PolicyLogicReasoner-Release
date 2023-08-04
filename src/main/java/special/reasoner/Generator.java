package special.reasoner;


import org.apache.log4j.Logger;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.OWLObjectTransformer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Luca Conte
 */

class Generator {

    private static void createDirectoryTree() throws IOException {
        String compliant = "./benchmarkWithNominal/dataset/pilot/compliance";
        String non_compliant = "./benchmarkWithNominal/dataset/pilot/non-compliance";
        String dataControllerPolicies = "/DataControllerPolicies";
        String dataSubjectPolicies = "/DataSubjectPolicies";
        Path root = Paths.get("./benchmarkWithNominal");
        Path dataset = Paths.get("./benchmarkWithNominal/dataset");
        Path pilot = Paths.get("./benchmarkWithNominal/dataset/pilot");
        Path imports = Paths.get("./benchmarkWithNominal/dataset/pilot/imports");
        String proximus = "/PROXIMUS";
        String tr = "/TR";
        Path compliance = Paths.get(compliant);
        Path non_compliance = Paths.get(non_compliant);

        Path dataControllerPolicies_compliance = Paths.get(compliant + dataControllerPolicies);
        Path dataControllerPolicies_non_compliance = Paths.get(non_compliance + dataControllerPolicies);
        Path dataSubjectPolicies_compliance = Paths.get(compliant + dataSubjectPolicies);
        Path dataSubjectPolicies_non_compliance = Paths.get(non_compliant + dataSubjectPolicies);

        Path dataControllerPolicies_compliance_PROXIMUS = Paths.get(compliant + dataControllerPolicies + proximus);
        Path dataControllerPolicies_non_compliance_PROXIMUS = Paths.get(non_compliance + dataControllerPolicies + proximus);
        Path dataSubjectPolicies_compliance_PROXIMUS = Paths.get(compliant + dataSubjectPolicies + proximus);
        Path dataSubjectPolicies_non_compliance_PROXIMUS = Paths.get(non_compliant + dataSubjectPolicies + proximus);

        Path dataControllerPolicies_compliance_TR = Paths.get(compliant + dataControllerPolicies + tr);
        Path dataControllerPolicies_non_compliance_TR = Paths.get(non_compliance + dataControllerPolicies + tr);
        Path dataSubjectPolicies_compliance_TR = Paths.get(compliant + dataSubjectPolicies + tr);
        Path dataSubjectPolicies_non_compliance_TR = Paths.get(non_compliant + dataSubjectPolicies + tr);


        Files.createDirectory(root);
        Files.createDirectory(dataset);
        Files.createDirectory(pilot);
        Files.createDirectory(imports);
        Files.createDirectory(compliance);
        Files.createDirectory(non_compliance);

        Files.createDirectory(dataControllerPolicies_compliance);
        Files.createDirectory(dataControllerPolicies_non_compliance);
        Files.createDirectory(dataSubjectPolicies_compliance);
        Files.createDirectory(dataSubjectPolicies_non_compliance);
        Files.createDirectory(dataControllerPolicies_compliance_PROXIMUS);
        Files.createDirectory(dataControllerPolicies_non_compliance_PROXIMUS);

        Files.createDirectory(dataSubjectPolicies_compliance_PROXIMUS);
        Files.createDirectory(dataSubjectPolicies_non_compliance_PROXIMUS);
        Files.createDirectory(dataControllerPolicies_compliance_TR);
        Files.createDirectory(dataControllerPolicies_non_compliance_TR);
        Files.createDirectory(dataSubjectPolicies_compliance_TR);
        Files.createDirectory(dataSubjectPolicies_non_compliance_TR);

    }

    private static final int UPPER_BOUND = 2;
    private static final int INDIVIDUAL_PERCENTAGE = 1;
    private static final Random random = new Random();

    private static int yes = 0;
    private static int no = 0;
    private static final Logger LOG = Logger.getLogger(Generator.class);
    private static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private static final OWLDataFactory dataFactory = manager.getOWLDataFactory();

    private static final Map<OWLClassExpression, OWLClassExpression> replacements = new HashMap<>();
    private static final Map<OWLClassExpression, OWLClassExpression> replacementsRight = new HashMap<>();
    private static final OWLOntology baseOntology = DataManager.loadOntology(DataManager.PATH_OLD_ONTOLOGY);
    private static final Map<OWLClass, OWLIndividual> classesIndividuals = new HashMap<>();
    private static final Set<OWLClassAssertionAxiom> classAssertions = new HashSet<>();

    public static void main(String[] args) throws IOException, OWLOntologyCreationException {
//        createDirectoryTree();

        createInstances(0);
        createClassAssertions();
        insertClassAssertionsToOntology();

        createInstances(1);
        createClassAssertions();
        insertClassAssertionsToOntology();

        Configuration configuration = new Configuration();
        ReasonerFactory reasonerFactory = new ReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createReasoner(baseOntology, configuration);

        boolean consistent = reasoner.isConsistent();
        System.out.println("New Ontology is consistent? " + consistent);
        saveOntology(DataManager.PATH_NEW_ONTOLOGY + "/import.owl");
        manager.clearOntologies();

//        makeCompliantReplacements(
//                reasoner,
//                DataManager.OLD_COMPLIANCE_DATA_CONTROLLER_PROXIMUS_FOLDER,
//                DataManager.COMPLIANCE_DATA_CONTROLLER_PROXIMUS_FOLDER,
//                DataManager.COMPLIANCE_DATA_SUBJECTS_PROXIMUS_FOLDER,
//                "benchmarkWithNominal/dataset/pilot/compliance/DataSubjectPolicies/PROXIMUS/PROXIMUS",
//                "/PROXIMUS"
//        );
//        makeCompliantReplacements(
//                reasoner,
//                DataManager.OLD_COMPLIANCE_DATA_CONTROLLER_TR_FOLDER,
//                DataManager.COMPLIANCE_DATA_CONTROLLER_TR_FOLDER,
//                DataManager.COMPLIANCE_DATA_SUBJECTS_TR_FOLDER,
//                "benchmarkWithNominal/dataset/pilot/compliance/DataSubjectPolicies/TR/TR",
//                "/TR"
//        );
//        makeCompliantReplacements(
//                reasoner,
//                DataManager.OLD_NOT_COMPLIANCE_DATA_CONTROLLER_PROXIMUS_FOLDER,
//                DataManager.NOT_COMPLIANCE_DATA_CONTROLLER_PROXIMUS_FOLDER,
//                DataManager.NOT_COMPLIANCE_DATA_SUBJECTS_PROXIMUS_FOLDER,
//                "benchmarkWithNominal/dataset/pilot/non-compliance/DataSubjectPolicies/PROXIMUS/PROXIMUS",
//                "/PROXIMUS"
//        );
//        makeCompliantReplacements(
//                reasoner,
//                DataManager.OLD_NOT_COMPLIANCE_DATA_CONTROLLER_TR_FOLDER,
//                DataManager.NOT_COMPLIANCE_DATA_CONTROLLER_TR_FOLDER,
//                DataManager.NOT_COMPLIANCE_DATA_SUBJECTS_TR_FOLDER,
//                "benchmarkWithNominal/dataset/pilot/non-compliance/DataSubjectPolicies/TR/TR",
//                "/TR"
//        );
    }

    private static void makeCompliantReplacements(
            OWLReasoner reasoner,
            String oldCompliantControllerPath,
            String newComplianceDataControllerFolder,
            String complianceDataSubjectsFolder,
            String pathToSave,
            String folderType
    ) throws OWLOntologyCreationException, IOException {

        DataManager.loadOldControllerPolicies(oldCompliantControllerPath);
        Map<String, OWLClassExpression> controllerPolicies = DataManager.getControllerPolicies();
        Map<String, String> controllerPoliciesFile = DataManager.getControllerPoliciesFile();

        for (String controllerID : controllerPolicies.keySet()) {

            OWLClassExpression expression = controllerPolicies.get(controllerID);
            makeRecursiveReplacements(expression);

            String file = controllerPoliciesFile.get(controllerID);
            OWLOntology controllerOntology = manager.loadOntologyFromOntologyDocument(new File(file));
            List<OWLOntologyChange> change = individualReplacer.change(controllerOntology);
            controllerOntology.applyChangesAndGetDetails(change);
            savePolicy(controllerOntology, newComplianceDataControllerFolder + "/" + controllerID + ".owl");


            Map<String, OWLClassExpression> subjectsPolicies = getSubjectPoliciesFromControllerPolicies(controllerID, complianceDataSubjectsFolder, folderType);
            Map<String, String> subjectsPoliciesFiles = DataManager.getSubjectsPoliciesFiles();
            String toSave = pathToSave + controllerID.substring(controllerID.indexOf("_") + 1);
            Path dir = Paths.get(toSave);
            Files.createDirectory(dir);

            int i = 0;
            for (String subID : subjectsPolicies.keySet()) {
                String fileSubject = subjectsPoliciesFiles.get(subID);
                OWLOntology subjectOntology = manager.loadOntologyFromOntologyDocument(new File(fileSubject));
                List<OWLOntologyChange> change1 = individualReplacerToDataSubjects.change(subjectOntology);
                subjectOntology.applyChangesAndGetDetails(change1);
                verifyAndSave(reasoner, controllerOntology, toSave, subID, fileSubject, subjectOntology);
                i++;
            }

            System.out.println("Processing: (" + controllerID + " , " + i + " )");
            replacements.clear();
        }
        System.out.println("Replaced: " + yes);
        System.out.println("Not replaced: " + no);

    }

    private static void verifyAndSave(OWLReasoner reasoner, OWLOntology controllerOntology, String toSave, String subID, String fileSubject, OWLOntology subjectOntology) throws OWLOntologyCreationException {
        if (!verifyEntail(reasoner, controllerOntology, subjectOntology)) {
            savePolicy(subjectOntology, toSave + "/" + subID + ".owl");
        } else {
            manager.clearOntologies();
            savePolicy(
                    manager.loadOntologyFromOntologyDocument(new File(fileSubject)),
                    toSave + "/" + subID + ".owl"
            );
        }
        manager.clearOntologies();
    }

    private static boolean verifyEntail(OWLReasoner reasoner, OWLOntology controllerOntology, OWLOntology subjectOntology) throws OWLOntologyCreationException {
        OWLEquivalentClassesAxiom a = controllerOntology.axioms(AxiomType.EQUIVALENT_CLASSES).toList().iterator().next();
        OWLEquivalentClassesAxiom b = subjectOntology.axioms(AxiomType.EQUIVALENT_CLASSES).toList().iterator().next();
        Iterator<OWLClassExpression> iA = a.getOperandsAsList().iterator();
        Iterator<OWLClassExpression> iB = b.getOperandsAsList().iterator();
        iA.next();
        iB.next();
        OWLClassExpression left = iA.next();
        OWLClassExpression right = iB.next();
        OWLSubClassOfAxiom owlSubClassOfAxiom = dataFactory.getOWLSubClassOfAxiom(left, right);
        return reasoner.isEntailed(owlSubClassOfAxiom);
    }


    private static Map<String, OWLClassExpression> getSubjectPoliciesFromControllerPolicies(String controllerID, String complianceDataSubjectsFolder, String folderType) {
        String id = controllerID.substring(controllerID.indexOf("_") + 1);
        DataManager.loadOldDataSubjectsPolicies(complianceDataSubjectsFolder + folderType + id);
        return DataManager.getSubjectsPolicies();
    }

    private static void makeRecursiveReplacements(OWLClassExpression expression) {
        if (expression instanceof OWLObjectIntersectionOf policy) {
            List<OWLObjectSomeValuesFrom> valuesFroms = policy
                    .getOperandsAsList()
                    .stream()
                    .filter(OWLObjectSomeValuesFrom.class::isInstance)
                    .map(OWLObjectSomeValuesFrom.class::cast)
                    .toList();
            for (OWLObjectSomeValuesFrom valuesFrom : valuesFroms) {
                OWLClassExpression filler = valuesFrom.getFiller();
                OWLObjectPropertyExpression property = valuesFrom.getProperty();
                if (filler instanceof OWLClass) {
                    List<OWLClassAssertionAxiom> assertionAxiomsFiltered = classAssertions
                            .stream()
                            .filter(owlClassAssertionAxiom -> owlClassAssertionAxiom.getClassExpression().equals(filler)).toList();
                    if (!assertionAxiomsFiltered.isEmpty()) {
                        OWLIndividual individual = assertionAxiomsFiltered.get(0).getIndividual();
                        OWLObjectOneOf oneOf = dataFactory.getOWLObjectOneOf(individual);
                        OWLObjectSomeValuesFrom newExistRd = dataFactory.getOWLObjectSomeValuesFrom(property, oneOf);
                        replacements.put(valuesFrom, newExistRd);
                    }

                } else {
                    makeRecursiveReplacements(filler);
                }
            }
        }
        if (expression instanceof OWLObjectUnionOf policy) {
            for (OWLClassExpression conjunct : policy.asDisjunctSet()) {
                makeRecursiveReplacements(conjunct);
            }
        }
    }


    private static void saveOntology(String owlPathFile) {
        try {
            manager.saveOntology(baseOntology, new FunctionalSyntaxDocumentFormat(), new FileOutputStream(owlPathFile));
        } catch (OWLOntologyStorageException | FileNotFoundException e) {
            System.err.println("Error during write ontology on " + owlPathFile);
        }
    }

    private static void savePolicy(OWLOntology policy, String owlPathFile) {
        try {
            manager.saveOntology(policy, new FunctionalSyntaxDocumentFormat(), new FileOutputStream(owlPathFile));
        } catch (OWLOntologyStorageException | FileNotFoundException e) {
            System.err.println("Error during write policy on " + owlPathFile);
        }
    }

    private static void insertClassAssertionsToOntology() {
        for (OWLClassAssertionAxiom classAssertion : classAssertions) {
            manager.addAxiom(baseOntology, classAssertion);
        }
    }

    private static void createClassAssertions() {
        classAssertions.clear();
        for (OWLClass owlClass : classesIndividuals.keySet()) {
            OWLIndividual individual = classesIndividuals.get(owlClass);
            OWLClassAssertionAxiom assertionAxiom = dataFactory.getOWLClassAssertionAxiom(owlClass, individual);
            classAssertions.add(assertionAxiom);
            System.out.println(assertionAxiom);
        }
    }

    private static void createInstances(int individualNumber) {
        for (OWLClass owlClass : baseOntology.getClassesInSignature()) {
            IRI iri = IRI.create("instance-" + individualNumber + "-of-" + owlClass.getIRI().getShortForm());
            OWLIndividual individual = createNewIndividual(iri);
            classesIndividuals.put(owlClass, individual);
        }
    }

    private static OWLIndividual createNewIndividual(IRI iri) {
        return dataFactory.getOWLNamedIndividual(iri);
    }


    private static final OWLObjectTransformer<OWLClassExpression> individualReplacer = new OWLObjectTransformer<>(
            item -> !(item instanceof OWLDataSomeValuesFrom),
            Generator::makeIndividualsReplaces,
            dataFactory,
            OWLClassExpression.class
    );
    private static final OWLObjectTransformer<OWLClassExpression> individualReplacerToDataSubjects = new OWLObjectTransformer<>(
            item -> !(item instanceof OWLDataSomeValuesFrom),
            Generator::makeIndividualsReplacesToDataSubject,
            dataFactory,
            OWLClassExpression.class
    );

    private static OWLClassExpression makeIndividualsReplaces(OWLClassExpression input) {
        OWLClassExpression value = replacements.get(input);

        if (value == null) {
            return input;
        } else {
            if (random.nextInt(UPPER_BOUND) < INDIVIDUAL_PERCENTAGE) {
                yes++;
                replacementsRight.put(input, value);
                return value;
            } else {
                no++;
                return input;
            }
        }
    }

    private static OWLClassExpression makeIndividualsReplacesToDataSubject(OWLClassExpression input) {
        OWLClassExpression value = replacementsRight.get(input);

        if (value == null) {
            return input;
        } else {
            return value;
        }
    }


}
