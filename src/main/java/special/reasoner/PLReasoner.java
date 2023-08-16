/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner;

import java.io.File;
import java.util.*;
import java.util.stream.*;
import javax.annotation.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.impl.*;
import org.semanticweb.owlapi.util.*;
import org.semanticweb.owlapi.vocab.*;
import special.model.*;
import special.model.exception.*;
import special.model.hierarchy.*;
import special.model.tree.ANDNODE;
import special.model.tree.EntityIntersectionNode;
import special.model.tree.IntRange;
import special.model.tree.ORNODE;
import special.reasoner.cache.*;
import special.model.SignedPolicy;
import special.reasoner.factory.PLConfiguration;
import special.reasoner.utility.OntologyAxioms;
import special.reasoner.utility.TranslatorEngine;

import static org.semanticweb.owlapi.util.OWLAPIPreconditions.*;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.*;

/**
 * @author Luca Conte
 */
public class PLReasoner implements OWLReasoner {

    public static final String REASONER_NAME = "PolicyLogicReasoner";
    private static final Version VERSION = new Version(1, 5, 3, 0);
    private static final Set<AxiomType<? extends OWLAxiom>> SUPPORTED_AXIOMS = new HashSet<>(Arrays.asList(AxiomType.SUBCLASS_OF, AxiomType.FUNCTIONAL_DATA_PROPERTY, AxiomType.DISJOINT_CLASSES, AxiomType.FUNCTIONAL_OBJECT_PROPERTY, AxiomType.OBJECT_PROPERTY_RANGE));
    private static final Set<InferenceType> PRECOMPUTED_INFERENCE_TYPES = new HashSet<>(Arrays.asList(InferenceType.CLASS_HIERARCHY, InferenceType.DISJOINT_CLASSES));
    private final SingleKeyCache<OWLClassExpression> fullSubClassConceptCache;
    private final SingleKeyCache<OWLClassExpression> fullSuperClassConceptCache;
    private final DoubleKeyCache<OWLClassExpression, OWLClassExpression, ANDNODE> fullConceptlIntervalSafeCache;
    private final SingleKeyCache<ANDNODE> simpleConceptCache;
    private final DoubleKeyCache<ANDNODE, ORNODE, ANDNODE> simpleConceptIntervalSafeCache;
    private final OWLReasonerConfiguration configuration;
    private final OWLOntologyManager manager;
    private final OWLOntology rootOntology; // Ontology
    private final BufferingMode bufferingMode; // Mode BUFFERING - NON_BUFFERING. Keeps any changes in memory or use them at runtime.
    private final List<OWLOntologyChange> pendingChanges; // Keeps pending changes in ontology - Only with BUFFERING
    private final RawOntologyChangeListener ontologyChangeListener; // Listener that catch changes in ontology
    private final HierarchyGraph<OWLClass> classHierarchy;
    private final HierarchyGraph<OWLDataProperty> dataPropertyHierarchy;
    private final HierarchyGraph<OWLObjectPropertyExpression> objectPropertyHierarchy;
    private final OWLClass bottomEntity;
    private boolean fullTreeCache; // Cache to store concepts at runtime. If false, any concept is processed at each call
    private boolean fullIntervalSafeCache = true; // Cache to store concepts interval-safety normalized at runtime. If false, any concept is processed at each call
    private boolean simpleTreeCache;
    private boolean simpleIntervalSafeCache;
    private boolean normalizeSuperClassConcept = false;
    private boolean interrupted = false;
    private boolean isInconsistent = false;

    private int stsCount = 0;


    public PLReasoner(OWLOntology rootOntology,
                      OWLReasonerConfiguration configuration,
                      BufferingMode bufferingMode,
                      SingleKeyCache<OWLClassExpression> singleKeyCache,
                      DoubleKeyCache<OWLClassExpression, OWLClassExpression, ANDNODE> doubleKeyCache) {
        this.rootOntology = checkNotNull(rootOntology, "rootOntology cannot be null");
        this.bufferingMode = checkNotNull(bufferingMode, "bufferingMode cannot be null");
        this.configuration = checkNotNull(configuration, "configuration cannot be null");
        this.fullSubClassConceptCache = checkNotNull(singleKeyCache, "singleKeyCache cannot be null");
        this.fullConceptlIntervalSafeCache = checkNotNull(doubleKeyCache, "doubleKeyCache cannot be null");

        this.fullSuperClassConceptCache = new PolicyCacheInMemory<>(8192);
        this.simpleConceptCache = new PolicyCacheInMemory<>(8192);
        this.simpleConceptIntervalSafeCache = new IntervalSafePoliciesCacheInMemory<>(8192);

        this.manager = this.rootOntology.getOWLOntologyManager();
        OWLDataFactory dataFactory = this.manager.getOWLDataFactory();
        this.pendingChanges = new LinkedList<>();

        this.ontologyChangeListener = new RawOntologyChangeListener();
        this.manager.addOntologyChangeListener(this.ontologyChangeListener);
        this.bottomEntity = dataFactory.getOWLNothing();
        OWLClass topEntity = dataFactory.getOWLThing();
        this.classHierarchy
                = new ClassHierarchyInOntology(
                topEntity,
                this.bottomEntity,
                new ClassInOntology(this.rootOntology, dataFactory));
        this.dataPropertyHierarchy
                = new DataPropertyHierarchyInOntology(
                dataFactory.getOWLTopDataProperty(),
                dataFactory.getOWLBottomDataProperty(),
                new DataPropertyInOntology(this.rootOntology, dataFactory));
        this.objectPropertyHierarchy
                = new ObjectPropertyHierarchyInOntology(
                dataFactory.getOWLTopObjectProperty(),
                dataFactory.getOWLBottomObjectProperty(),
                new ObjectPropertyInOntology(this.rootOntology, dataFactory));
        this.prepareHierarchy();

        if (configuration.getClass().equals(PLConfiguration.class)) {
            PLConfiguration conf = (PLConfiguration) configuration;
            this.fullTreeCache = conf.hasCacheFullConcept();
            this.fullIntervalSafeCache = conf.hasCacheFullConceptIntervalSafe();
            this.simpleTreeCache = conf.hasCacheSimpleConcept();
            this.simpleIntervalSafeCache = conf.hasCacheSimpleConceptIntervalSafe();
            this.normalizeSuperClassConcept = conf.hasNormalizationSuperClassConcept();
        } else {
            this.fullTreeCache = PLConfiguration.DEFAULT_FULL_TREE_CACHE;
            this.fullIntervalSafeCache = PLConfiguration.DEFAULT_FULL_INTERVAL_SAFE_CACHE;
            this.simpleTreeCache = PLConfiguration.DEFAULT_SIMPLE_TREE_CACHE;
            this.simpleIntervalSafeCache = PLConfiguration.DEFAULT_SIMPLE_INTERVAL_SAFE_CACHE;
            this.normalizeSuperClassConcept = PLConfiguration.DEFAULT_NORMALIZE_SUPER_CLASS_CONCEPT;
        }
    }

    public PLReasoner(OWLOntology rootOntology, OWLReasonerConfiguration configuration,
                      BufferingMode bufferingMode) {
        this(rootOntology,
                configuration,
                bufferingMode,
                new PolicyCacheInMemory<>(8192),
                new IntervalSafePoliciesCacheInMemory<>(8192));
    }

    /**
     * Compute the hierarchy's classification
     */
    public final void prepareHierarchy() {
        this.classHierarchy.compute();
        this.dataPropertyHierarchy.compute();
        this.objectPropertyHierarchy.compute();
    }

    /**
     * Enable or disable the in memory cache for Full Concept
     *
     * @param treeCache      Cache to preserve a tree after normalization of 7 rules
     * @param intervalSafety Preserves a tree after interval safety
     *                       normalization
     */
    public final void setFullConceptCache(boolean treeCache, boolean intervalSafety) {
        this.fullTreeCache = treeCache;
        this.fullIntervalSafeCache = intervalSafety;
    }

    /**
     * Enable or disable the in memory cache for Simple Concept
     *
     * @param treeCache      Cache to preserve a tree after normalization of 7 rules
     * @param intervalSafety Preserves a tree after interval safety
     *                       normalization
     */
    public final void setSimpleConceptCache(boolean treeCache, boolean intervalSafety) {
        this.simpleTreeCache = treeCache;
        this.simpleIntervalSafeCache = intervalSafety;
    }

    /**
     * Clear internal caches
     */
    public void clearCache() {
        if (this.fullSubClassConceptCache != null) {
            this.fullSubClassConceptCache.clear();
        }
        if (this.fullSuperClassConceptCache != null) {
            this.fullSuperClassConceptCache.clear();
        }
        if (this.fullConceptlIntervalSafeCache != null) {
            this.fullConceptlIntervalSafeCache.clear();
        }
        if (this.simpleConceptIntervalSafeCache != null) {
            this.simpleConceptIntervalSafeCache.clear();
        }
        if (this.simpleConceptCache != null) {
            this.simpleConceptCache.clear();
        }
    }

    /**
     * Return the OWLDataFactory
     *
     * @return OWLDataFactory
     */
    public OWLDataFactory getOWLDataFactory() {
        return rootOntology.getOWLOntologyManager().getOWLDataFactory();
    }

    /**
     * Return the reasoner name
     *
     * @return String
     */
    @Override
    public String getReasonerName() {
        return REASONER_NAME;
    }

    /**
     * Return the reasoner version
     *
     * @return Version
     */
    @Override
    public Version getReasonerVersion() {
        return VERSION;
    }

    /**
     * Return the root ontology used by the reasoner
     *
     * @return String
     */
    @Override
    public OWLOntology getRootOntology() {
        return rootOntology;
    }

    /**
     * Build a ANDNODE tree parsing the concept specified
     *
     * @param inputG OWLClassExpression of the concept specified
     * @return root node ANDNODE
     * @throws special.model.exception.IllegalPolicyLogicExpressionException if
     *                                                                       the specified concept is not a PL concept
     */
    public ANDNODE buildTree(@Nonnull OWLClassExpression inputG) throws IllegalPolicyLogicExpressionException {
        ANDNODE root = new ANDNODE();
        for (OWLClassExpression conjunct : inputG.asConjunctSet()) {
            switch (conjunct.getClassExpressionType()) {
                case OBJECT_ONE_OF -> {
                    OWLObjectOneOf x = (OWLObjectOneOf) conjunct;
                    OWLIndividual i = x.getOperandsAsList().get(0);
                    root.addIndividualName(i);
                }
                case OWL_CLASS -> root.addConceptName(conjunct.asOWLClass());
                case DATA_SOME_VALUES_FROM -> {
                    int minCard = 0;
                    int maxCard = Integer.MAX_VALUE;
                    OWLDataSomeValuesFrom dataConstraint = (OWLDataSomeValuesFrom) conjunct;
                    OWLDataProperty p = dataConstraint.getProperty().asOWLDataProperty();     //data constraint
                    OWLDatatypeRestriction restriction = (OWLDatatypeRestriction) dataConstraint.getFiller();
                    for (OWLFacetRestriction facetRestriction : restriction.facetRestrictionsAsList()) {
                        OWLFacet facet = facetRestriction.getFacet();
                        switch (facet) {
                            case MIN_INCLUSIVE -> minCard = facetRestriction.getFacetValue().parseInteger();
                            case MAX_INCLUSIVE -> maxCard = facetRestriction.getFacetValue().parseInteger();
                            case MIN_EXCLUSIVE -> minCard = facetRestriction.getFacetValue().parseInteger() + 1;
                            case MAX_EXCLUSIVE -> maxCard = facetRestriction.getFacetValue().parseInteger() - 1;
                            default ->
                                    throw new IllegalPolicyLogicExpressionException("Facet type non allowed:" + facet + "\nType found: " + conjunct.getNNF());
                        }
                    }
                    root.addDataProperty(p, new IntRange(minCard, maxCard));
                }
                case OBJECT_SOME_VALUES_FROM -> {
                    OWLObjectSomeValuesFrom existential = (OWLObjectSomeValuesFrom) conjunct;
                    OWLClassExpression fillerC = existential.getFiller();
                    ANDNODE child = buildTree(fillerC);
                    root.addChild(existential, child);
                }
                case OBJECT_UNION_OF -> {
                    Set<OWLClassExpression> disjunctive = conjunct.asDisjunctSet();
                    Deque<ANDNODE> nodes = new ArrayDeque<>(disjunctive.size());
                    for (OWLClassExpression expression : disjunctive) {
                        ANDNODE node = buildTree(expression);
                        nodes.add(node);
                    }
                    root.addDisjuncts(nodes);
                }
                default -> throw new IllegalPolicyLogicExpressionException("Type found: " + conjunct.getNNF());
            }
        }
        return root;
    }

    /**
     * Normalize the tree moving disjuncts at top level of the tree (in root
     * node). The result is a DNF concept.
     *
     * @param root ANDNODE
     * @return ORNODE with a tree for each disjunct normalized
     */
    public ORNODE normalizeUnion(final @Nonnull ANDNODE root) {
        ORNODE node = null;
        if (root.hasORNodes()) {
            Deque<ORNODE> rootOrNodes = root.getORNodes();
            Deque<ORNODE> orNodes = new ArrayDeque<>(rootOrNodes.size());
            while (!rootOrNodes.isEmpty()) {
                ORNODE orNode = rootOrNodes.pollFirst();
                node = new ORNODE();
                while (!orNode.isEmpty()) {
                    for (ANDNODE subDisjunction : normalizeUnion(orNode.pollFirst())) {
                        node.addTree(subDisjunction);
                    }
                }
                orNodes.add(node);
            }
            root.clearORNodes();
            root.addORnodes(orNodes);
        }

        for (Iterator<Map.Entry<OWLObjectProperty, List<ANDNODE>>> childrenKeySetIterator = root.getChildrenEntrySet().iterator(); childrenKeySetIterator.hasNext(); ) {
            Map.Entry<OWLObjectProperty, List<ANDNODE>> entry = childrenKeySetIterator.next();
            OWLObjectProperty property = entry.getKey();
            for (Iterator<ANDNODE> children = entry.getValue().iterator(); children.hasNext(); ) {
                ORNODE disjunctsSubTree = normalizeUnion(children.next());
                if (disjunctsSubTree.size() > 1) {
                    ORNODE tmp = new ORNODE(disjunctsSubTree.size());
                    while (!disjunctsSubTree.isEmpty()) {
                        ANDNODE rootSubTree = new ANDNODE();
                        rootSubTree.addChild(property, disjunctsSubTree.pollFirst());
                        tmp.addTree(rootSubTree);
                    }
                    root.addORnode(tmp);
                    children.remove();
                }
            }
            if (root.existsButIsEmpty(property)) {
                childrenKeySetIterator.remove();
            }
        }
        if (root.hasORNodes()) {
            Deque<ORNODE> disjuncts = root.getORNodes();
            if (disjuncts.size() > 1) {
                Collection<ANDNODE> combinations = applyDistributivityIterative(disjuncts);
                node = new ORNODE(combinations.size());
                node.addAll(combinations);
            } else {
                node = new ORNODE();
                node.addTrees(disjuncts.pollFirst());
            }
            root.clearORNodes();
        }
        if (node != null) {
            for (ANDNODE disjunction : node) {
                disjunction.addConceptName(root.getConceptNames());
                for (Map.Entry<OWLDataProperty, List<IntRange>> entry : root.getDataPropertyEntrySet()) {
                    disjunction.addDataProperty(entry.getKey(), entry.getValue());
                }
                for (Map.Entry<OWLObjectProperty, List<ANDNODE>> entry : root.getChildrenEntrySet()) {
                    disjunction.addChild(entry.getKey(), entry.getValue());
                }
            }
        } else {
            node = new ORNODE();
            node.addTree(root);
        }
        return node;
    }

    /**
     * Apply distributivity on orNodes in input. Iterative version.
     *
     * @param orNodes Data structure created by normalizeUnion function
     *                conjuncts Support set with combinations of elements.
     * @return List<ANDNODE> with applied distributivity
     */
    private Collection<ANDNODE> applyDistributivityIterative(final @Nonnull Deque<ORNODE> orNodes) {
        if (orNodes.isEmpty()) {
            return Collections.emptyList();
        }
        Deque<Deque<ANDNODE>> combinations = new ArrayDeque<>(orNodes.getFirst().size());
        Deque<Deque<ANDNODE>> newCombinations;
        for (ANDNODE i : orNodes.pollFirst()) {
            Deque<ANDNODE> newList = new ArrayDeque<>(1);
            newList.add(i);
            combinations.add(newList);
        }
        while (!orNodes.isEmpty()) {
            Deque<ANDNODE> nextList = orNodes.pollFirst();
            newCombinations = new ArrayDeque<>(combinations.size() * nextList.size());
            for (Deque<ANDNODE> first : combinations) {
                for (ANDNODE second : nextList) {
                    Deque<ANDNODE> tmp = new ArrayDeque<>(first.size() + 1);
                    tmp.addAll(first);
                    tmp.add(second);
                    newCombinations.add(tmp);
                }
            }
            combinations = newCombinations;
        }
        Collection<ANDNODE> newOrNodes = new ArrayList<>(combinations.size());
        for (Collection<ANDNODE> combination : combinations) {
            newOrNodes.add(mergeANDNODEs(combination));
        }
        return newOrNodes;
    }

    /**
     * Merge the set of trees in a unique tree
     *
     * @param trees Deque of trees
     * @return ANDNODE merged
     */
    private ANDNODE mergeANDNODEs(final @Nonnull Collection<ANDNODE> trees) {
        ANDNODE root = null;
        if (!trees.isEmpty()) {
            root = new ANDNODE();
            for (ANDNODE tree : trees) {
                root.addConceptName(tree.getConceptNames());
                root.addIndividualName(tree.getIndividualNames());
                for (Map.Entry<OWLDataProperty, List<IntRange>> entry : tree.getDataPropertyEntrySet()) {
                    root.addDataProperty(entry.getKey(), entry.getValue());
                }
                for (Map.Entry<OWLObjectProperty, List<ANDNODE>> entry : tree.getChildrenEntrySet()) {
                    root.addChild(entry.getKey(), entry.getValue());
                }
                if (tree.hasORNodes()) {
                    root.addORnodes(tree.getORNodes());
                }
            }
        }
        return root;
    }

    /**
     * Merge each functional property applying the policy logic's rules. This
     * function apply rules #4, #5 and #6
     *
     * @param root    ANDNODE root node of a tree
     * @param wrapper Support's object to help with normalization of union
     */
    private void mergeFunctional(@Nonnull ANDNODE root, final @Nonnull WrapperBoolean wrapper) {
        Deque<ANDNODE> queueDown = new LinkedList<>();
        queueDown.add(root);
        while (!queueDown.isEmpty()) {
            root = queueDown.pollFirst();
            mergeConstraints(root);
            mergeExistential(root);
            for (Map.Entry<OWLObjectProperty, List<ANDNODE>> entry : root.getChildrenEntrySet()) {
                Set<EntityIntersectionNode<OWLClass>> setRanges = objectPropertyHierarchy.getPropertyRange(entry.getKey());
                for (ANDNODE child : entry.getValue()) {
                    if (!child.hasORNodes()) {
                        if (setRanges.size() > 1) {
                            /* The range is a disjunction */
                            boolean foundDisjunct = false;
                            for (EntityIntersectionNode<OWLClass> intersectionNode : setRanges) {
                                boolean foundConjunct = true;
                                for (OWLClass A : intersectionNode) {
                                    if (!child.containsConceptName(A)) {
                                        foundConjunct = false;
                                        break;
                                    }
                                }
                                if (intersectionNode.getSize() > 0 && foundConjunct) {
                                    foundDisjunct = true;
                                    break;
                                }
                            }
                            if (!foundDisjunct) {
                                List<ANDNODE> disjunctions = new ArrayList<>(setRanges.size());
                                for (EntityIntersectionNode<OWLClass> intersectionNode : setRanges) {
                                    ANDNODE conjunct = new ANDNODE();
                                    for (OWLClass A : intersectionNode) {
                                        if (!child.containsConceptName(A)) {
                                            conjunct.addConceptName(A);
                                        }
                                    }
                                    disjunctions.add(conjunct);
                                }
                                child.addDisjuncts(disjunctions);
                                wrapper.setValue(true);
                            }
                        } else {
                            for (EntityIntersectionNode<OWLClass> intersectionNode : setRanges) {
                                for (OWLClass A : intersectionNode) {
                                    child.addConceptName(A);
                                }
                            }
                        }
                    }
                    queueDown.addFirst(child);
                }
            }
        }
    }

    /* below is new code wrote by Luca Conte
     *  applyRange
     *  mergeNominalInChildren
     *  copyALL
     *  mergeNominal
     *  mergeFunctional
     *  verifyConsistencyRole
     *  preBuild
     * */

    /**
     * Apply each range property on the policy logic's rules.
     * Check merge constraint far all node
     *
     * @param root ANDNODE root node of a tree
     * @author Luca Conte
     */
    public void applyRange(@Nonnull ORNODE root) {
        for (ANDNODE disjunction : root.getDisjunction()) {
            applyRange(disjunction);
        }
    }

    private void applyRange(@Nonnull ANDNODE root) {
        Set<OWLObjectProperty> roles = root.getChildrenKeySet();
        Deque<ORNODE> orNodes = root.getORNodes();

        mergeConstraints(root);    /* before apply range check constraint merging on root */
        for (ORNODE trees : orNodes) { /* if root contains disjunctions then visit them */
            applyRange(trees);
        }
        for (OWLObjectProperty role : roles) {       /* foreach role r */
            Set<EntityIntersectionNode<OWLClass>> rangeSet = objectPropertyHierarchy.getPropertyRange(role); /* get all ranges of r */
            for (ANDNODE child : root.getChildren(role)) {   /* for all child belongs r */
                for (EntityIntersectionNode<OWLClass> e : rangeSet) {
                    for (OWLClass conceptRange : e) {
                        child.addConceptName(conceptRange);      /* add all ranges into child */
                        applyRange(child);
                    }
                }
            }
        }
    }

    /**
     * Merge each individual name if is duplicated in different node.
     * This method modify children
     *
     * @param root ANDNODE root node
     * @param map  Support's map to help with merging of individual names
     * @author Luca Conte
     */
    private void mergeNominalInChildren(@Nonnull ANDNODE root, @Nonnull Map<OWLIndividual, ANDNODE> map) {
        Set<OWLObjectProperty> roles0 = root.getChildrenKeySet();
        ArrayList<OWLObjectProperty> roles = new ArrayList<>(roles0);

        for (OWLObjectProperty role : roles) {
            List<ANDNODE> oldChildren = root.getChildren(role);
            root.removeChildren(role);

            for (ANDNODE child : oldChildren) {
                root.addChild(role, mergeNominal(child, map));
            }
        }

    }

    /**
     * Copy all entities' reference from source node to destination node
     *
     * @param src ANDNODE source node
     * @param dst ANDNODE destination node
     * @author Luca Conte
     */
    private void copyALL(@Nonnull ANDNODE src, @Nonnull ANDNODE dst) {
        Set<OWLClass> conceptNames = src.getConceptNames();
        Set<OWLIndividual> individualNames = src.getIndividualNames();
        Map<OWLDataProperty, List<IntRange>> dataProperty = src.getDataProperty();
        Map<OWLObjectProperty, List<ANDNODE>> children = src.getChildren();
        Deque<ORNODE> orNodes = src.getORNodes();

        dst.addConceptName(conceptNames);
        dst.addIndividualName(individualNames);
        dst.addORnodes(orNodes);
        dataProperty.keySet().forEach(owlDataProperty -> dst.addDataProperty(owlDataProperty, dataProperty.get(owlDataProperty)));
        children.keySet().forEach(property -> dst.addChild(property, children.get(property)));
    }

    /**
     * Merge each duplicated individual name applying the policy logic's rules. This
     * function apply Unique Name Assumption rule
     *
     * @param root ANDNODE root node of a tree
     * @param map  Support's map to help with merging of individual names
     * @author Luca Conte
     */
    private ANDNODE mergeNominal(@Nonnull ANDNODE root, @Nonnull Map<OWLIndividual, ANDNODE> map) {
        if (!isInconsistent) {
            Set<OWLIndividual> individualNames = root.getIndividualNames();
            if (individualNames.size() > 1) {     // root contains different individuals
                isInconsistent = true;    // unique name assumption
                root.addConceptName(this.bottomEntity);
            } else {
                if (!individualNames.isEmpty()) {
                    Iterator<OWLIndividual> iterator = individualNames.iterator();
                    OWLIndividual a = iterator.next();
                    if (!map.containsKey(a)) {  // {a} not in map.keys()
                        map.put(a, root);    // add root into map
                        mergeNominalInChildren(root, map);
                    } else {       // {a} in map.keys()
                        ANDNODE tempNode = map.get(a);
                        mergeNominalInChildren(root, map);
                        copyALL(root, tempNode);
                        return tempNode;
                    }
                } else {
                    mergeNominalInChildren(root, map);
                }
            }
        }
        return root;
    }

    /**
     * Merge each functional property applying the policy logic's rules. This
     * function apply rules #4, #5 and #6
     *
     * @param root          ANDNODE root node of a tree
     * @param justProcessed is a Set of Node just processed
     * @author Luca Conte
     */
    private ANDNODE mergeFunctional(@Nonnull ANDNODE root, @Nonnull Set<ANDNODE> justProcessed) {
        if (justProcessed.contains(root))
            return root;
        else {    // I am processing root
            justProcessed.add(root);
            ArrayList<OWLObjectProperty> arrayList = new ArrayList<>(root.getChildrenKeySet());
            for (OWLObjectProperty role : arrayList) {
                if (objectPropertyHierarchy.isFunctional(role)) {
                    List<ANDNODE> children = root.getChildren(role);
                    List<ANDNODE> temp = root.getChildren(role);
                    ANDNODE target;
                    if (children.size() < 2)
                        break;
                    else {
                        if (children.contains(root)) {
                            target = root;
                        } else {
                            target = children.get(0);
                        }
                        for (ANDNODE child : children) {
                            if (child != target) {
                                copyALL(child, target);
                                temp.remove(child);
                            }
                        }
                        root.removeChildren(role);
                        root.addChild(role, temp);
                        justProcessed.remove(target);
                    }
                }
            }
            Set<OWLObjectProperty> roles0 = root.getChildrenKeySet();
            ArrayList<OWLObjectProperty> roles = new ArrayList<>(roles0);
            for (OWLObjectProperty role : roles) {
                List<ANDNODE> children = root.getChildren(role);
                root.removeChildren(role);
                for (ANDNODE child : children) {
                    root.addChild(role, mergeFunctional(child, justProcessed));
                }
            }

        }
        return root;
    }

    /**
     * Check tree's inconsistency
     * function apply rules #4, #5 and #6
     *
     * @param root    ANDNODE root node of a tree
     * @param visited is a Set of Node just visited
     * @author Luca Conte
     */
    private boolean isInconsistency(@Nonnull ANDNODE root, @Nonnull Set<ANDNODE> visited) {
        if (root.containsConceptName(this.bottomEntity)) {
            return true;
        }
        for (List<IntRange> ranges : root.getDataPropertyValuesSet()) {
            for (IntRange range : ranges) {
                if (range.min() > range.max()) return true;
            }
        }
        if (hasDisjunction(root.getConceptNames())) {
            return true;
        }
        if (root.getIndividualNames().size() > 1) {
            return true;
        }

        if (visited.contains(root)) {
            return false;
        }

        visited.add(root);
        for (OWLObjectProperty role : root.getChildrenKeySet()) {
            for (ANDNODE child : root.getChildren(role)) {
                if (isInconsistency(child, visited))
                    return true;
            }
        }
        return false;
    }

    public ORNODE mergeNominal(ORNODE normalized) {
        Deque<ANDNODE> trees = normalized.getDisjunction();
        List<ANDNODE> graphs = new LinkedList<>();
        for (ANDNODE tree : trees) {
            isInconsistent = false;
            mergeConstraints(tree);
            Map<OWLIndividual, ANDNODE> map = new HashMap<>();
            ANDNODE withNominalGraph = mergeNominal(tree, map);
            if (isInconsistent) {
                ANDNODE newNode = new ANDNODE();
                newNode.addConceptName(this.bottomEntity);
                graphs.add(newNode);
            } else {
                ANDNODE andnode = mergeFunctional(withNominalGraph, new HashSet<>());
                if (isInconsistency(andnode, new HashSet<>())) {
                    ANDNODE newNode = new ANDNODE();
                    newNode.addConceptName(this.bottomEntity);
                    graphs.add(newNode);
                } else {
                    mergeSameProperty(andnode);
                    graphs.add(andnode);
                }
            }
        }
        ORNODE list = new ORNODE();
        list.addAll(graphs);
        return list;
    }

    private void mergeSameProperty(ANDNODE node) {
        for (ORNODE orNode : node.getORNodes()) {
            for (ANDNODE disj : orNode.getDisjunction()) {
                mergeSameProperty(disj);
            }
        }

        for (OWLObjectProperty property : node.getChildrenKeySet()) {
            for (ANDNODE child : node.getChildren(property)) {
                mergeSameProperty(child);
            }
            List<ANDNODE> children = node.getChildren(property);
            for (int i = 1; i < children.size(); i++) {
                copyALL(children.get(i), children.get(0));
            }
        }
    }


    /**
     * Merge each Role Property functional applying the policy logic's rules.
     * Apply rule #4
     *
     * @param root ANDNODE root node of a tree
     */
    protected void mergeExistential(@Nonnull ANDNODE root) {
        for (Map.Entry<OWLObjectProperty, List<ANDNODE>> entry : root.getChildrenEntrySet()) {
            OWLObjectProperty property = entry.getKey();
            if (objectPropertyHierarchy.isFunctional(property)) {
                root.makeSingletonObjectProperty(property, mergeANDNODEs(entry.getValue()));
            }
        }
    }

    /**
     * Merge each Data Constraint functional applying the policy logic's rules.
     * Apply rule #5
     *
     * @param root ANDNODE root node of a tree
     */
    protected void mergeConstraints(@Nonnull ANDNODE root) {
        for (Map.Entry<OWLDataProperty, List<IntRange>> entry : root.getDataPropertyEntrySet()) {
            OWLDataProperty property = entry.getKey();
            if (dataPropertyHierarchy.isFunctional(property)) {
                int minCard = 0;
                int maxCard = Integer.MAX_VALUE;
                for (IntRange interval : entry.getValue()) {
                    if (minCard < interval.min()) {
                        minCard = interval.min();
                    }
                    if (maxCard > interval.max()) {
                        maxCard = interval.max();
                    }
                }
                root.makeSingletonDataProperty(property, new IntRange(minCard, maxCard));
            }
        }
    }

    /**
     * Check the tree's consistency applying the policy logic's rules. Apply
     * rule #1, #2, #3 and #7
     *
     * @param root ANDNODE root node of a tree
     * @return root ANDNODE node of the normalized tree
     */
    protected ANDNODE consistencyTree(@Nonnull ANDNODE root) {
        boolean isBottom = false;
        for (List<ANDNODE> children : root.getChildrenValuesSet()) {
            if (isBottom) {
                break;
            }
            for (ANDNODE child : children) {
                consistencyTree(child);
                if (child.containsConceptName(this.bottomEntity)) {
                    isBottom = true;
                    break;
                }
            }
        }
        if (!isBottom && root.containsConceptName(this.bottomEntity)) {
            isBottom = true;
        }
        if (!isBottom) {
            isBottom = hasDisjunction(root.getConceptNames());
        }
        for (List<IntRange> ranges : root.getDataPropertyValuesSet()) {
            if (isBottom) {
                break;
            }
            for (IntRange interval : ranges) {
                if (interval.min() > interval.max()) {
                    isBottom = true;
                    break;
                }
            }
        }
        if (isBottom) {
            root.makeBottomNode(this.bottomEntity);
        }
        return root;
    }

    private boolean hasDisjunction(final @Nonnull Collection<OWLClass> entitySet) {
        if (entitySet.size() <= 1) {
            return false;
        }
        final Set<OWLClass> visited = new HashSet<>();
        final Deque<OWLClass> queue = new ArrayDeque<>(entitySet.size() * 2);
        queue.addAll(entitySet);
        while (!queue.isEmpty()) {
            OWLClass a = queue.pop();
            if (!visited.contains(a)) {
                for (OWLClass B : classHierarchy.getDisjunctions(a)) {
                    if (visited.contains(B)) {
                        return true;
                    }
                }
                visited.add(a);
                for (OWLClass y : classHierarchy.getParentNodes(a)) {
                    if (!y.isOWLThing()) {
                        queue.addLast(y);
                    }
                }
            }
        }
        return false;
    }

    protected void getAllIntervals(ANDNODE tree, Set<IntRange> intervals) {
        for (OWLDataProperty property : tree.getDataPropertyKeySet()) {
            List<IntRange> ranges = tree.getDataProperty(property);
            intervals.addAll(ranges);
        }
        for (OWLObjectProperty child : tree.getChildrenKeySet()) {
            List<ANDNODE> value = tree.getChildren(child);
            for (ANDNODE item : value) {
                getAllIntervals(item, intervals);
            }
        }

        for (ORNODE orNode : tree.getORNodes()) {
            getAllIntervals(orNode, intervals);
        }


    }

    private void getAllIntervals(ORNODE tree, Set<IntRange> intervals) {
        for (ANDNODE disjunct : tree.getDisjunction()) {
            getAllIntervals(disjunct, intervals);
        }
    }

    /**
     * Normalize interval safety a list of disjuncts trees C respect to another
     * list of disjuncts D
     *
     * @param cCollection Disjuncts to normalize
     * @param d           Disjuncts to consider
     * @return ORNODE with a tree for each disjunct normalized. It's a copy so
     * the original tree isn't touched
     */
    public ORNODE normalizeIntervalSafety(final @Nonnull Collection<ANDNODE> cCollection, final @Nonnull ORNODE d) {
        final ORNODE disjunctionC = new ORNODE();
        for (ANDNODE C : cCollection) {
            Set<IntRange> intervalsInD = new HashSet<>();
            this.getAllIntervals(d, intervalsInD);
            ANDNODE cCopy = applyIntervalSafe(C.copy(), intervalsInD);
            disjunctionC.addTree(cCopy);
        }
        return disjunctionC;
    }


    public ORNODE normalizeIntervalSafety(final @Nonnull ANDNODE c,final @Nonnull SignedPolicy<ANDNODE>[] history) {
        Collection<ANDNODE> left = new HashSet<>();
        left.add(c);
        ORNODE right = new ORNODE();
        for (SignedPolicy<ANDNODE> signedPolicy : history) {
            right.add(signedPolicy.data());
        }
        return normalizeIntervalSafety(left, right);

    }

    public ORNODE normalizeIntervalSafetyCache(final @Nonnull Collection<ANDNODE> treesC, final @Nonnull ORNODE treesD) {
        final ORNODE disjunctOfC = new ORNODE();
        final WrapperBoolean wrapper = new WrapperBoolean(false);
        /* Normalizzazione di ogni albero di C - C potrebbe avere l'unione, quindi sarebbe una moltitudine di alberi */
        for (ANDNODE C : treesC) {
            wrapper.setValue(false);
            ORNODE normalized = this.simpleConceptIntervalSafeCache.get(C, treesD);

            if (normalized == null) {
                Set<IntRange> intervalsInD = new HashSet<>();
                this.getAllIntervals(treesD, intervalsInD);
                ANDNODE cCopy = applyIntervalSafe(C.copy(), intervalsInD);
                if (wrapper.getValue()) {
                    ORNODE disjunction = normalizeUnion(cCopy);
                    disjunctOfC.addTrees(disjunction.getDisjunction());
                    this.simpleConceptIntervalSafeCache.put(C, treesD, disjunction);
                } else {
                    disjunctOfC.addTree(cCopy);
                    this.simpleConceptIntervalSafeCache.put(C, treesD, cCopy);
                }
            } else {
                disjunctOfC.addTrees(normalized);
            }
        }
        return disjunctOfC;
    }

    private ANDNODE applyIntervalSafe(final @Nonnull ANDNODE treeC,final @Nonnull Set<IntRange> intervalsInD) throws UnionNotNormalizedException {
        if (treeC.hasORNodes()) {
            Deque<ORNODE> temp = new ArrayDeque<>();
            for (ORNODE orNode : treeC.getORNodes()) {
                ORNODE newORnode = new ORNODE();
                for (ANDNODE disjunct : orNode.getDisjunction()) {
                    ANDNODE applied = applyIntervalSafe(disjunct, intervalsInD);
                    newORnode.add(applied);
                }
                temp.add(newORnode);
            }
            treeC.setOrNodes(temp);
        }

        Deque<ANDNODE> queueDown = new LinkedList<>();
        queueDown.add(treeC);
        while (!queueDown.isEmpty()) {
            ANDNODE tree = queueDown.pollFirst();
            for (Iterator<Map.Entry<OWLDataProperty, List<IntRange>>> propertyIterator = tree.getDataPropertyEntrySet().iterator(); propertyIterator.hasNext(); ) {
                Map.Entry<OWLDataProperty, List<IntRange>> entry = propertyIterator.next();
                OWLDataProperty property = entry.getKey();
                if (!intervalsInD.isEmpty()) {
                    final int extremesSize = intervalsInD.size() * 2 + 2;
                    for (ListIterator<IntRange> intervalInCiterator = entry.getValue().listIterator(); intervalInCiterator.hasNext(); ) {
                        IntRange intervalC = intervalInCiterator.next();
                        final int min = intervalC.min();
                        final int max = intervalC.max();
                        final Set<Integer> left = new HashSet<>(extremesSize);
                        final Set<Integer> right = new HashSet<>(extremesSize);
                        final Set<Integer> extremesSet = new HashSet<>(extremesSize);
                        left.add(min);
                        right.add(max);
                        extremesSet.add(min);
                        extremesSet.add(max);
                        for (IntRange intervalD : intervalsInD) {
                            int minD = intervalD.min();
                            int maxD = intervalD.max();
                            if (minD >= min && minD <= max) {
                                left.add(minD);
                                extremesSet.add(minD);
                            }
                            if (maxD >= min && maxD <= max) {
                                right.add(maxD);
                                extremesSet.add(maxD);
                            }
                        }
                        final int[] extremes = extremesSet.stream().mapToInt(x -> x).toArray();
                        Arrays.sort(extremes);
                        if (extremes.length == 1) {
                            intervalInCiterator.set(new IntRange(extremes[0], extremes[0]));
                        } else {
                            final Deque<IntRange> rangesCreated = new ArrayDeque<>(max - min + 1);
                            for (int x = 0; x < extremes.length - 1; x++) {
                                int current = extremes[x];
                                int next = extremes[x + 1];
                                if (left.contains(current) && right.contains(current)) {
                                    rangesCreated.add(new IntRange(current, current));
                                }
                                if (right.contains(current)) {
                                    current += 1;
                                }
                                if (left.contains(next)) {
                                    next -= 1;
                                }
                                if (current <= next) {
                                    rangesCreated.add(new IntRange(current, next));
                                }
                            }
                            int last = extremes[extremes.length - 1];
                            if (left.contains(last) && right.contains(last)) {
                                rangesCreated.add(new IntRange(last, last));
                            }
                            if (rangesCreated.size() <= 1) {
                                intervalInCiterator.set(rangesCreated.getFirst());
                            } else {
                                ORNODE orNodes = new ORNODE(rangesCreated.size());
                                for (IntRange interval : rangesCreated) {
                                    ANDNODE disjunct = new ANDNODE();
                                    disjunct.addDataProperty(property, interval);
                                    orNodes.addTree(disjunct);
                                }
                                tree.addORnode(orNodes);
                                intervalInCiterator.remove();
                                if (tree.existsButIsEmpty(property)) {
                                    propertyIterator.remove();
                                }
                            }
                        }
                    }
                }
            }
            for (List<ANDNODE> children : tree.getChildrenValuesSet()) {
                queueDown.addAll(children);
            }
        }
        return treeC;
    }

    /**
     * Check if a class A is subclass of another class B
     *
     * @param a First class, it rapresents the child
     * @param b Second Class, it rapresents the parent
     * @return true if A is a subclass of B, false otherwise
     */
    private boolean isSubClassOf(@Nonnull OWLClass a, @Nonnull OWLClass b) {
        if (a.equals(b)) {
            return true;
        }
        final Set<OWLClass> visited = new HashSet<>(512);
        final Deque<OWLClass> queueUp = new ArrayDeque<>(512);
        queueUp.add(a);
        while (!queueUp.isEmpty()) {
            OWLClass u = queueUp.pollFirst();
            Iterable<OWLClass> parents = null;
            if (classHierarchy.containsEntity(u)) {
                parents = classHierarchy.getParentNodes(u);
            } else {
                parents = classHierarchy.getTopNode();
            }
            for (OWLClass parent : parents) {
                if (parent.equals(b)) {
                    return true;
                }
                if (!parent.isOWLThing() && !visited.contains(parent)) {
                    queueUp.addLast(parent);
                    visited.add(parent);
                }
            }
        }
        return false;
    }

    /**
     * Check if the tree C is subsumed by the tree D. STS algorithm
     *
     * @param c Simple PL Concept to check respect to treesD
     * @param d Full PL Concept, it rapresents the consent policy
     * @return true if C is subsumed by D, false otherwise
     * @throws UnionNotNormalizedException if a tree's node contains a disjunct
     */
    private boolean structuralSubsumption(@Nonnull ANDNODE c, @Nonnull ORNODE d) throws UnionNotNormalizedException {
        boolean result = false;
        for (ANDNODE D : d) {
            checkIfInterrupted();
            result = structuralSubsumption(c, D);
            if (result) {
                break;
            }
        }
        return result;
    }

    /**
     * Check if the tree C is subsumed by the tree D. STS algorithm
     *
     * @param c Full PL Concept
     * @param d Full PL Concept
     * @return true if C is subsumed by D, false otherwise
     * @throws UnionNotNormalizedException if a tree's node contains a disjunct
     */
    public boolean structuralSubsumption(@Nonnull ORNODE c, @Nonnull ORNODE d) throws UnionNotNormalizedException {
        boolean result = true;
        for (ANDNODE C : c) {
            result = structuralSubsumption(C, d);
            if (!result) {
                break;
            }
        }
        return result;
    }

    /**
     * Check if the tree C is subsumed by the tree D. STS algorithm
     *
     * @param c Simple PL Concept
     * @param d Simple PL Concept
     * @return true if C is subsumed by D, false otherwise
     * @throws UnionNotNormalizedException if a tree's node contains a disjunct
     */
    private boolean structuralSubsumption(@Nonnull ANDNODE c, @Nonnull ANDNODE d) throws UnionNotNormalizedException {
        this.stsCount++;
        if (c.hasORNodes()) {
            throw new UnionNotNormalizedException("Subtree at left has disjuncts in some nodes. Normalize them before structural subsumption.");
        } else if (d.hasORNodes()) {
            throw new UnionNotNormalizedException("Subtree at right has disjuncts in some nodes. Normalize them before structural subsumption.");
        }
        if (c.containsConceptName(this.bottomEntity)) {
            return true;
        }
        boolean isSubsumpted = true;

        Set<OWLIndividual> individualNamesController = c.getIndividualNames();
        Set<OWLIndividual> individualNamesSubjects = d.getIndividualNames();

        for (OWLClass B : d.getConceptNames()) {
            if (!isSubsumpted) {
                break;
            }

            isSubsumpted = false;
            for (OWLClass A : c.getConceptNames()) {
                if (isSubClassOf(A, B)) {
                    isSubsumpted = true;
                    break;
                }
            }

            if (!isSubsumpted && !individualNamesController.isEmpty()) {
                OWLIndividual individual = individualNamesController.iterator().next();
                if (isInstanceOf(individual, B)) {
                    isSubsumpted = true;
                }
            }
        }
        for (OWLIndividual individual_D : individualNamesSubjects) {
            if (!individualNamesController.contains(individual_D)) {
                return false;
            }
        }
        for (Map.Entry<OWLDataProperty, List<IntRange>> constraint : d.getDataPropertyEntrySet()) {
            if (!isSubsumpted) {
                break;
            }
            OWLDataProperty property = constraint.getKey();
            for (IntRange intervalD : constraint.getValue()) {
                if (!isSubsumpted) {
                    break;
                }
                isSubsumpted = false;
                for (IntRange intervalC : c.getDataProperty(property)) {
                    if (intervalC.isInclusion(intervalD)) {
                        isSubsumpted = true;
                        break;
                    }
                }
            }
        }
        for (Map.Entry<OWLObjectProperty, List<ANDNODE>> child : d.getChildrenEntrySet()) {
            if (!isSubsumpted) {
                break;
            }
            OWLObjectProperty property = child.getKey();
            for (ANDNODE childD : child.getValue()) {
                if (!isSubsumpted) {
                    break;
                }
                isSubsumpted = false;
                for (ANDNODE childC : c.getChildren(property)) {
                    isSubsumpted = structuralSubsumption(childC, childD);
                    if (isSubsumpted) {
                        break;
                    }
                }
            }
        }
        return isSubsumpted;
    }

    public boolean isEntailed(@Nonnull PolicyLogic<OWLClassExpression> c, @Nonnull History history) {
        this.stsCount = 0;
        ANDNODE buildTree = this.buildTree(c.expression());
        //apply interval safe C,H
        ORNODE normalizedIntervalSafety = this.normalizeIntervalSafety(buildTree, history.signedPolicy());
        ORNODE normalizedUnion = this.normalizeUnion(normalizedIntervalSafety.getDisjunction().getFirst());
        this.applyRange(normalizedUnion);
        ORNODE merged = this.mergeNominal(normalizedUnion);

        return structuralSubsumption(merged, history.signedPolicy());
    }
    public boolean isEntailed(@Nonnull PolicyLogic<OWLClassExpression> c, @Nonnull PolicyLogic<OWLClassExpression> d) {
        this.stsCount = 0;
        ANDNODE buildTree = this.buildTree(c.expression());
        ANDNODE buildTreeD = this.buildTree(d.expression());
        Collection<ANDNODE> collectionOfTrees = new ArrayList<>();
        collectionOfTrees.add(buildTree);
        //apply interval safe C,H
        ORNODE normalizedIntervalSafety = this.normalizeIntervalSafety(collectionOfTrees,buildTreeD.getORNodes().getFirst());
        ORNODE normalizedUnion = this.normalizeUnion(normalizedIntervalSafety.getDisjunction().getFirst());
        this.applyRange(normalizedUnion);
        ORNODE merged = this.mergeNominal(normalizedUnion);

        return structuralSubsumption(merged, merged);
    }
    public boolean isEntailed(@Nonnull OWLClassExpression c, @Nonnull OWLClassExpression d) {
        this.stsCount = 0;
        ANDNODE buildTree = this.buildTree(c);
        ANDNODE buildTreeD = this.buildTree(d);
        Collection<ANDNODE> collectionOfTrees = new ArrayList<>();
        collectionOfTrees.add(buildTree);
        //apply interval safe C,H
        ORNODE normalizedIntervalSafety = this.normalizeIntervalSafety(collectionOfTrees,buildTreeD.getORNodes().getFirst());
        ORNODE normalizedUnion = this.normalizeUnion(normalizedIntervalSafety.getDisjunction().getFirst());
        this.applyRange(normalizedUnion);
        ORNODE merged = this.mergeNominal(normalizedUnion);

        return structuralSubsumption(merged, merged);
    }
    public boolean isEntailed(@Nonnull ANDNODE c,@Nonnull ANDNODE d){
        this.stsCount = 0;
        Collection<ANDNODE> collectionOfTrees = new ArrayList<>();
        collectionOfTrees.add(c);
        //apply interval safe C,H
        ORNODE normalizedIntervalSafety = this.normalizeIntervalSafety(collectionOfTrees,d.getORNodes().getFirst());
        ORNODE normalizedUnion = this.normalizeUnion(normalizedIntervalSafety.getDisjunction().getFirst());
        this.applyRange(normalizedUnion);
        ORNODE merged = this.mergeNominal(normalizedUnion);

        return structuralSubsumption(merged, merged);

    }
    public boolean isEntailed(@Nonnull ANDNODE c,@Nonnull SignedPolicy<ANDNODE>[] history){
        this.stsCount = 0;
        //apply interval safe C,H
        ORNODE normalizedIntervalSafety = this.normalizeIntervalSafety(c, history);
        ORNODE normalizedUnion = this.normalizeUnion(normalizedIntervalSafety.getDisjunction().getFirst());
        this.applyRange(normalizedUnion);
        ORNODE merged = this.mergeNominal(normalizedUnion);

        return structuralSubsumption(merged, history);

    }
    public boolean isEntailed(@Nonnull File c, @Nonnull File d) {
        this.stsCount = 0;
        TranslatorEngine translatorEngine = new TranslatorEngine(new OntologyAxioms(this.rootOntology));
        PolicyLogic<JSONArray> cc = translatorEngine.getArrayPolicyLogic(c);
        PolicyLogic<JSONArray> dd = translatorEngine.getArrayPolicyLogic(d);

        ORNODE ornode = new ORNODE();

        for (Object o : cc.expression()) {
            ANDNODE andnode = new ANDNODE();
                translatorEngine.convertWithNoCheck((JSONObject) o, andnode, new boolean[1]);
            ornode.add(andnode);
        }
        PolicyLogic<ORNODE> left = new PolicyLogic<>("0", ornode);

        ornode = new ORNODE();

        for (Object o : dd.expression()) {
            ANDNODE andnode = new ANDNODE();
                translatorEngine.convertWithNoCheck((JSONObject) o, andnode, new boolean[1]);
            ornode.add(andnode);
        }
        PolicyLogic<ORNODE> right = new PolicyLogic<>("1", ornode);

        ANDNODE x = new ANDNODE();
        x.addORnode(left.expression());
        Collection<ANDNODE> collectionOfTrees = new ArrayList<>();
        collectionOfTrees.add(x);
        ORNODE normalizedIntervalSafety = this.normalizeIntervalSafety(collectionOfTrees,right.expression());
        ORNODE normalizedUnion = this.normalizeUnion(normalizedIntervalSafety.getDisjunction().getFirst());
        this.applyRange(normalizedUnion);
        ORNODE merged = this.mergeNominal(normalizedUnion);

        return structuralSubsumption(merged, merged);
    }
    public boolean isEntailedHistory(@Nonnull File c, @Nonnull File history) {
        this.stsCount = 0;
        TranslatorEngine translatorEngine = new TranslatorEngine(new OntologyAxioms(this.rootOntology));
        PolicyLogic<JSONArray> cc = translatorEngine.getArrayPolicyLogic(c);
        JSONArray policyLogic = translatorEngine.getJsonArray(history);

        ORNODE ornode = new ORNODE();

        for (Object o : cc.expression()) {
            ANDNODE andnode = new ANDNODE();
                translatorEngine.convertWithNoCheck((JSONObject) o, andnode, new boolean[1]);
            ornode.add(andnode);
        }
        PolicyLogic<ORNODE> left = new PolicyLogic<>("0", ornode);

        SignedPolicy<ANDNODE>[] signedPolicies = new SignedPolicy[policyLogic.length()];
        int index = 0;
        for (Object o : policyLogic) {
            ANDNODE andnode = new ANDNODE();
            boolean[] action = new boolean[1];

                translatorEngine.convertWithNoCheckHistory((JSONObject) o, andnode, action);

            signedPolicies[index++] = new SignedPolicy<>(action[0], andnode);
        }

        ANDNODE x = new ANDNODE();
        x.addORnode(left.expression());
        ORNODE normalizedIntervalSafety = this.normalizeIntervalSafety(x,signedPolicies);
        ORNODE normalizedUnion = this.normalizeUnion(normalizedIntervalSafety.getDisjunction().getFirst());
        this.applyRange(normalizedUnion);
        ORNODE merged = this.mergeNominal(normalizedUnion);

        return structuralSubsumption(merged, merged);
    }

    private boolean structuralSubsumption(@Nonnull ORNODE c, @Nonnull ANDNODE signedPolicy) {
        boolean result = true;
        for (ANDNODE C : c) {
            result = structuralSubsumption(C, signedPolicy);
            if (!result) {
                break;
            }
        }
        return result;
    }
    private boolean structuralSubsumption(@Nonnull ORNODE c, @Nonnull SignedPolicy<ANDNODE>[] history) {
        boolean result;
        for (ANDNODE andnode : c) {
            result = structuralSubsumption(andnode, history);
            if (!result) {
                return false;
            }
        }
        return true;
    }
    private boolean structuralSubsumption(@Nonnull ANDNODE c, @Nonnull SignedPolicy<ANDNODE>[] history) {
        ANDNODE bottom = new ANDNODE();
        bottom.addConceptName(this.bottomEntity);

        if (this.structuralSubsumption(c, bottom)) {
            return true;
        }

        int k = history.length;

        do {
            k--;

        } while (k >= 0 && (!history[k].permit()
                || !structuralSubsumption(c, history[k].data())));


        if (k < 0) return false;
        for (int i = k + 1; i < history.length; i++) {              // itera sulle deny
            int j;

            ANDNODE cANDci = this.createIntersectionOf(c, history[i].data());
            mergeSameProperty(cANDci);
            ORNODE normalizedUnion = this.normalizeUnion(cANDci);
            ORNODE merged = this.mergeNominal(normalizedUnion);


            if (!history[i].permit() && !structuralSubsumption(merged, bottom)) {
                j = i + 1;

                while (j < history.length &&
                        (!history[j].permit()
                                || !structuralSubsumption(merged, history[j].data()))) {
                    j++;
                }
                if (j >= history.length) return false;

            }
        }
        return true;
    }
    private boolean isInstanceOf(OWLIndividual individual, OWLClass owlClass) {
        Stream<OWLClassAssertionAxiom> assertionAxiomStream = rootOntology.logicalAxioms()
                .filter(OWLClassAssertionAxiom.class::isInstance)
                .map(OWLClassAssertionAxiom.class::cast);
        Set<OWLClassAssertionAxiom> classAssertionAxiomSet = assertionAxiomStream.collect(Collectors.toSet());

        for (OWLClassAssertionAxiom assertionAxiom : classAssertionAxiomSet) {
            OWLClass classExpression = (OWLClass) assertionAxiom.getClassExpression();

            if (assertionAxiom.getIndividual() == individual && isSubClassOf(classExpression, owlClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Take an OWLClassExpression to normalize and build a tree
     *
     * @param ce FULL PL Concept to normalize respect to 7 rules
     * @return ORNODE with a tree for each disjunct normalized
     */
    private ORNODE normalizeSatisfiability(@Nonnull OWLClassExpression ce) {
        final ORNODE trees = normalizeUnion(buildTree(ce));
        final ORNODE results = new ORNODE(trees.size() + 1);
        final WrapperBoolean wrapper = new WrapperBoolean(false);
        while (!trees.isEmpty()) {
            ANDNODE tree = trees.pollFirst();
            wrapper.setValue(false);
            mergeFunctional(tree, wrapper);
            if (wrapper.getValue()) {
                /* The Rule #6 with Union is applied. The tree output is a Complex PL concept. */
                ORNODE treeToNormalize = normalizeUnion(tree);
                while (!treeToNormalize.isEmpty()) {
                    ANDNODE treeDNF = treeToNormalize.pollFirst();
                    consistencyTree(treeDNF);
                    results.addTree(treeDNF);
                }
            } else {
                /* The Rule #6 with Union was not applied. The tree output is a Simple PL concept. */
                consistencyTree(tree);
                results.addTree(tree);
            }
        }
        return results;
    }

    public ORNODE normalizeSatisfiabilityCache(@Nonnull OWLClassExpression ce) {
        final ORNODE trees = normalizeUnion(buildTree(ce));
        final ORNODE results = new ORNODE(trees.size() + 1);
        final WrapperBoolean wrapper = new WrapperBoolean(false);
        while (!trees.isEmpty()) {
            ANDNODE tree = trees.pollFirst();
            ORNODE normalized = this.simpleConceptCache.get(tree);
            if (normalized == null) {
                ANDNODE keyCache = tree.copy();
                wrapper.setValue(false);
                mergeFunctional(tree, wrapper);
                if (wrapper.getValue()) {
                    /* The Rule #6 with Union is applied. The tree output is a Complex PL concept. */
                    ORNODE treeToNormalize = normalizeUnion(tree);
                    while (!treeToNormalize.isEmpty()) {
                        ANDNODE treeDNF = treeToNormalize.pollFirst();
                        consistencyTree(treeDNF);
                        results.addTree(treeDNF);
                        this.simpleConceptCache.put(keyCache, treeDNF);
                    }
                } else {
                    /* The Rule #6 with Union was not applied. The tree output is a Simple PL concept. */
                    consistencyTree(tree);
                    results.addTree(tree);
                    this.simpleConceptCache.put(keyCache, tree);
                }
            } else {
                for (ANDNODE treeNormalized : normalized) {
                    results.addTree(treeNormalized);
                }
            }
        }
        return results;
    }

    /**
     * Return a NodeSet of OWLClass subclasses of the specified concept.
     *
     * @param ce     The specified class. If anonymouse then the result is empty
     * @param direct true to direct subclasses or false to all subclasses
     * @return NodeSet
     */
    @Override
    public NodeSet<OWLClass> getSubClasses(@Nonnull OWLClassExpression ce, boolean direct) {
        OWLClassNodeSet ns = new OWLClassNodeSet();
        if (!ce.isAnonymous()) {
            if (classHierarchy.containsEntity(ce.asOWLClass())) {
                return classHierarchy.getChildNodes(ce.asOWLClass(), direct, ns);
            } else {
                ns.addNode(classHierarchy.getBottomNode());
            }
        }
        return ns;
    }

    /**
     * Return a NodeSet of OWLClass superclasses of the specified concept.
     *
     * @param ce     The specified class. If anonymouse then the result is empty
     * @param direct true to direct superclasses or false to all superclasses
     * @return NodeSet
     */
    @Override
    public NodeSet<OWLClass> getSuperClasses(@Nonnull OWLClassExpression ce, boolean direct) {
        OWLClassNodeSet ns = new OWLClassNodeSet();
        if (!ce.isAnonymous()) {
            if (classHierarchy.containsEntity(ce.asOWLClass())) {
                return classHierarchy.getParentNodes(ce.asOWLClass(), direct, ns);
            } else {
                ns.addNode(classHierarchy.getTopNode());
            }
        }
        return ns;
    }

    /**
     * Preprocess a concept OWLClassExpression (a policy) respect to the
     * normalization with seven rules. A concept preprocessed is a tree with
     * ANDNODE and ORNODE as nodes. This method cache the result in memory.
     *
     * @param ce Full PL Concept to normalize respect to 7 rules
     */
    public void preProcess(@Nonnull OWLClassExpression ce) {
        if (this.fullTreeCache) {
            ORNODE treesNormalized = fullSubClassConceptCache.get(ce);
            if (treesNormalized == null) {
                if (this.simpleTreeCache) {
                    treesNormalized = normalizeSatisfiabilityCache(ce);
                } else {
                    treesNormalized = normalizeSatisfiability(ce);
                }
                fullSubClassConceptCache.put(ce, treesNormalized);
            }
        }
    }

    public void preProcess(@Nonnull Collection<OWLClassExpression> exs) {
        if (this.fullTreeCache) {
            for (OWLClassExpression ce : exs) {
                preProcess(ce);
            }
        }
    }

    /**
     * Preprocess a SubClassOf axiom and save it in a cache in memory.
     * Preprocess each axiom's concept as a tree with ANDNODE and ORNODE nodes.
     *
     * @param axiom Query to preprocess respect each rules in the reasoner
     */
    public void preProcessIntervalSafety(@Nonnull OWLAxiom axiom) {
        if (!(axiom instanceof OWLSubClassOfAxiom)) {
            throw new UnsupportedOperationException("Expected to be encoded as OWLSubClassOfAxioms.");
        }
        if (fullConceptlIntervalSafeCache != null && !fullConceptlIntervalSafeCache.isFull()) {
            OWLSubClassOfAxiom subClassOfAxiom = (OWLSubClassOfAxiom) axiom;
            OWLClassExpression superClass = subClassOfAxiom.getSuperClass();
            OWLClassExpression subClass = subClassOfAxiom.getSubClass();

            /*
            Check if the entities in thr expression there are already in the
            classification graph.
            If not, then this adds to the DAG
             */
            ORNODE treesC = fullSubClassConceptCache.get(subClass);
            if (treesC == null) {
                if (this.simpleTreeCache) {
                    treesC = normalizeSatisfiabilityCache(subClass);
                } else {
                    treesC = normalizeSatisfiability(subClass);
                }
                if (this.fullTreeCache) {
                    fullSubClassConceptCache.put(subClass, treesC);
                }
            }
            ORNODE treesD = fullSuperClassConceptCache.get(superClass);
            if (treesD == null) {
                if (this.normalizeSuperClassConcept) {
                    if (this.simpleTreeCache) {
                        treesD = normalizeSatisfiabilityCache(superClass);
                    } else {
                        treesD = normalizeSatisfiability(superClass);
                    }
                } else {
                    treesD = normalizeUnion(buildTree(superClass));
                }
                if (this.fullTreeCache) {
                    fullSuperClassConceptCache.put(superClass, treesD);
                }
            }
            if (!this.fullIntervalSafeCache) {
                return;
            }
            checkIfInterrupted();
            ORNODE disjunctOfC = fullConceptlIntervalSafeCache.get(subClass, superClass);
            if (disjunctOfC == null) {
                if (this.simpleIntervalSafeCache) {
                    disjunctOfC = normalizeIntervalSafetyCache(treesC, treesD);
                } else {
                    disjunctOfC = normalizeIntervalSafety(treesC, treesD);
                }
                fullConceptlIntervalSafeCache.put(subClass, superClass, disjunctOfC);
            }
        }
    }

    /**
     * Preprocess a collection of SubClassOf axioms and save it in a cache in
     * memory.
     *
     * @param axioms Collection of axioms to preprocess.
     */
    public void preProcessIntervalSafety(@Nonnull Collection<OWLAxiom> axioms) {
        for (OWLAxiom axiom : axioms) {
            preProcessIntervalSafety(axiom);
        }
    }

    @Override
    public boolean isSatisfiable(@Nonnull OWLClassExpression ce) {
        Timer timer = new Timer(this.getTimeOut());
        ORNODE treesNormalized = null;
        if (this.fullTreeCache) {
            treesNormalized = fullSubClassConceptCache.computeIfAbsent(ce, k -> normalizeSatisfiability(ce));
        } else {
            treesNormalized = normalizeSatisfiability(ce);
        }
        for (ANDNODE tree : treesNormalized) {
            timer.checkTime();
            checkIfInterrupted();
            if (!tree.containsConceptName(this.bottomEntity)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEntailed(@Nonnull OWLDisjointClassesAxiom axiom) {
        boolean result = false;
        Timer timer = new Timer(this.getTimeOut());
        Collection<OWLClass> queue = axiom.classesInSignature().collect(Collectors.toCollection(LinkedList::new));
        for (OWLClass clazz : queue) {
            NodeSet<OWLClass> disjoints = getDisjointClasses(clazz);
            for (OWLClass cl : queue) {
                if (cl.equals(clazz)) {
                    continue;
                }
                result = disjoints.containsEntity(cl);
                if (!result) {
                    break;
                }
            }
            timer.checkTime();
            if (!result) {
                break;
            }
        }
        return result;
    }

    public boolean isEntailed(@Nonnull OWLFunctionalObjectPropertyAxiom axiom) {
        boolean result = false;
        Timer timer = new Timer(this.getTimeOut());
        for (OWLObjectProperty property : axiom.objectPropertiesInSignature().collect(Collectors.toCollection(LinkedList::new))) {
            result = this.objectPropertyHierarchy.isFunctional(property);
            if (!result) {
                break;
            }
            timer.checkTime();
        }
        return result;
    }

    public boolean isEntailed(@Nonnull OWLFunctionalDataPropertyAxiom axiom) {
        boolean result = false;
        Timer timer = new Timer(this.getTimeOut());
        for (OWLDataProperty property : axiom.dataPropertiesInSignature().collect(Collectors.toCollection(LinkedList::new))) {
            result = this.dataPropertyHierarchy.isFunctional(property);
            if (!result) {
                break;
            }
            timer.checkTime();
        }
        return result;
    }

    public boolean isEntailed(@Nonnull OWLObjectPropertyRangeAxiom axiom) {
        boolean result = false;
        Timer timer = new Timer(this.getTimeOut());
        OWLObjectPropertyExpression property = axiom.getProperty();
        Set<OWLClassExpression> supposedRanges = axiom.getRange().asDisjunctSet();
        NodeSet<OWLClass> ranges = getObjectPropertyRanges(property);
        for (OWLClassExpression ce : supposedRanges) {
            result = false;
            if (!ce.isAnonymous()) {
                result = ranges.containsEntity(ce.asOWLClass());
            }
            if (!result) {
                break;
            }
            timer.checkTime();
        }
        return result;
    }
    public int getStsCount() {
        return this.stsCount;
    }

    public boolean isEntailed(@Nonnull OWLSubClassOfAxiom axiom) {
        final Timer timer = new Timer(this.getTimeOut());
        final OWLClassExpression superClass = axiom.getSuperClass();
        final OWLClassExpression subClass = axiom.getSubClass();

        /*
            Check if the entities in thr expression there are already in the
            classification graph.
            If not, then this adds to the DAG
         */

        ORNODE treesC;
        ORNODE treesD;
        ORNODE disjunctOfC;
        /* SubClass Expression - C - Business Policy */
        if (this.fullTreeCache && this.simpleTreeCache) {
            treesC = fullSubClassConceptCache.computeIfAbsent(subClass, k -> normalizeSatisfiabilityCache(subClass));
        } else if (this.fullTreeCache) {
            treesC = fullSubClassConceptCache.computeIfAbsent(subClass, k -> normalizeSatisfiability(subClass));
        } else if (this.simpleTreeCache) {
            treesC = normalizeSatisfiabilityCache(subClass);
        } else {
            treesC = normalizeSatisfiability(subClass);
        }
        /* SuperClass Expression - D - Consent Policy */
        if (this.normalizeSuperClassConcept) {
            if (this.fullTreeCache && this.simpleTreeCache) {
                treesD = fullSuperClassConceptCache.computeIfAbsent(superClass, k -> normalizeSatisfiabilityCache(superClass));
            } else if (this.fullTreeCache) {
                treesD = fullSuperClassConceptCache.computeIfAbsent(superClass, k -> normalizeSatisfiability(superClass));
            } else if (this.simpleTreeCache) {
                treesD = normalizeSatisfiabilityCache(superClass);
            } else {
                treesD = normalizeSatisfiability(superClass);
            }
        } else {
            if (this.fullTreeCache) {
                treesD = fullSuperClassConceptCache.computeIfAbsent(superClass, k -> normalizeUnion(buildTree(superClass)));
            } else {
                treesD = normalizeUnion(buildTree(superClass));
            }
        }
        /* Interval Safety */
        if (this.fullIntervalSafeCache) {
            /* Normalize each subTree of C - C can have a disjuntion, so C'll be a group of trees */
            disjunctOfC = fullConceptlIntervalSafeCache.get(subClass, superClass);
            if (disjunctOfC == null) {
                if (this.simpleIntervalSafeCache) {
                    disjunctOfC = normalizeIntervalSafetyCache(treesC, treesD);
                } else {
                    disjunctOfC = normalizeIntervalSafety(treesC, treesD);
                }
                fullConceptlIntervalSafeCache.put(subClass, superClass, disjunctOfC);
            }
        } else {
            if (this.simpleIntervalSafeCache) {
                disjunctOfC = normalizeIntervalSafetyCache(treesC, treesD);
            } else {
                disjunctOfC = normalizeIntervalSafety(treesC, treesD);
            }
        }
        checkIfInterrupted();
        timer.checkTime();
        return structuralSubsumption(disjunctOfC, treesD);
    }

    /**
     * Check if the specified axiom is entailed respect to current ontology
     *
     * @param axiom Only SubClassOf axiom
     * @return True if it is entailed, false otherwise
     */
    @Override
    public boolean isEntailed(@Nonnull OWLAxiom axiom) {
        AxiomType<? extends OWLAxiom> type = axiom.getAxiomType();
        if (type.equals(AxiomType.SUBCLASS_OF)) { //STS
            return isEntailed((OWLSubClassOfAxiom) axiom);
        }
        else if (type.equals(AxiomType.DISJOINT_CLASSES)) {
            return isEntailed((OWLDisjointClassesAxiom) axiom);
        }
        else if (type.equals(AxiomType.FUNCTIONAL_OBJECT_PROPERTY)) {
            return isEntailed((OWLFunctionalObjectPropertyAxiom) axiom);
        }
        else if (type.equals(AxiomType.FUNCTIONAL_DATA_PROPERTY)) {
            return isEntailed((OWLFunctionalDataPropertyAxiom) axiom);
        }
        else if (type.equals(AxiomType.OBJECT_PROPERTY_RANGE)) {
            return isEntailed((OWLObjectPropertyRangeAxiom) axiom);
        }
        else {
            throw new UnsupportedOperationException("Expected to be encoded as one of " + SUPPORTED_AXIOMS.stream()
                    .map(AxiomType::getName).toList());
        }
    }

    @Override
    public boolean isEntailed(@Nonnull Set<? extends OWLAxiom> axioms) {
        for (OWLAxiom ax : axioms) {
            if (!isEntailed(ax)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public BufferingMode getBufferingMode() {
        return bufferingMode;
    }

    @Override
    public List<OWLOntologyChange> getPendingChanges() {
        return new ArrayList<>(pendingChanges);
    }

    @Override
    public Set<OWLAxiom> getPendingAxiomAdditions() {
        Set<OWLAxiom> added = new HashSet<>(pendingChanges.size());
        for (OWLOntologyChange change : pendingChanges) {
            if (change.isAddAxiom()) {
                added.add(change.getAxiom());
            }
        }
        return added;
    }

    @Override
    public Set<OWLAxiom> getPendingAxiomRemovals() {
        Set<OWLAxiom> removed = new HashSet<>(pendingChanges.size());
        for (OWLOntologyChange change : pendingChanges) {
            if (change.isRemoveAxiom()) {
                removed.add(change.getAxiom());
            }
        }
        return removed;
    }

    /**
     * Disposes of this reasoner. This frees up any resources used by the
     * reasoner and detaches the reasoner as an OWLOntologyChangeListener from
     * the OWLOntologyManager that manages the ontologies contained within the
     * reasoner.
     */
    @Override
    public void dispose() {
        manager.removeOntologyChangeListener(ontologyChangeListener);
        pendingChanges.clear();
        clearCache();
        prepareHierarchy();
    }

    /**
     * Flushes any changes stored in the buffer, which causes the reasoner to
     * take into consideration the changes the current root ontology specified
     * by the changes. If the reasoner buffering mode is
     * BufferingMode.NON_BUFFERING then this method will have no effect.
     */
    @Override
    public void flush() {
        clearCache();
        pendingChanges.clear();
        prepareHierarchy();
    }

    @Override
    public void interrupt() {
        interrupted = true;
    }

    private void checkIfInterrupted() {
        if (interrupted) {
            interrupted = false;
            throw new ReasonerInterruptedException(this.getReasonerName() + " is interrupted!");
        }
    }

    @Override
    public void precomputeInferences(@Nonnull InferenceType... inferenceTypes) {
        Set<InferenceType> requiredInferences = new HashSet<>(Arrays.asList(inferenceTypes));
        if (requiredInferences.contains(InferenceType.CLASS_HIERARCHY)) {
            prepareHierarchy();
        }
    }

    @Override
    public boolean isPrecomputed(@Nonnull InferenceType inferenceType) {
        return PRECOMPUTED_INFERENCE_TYPES.contains(inferenceType);
    }

    @Override
    public Set<InferenceType> getPrecomputableInferenceTypes() {
        return PRECOMPUTED_INFERENCE_TYPES;
    }

    @Override
    public boolean isConsistent() {
        boolean reasonerIsConsistent = false;
        if (this.bufferingMode.equals(BufferingMode.NON_BUFFERING)) {
            this.flush();
        }
        reasonerIsConsistent = true;
        return reasonerIsConsistent;
    }

    @Override
    public Node<OWLClass> getUnsatisfiableClasses() {
        return this.classHierarchy.getBottomNode();
    }

    @Override
    public boolean isEntailmentCheckingSupported(AxiomType<?> axiomType) {
        return axiomType.equals(AxiomType.SUBCLASS_OF);
    }

    @Override
    public Node<OWLClass> getTopClassNode() {
        return this.classHierarchy.getTopNode();
    }

    @Override
    public Node<OWLClass> getBottomClassNode() {
        return this.classHierarchy.getBottomNode();
    }

    @Override
    public Node<OWLClass> getEquivalentClasses(OWLClassExpression ce) {
        if (!ce.isAnonymous()) {
            return classHierarchy.getEquivalentEntity(ce.asOWLClass());
        } else {
            return new OWLClassNode();
        }
    }

    @Override
    public NodeSet<OWLClass> getDisjointClasses(@Nonnull OWLClassExpression ce) {
        OWLClassNodeSet ns = new OWLClassNodeSet();
        if (!ce.isAnonymous()) {
            ns.addAllNodes(
                    classHierarchy.getDisjunctions(ce.asOWLClass(), false, new HashSet<>())
                            .stream()
                            .map(OWLClassNode::new)
            );
        }
        return ns;
    }

    @Override
    public Node<OWLObjectPropertyExpression> getTopObjectPropertyNode() {
        throw new UnsupportedOperationException("getTopObjectPropertyNode() Not supported.");
    }

    @Override
    public Node<OWLObjectPropertyExpression> getBottomObjectPropertyNode() {
        throw new UnsupportedOperationException("getBottomObjectPropertyNode() Not supported.");
    }

    @Override
    public NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(@Nonnull OWLObjectPropertyExpression pe, boolean direct) {
        throw new UnsupportedOperationException("getSubObjectProperties(...) Not supported.");
    }

    @Override
    public NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(@Nonnull OWLObjectPropertyExpression pe, boolean direct) {
        throw new UnsupportedOperationException("getSuperObjectProperties(...) Not supported.");
    }

    @Override
    public Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(@Nonnull OWLObjectPropertyExpression pe) {
        throw new UnsupportedOperationException("getEquivalentObjectProperties(...) Not supported.");
    }

    @Override
    public NodeSet<OWLObjectPropertyExpression> getDisjointObjectProperties(@Nonnull OWLObjectPropertyExpression pe) {
        throw new UnsupportedOperationException("getDisjointObjectProperties(...) Not supported.");
    }

    @Override
    public Node<OWLObjectPropertyExpression> getInverseObjectProperties(@Nonnull OWLObjectPropertyExpression pe) {
        throw new UnsupportedOperationException("getInverseObjectProperties(...) Not supported.");
    }

    @Override
    public NodeSet<OWLClass> getObjectPropertyDomains(@Nonnull OWLObjectPropertyExpression pe, boolean direct) {
        throw new UnsupportedOperationException("getObjectPropertyDomains(...) Not supported.");
    }

    @Override
    public NodeSet<OWLClass> getObjectPropertyRanges(@Nonnull OWLObjectPropertyExpression pe, boolean direct) {
        OWLClassNodeSet ns = new OWLClassNodeSet();
        for (EntityIntersectionNode<OWLClass> entityIntersection : objectPropertyHierarchy.getPropertyRange(pe, direct, new HashSet<>())) {
            if (entityIntersection.getSize() <= 1) {
                for (OWLClass clazz : entityIntersection) {
                    ns.addNode(classHierarchy.getEquivalentEntity(clazz));
                }
            }
        }
        if (!direct || ns.isEmpty()) {
            ns.addNode(classHierarchy.getTopNode());
        }
        return ns;
    }

    @Override
    public Node<OWLDataProperty> getTopDataPropertyNode() {
        throw new UnsupportedOperationException("getTopDataPropertyNode(...) Not supported.");
    }

    @Override
    public Node<OWLDataProperty> getBottomDataPropertyNode() {
        throw new UnsupportedOperationException("getBottomDataPropertyNode Not supported.");
    }

    @Override
    public NodeSet<OWLDataProperty> getSubDataProperties(@Nonnull OWLDataProperty pe, boolean direct) {
        throw new UnsupportedOperationException("getSubDataProperties Not supported.");
    }

    @Override
    public NodeSet<OWLDataProperty> getSuperDataProperties(@Nonnull OWLDataProperty pe, boolean direct) {
        throw new UnsupportedOperationException("getSuperDataProperties Not supported.");
    }

    @Override
    public Node<OWLDataProperty> getEquivalentDataProperties(@Nonnull OWLDataProperty pe) {
        throw new UnsupportedOperationException("getEquivalentDataProperties Not supported.");
    }

    @Override
    public NodeSet<OWLDataProperty> getDisjointDataProperties(@Nonnull OWLDataPropertyExpression pe) {
        throw new UnsupportedOperationException("getDisjointDataProperties Not supported.");
    }

    @Override
    public NodeSet<OWLClass> getDataPropertyDomains(@Nonnull OWLDataProperty pe, boolean direct) {
        throw new UnsupportedOperationException("getDataPropertyDomains Not supported.");
    }

    @Override
    public NodeSet<OWLClass> getTypes(@Nonnull OWLNamedIndividual ind, boolean direct) {
        throw new UnsupportedOperationException("getTypes Not supported.");
    }

    @Override
    public NodeSet<OWLNamedIndividual> getInstances(@Nonnull OWLClassExpression ce, boolean direct) {
        throw new UnsupportedOperationException("getInstances Not supported.");
    }

    @Override
    public NodeSet<OWLNamedIndividual> getObjectPropertyValues(@Nonnull OWLNamedIndividual ind, @Nonnull OWLObjectPropertyExpression pe) {
        throw new UnsupportedOperationException("getObjectPropertyValues Not supported.");
    }

    @Override
    public Set<OWLLiteral> getDataPropertyValues(@Nonnull OWLNamedIndividual ind, @Nonnull OWLDataProperty pe) {
        throw new UnsupportedOperationException("getDataPropertyValues Not supported.");
    }

    @Override
    public Node<OWLNamedIndividual> getSameIndividuals(@Nonnull OWLNamedIndividual ind) {
        throw new UnsupportedOperationException("getSameIndividuals Not supported.");
    }

    @Override
    public NodeSet<OWLNamedIndividual> getDifferentIndividuals(@Nonnull OWLNamedIndividual ind) {
        throw new UnsupportedOperationException("getDifferentIndividuals Not supported.");
    }

    @Override
    public long getTimeOut() {
        return configuration.getTimeOut();
    }

    @Override
    public FreshEntityPolicy getFreshEntityPolicy() {
        return configuration.getFreshEntityPolicy();
    }

    @Override
    public IndividualNodeSetPolicy getIndividualNodeSetPolicy() {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * Listener about changes in the ontology. For each change, check and save
     * it in a list, so we can elaborate it later.
     */
    protected class RawOntologyChangeListener implements OWLOntologyChangeListener {

        @Override
        public void ontologiesChanged(List<? extends OWLOntologyChange> changes) {
            for (OWLOntologyChange change : changes) {
                if (!(change instanceof AnnotationChange
                        || change instanceof RemoveOntologyAnnotation
                        || change instanceof AddOntologyAnnotation)) {
                    pendingChanges.add(change);
                }
            }
            if (bufferingMode.equals(BufferingMode.NON_BUFFERING)) {
                flush();
            }
        }
    }

    /**
     * Generic class that rapresent the hierarchy of a type T
     *
     * @param <T>
     */
    private abstract class HierarchyGraph<T extends OWLObject> {

        protected final OntologyReadable<T> entityInHierarchyReadable;
        private final Set<NodeHierarchy<T>> directChildrenOfTopNode = new HashSet<>();
        private final Set<NodeHierarchy<T>> directParentsOfBottomNode = new HashSet<>();
        private final Map<T, NodeHierarchy<T>> mapHierarchy = new HashMap<>();
        private final T topEntity;
        private final T bottomEntity;
        @Nullable
        protected NodeHierarchy<T> topNode;
        @Nullable
        protected NodeHierarchy<T> bottomNode;

        HierarchyGraph(T topEntity, T bottomEntity, OntologyReadable<T> entityInHierarchyReadable) {
            this.topEntity = topEntity;
            this.bottomEntity = bottomEntity;
            this.entityInHierarchyReadable = entityInHierarchyReadable;
        }

        protected void clearHierarchy() {
            this.directChildrenOfTopNode.clear();
            this.directParentsOfBottomNode.clear();
            this.mapHierarchy.clear();
            this.topNode = null;
            this.bottomNode = null;
        }


        protected boolean containsEntity(T e) {
            return mapHierarchy.containsKey(e);
        }

        protected NodeHierarchy<T> getTopNodeHierarchy() {
            return this.topNode;
        }


        public Node<T> getTopNode() {
            if (topNode != null) {
                return topNode.node;
            }
            return null;
        }

        public Node<T> getBottomNode() {
            if (bottomNode != null) {
                return bottomNode.node;
            }
            return null;
        }

        public Set<T> getParentNodes(T child) {
            NodeHierarchy<T> nodeHierarchy = mapHierarchy.get(child);
            if (nodeHierarchy == null || !nodeHierarchy.hasParentsNodes()) {
                return Collections.emptySet();
            }
            Set<T> ns = new HashSet<>();
            for (NodeHierarchy<T> parentNode : nodeHierarchy.getParentsNodes()) {
                parentNode.getValue()
                        .entities()
                        .forEach(ns::add);
            }
            return ns;
        }

        public NodeSet<T> getParentNodes(T child, boolean direct, DefaultNodeSet<T> ns) {
            NodeHierarchy<T> nodeHierarchy = mapHierarchy.get(child);
            if (nodeHierarchy == null) {
                return ns;
            } else {
                Node<T> node = nodeHierarchy.getValue();
                if (node == null/* || node.isBottomNode() */) {
                    return ns;
                }
                Set<Node<T>> directParentsNodes = new HashSet<>();
                Set<T> directParentsEntity = new HashSet<>();
                if (nodeHierarchy.hasParentsNodes()) {
                    for (NodeHierarchy<T> parentNode : nodeHierarchy.getParentsNodes()) {
                        directParentsNodes.add(parentNode.getValue());
                        if (!direct) {
                            for (T equivParent : parentNode.getValue()) {
                                directParentsEntity.add(equivParent);
                            }
                        }
                    }
                }
                if (node.isBottomNode()) {
                    for (NodeHierarchy<T> parentNode : directParentsOfBottomNode) {
                        directParentsNodes.add(parentNode.getValue());
                        if (!direct) {
                            for (T equivParent : parentNode.getValue()) {
                                directParentsEntity.add(equivParent);
                            }
                        }
                    }
                }
                for (Node<T> parentNode : directParentsNodes) {
                    ns.addNode(parentNode);
                }
                if (!direct) {
                    for (T parent : directParentsEntity) {
                        getParentNodes(parent, direct, ns);
                    }
                }
            }
            return ns;
        }


        public NodeSet<T> getChildNodes(T parent, boolean direct, DefaultNodeSet<T> ns) {
            NodeHierarchy<T> nodeHierarchy = mapHierarchy.get(parent);
            if (nodeHierarchy == null) {
                return ns;
            } else {
                Node<T> node = nodeHierarchy.getValue();
                if (node == null || node.isBottomNode()) {
                    return ns;
                }
                Set<Node<T>> directChildrenNodes = new HashSet<>();
                Set<T> directChildrenEntity = new HashSet<>();
                if (nodeHierarchy.hasChildrenNodes()) {
                    for (NodeHierarchy<T> child : nodeHierarchy.getChildrenNodes()) {
                        directChildrenNodes.add(child.getValue());
                        if (!direct) {
                            for (T equivChild : child.getValue()) {
                                directChildrenEntity.add(equivChild);
                            }
                        }
                    }
                }
                if (node.isTopNode()) {
                    for (NodeHierarchy<T> child : directChildrenOfTopNode) {
                        directChildrenNodes.add(child.getValue());
                        if (!direct) {
                            for (T equivChild : child.getValue()) {
                                directChildrenEntity.add(equivChild);
                            }
                        }
                    }
                }
                for (Node<T> childNode : directChildrenNodes) {
                    ns.addNode(childNode);
                }
                if (!direct) {
                    for (T child : directChildrenEntity) {
                        getChildNodes(child, direct, ns);
                    }
                }
            }
            return ns;
        }

        public Set<T> getDisjunctions(T entity) {
            Set<T> ns = new HashSet<>();
            NodeHierarchy<T> nodeHierarchy = mapHierarchy.get(entity);
            if (nodeHierarchy == null) {
                return ns;
            }
            if (nodeHierarchy.hasDisjuncts()) {
                ns.addAll(nodeHierarchy.getDisjuncts());
            }
            if (ns.isEmpty()) {
                ns.add(bottomEntity);
            }
            return ns;
        }

        public Set<T> getDisjunctions(T entity, boolean direct, Set<T> ns) {
            if (entity.isBottomEntity()) {
                ns.addAll(mapHierarchy.keySet());
            } else {
                NodeHierarchy<T> nodeHierarchy = mapHierarchy.get(entity);
                if (nodeHierarchy != null) {
                    Deque<T> disjointsClasses = new LinkedList<>();
                    Deque<NodeHierarchy<T>> queueUp = new LinkedList<>();
                    queueUp.add(nodeHierarchy);
                    while (!queueUp.isEmpty()) {
                        NodeHierarchy<T> node = queueUp.pollFirst();
                        if (node.hasDisjuncts()) {
                            disjointsClasses.addAll(node.getDisjuncts());
                            ns.addAll(node.getDisjuncts());
                        }
                        if (node.hasParentsNodes()) {
                            queueUp.addAll(node.getParentsNodes());
                        }
                    }
                    if (!direct) {
                        for (T djn : disjointsClasses) {
                            NodeHierarchy<T> node = mapHierarchy.get(djn);
                            if (node != null && node.hasChildrenNodes()) {
                                Deque<NodeHierarchy<T>> queueDown = new LinkedList<>();
                                queueDown.add(node);
                                while (!queueDown.isEmpty()) {
                                    NodeHierarchy<T> el = queueDown.pollFirst();
                                    el.getValue().forEach(ns::add);
                                    if (el.hasChildrenNodes()) {
                                        queueDown.addAll(el.getChildrenNodes());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (ns.isEmpty()) {
                ns.add(bottomEntity);
            }
            return ns;
        }

        public boolean isFunctional(T el) {
            PropertyNodeHierarchy<T, OWLClass> node = (PropertyNodeHierarchy<T, OWLClass>) getNodeFromHierarchy(el);
            if (node != null) {
                return node.isFunctional();
            }
            return false;
        }

        protected Set<EntityIntersectionNode<OWLClass>> getPropertyRange(T entity) {
            PropertyNodeHierarchy<T, OWLClass> nodeHierarchy = (PropertyNodeHierarchy<T, OWLClass>) mapHierarchy.get(entity);
            if (nodeHierarchy != null && nodeHierarchy.hasRanges()) {
                return new HashSet<>(nodeHierarchy.getRanges());
            }
            return Collections.emptySet();
        }

        protected Set<EntityIntersectionNode<OWLClass>> getPropertyRange(T entity, boolean direct, Set<EntityIntersectionNode<OWLClass>> ns) {
            PropertyNodeHierarchy<T, OWLClass> nodeHierarchy = (PropertyNodeHierarchy<T, OWLClass>) mapHierarchy.get(entity);
            if (nodeHierarchy != null) {
                if (nodeHierarchy.hasRanges()) {
                    ns.addAll(nodeHierarchy.getRanges());
                }
                if (!direct && nodeHierarchy.hasParentsNodes()) {
                    for (NodeHierarchy<T> parent : nodeHierarchy.getParentsNodes()) {
                        for (T ent : parent.getValue().entities().toList()) {
                            ns.addAll(getPropertyRange(ent, false, ns));
                        }
                    }
                }
            }
            return ns;
        }

        protected void compute() {
            clearHierarchy();
            topNode = mapHierarchy.computeIfAbsent(topEntity,
                    this::getNodeHierarchy
            );
            bottomNode = mapHierarchy.computeIfAbsent(bottomEntity,
                    this::getNodeHierarchy
            );
            for (OWLOntology ont : asList(getRootOntology().importsClosure())) {
                for (T entity : getEntityInSignature(ont).toList()) {
                    checkIfInterrupted();
                    NodeHierarchy<T> node = mapHierarchy.computeIfAbsent(entity,
                            this::getNodeHierarchy
                    );
                    Set<T> disjoints = entityInHierarchyReadable.getDisjointsInRawHierarchy(entity);
                    if (!disjoints.isEmpty()) {
                        node.addDisjuncts(disjoints);
                    }
                    Set<T> parents = entityInHierarchyReadable.getParentsInRawHierarchy(entity);
                    Set<T> children = entityInHierarchyReadable.getChildrenInRawHierarchy(entity);
                    if (!entity.equals(topEntity)) {
                        if (parents.isEmpty() || parents.contains(topEntity)) {
                            directChildrenOfTopNode.add(node);
                        } else {
                            for (T parent : parents) {
                                NodeHierarchy<T> parentNode = mapHierarchy.get(parent);
                                if (parentNode == null) {
                                    parentNode = node.addParent(parent);
                                    mapHierarchy.put(parent, parentNode);
                                } else {
                                    node.addParentNode(parentNode);
                                }
                            }
                        }
                    }
                    if (!entity.equals(bottomEntity)) {
                        if (children.isEmpty() || children.contains(bottomEntity)) {
                            directParentsOfBottomNode.add(node);
                        } else {
                            for (T child : children) {
                                NodeHierarchy<T> childNode = mapHierarchy.get(child);
                                if (childNode == null) {
                                    childNode = node.addChild(child);
                                    mapHierarchy.put(child, childNode);
                                } else {
                                    node.addChildNode(childNode);
                                }
                            }
                        }
                    }
                }
            }
            if (!directChildrenOfTopNode.isEmpty()) {
                assert topNode != null;
                topNode.addChildrenNode(directChildrenOfTopNode);
            }
            if (!directParentsOfBottomNode.isEmpty()) {
                assert bottomNode != null;
                bottomNode.addParentsNode(directParentsOfBottomNode);
            }
            checkCycles();
        }

        protected abstract Stream<T> getEntityInSignature(OWLOntology o);

        protected abstract NodeHierarchy<T> getNodeHierarchy(T e);

        protected abstract NodeHierarchy<T> getNodeHierarchy(Collection<T> e);

        protected NodeHierarchy<T> getNodeFromHierarchy(T e) {
            return this.mapHierarchy.get(e);
        }

        protected Node<T> getEquivalentEntity(T e) {
            NodeHierarchy<T> node = this.mapHierarchy.get(e);
            if (node != null) {
                return node.getValue();
            }
            return null;
        }

        private void checkCycles() {
            Set<Set<NodeHierarchy<T>>> setSCCs = new HashSet<>();
            Map<NodeHierarchy<T>, Integer> processedNodes = new HashMap<>();
            for (NodeHierarchy<T> node : new HashSet<>(this.mapHierarchy.values())) {
                if (!processedNodes.containsKey(node)) {
                    tarjanSCC(node, 0, new LinkedList<>(), new HashSet<>(), setSCCs, new HashMap<>(), processedNodes);
                }
            }
            for (Set<NodeHierarchy<T>> scc : setSCCs) {
                checkIfInterrupted();
                Set<T> allEntities = new HashSet<>();
                for (NodeHierarchy<T> node : scc) {
                    allEntities.addAll(node.getValue().entities().toList());
                }
                NodeHierarchy<T> mergedNode = getNodeHierarchy(allEntities);
                for (T entity : allEntities) {
                    this.mapHierarchy.put(entity, mergedNode);
                }
                for (NodeHierarchy<T> node : scc) {
                    if (node.hasParentsNodes()) {
                        for (NodeHierarchy parent : new HashSet<>(node.getParentsNodes())) {
                            if (!scc.contains(parent)) {
                                parent.addChildNode(mergedNode);
                            }
                            parent.removeChildNode(node);
                        }
                    }
                    if (node.hasChildrenNodes()) {
                        for (NodeHierarchy child : new HashSet<>(node.getChildrenNodes())) {
                            if (!scc.contains(child)) {
                                child.addParentNode(mergedNode);
                            }
                            child.removeParentNode(node);
                        }
                    }
                    if (node.hasDisjuncts()) {
                        for (T entity : node.getDisjuncts()) {
                            mergedNode.addDisjunct(entity);
                            NodeHierarchy<T> nodeDisjunct = this.mapHierarchy.get(entity);
                            if (nodeDisjunct != null) {
                                nodeDisjunct.addDisjuncts(allEntities);
                            }
                        }
                    }
                }
                if (!mergedNode.hasParentsNodes()) {
                    assert topNode != null;
                    topNode.addChildNode(mergedNode);
                }
                if (!mergedNode.hasChildrenNodes()) {
                    assert bottomNode != null;
                    bottomNode.addParentNode(mergedNode);
                }
            }
        }

        private void tarjanSCC(NodeHierarchy<T> node,
                               Integer index,
                               Deque<NodeHierarchy<T>> queue,
                               Set<NodeHierarchy<T>> queueSet,
                               Set<Set<NodeHierarchy<T>>> setSCCs,
                               Map<NodeHierarchy<T>, Integer> lowLinkNodes,
                               Map<NodeHierarchy<T>, Integer> indexNodes) {
            indexNodes.put(node, index);
            lowLinkNodes.put(node, index);
            index++;
            queue.push(node);
            queueSet.add(node);
            if (node.hasParentsNodes()) {
                for (NodeHierarchy<T> parent : node.getParentsNodes()) {
                    if (!indexNodes.containsKey(parent)) {
                        tarjanSCC(parent, index, queue, queueSet, setSCCs, lowLinkNodes, indexNodes);
                        Integer min = Math.min(lowLinkNodes.get(node), lowLinkNodes.get(parent));
                        lowLinkNodes.put(node, min);
                    } else if (queueSet.contains(parent)) {
                        Integer min = Math.min(lowLinkNodes.get(node), lowLinkNodes.get(parent));
                        lowLinkNodes.put(node, min);
                    }
                }
            }
            if (indexNodes.get(node).equals(lowLinkNodes.get(node))) { //Set of scc, node is the root of the scc
                Set<NodeHierarchy<T>> scc = new HashSet<>();
                NodeHierarchy<T> el = null;
                do {
                    el = queue.pop();
                    queueSet.remove(el);
                    scc.add(el);
                } while (!el.equals(node));
                if (scc.size() > 1) {
                    setSCCs.add(scc);
                }
            }
            checkIfInterrupted();
        }
    }

    /**
     * Class that rapresent the hierarchy of OWLClass in the imports closure's
     * signature.
     */
    private class ClassHierarchyInOntology extends HierarchyGraph<OWLClass> {

        ClassHierarchyInOntology(OWLClass topEntity, OWLClass bottomEntity, OntologyReadable<OWLClass> classInHierarchyReadable) {
            super(topEntity, bottomEntity, classInHierarchyReadable);
        }

        @Override
        public boolean isFunctional(OWLClass el) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        protected Set<EntityIntersectionNode<OWLClass>> getPropertyRange(OWLClass entity) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        protected Set<EntityIntersectionNode<OWLClass>> getPropertyRange(OWLClass entity, boolean direct, Set<EntityIntersectionNode<OWLClass>> ns) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        protected Stream<OWLClass> getEntityInSignature(OWLOntology o) {
            return o.classesInSignature();
        }

        @Override
        protected NodeHierarchy<OWLClass> getNodeHierarchy(OWLClass e) {
            return new ClassNodeHierarchy(e);
        }

        @Override
        protected NodeHierarchy<OWLClass> getNodeHierarchy(Collection<OWLClass> e) {
            return new ClassNodeHierarchy(e);
        }
    }

    /**
     * Class that rapresent the hierarchy of OWLDataProperty in the imports
     * closure's signature.
     */
    private class DataPropertyHierarchyInOntology extends HierarchyGraph<OWLDataProperty> {

        DataPropertyHierarchyInOntology(OWLDataProperty topEntity, OWLDataProperty bottomEntity, OntologyReadable<OWLDataProperty> dataPropertyInHierarchyReadable) {
            super(topEntity, bottomEntity, dataPropertyInHierarchyReadable);
        }

        @Override
        protected void compute() {
            clearHierarchy();
            super.compute();
            NodeHierarchy<OWLDataProperty> top = getTopNodeHierarchy();
            if (top != null) {
                Deque<NodeHierarchy<OWLDataProperty>> queueDown = new LinkedList<>();
                queueDown.add(top);
                while (!queueDown.isEmpty()) {
                    PropertyNodeHierarchy<OWLDataProperty, OWLClass> node = (PropertyNodeHierarchy<OWLDataProperty, OWLClass>) queueDown.pollFirst();
                    for (OWLDataProperty el : node.getValue()) {
                        if (entityInHierarchyReadable.isFunctional(el)) {
                            node.setFunctional(true);
                        }
                    }
                    if (node.hasChildrenNodes()) {
                        queueDown.addAll(node.getChildrenNodes());
                    }
                }
            }
        }

        @Override
        protected Stream<OWLDataProperty> getEntityInSignature(OWLOntology o) {
            return o.dataPropertiesInSignature();
        }

        @Override
        protected NodeHierarchy<OWLDataProperty> getNodeHierarchy(OWLDataProperty e) {
            return new DataPropertyNodeHierarchy(e);
        }

        @Override
        protected Set<EntityIntersectionNode<OWLClass>> getPropertyRange(OWLDataProperty entity) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        protected Set<EntityIntersectionNode<OWLClass>> getPropertyRange(OWLDataProperty entity, boolean direct, Set<EntityIntersectionNode<OWLClass>> ns) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        protected NodeHierarchy<OWLDataProperty> getNodeHierarchy(Collection<OWLDataProperty> e) {
            return new DataPropertyNodeHierarchy(e);
        }
    }

    /**
     * Class that rapresent the hierarchy of OWLObjectPropertyExpression in the
     * imports closure's signature.
     */
    private class ObjectPropertyHierarchyInOntology extends HierarchyGraph<OWLObjectPropertyExpression> {

        ObjectPropertyHierarchyInOntology(OWLObjectPropertyExpression topEntity, OWLObjectPropertyExpression bottomEntity, OntologyReadable<OWLObjectPropertyExpression> objectPropertyInHierarchyReadable) {
            super(topEntity, bottomEntity, objectPropertyInHierarchyReadable);
        }

        @Override
        protected void compute() {
            clearHierarchy();
            super.compute();
            NodeHierarchy<OWLObjectPropertyExpression> top = getTopNodeHierarchy();
            if (top != null) {
                Deque<NodeHierarchy<OWLObjectPropertyExpression>> queueDown = new LinkedList<>();
                queueDown.add(top);
                while (!queueDown.isEmpty()) {
                    PropertyNodeHierarchy<OWLObjectPropertyExpression, OWLClass> node =
                            (PropertyNodeHierarchy<OWLObjectPropertyExpression, OWLClass>) queueDown.pollFirst();
                    for (OWLObjectPropertyExpression el : node.getValue()) {
                        if (entityInHierarchyReadable.isFunctional(el)) {
                            node.setFunctional(true);
                        }
                        for (OWLOntology ont : asList(getRootOntology().importsClosure())) {
                            List<OWLClassExpression> ranges
                                    = ont.objectPropertyRangeAxioms(el)
                                    .map(HasRange::getRange)
                                    .filter(
                                            x -> x.isOWLClass()
                                                    || x instanceof OWLObjectIntersectionOf
                                                    || x instanceof OWLObjectUnionOf)
                                    .collect(Collectors.toCollection(LinkedList::new));
                            OWLObjectIntersectionOf intersection = entityInHierarchyReadable.getDataFactory().getOWLObjectIntersectionOf(ranges);
                            for (OWLClassExpression disjunction : getDNF(intersection).asDisjunctSet()) {
                                EntityIntersectionNode<OWLClass> nodeRange = new EntityIntersectionNode<>();
                                for (OWLClass range : disjunction.asConjunctSet()
                                        .stream()
                                        .filter(AsOWLClass::isOWLClass)
                                        .map(AsOWLClass::asOWLClass)
                                        .collect(Collectors.toCollection(LinkedList::new))) {
                                    nodeRange.add(range);
                                }
                                node.addRange(nodeRange);
                            }
                            ont.objectPropertyRangeAxioms(el)
                                    .map(HasRange::getRange)
                                    .filter(AsOWLClass::isOWLClass)
                                    .forEach(clazz -> node.addRangeClassOnly(clazz.asOWLClass()));
                        }
                    }
                    if (node.hasChildrenNodes()) {
                        queueDown.addAll(node.getChildrenNodes());
                    }
                }
            }
        }

        private OWLClassExpression getDNF(@Nonnull OWLClassExpression ce) {
            Deque<OWLClassExpression> results = new LinkedList<>();
            Deque<OWLClassExpression> conjuncts = new LinkedList<>();
            Deque<OWLClassExpression> unionOfQueue = new LinkedList<>();
            for (OWLClassExpression conjunct : ce.asConjunctSet()) {
                if (conjunct instanceof OWLObjectUnionOf) {
                    unionOfQueue.add(conjunct);
                } else if (conjunct.isOWLClass()) {
                    conjuncts.add(conjunct);
                }
            }
            getDNF(unionOfQueue, conjuncts, results);
            if (results.size() == 1) {
                return results.getFirst();
            }
            return entityInHierarchyReadable.getDataFactory().getOWLObjectUnionOf(results);
        }

        private void getDNF(@Nonnull Deque<OWLClassExpression> unionOfQueue, Deque<OWLClassExpression> conjuncts, Deque<OWLClassExpression> results) {
            if (!unionOfQueue.isEmpty()) {
                OWLClassExpression unionOf = unionOfQueue.pollFirst();
                for (OWLClassExpression disjunct : unionOf.asDisjunctSet()) {
                    conjuncts.addFirst(disjunct);
                    getDNF(unionOfQueue, conjuncts, results);
                    conjuncts.pollFirst();
                }
                unionOfQueue.addFirst(unionOf);
            } else {
                OWLObjectIntersectionOf intersection = entityInHierarchyReadable.getDataFactory().getOWLObjectIntersectionOf(conjuncts);
                results.add(intersection);
            }
        }

        @Override
        protected Stream<OWLObjectPropertyExpression> getEntityInSignature(OWLOntology o) {
            return o
                    .objectPropertiesInSignature()
                    .map(OWLPropertyExpression::asObjectPropertyExpression);
        }

        @Override
        protected NodeHierarchy<OWLObjectPropertyExpression> getNodeHierarchy(OWLObjectPropertyExpression e) {
            return new ObjectPropertyNodeHierarchy(e);
        }

        @Override
        protected NodeHierarchy<OWLObjectPropertyExpression> getNodeHierarchy(Collection<OWLObjectPropertyExpression> e) {
            return new ObjectPropertyNodeHierarchy(e);
        }
    }

    private static class Timer {

        private final long start;
        private final long max;

        Timer(long max) {
            this.start = System.currentTimeMillis();
            this.max = max;
        }

        void checkTime() {
            long current = System.currentTimeMillis();
            if (this.max < (current - this.start)) {
                throw new TimeOutException("Timeout occurred while reasoning! Time: " + (current - this.start) + " ms");
            }
        }
    }

    private ANDNODE createIntersectionOf(ANDNODE a, ANDNODE b) {
        ANDNODE aANDb = a.copy();
        ANDNODE bCopied = b.copy();
        aANDb.addConceptName(bCopied.getConceptNames());
        aANDb.addIndividualName(bCopied.getIndividualNames());
        for (Map.Entry<OWLDataProperty, List<IntRange>> dataProperty : bCopied.getDataPropertyEntrySet()) {
            aANDb.addDataProperty(dataProperty.getKey(), dataProperty.getValue());
        }
        for (Map.Entry<OWLObjectProperty, List<ANDNODE>> property : bCopied.getChildrenEntrySet()) {
            aANDb.addChild(property.getKey(), property.getValue());
        }
        aANDb.addORnodes(bCopied.getORNodes());
        return aANDb;
    }

}
