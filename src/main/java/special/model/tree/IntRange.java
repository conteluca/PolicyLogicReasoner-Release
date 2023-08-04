/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package special.model.tree;

/**
 * @author Luca Conte
 */
public record IntRange(int min, int max) {

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof IntRange cc)) {
            return false;
        }
        return cc.max == this.max && cc.min == this.min;
    }

    public boolean isInclusion(final IntRange i) {
        if (i != null) {
            return i.min <= this.min && this.max <= i.max;
        }
        return false;
    }

    public boolean include(final IntRange i) {
        if (i != null) {
            return this.min <= i.min && i.max <= this.max;
        }
        return false;
    }

    @Override
    public String toString() {
        return "{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }
}
