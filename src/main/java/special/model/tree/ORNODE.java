/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.model.tree;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.*;

/**
 * OR Node of a Policy Logic Tree
 *
 * @author Luca Ioffredo
 */
public class ORNODE implements Iterable<ANDNODE>, Deque<ANDNODE>, Serializable {

    private final Deque<ANDNODE> disjunction;

    @Override
    public String toString() {
        return " ("+ disjunction.size()+") " + disjunction + " \n";

    }

    public OWLClassExpression toOWLClassExpression(OWLDataFactory factory){
        Set<OWLClassExpression> classSet = new HashSet<>();
        for (ANDNODE disjunctive : this.disjunction) {
            OWLClassExpression owlClassExpression = disjunctive.toOWLClassExpression(factory);
            classSet.add(owlClassExpression);
        }
        return factory.getOWLObjectUnionOf(classSet);
    }
    public String toJson(){
        StringBuilder main = new StringBuilder("[");
        int ndisj = 0;
        String action;
        for (ANDNODE i : disjunction) {
            if(ndisj%2==0){
                action = "\n\"@action\":\"permit\",\n";
            }else{
                action = "\n\"@action\":\"deny\",\n";
            }
            main.append("{").append(action).append(i.toJson()).append("\n}");
            ndisj++;
            if (ndisj< this.disjunction.size()){
                main.append(",\n");
            }
        }
       return main.append("]").toString();
    }
    public ORNODE() {
        this.disjunction = new LinkedList<>();
    }

    public ORNODE(int size) {
        if (size <= 0) {
            size = 32;
        }
        this.disjunction = new ArrayDeque<>(size);
    }

    public ORNODE(Collection<? extends ANDNODE> nodes) {
        this(nodes.size() + 1);
        this.disjunction.addAll(nodes);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ORNODE cc)) {
            return false;
        }
        return cc.disjunction == null ? this.disjunction == null : this.disjunction != null && cc.disjunction.containsAll(this.disjunction)
                && this.disjunction.containsAll(cc.disjunction);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for(ANDNODE node : this.disjunction) {
            hashCode += node.hashCode();
        }
        return 53 * 3 + hashCode;
    }

    public boolean isEmpty() {
        return this.disjunction.isEmpty();
    }

    public int size() {
        return this.disjunction.size();
    }

    public void clear() {
        this.disjunction.clear();
    }

    public Deque<ANDNODE> getDisjunction() {
        return this.disjunction;
    }

    public boolean addTree(ANDNODE tree) {
        return tree != null && this.disjunction.add(tree);
    }

    public boolean addTrees(Collection<? extends ANDNODE> trees) {
        return trees != null && this.disjunction.addAll(trees);
    }

    @Override
    public Iterator<ANDNODE> iterator() {
        return this.disjunction.iterator();
    }

    @Override
    public Object[] toArray() {
        return this.disjunction.toArray();
    }

    @Override
    public <T> T[] toArray(@Nonnull T[] a) {
        return this.disjunction.toArray(a);
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        return this.disjunction.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends ANDNODE> c) {
        return this.disjunction.addAll(c);
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        return this.disjunction.removeAll(c);
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        return this.disjunction.retainAll(c);
    }

    @Override
    public void addFirst(ANDNODE e) {
        this.disjunction.addFirst(e);
    }

    @Override
    public void addLast(ANDNODE e) {
        this.disjunction.addLast(e);
    }

    @Override
    public boolean offerFirst(ANDNODE e) {
        return this.disjunction.offerFirst(e);
    }

    @Override
    public boolean offerLast(ANDNODE e) {
        return this.disjunction.offerLast(e);
    }

    @Override
    public ANDNODE removeFirst() {
        return this.disjunction.removeFirst();
    }

    @Override
    public ANDNODE removeLast() {
        return this.disjunction.removeLast();
    }

    @Override
    public ANDNODE pollFirst() {
        return this.disjunction.pollFirst();
    }

    @Override
    public ANDNODE pollLast() {
        return this.disjunction.pollLast();
    }

    @Override
    public ANDNODE getFirst() {
        return this.disjunction.getFirst();
    }

    @Override
    public ANDNODE getLast() {
        return this.disjunction.getLast();
    }

    @Override
    public ANDNODE peekFirst() {
        return this.disjunction.peekFirst();
    }

    @Override
    public ANDNODE peekLast() {
        return this.disjunction.peekLast();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return this.disjunction.removeFirstOccurrence(o);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        return this.disjunction.removeLastOccurrence(o);
    }

    @Override
    public boolean add(ANDNODE e) {
        return this.disjunction.add(e);
    }

    @Override
    public boolean offer(ANDNODE e) {
        return this.disjunction.offer(e);
    }

    @Override
    public ANDNODE remove() {
        return this.disjunction.remove();
    }

    @Override
    public ANDNODE poll() {
        return this.disjunction.poll();
    }

    @Override
    public ANDNODE element() {
        return this.disjunction.element();
    }

    @Override
    public ANDNODE peek() {
        return this.disjunction.peek();
    }

    @Override
    public void push(ANDNODE e) {
        this.disjunction.push(e);
    }

    @Override
    public ANDNODE pop() {
        return this.disjunction.pop();
    }

    @Override
    public boolean remove(Object o) {
        return this.disjunction.remove(o);
    }

    @Override
    public boolean contains(Object o) {
        return this.disjunction.contains(o);
    }

    @Override
    public Iterator<ANDNODE> descendingIterator() {
        return this.disjunction.descendingIterator();
    }
}
