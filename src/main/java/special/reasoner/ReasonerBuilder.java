package special.reasoner;

import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import special.model.tree.ANDNODE;
import special.model.tree.ORNODE;
import special.reasoner.cache.IntervalSafePoliciesCacheInMemory;
import special.reasoner.cache.PolicyCacheInMemory;

public
class ReasonerBuilder {
    private ReasonerBuilder() {
    }

    public static PLReasoner buildReasoner(OWLOntology ontology) {
//        System.out.println("Building Default PLR  ...");
        PLConfiguration configuration = new PLConfiguration();
        PLReasonerFactory reasonerFactory = new PLReasonerFactory();
        return (PLReasoner) reasonerFactory
                .createPolicyLogicReasoner(ontology, configuration, BufferingMode.NON_BUFFERING);
    }

    public static PLReasoner buildBufferingReasoner(OWLOntology ontology) {
        System.out.println("Building Buffering PLR ...");
        PLConfiguration configuration = new PLConfiguration();
        PLReasonerFactory reasonerFactory = new PLReasonerFactory();
        return (PLReasoner) reasonerFactory
                .createPolicyLogicReasoner(ontology, configuration, BufferingMode.BUFFERING);
    }

    public static PLReasoner buildSingleCacheReasoner(OWLOntology ontology, int cacheSize) {
        System.out.println("Building Single Cache PLR with cacheSize " + cacheSize + "...");
        PLConfiguration configuration = new PLConfiguration();
        PLReasonerFactory reasonerFactory = new PLReasonerFactory();
        PLReasoner plReasoner = (PLReasoner) reasonerFactory
                .createPolicyLogicReasoner(ontology, configuration, BufferingMode.BUFFERING,
                        new PolicyCacheInMemory<ANDNODE>(cacheSize),
                        new IntervalSafePoliciesCacheInMemory<ANDNODE, ORNODE>());
        plReasoner.setFullConceptCache(false, false);
        plReasoner.setSimpleConceptCache(true, true);
        return plReasoner;
    }

    public static PLReasoner buildDoubleCacheReasoner(OWLOntology ontology, int cacheSize) {
        System.out.println("Building Double PLR with cacheSize " + cacheSize + "...");
        PLConfiguration configuration = new PLConfiguration(true, true, 60000);
        PLReasonerFactory reasonerFactory = new PLReasonerFactory();
        PLReasoner plReasoner = (PLReasoner) reasonerFactory
                .createPolicyLogicReasoner(ontology, configuration, BufferingMode.BUFFERING,
                        new PolicyCacheInMemory<ANDNODE>(cacheSize),
                        new IntervalSafePoliciesCacheInMemory<ANDNODE, ORNODE>());
        plReasoner.setFullConceptCache(true, true);
        plReasoner.setSimpleConceptCache(true, true);
        return plReasoner;
    }

    public static Reasoner buildHermitReasoner(OWLOntology ontology) {
        System.out.println("Building Hermit Reasoner...");
        Configuration configuration = new Configuration();
        ReasonerFactory reasonerFactory = new ReasonerFactory();
        return (Reasoner) reasonerFactory.createReasoner(ontology, configuration);
    }

}
