package special.model;

import special.model.tree.ANDNODE;

import java.util.Arrays;
import java.util.Objects;

public record History(String id, SignedPolicy<ANDNODE>[] signedPolicy) {
    @Override
    public String toString() {
        return "History{" +
                "id='" + id + '\'' +
                ", signedPolicy=" + Arrays.toString(signedPolicy) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        History history = (History) o;
        return Objects.equals(id, history.id) && Arrays.equals(signedPolicy, history.signedPolicy);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id);
        result = 31 * result + Arrays.hashCode(signedPolicy);
        return result;
    }
}
