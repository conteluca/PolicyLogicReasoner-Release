/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.reasoner.cache;

import special.model.tree.ORNODE;

/**
 *
 * @author Luca Ioffredo
 * @param <T1> Key of the cache
 * @param <E> Value's type of the Key
 */
public interface DoubleKeyCache<T1, T2, E> {

    boolean put(T1 key1, T2 key2, E value);

    boolean put(T1 key1, T2 key2, ORNODE disjunction);

    ORNODE get(T1 key1, T2 key2);

    boolean isFull();

    int size();

    void clear();
}
