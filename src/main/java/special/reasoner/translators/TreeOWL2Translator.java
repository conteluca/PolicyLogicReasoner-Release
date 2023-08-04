package special.reasoner.translators;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.*;
import special.model.ORNODE;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class TreeOWL2Translator {
    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private final OWLDataFactory factory = manager.getOWLDataFactory();
    private final OWLOntology ontology;
    private final ORNODE tree;

    private OWLEquivalentClassesAxiom expression;

    public TreeOWL2Translator(ORNODE tree)  {
        this.tree = tree;
        try {
            ontology = manager.createOntology();
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }

    }

    public OWLEquivalentClassesAxiom translate(String ID) {
        OWLClassExpression classExpression = this.tree.toOWLClassExpression(this.factory);

        this.expression = factory.getOWLEquivalentClassesAxiom(
                factory.getOWLClass(IRI.create("bp:DataControllerPolicy_" + ID)),
                classExpression
        );
        return this.expression;
    }

    public boolean save(String path) {
        manager.addAxiom(this.ontology, this.expression);
        try {
            manager.saveOntology(ontology, new FunctionalSyntaxDocumentFormat(), new FileOutputStream(path));
            return true;
        } catch (OWLOntologyStorageException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
