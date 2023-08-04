package special.model;
import org.json.JSONArray;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import special.reasoner.PLReasoner;
import special.reasoner.PLReasonerFactory;

import java.io.File;
import java.util.*;


/**
 * @author Luca Conte
 */


public class OWL2PolicyIterator implements Iterator<PolicyLogic<OWLClassExpression>> {
    private List<PolicyLogic<OWLClassExpression>> policies;
    private int currentIndex;
    private static final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();


    public OWL2PolicyIterator(String path){
        File directory = new File(path);
        if (directory.isDirectory()) {
            List<File> owlFiles = Arrays.stream(Objects.requireNonNull(directory.listFiles())).filter(x -> x.getName().endsWith(".owl")).toList();
            this.policies = new LinkedList<>();
            for (File fi : owlFiles) {
                try {
                    OWLOntology expression = manager.loadOntologyFromOntologyDocument(fi);
                    List<OWLEquivalentClassesAxiom> owlEquivalentClassesAxioms = expression.axioms(AxiomType.EQUIVALENT_CLASSES).toList();
                    for (OWLEquivalentClassesAxiom classesAxiom : owlEquivalentClassesAxioms) {
                        List<OWLClassExpression> operands = classesAxiom.getOperandsAsList();
                        Iterator<OWLClassExpression> iterator = operands.iterator();
                        String left = iterator.next().toString();
                        OWLClassExpression right = iterator.next();
                        String id = left.substring(left.indexOf("_") + 1, left.length() - 1);
                        PolicyLogic<OWLClassExpression> p = new PolicyLogic<>(id, right);
                        this.policies.add(p);
                    }
                } catch (OWLOntologyCreationException e) {
                    throw new OWLOntologyResourceAccessException(e);
                }
            }

        }else{
            System.err.println(path+" is not a directory");
            System.exit(1);
        }
        this.currentIndex = 0;
    }

    @Override
    public boolean hasNext() {
        return currentIndex < policies.size();
    }

    @Override
    public PolicyLogic<OWLClassExpression> next() {
        return policies.get(currentIndex++);
    }
    public PolicyLogic<JSONArray> toJson() {
        PolicyLogic<OWLClassExpression> next = next();
        //TODO
        return null;
    }
    public PolicyLogic<ORNODE> toTree() {
        PolicyLogic<OWLClassExpression> next = next();
        //TODO
        return null;
    }

}
