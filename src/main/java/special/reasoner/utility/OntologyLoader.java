package special.reasoner.utility;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasonerRuntimeException;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class OntologyLoader {
    private OntologyLoader() {}

    private static final Logger LOG = Logger.getLogger(OntologyLoader.class);
    private static final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private static final Set<OWLOntology> ontologies = new HashSet<>();
    public static final String PATH_ONTOLOGY = "ontology/";

    private static int ontologyNumber = 0;

    public static Set<String> getOntologyNames(@Nonnull File policy) {
        if(!policy.getName().endsWith(".json")){
            throw new IllegalArgumentException();
        }
        byte[] data = new byte[(int) policy.length()];
        FileInputStream stream;
        try {
            stream = new FileInputStream(policy);
            stream.read(data);
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String p = new String(data, StandardCharsets.UTF_8);
        JSONObject objects = new JSONObject(p);
        JSONArray o = (JSONArray) objects.get("@ontologies");
        Set<String> set = new HashSet<>();
        for (Object object : o) {
            set.add((String) object);
        }
        return set;
    }
    public static Set<String> getOntologyNames(@Nonnull Set<File> policies) {
        Set<String> full = new HashSet<>();
        for (File policy : policies) {
            Set<String> tmp = getOntologyNames(policy);
            full.addAll(tmp);
        }
        return full;
    }
    public static OWLOntology load(@Nonnull Set<String> set) {
        ontologies.clear();

        for (String uri : set) {
            IRI ontologyIRI = IRI.create(uri);
            OWLOntology load = OntologyLoader.load(ontologyIRI);
            ontologies.add(load);
        }
        try {
            return manager.createOntology(IRI.create("https://trapeze-project.eu/tmp.ontology"+ontologyNumber++),
                    ontologies);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
    }
    public static OWLOntology load(@Nonnull IRI iri) {

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        try {
            return manager.loadOntology(iri);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static OWLOntology load(File file) {
        System.out.println("Loading ontology from " + file.getAbsolutePath());
        ontologies.clear();
        if (!file.exists()) {
            LOG.error("Ontologies folder/file not exists! Path: " + ontologies);
            System.exit(1);
        }
        for (File i : Objects.requireNonNull(file.listFiles())) {
            if (i.isFile() && i.getName().endsWith(".owl")) {
                try {
                    ontologies.add(manager.loadOntologyFromOntologyDocument(new File(i.getAbsolutePath())));
                } catch (OWLOntologyCreationException e) {
                    throw new OWLReasonerRuntimeException();
                }
            }
        }
        manager.clearOntologies();
        try {
            return manager.createOntology(IRI.create("https://trapeze-project.eu/tmp.ontology"+ontologyNumber++), ontologies);
        } catch (OWLOntologyCreationException e) {
            throw new OWLReasonerRuntimeException();
        }

    }
}
