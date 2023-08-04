package special.model;

import java.io.Serializable;
import java.util.*;
import java.util.stream.*;
import javax.annotation.*;

import org.semanticweb.owlapi.model.*;

/**
 * AND Node of a Policy Logic Tree
 *
 * @author Luca Ioffredo
 */
public class ANDNODE implements Serializable {

    private static final short SIZE_INDIVIDUAL_DATA_STRUCTURE = 32;
    private static final short SIZE_CLASS_DATA_STRUCTURE = 32;
    private static final short SIZE_DATA_PROPERTY_DATA_STRUCTURE = 8;
    private static final short SIZE_OBJECT_PROPERTY_DATA_STRUCTURE = 32;
    private static final float LOAD_FACTOR_DATA_STRUCTURE = 0.75f;
    private static final Set EMPTY_SET = Collections.emptySet();
    private static final Map EMPTY_MAP = Collections.emptyMap();
    private static final List EMPTY_LIST = Collections.emptyList();
    private static final Deque EMPTY_DEQUE = new ArrayDeque(0);

    private Set<OWLIndividual> individualNames = null;                      //Individual Names    ###
    private Set<OWLClass> conceptNames = null; //Concept Names
    private Map<OWLDataProperty, List<IntRange>> dataConstraints = null; //Data Contraint
    private Map<OWLObjectProperty, List<ANDNODE>> children = null;
    private Deque<ORNODE> orNodes = null;

    public ANDNODE() {
        this.orNodes = new ArrayDeque<>();
    }

    public void setOrNodes(Deque<ORNODE> newORnode){
        this.orNodes = newORnode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ANDNODE)) {
            return false;
        }
        boolean in = false;
        boolean cn = false;
        boolean dc = false;
        boolean ex = false;
        boolean or = false;
        ANDNODE cc = (ANDNODE) o;
        in = (cc.individualNames == null
                ? this.individualNames == null
                : (this.individualNames != null && cc.individualNames.size() == this.individualNames.size()
                && cc.individualNames.containsAll(this.individualNames)
        && this.individualNames.containsAll(cc.individualNames)));
        if (!in) {
            return false;
        }
        cn = (cc.conceptNames == null
                ? this.conceptNames == null
                : (this.conceptNames != null && cc.conceptNames.size() == this.conceptNames.size()
                && cc.conceptNames.containsAll(this.conceptNames)
        && this.conceptNames.containsAll(cc.conceptNames)));
        if (!cn) {
            return false;
        }
        dc = (cc.dataConstraints == null || cc.dataConstraints.isEmpty()
                ? this.dataConstraints == null || this.dataConstraints.isEmpty()
                : (this.dataConstraints != null && cc.dataConstraints.size() == this.dataConstraints.size()
                && cc.dataConstraints.entrySet().stream()
                .allMatch(entry -> {
                    List<IntRange> tmp = this.dataConstraints.get(entry.getKey());
                    return tmp != null && entry.getValue().containsAll(tmp);
                })
                && this.dataConstraints.entrySet().stream()
                .allMatch(entry -> {
                    List<IntRange> tmp = cc.dataConstraints.get(entry.getKey());
                    return tmp != null && entry.getValue().containsAll(tmp);
                })));
        if (!dc) {
            return false;
        }
        ex = areEqualChildren(cc.children, this.children);
        if (!ex) {
            return false;
        }
        or = (cc.orNodes == null
                ? this.orNodes == null
                : (this.orNodes != null && cc.orNodes.size() == this.orNodes.size()
                && cc.orNodes.containsAll(this.orNodes)
        && this.orNodes.containsAll(cc.orNodes)));
        return or;
    }

    @Override
    public int hashCode() {
        return hashCode(new ArrayList<>());
    }

    public int hashCode(List<ANDNODE> visited) {
        if (visited.contains(this)) {
            return 0;
        }
        visited.add(this);

        int hashCode = 1;
        if (this.individualNames != null) {
            for (OWLIndividual individual : this.individualNames) {
                hashCode += individual.hashCode();
            }
        }
        if (this.conceptNames != null) {
            for (OWLClass clazz : this.conceptNames) {
                hashCode += clazz.hashCode();
            }
        }
        if (this.orNodes != null) {
            for (ORNODE ornode : this.orNodes) {
                hashCode += ornode.hashCode();
            }
        }
        if (this.dataConstraints != null) {
            int hashKeys = 1, hashValues = 1;
            for (Map.Entry<OWLDataProperty, List<IntRange>> entry : this.dataConstraints.entrySet()) {
                hashKeys = 31 * hashKeys + entry.getKey().hashCode();
                for (IntRange interval : entry.getValue()) {
                    hashValues = 13 * hashValues + interval.hashCode();
                }
            }
            hashCode = hashCode + hashValues + hashKeys;
        }
        if (this.children != null) {
            int hashKeys = 1, hashValues = 1;
            for (Map.Entry<OWLObjectProperty, List<ANDNODE>> entry : this.children.entrySet()) {
                hashKeys = 31 * hashKeys + entry.getKey().hashCode();
                for (ANDNODE node : entry.getValue()) {
                    hashValues = 13 * hashValues + node.hashCode(visited); /// children
                }
            }
            hashCode = hashCode + hashValues + hashKeys;
        }
        return 17 * 3 + hashCode;
//        return 17 * 3 + Objects.hash(this.conceptNames, this.dataConstraints, this.children, this.orNodes);
    }

    private boolean areEqualChildren(Map<OWLObjectProperty, List<ANDNODE>> map1, Map<OWLObjectProperty, List<ANDNODE>> map2) {
        boolean result = false;
        if (map1 == null || map1.isEmpty()) {
            return map2 == null || map2.isEmpty();
        } else if (map2 != null && !map2.isEmpty() && map1.size() == map2.size()) {
            for (Map.Entry<OWLObjectProperty, List<ANDNODE>> entry : map2.entrySet()) {
                List<ANDNODE> tmp2 = entry.getValue();
                List<ANDNODE> tmp1 = map1.get(entry.getKey());
                if (tmp1 == null || tmp1.isEmpty()) {
                    result = tmp2 == null || tmp2.isEmpty();
                } else {
                    result = tmp2 != null && tmp1.size() == tmp2.size() && tmp2.containsAll(tmp1) && tmp1.containsAll(tmp2);
                }
                if (!result) {
                    break;
                }
            }
        }
        return result;
    }

    private void createORNodesStructure() {
        if (this.orNodes == null) {
            this.orNodes = new ArrayDeque<>();
        }
    }

    private void createIndividualNamesStructure() {                              // ###
        if (this.individualNames == null) {
            this.individualNames = new HashSet<>(SIZE_INDIVIDUAL_DATA_STRUCTURE, LOAD_FACTOR_DATA_STRUCTURE);
        }
    }

    private void createIndividualNamesStructure(@Nonnull Collection<OWLIndividual> c) { // ###
        if (this.individualNames == null) {
            this.individualNames = new HashSet<>(c.size() + 1, LOAD_FACTOR_DATA_STRUCTURE);
        }
    }

    private void createConceptNamesStructure() {
        if (this.conceptNames == null) {
            this.conceptNames = new HashSet<>(SIZE_CLASS_DATA_STRUCTURE, LOAD_FACTOR_DATA_STRUCTURE);
        }
    }

    private void createConceptNamesStructure(@Nonnull Collection<OWLClass> c) {
        if (this.conceptNames == null) {
            this.conceptNames = new HashSet<>(c.size() + 1, LOAD_FACTOR_DATA_STRUCTURE);
        }
    }

    private void createDataConstraintStructure() {
        if (this.dataConstraints == null) {
            this.dataConstraints = new HashMap<>(SIZE_DATA_PROPERTY_DATA_STRUCTURE, LOAD_FACTOR_DATA_STRUCTURE);
        }
    }

    private void createChildrenStructure() {
        if (this.children == null) {
            this.children = new HashMap<>(SIZE_OBJECT_PROPERTY_DATA_STRUCTURE, LOAD_FACTOR_DATA_STRUCTURE);
        }
    }

    /* Individual Names ----------------------------------------------------------------------------------------------------------------------------- */

    /**
     * Check if Individual Name contains the input class
     *
     * @param nominal OWLIndividual
     * @return true if IN contains the individual, false otherwise
     */
    public boolean containsIndividualName(OWLIndividual nominal) {
        return nominal != null && this.individualNames != null && nominal.isIndividual() && this.individualNames.contains(nominal.asOWLNamedIndividual());
    }

    /**
     * Add the input nominal in the Individual Name Set (IN)
     *
     * @param nominal OWLIndividual
     * @return true if this set did not already contain the specified element
     */
    public boolean addIndividualName(OWLIndividual nominal) {
        if (nominal != null) {
            createIndividualNamesStructure();
            return this.individualNames.add(nominal);
        } else {
            return false;
        }
    }

    /**
     * Add the input set in the Individual Name Set (IN)
     *
     * @param c Collection of OWLIndividual
     * @return true if this collection changed as a result of the call
     */
    public boolean addIndividualName(Collection<OWLIndividual> c) {
        if (c != null && !c.isEmpty()) {
            createIndividualNamesStructure(c);
            return this.individualNames.addAll(c);
        }
        return false;
    }

    public Set<OWLIndividual> getIndividualNames() {
        if (this.individualNames == null) {
            return EMPTY_SET;
        }
        return this.individualNames;
    }

    public Stream<OWLIndividual> individualNames() {
        if (this.individualNames == null) {
            return EMPTY_SET.stream();
        }
        return this.individualNames.stream();
    }

    public boolean hasIndividualNames() {
        return this.individualNames != null && !this.individualNames.isEmpty();
    }

    public void clearIndividualNames() {
        if (this.individualNames != null) {
            this.individualNames.clear();
        }
    }



    /* Concept Names ----------------------------------------------------------------------------------------------------------------------------- */

    /**
     * Check if Concept Name Set (CN) contains the input class
     *
     * @param clazz OWLClassExpression as OWLClass
     * @return true if CN contains the input class, false otherwise
     */
    public boolean containsConceptName(OWLClassExpression clazz) {
        return clazz != null && this.conceptNames != null && clazz.isOWLClass() && this.conceptNames.contains(clazz.asOWLClass());
    }

    /**
     * Add the input class in the Concept Name Set (CN)
     *
     * @param clazz OWLClassExpression as OWLClass
     * @return true if this set did not already contain the specified element
     */
    public boolean addConceptName(OWLClass clazz) {
        if (clazz != null) {
            createConceptNamesStructure();
            return this.conceptNames.add(clazz);
        } else {
            return false;
        }
    }

    /**
     * Add the input set in the Concept Name Set (CN)
     *
     * @param c Collection of OWLClassExpression
     * @return true if this collection changed as a result of the call
     */
    public boolean addConceptName(Collection<OWLClass> c) {
        if (c != null && !c.isEmpty()) {
            createConceptNamesStructure(c);
            return this.conceptNames.addAll(c);
        }
        return false;
    }

    public Set<OWLClass> getConceptNames() {
        if (this.conceptNames == null) {
            return EMPTY_SET;
        }
        return this.conceptNames;
    }

    public Stream<OWLClass> conceptNames() {
        if (this.conceptNames == null) {
            return EMPTY_SET.stream();
        }
        return this.conceptNames.stream();
    }

    public boolean hasConceptNames() {
        return this.conceptNames != null && !this.conceptNames.isEmpty();
    }

    public void clearConceptNames() {
        if (this.conceptNames != null) {
            this.conceptNames.clear();
        }
    }

    /* Data Constraints ----------------------------------------------------------------------------------------------------------------------------- */

    /**
     * Add the interval to the specified IRI
     *
     * @param p        OWLDataProperty to add
     * @param interval Array of the interval. Size = 2
     */
    public void addDataProperty(OWLDataProperty p, IntRange interval) {
        if (p != null && interval != null) {
            createDataConstraintStructure();
            List<IntRange> intervals = this.dataConstraints.get(p);
            if (intervals == null) {
                intervals = new LinkedList<>();
                this.dataConstraints.put(p, intervals);
            }
            intervals.add(interval);
        }
    }

    /**
     * Add the intervals's set to the specified IRI
     *
     * @param p         OWLDataProperty to add
     * @param intervals Set of intervals. Each interval has size of 2
     */
    public void addDataProperty(OWLDataProperty p, Collection<IntRange> intervals) {
        if (p != null && intervals != null && !intervals.isEmpty()) {
            createDataConstraintStructure();
            List<IntRange> tmp = this.dataConstraints.get(p);
            if (tmp == null) {
                tmp = new LinkedList<>();
                this.dataConstraints.put(p, tmp);
            }
            tmp.addAll(intervals);
        }
    }

    public void makeSingletonDataProperty(OWLDataProperty p, IntRange interval) {
        if (p != null && interval != null) {
            createDataConstraintStructure();
            List<IntRange> tmp = new LinkedList<>();
            tmp.add(interval);
            this.dataConstraints.put(p, tmp);
        }
    }

    /**
     * Check if Data Constraint Set (DC) contains the constraint specified
     *
     * @param <T>            Subclass of OWLPropertyExpression as
     *                       OWLObjectPropertyExpression or OWLDataPropertyExpression
     * @param dataConstraint A subtype of OWLPropertyExpression
     * @return true if DC containts che constraint specified, false otherwise
     */
    public <T extends OWLPropertyExpression> boolean containsDataProperty(T dataConstraint) {
        if (dataConstraint == null) {
            return false;
        }
        OWLDataProperty p = (OWLDataProperty) dataConstraint;
        return this.dataConstraints != null && this.dataConstraints.containsKey(p.getIRI());
    }

    public boolean containsDataProperty(OWLDataProperty property) {
        return this.dataConstraints != null && property != null && this.dataConstraints.containsKey(property);
    }

    public Map<OWLDataProperty, List<IntRange>> getDataProperty() {
        if (this.dataConstraints == null) {
            return EMPTY_MAP;
        }
        return this.dataConstraints;
    }

    public List<IntRange> removeDataProperty(OWLDataProperty property) {
        if (this.dataConstraints == null || property == null) {
            return EMPTY_LIST;
        }
        return this.dataConstraints.remove(property);
    }

    public void clearDataConstraints() {
        if (this.dataConstraints != null) {
            this.dataConstraints.clear();
        }
    }

    public List<IntRange> getDataProperty(OWLDataProperty property) {
        if (this.dataConstraints == null || property == null || !this.dataConstraints.containsKey(property)) {
            return EMPTY_LIST;
        }
        return this.dataConstraints.get(property);
    }

    public Set<Map.Entry<OWLDataProperty, List<IntRange>>> getDataPropertyEntrySet() {
        if (this.dataConstraints == null) {
            return EMPTY_SET;
        }
        return this.dataConstraints.entrySet();
    }

    public Set<OWLDataProperty> getDataPropertyKeySet() {
        if (this.dataConstraints == null) {
            return EMPTY_SET;
        }
        return this.dataConstraints.keySet();
    }

    public Collection<List<IntRange>> getDataPropertyValuesSet() {
        if (this.dataConstraints == null) {
            return EMPTY_SET;
        }
        return this.dataConstraints.values();
    }

    public boolean hasDataProperties() {
        return this.dataConstraints != null && !this.dataConstraints.isEmpty();
    }

    public void removeIfEmpty(OWLDataProperty property) {
        if (property != null && this.dataConstraints != null && this.dataConstraints.containsKey(property) && this.dataConstraints.get(property).isEmpty()) {
            this.dataConstraints.remove(property);
        }
    }

    public boolean existsButIsEmpty(OWLDataProperty property) {
        return property != null && this.dataConstraints != null && this.dataConstraints.containsKey(property) && this.dataConstraints.get(property).isEmpty();
    }

    /* ORnodes - alias Disjuncts -------------------------------------------------------------------------------------------------------------------- */
    public boolean addDisjuncts(Collection<ANDNODE> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        createORNodesStructure();
        return this.orNodes.add(new ORNODE(nodes));
    }

    public boolean hasORNodes() {
        return this.orNodes != null && !this.orNodes.isEmpty();
    }

    public boolean hasORNodesInAllTree() {
        if (hasORNodes()) {
            return true;
        } else {
            if (this.children != null) {
                for (List<ANDNODE> childrenNode : this.children.values()) {
                    for (ANDNODE child : childrenNode) {
                        if (child.hasORNodesInAllTree()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean addORnode(ORNODE node) {
        if (node == null) {
            return false;
        }
        createORNodesStructure();
        return this.orNodes.add(node);
    }

    public boolean addORnodes(Collection<ORNODE> nodes) {
        if (nodes != null && !nodes.isEmpty()) {
            createORNodesStructure();
            return this.orNodes.addAll(nodes);
        }
        return false;
    }

    public Deque<ORNODE> getORNodes() {
        if (this.orNodes == null) {
            return EMPTY_DEQUE;
        }
        return this.orNodes;
    }

    public int getSizeOfORNodes() {
        if (this.orNodes != null) {
            return this.orNodes.size();
        }
        return 0;
    }

    public void clearORNodes() {
        if (this.orNodes != null) {
            this.orNodes.clear();
        }
    }

    /* children - alias Roles or Existential Quantifiers -------------------------------------------------------------------------------------------- */

    /**
     * Add the existential in input to the collection of children
     *
     * @param existential OWLObjectSomeValuesFrom to add
     * @return ConcepTree node child created by the call
     */
    public ANDNODE addChild(@Nonnull OWLObjectSomeValuesFrom existential) {
        createChildrenStructure();
        ANDNODE child = new ANDNODE();
        OWLObjectPropertyExpression p = existential.getProperty();
        OWLObjectProperty property = p.getNamedProperty();
        return this.addChild(property, child);
    }

    /**
     * Add the child node in input to the children set with the IRI of
     * OWLObjectSomeValuesFrom specified
     *
     * @param existential OWLObjectSomeValuesFrom
     * @param child       ANDNODE node of the child
     * @return ConcepTree node child created by the call
     */
    public ANDNODE addChild(@Nonnull OWLObjectSomeValuesFrom existential, @Nonnull ANDNODE child) {
        createChildrenStructure();
        OWLObjectPropertyExpression p = existential.getProperty();
        OWLObjectProperty property = p.getNamedProperty();
        return this.addChild(property, child);
    }

    /**
     * Add the child node in input to the children set with the IRI specified
     *
     * @param property Role to add
     * @param child    ANDNODE node of the child
     * @return ConcepTree node child created by the call
     */
    public ANDNODE addChild(@Nonnull OWLObjectProperty property, @Nonnull ANDNODE child) {
        createChildrenStructure();
        List<ANDNODE> tmp = this.children.get(property);
        if (tmp == null) {
            tmp = new LinkedList<>();
        }
        tmp.add(child);
        this.children.put(property, tmp);
        return child;
    }

    public void addChild(OWLObjectProperty property, Collection<ANDNODE> newChildren) {
        if (property != null && newChildren != null && !newChildren.isEmpty()) {
            createChildrenStructure();
            List<ANDNODE> tmp = this.children.get(property);
            if (tmp == null) {
                tmp = new LinkedList<>();
            }
            tmp.addAll(newChildren);
            this.children.put(property, tmp);
        }
    }

    public void makeSingletonObjectProperty(OWLObjectProperty p, ANDNODE child) {
        if (p != null && child != null) {
            createChildrenStructure();
            List<ANDNODE> tmp = new ArrayList<>(1);
            tmp.add(child);
            this.children.put(p, tmp);
        }
    }

    public Map<OWLObjectProperty, List<ANDNODE>> getChildren() {
        if (this.children == null) {
            return EMPTY_MAP;
        }
        return this.children;
    }

    public boolean containsChildren(OWLObjectProperty property) {
        return this.children != null && property != null && this.children.containsKey(property);
    }

    public void clearChildren() {
        if (this.children != null) {
            this.children.clear();
        }
    }

    public void setChildren(Map<OWLObjectProperty, List<ANDNODE>> newChildren) {
        this.children = newChildren;
    }

    public Set<Map.Entry<OWLObjectProperty, List<ANDNODE>>> getChildrenEntrySet() {
        if (this.children == null) {
            return EMPTY_SET;
        }
        return this.children.entrySet();
    }

    public Collection<List<ANDNODE>> getChildrenValuesSet() {
        if (this.children == null) {
            return EMPTY_SET;
        }
        return this.children.values();
    }

    public Set<OWLObjectProperty> getChildrenKeySet() {
        if (this.children == null) {
            return EMPTY_SET;
        }
        return this.children.keySet();
    }

    public List<ANDNODE> getChildren(OWLObjectProperty property) {
        if (this.children == null || property == null || !this.children.containsKey(property)) {
            return EMPTY_LIST;
        }
        return this.children.get(property);
    }

    public List<ANDNODE> removeChildren(OWLObjectProperty property) {
        if (this.children == null || property == null) {
            return EMPTY_LIST;
        }
        return this.children.remove(property);
    }

    public void removeIfEmpty(OWLObjectProperty property) {
        if (this.children != null && property != null && this.children.containsKey(property) && this.children.get(property).isEmpty()) {
            this.children.remove(property);
        }
    }

    public boolean existsButIsEmpty(OWLObjectProperty property) {
        if (this.children != null && property != null) {
            List<ANDNODE> tmp = this.children.get(property);
            return tmp != null && tmp.isEmpty();
        }
        return false;
    }

    public void clearData() {
        this.children = null;
        this.conceptNames = null;
        this.dataConstraints = null;
        this.orNodes = null;
    }

    public void makeBottomNode(OWLClass entity) {
        if (entity != null) {
            clearData();
            this.conceptNames = Collections.singleton(entity);
        }
    }

    public ANDNODE copy() {
        ANDNODE newNode = new ANDNODE();
        if (this.conceptNames != null) {
            newNode.addConceptName(this.conceptNames);
        }
        if (this.dataConstraints != null) {
            for (Map.Entry<OWLDataProperty, List<IntRange>> entry : this.dataConstraints.entrySet()) {
                newNode.addDataProperty(entry.getKey(), entry.getValue());
            }
        }
        if (this.orNodes != null) {
            for (ORNODE node : this.orNodes) {
                ORNODE newORNode = new ORNODE();
                for (ANDNODE tree : node) {
                    newORNode.addTree(tree.copy());
                }
                newNode.addORnode(newORNode);
            }
        }
        if (this.children != null) {
            for (Map.Entry<OWLObjectProperty, List<ANDNODE>> entry : this.children.entrySet()) {
                OWLObjectProperty property = entry.getKey();
                for (ANDNODE child : entry.getValue()) {
                    newNode.addChild(property, child.copy());
                }
            }
        }
        if(this.hasIndividualNames()){
            for (OWLIndividual individualName : this.individualNames) {
                newNode.addIndividualName(individualName);
            }

        }
        return newNode;
    }

    public boolean replaceConceptNames(@Nonnull Set<OWLClass> newSet) {
        if (this.conceptNames == null && !newSet.isEmpty()) {
            this.conceptNames = newSet;
            return true;
        }
        return this.conceptNames != null && this.conceptNames.equals(newSet);
    }

    public boolean replaceDataProperties(@Nonnull Map<OWLDataProperty, List<IntRange>> newProperties) {
        if (this.dataConstraints == null && !newProperties.isEmpty()) {
            this.dataConstraints = newProperties;
            return true;
        }
        return this.dataConstraints != null && this.dataConstraints.equals(newProperties);
    }

    public boolean replaceChildren(@Nonnull Map<OWLObjectProperty, List<ANDNODE>> newProperties) {
        if (this.children == null && !newProperties.isEmpty()) {
            this.children = newProperties;
            return true;
        }
        return this.children != null && this.children.equals(newProperties);
    }

    public boolean replaceNodeOfChild(@Nonnull OWLObjectProperty property, @Nonnull Collection<ANDNODE> newChildren) {
        if (!containsChildren(property)) {
            addChild(property, newChildren);
            return true;
        }
        return false;
    }


    private int childrenSize() {
        return children != null ? children.size() : 0;
    }

    private int conceptNameSize() {
        return conceptNames != null ? conceptNames.size() : 0;
    }

    private int individualNameSize() {
        return individualNames != null ? individualNames.size() : 0;
    }

    private int dataConstraintsSize() {
        return dataConstraints != null ? dataConstraints.size() : 0;
    }

    private int getDisjointSize() {
        return orNodes != null ? orNodes.size() : 0;
    }

    public boolean hasOnlyOneConcept() {
        return childrenSize() == 0 &&
                individualNameSize() == 0 &&
                dataConstraintsSize() == 0 &&
                getDisjointSize() == 0 &&
                conceptNameSize() == 1;
    }

    private String childrenToJson() {
        StringBuilder builder = new StringBuilder();

        if (childrenSize() > 0) {
            int i = 0;
            for (OWLObjectProperty edge : children.keySet()) {
                String property = edge.getIRI().getShortForm();
                builder.append("\n\"").append(property).append("\":");
                for (ANDNODE node : children.get(edge)) {
                    String jsonSubTree = node.toJson();   // recursive call
                    if (node.hasOnlyOneConcept()) {
                        builder.append(jsonSubTree);
                    } else {
                        builder.append("{").append(jsonSubTree).append("}");
                    }
                }
                i++;
                if (i < children.size()) {
                    builder.append(",");
                }

            }

        }
        return builder.toString();
    }

    private String dataConstraintToJson() {
        StringBuilder builder = new StringBuilder();

        if (dataConstraintsSize() > 0) {
            for (OWLDataProperty dataProperty : dataConstraints.keySet()) {
                builder.append("\"").append(dataProperty.getIRI().getShortForm()).append("\":{");

                for (IntRange range : dataConstraints.get(dataProperty)) {
                    int min = range.getMin();
                    int max = range.getMax();
                    builder.append("\"@interval\":[" + min + "," + max + "]");
                }

                builder.append("},");
            }

        }
        return builder.toString();
    }

    private String individualToJson() {
        StringBuilder builder = new StringBuilder();
        if (individualNameSize() == 1) {
            builder.append(",{\n\"@instance\":\"" + individualNames.iterator().next().toStringID() + "\"\n}");
        }
        if (individualNameSize() > 1) {
            int i = 0;
            builder.append(",{\n\"@instance\":[");
            for (OWLIndividual individual : individualNames) {
                builder.append("\"" + individual.toStringID() + "\"");
                i++;
                if (i < individualNameSize()) {
                    builder.append(",");
                }
            }
            builder.append("]");
        }

        return builder.toString();
    }

    private String conceptIndividualToJson() {
        StringBuilder builder = new StringBuilder();
        boolean hasChildren = childrenSize() > 0;

        if (hasOnlyOneConcept()) {
            builder.append("\"").append(conceptNames.iterator().next().getIRI().getShortForm()).append("\"");
        } else if (conceptNameSize() == 0 && individualNameSize() == 1) {
            builder.append("\"@instance\":\"").append(individualNames.iterator().next().toStringID()).append("\"");
            builder.append(hasChildren ? "," : "");
        } else if (conceptNameSize() == 1 && individualNameSize() == 0) {
            builder.append("\"@class\":\"").append(conceptNames.iterator().next().getIRI().getShortForm()).append("\"");
            builder.append(hasChildren ? "," : "");
        } else {
            if (conceptNameSize() > 0) {
                builder.append("\"@intersection\":[");
                int i = 0;
                for (OWLClass concept : conceptNames) {
                    builder.append("\"" + concept.getIRI().getShortForm() + "\"");
                    i++;
                    if (i < conceptNameSize())
                        builder.append(",");
                }
                builder.append(individualToJson());
                builder.append("]");
            } else {
                builder.append(individualToJson());
            }

        }


        return builder.toString();
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();

        /* or node recursive call */
        for (ORNODE orNode : orNodes) builder.append(orNode.toJson());

        /* concept names + individual names to json */
        String names = conceptIndividualToJson();
        builder.append(names);

        /* data property call */
        String dataConstraint = dataConstraintToJson();
        builder.append(dataConstraint);


        /* and-node recursive call */
        String children = childrenToJson();
        builder.append(children);

        return builder.toString();

    }


    @Override
    public String toString() {
        return toString(0, new ArrayList<>());
    }

    public String toString(int depth, List<ANDNODE> visited) {
        depth += 1;
        StringBuilder space = new StringBuilder(" ");
        for (int i = 0; i < depth; i++) {
            space.append("\t");
        }

        if (visited.contains(this))
            return getIndividualNameToString() + "\t [Depth is : "+depth + "...(cycle)]";

        visited.add(this);

        return space + "ANDNODE: {\n" +
                space + "\t CLASSES: (" + conceptNameSize() + ") { " + getConceptNameToString() + " }\n" +
                space + "\t INDIVIDUAL: (" + individualNameSize() + ") { " + getIndividualNameToString() + " }\n" +
                space + "\t CONSTRAINTS: (" + dataConstraintsSize() + ") (" + getDataConstraintsToString() + " )\n" +
                space + "\t DISJOINTS: (" + getDisjointSize() + ") " + getDisjointToString() + "\n" +
                space + "\t CHILDREN: (" + childrenSize() + ") \n" + space + "\t\t" + getChildrenToString(depth, visited) + "\n" +
                space + "}";
    }


    private String getConceptNameToString() {
        StringBuilder s = new StringBuilder();
        if (conceptNames != null)
            for (OWLClass conceptName : conceptNames) {
                if (!conceptName.isBottomEntity()) {
                    s.append(conceptName.getIRI().getShortForm()).append(" ,");
                } else {
                    s.append(conceptName).append(" , ");
                }
            }
        return s.toString();
    }

    private String getIndividualNameToString() {
        StringBuilder s = new StringBuilder();
        if (individualNames != null)
            for (OWLIndividual individual : individualNames) {
                String iString = individual.toString();
                s.append(iString).append(" ,");
//                s.append(iString, iString.indexOf('-'), iString.length() - 1).append(" ,");
            }
        return s.toString();
    }

    private String getDataConstraintsToString() {
        StringBuilder s = new StringBuilder();
        if (dataConstraints != null)
            for (OWLDataProperty dataConstraint : dataConstraints.keySet()) {
                StringBuilder ranges = new StringBuilder();
                for (IntRange intRange : dataConstraints.get(dataConstraint)) {
                    ranges.append(intRange).append(" ");
                }
                s.append(dataConstraint.getIRI().getShortForm()).append(ranges).append(", ");
            }
        return s.toString();
    }

    private String getDisjointToString() {
        StringBuilder s = new StringBuilder();
        if (dataConstraints != null)
            for (ORNODE ornode : orNodes) {
                s.append(ornode).append(" ,");
            }
        return s.toString();
    }

    private String getChildrenToString(int depth, List<ANDNODE> visited) {
        StringBuilder childToString = new StringBuilder();
        for (OWLObjectProperty property : getChildrenKeySet()) {
            StringBuilder childString = new StringBuilder();
            for (ANDNODE child : getChildren(property)) {
                childString.append(child.toString(depth, visited));
            }
            String nf = "\n" + childString;
            childToString.append("\t- ").append(property.getIRI().getShortForm()).append(nf);
        }
        return childToString.toString();
    }

    public OWLClassExpression toOWLClassExpression(OWLDataFactory factory) {
        Set<OWLClassExpression> classSet = new HashSet<>();
        if (childrenSize() > 0) {
            for (OWLObjectProperty property : this.children.keySet()) {
                List<ANDNODE> andnodes = this.children.get(property);
                for (ANDNODE andnode : andnodes) {
                    OWLClassExpression temp = andnode.toOWLClassExpression(factory);
                    OWLObjectSomeValuesFrom owlObjectSomeValuesFrom = factory.getOWLObjectSomeValuesFrom(property, temp);
                    classSet.add(owlObjectSomeValuesFrom);
                }
            }
        }
        if (individualNameSize() > 0) {
            for (OWLIndividual individualName : individualNames) {
                OWLObjectOneOf owlObjectOneOf = factory.getOWLObjectOneOf(individualName);
                classSet.add(owlObjectOneOf);
            }
        }
        if (conceptNameSize() > 0) {
            classSet.addAll(conceptNames);
        }
        if (dataConstraintsSize() > 0) {
            for (OWLDataProperty dataProperty : dataConstraints.keySet()) {
                for (IntRange range : dataConstraints.get(dataProperty)) {
                    OWLDataSomeValuesFrom valuesFrom = factory.getOWLDataSomeValuesFrom(
                            dataProperty,
                            factory.getOWLDatatypeMinMaxInclusiveRestriction(range.getMin(),range.getMax()));
                    classSet.add(valuesFrom);
                }
            }
        }
        if (orNodes != null) {
            for (ORNODE disjunction : orNodes) {
                OWLClassExpression owlClassExpression = disjunction.toOWLClassExpression(factory);
                classSet.add(owlClassExpression);
            }

        }
        return factory.getOWLObjectIntersectionOf(classSet);
    }
}
