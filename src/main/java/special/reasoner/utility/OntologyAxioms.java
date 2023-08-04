package special.reasoner.utility;
import org.semanticweb.owlapi.model.*;

import java.util.List;
import java.util.Set;

/**
 * @author Luca Conte
 */

public class OntologyAxioms {
    private final Set<OWLObjectProperty> properties;
    private final Set<OWLClass> classes;
    private final Set<OWLNamedIndividual> individuals;
    private final Set<OWLDataProperty> dataProperties;

    public Set<OWLObjectProperty> getProperties() {
        return properties;
    }

    public Set<OWLClass> getClasses() {
        return classes;
    }

    public OntologyAxioms(OWLOntology ontology ) {
        this.properties = ontology.getObjectPropertiesInSignature();
        this.classes = ontology.getClassesInSignature();
        this.individuals = ontology.getIndividualsInSignature();
        this.dataProperties = ontology.getDataPropertiesInSignature();
    }

    public OWLObjectProperty getProperty(String property) {
        return properties
                .stream()
                .filter(prop -> prop.getIRI().toString().equals(property))
                .toList()
                .get(0);
    }

    public boolean isProperty(String property) {

        List<String> list = this.properties
                .stream()
                .map(dataProperty -> dataProperty.getIRI().toString())
                .toList();
        return list.contains(property);
    }

    public OWLClass getConceptName(String clazz) {
        return this.classes
                .stream()
                .filter(concept -> concept.getIRI().toString().equals(clazz))
                .toList()
                .get(0);
    }

    public boolean isClass(String clazz) {
        return this.classes
                .stream()
                .map(concept -> concept.getIRI().toString())
                .toList()
                .contains(clazz);
    }

    public OWLIndividual getIndividual(String individual){
        return this.individuals
                .stream()
                .filter(nominal-> nominal.getIRI().toString().equals(individual))
                .toList()
                .get(0);
    }
    public boolean isIndividual(String individual) {
        return this.individuals
                .stream()
                .map(item -> item.getIRI().toString())
                .toList()
                .contains(individual);
    }

    public OWLDataProperty getDataProperty(String property) {
        return this.dataProperties
                .stream()
                .filter(dataProperty -> dataProperty.getIRI().toString().equals(property))
                .toList()
                .get(0);
    }

    public boolean isDataProperty(String dataProperty) {
        return this.dataProperties
                .stream()
                .map(property -> property.getIRI().toString())
                .toList()
                .contains(dataProperty);
    }

}

