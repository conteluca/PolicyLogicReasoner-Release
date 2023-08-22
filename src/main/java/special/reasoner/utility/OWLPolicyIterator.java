package special.reasoner.utility;


import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import special.model.PolicyLogic;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;

public class OWLPolicyIterator implements Iterator<PolicyLogic<OWLClassExpression>> {
    private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    private final List<PolicyLogic<OWLClassExpression>> policies = new LinkedList<>();

    private int currentIndex = 0;

    public OWLPolicyIterator(
                             final @Nonnull String directoryOWLPolicyPath) {
        try {
            File dir = new File(directoryOWLPolicyPath);
            List<File> files = Arrays.stream(Objects.requireNonNull(dir.listFiles()))
                    .filter(file -> file.getName().endsWith(".owl")).toList();
            for (File file : files) {
                OWLOntology policy = manager.loadOntologyFromOntologyDocument(file);
                List<OWLAxiom> list = policy.axioms().toList();
                OWLEquivalentClassesAxiom owlEquivalentClassesAxiom = (OWLEquivalentClassesAxiom) list.get(0);
                OWLClassExpression l = owlEquivalentClassesAxiom.getClassExpressionsAsList().get(0);
                OWLClassExpression r = owlEquivalentClassesAxiom.getClassExpressionsAsList().get(1);
                String id = l.toString().substring(l.toString().indexOf('#')+1,l.toString().length()-1);
                policies.add(new PolicyLogic<>(id,r));
            }

        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasNext() {
        return currentIndex<this.policies.size();
    }

    @Override
    public PolicyLogic<OWLClassExpression> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return this.policies.get(currentIndex++);
    }
}
