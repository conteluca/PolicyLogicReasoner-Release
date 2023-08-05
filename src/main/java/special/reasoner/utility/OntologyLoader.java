package special.reasoner.utility;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasonerRuntimeException;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class OntologyLoader {
    private OntologyLoader() {
    }

    private static final Logger LOG = Logger.getLogger(OntologyLoader.class);
    private static final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private static final Set<OWLOntology> ontologies = new HashSet<>();
    private static final String PATH_ONTOLOGY = "ontology/";

    public static OWLOntology load() {
        File file = new File(PATH_ONTOLOGY);
        System.out.println("Loading ontology from "+file.getAbsolutePath());
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
            return manager.createOntology(IRI.create("http://base.ontologies.com/base"), ontologies);
        } catch (OWLOntologyCreationException e) {
            throw new OWLReasonerRuntimeException();
        }

    }
}