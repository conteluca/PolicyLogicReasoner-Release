package special.model;

public record SignedPolicy<T>(boolean permit, T data) {
    @Override
    public String toString() {
        return "History{" +
                "permit=" + permit +
                ", data= "+data+"}";
    }
}
