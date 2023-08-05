package special.reasoner.utility;

public class Benchmark{
    private static final String REALISTIC = "test/testbed-LBS-old-onto-full/realistic/";
    private static final String SIZE_10_2 = REALISTIC + "size-10-2";
    private static final String SIZE_50_10 = REALISTIC + "size-50-10";
    private static final String SIZE_100_20 = REALISTIC + "size-100-20";
    private Benchmark() {
        throw new IllegalStateException("Utility class Benchmark");
    }

    public static class Policy {
        private Policy() {
            throw new IllegalStateException("Utility class Policy");
        }

        public static class Compliant {
            private static final String COMPLIANT_POLICY = "/compliant/";

            private Compliant() {
                throw new IllegalStateException("Utility class Compliant");
            }

            public static final String SIZE_10_2 = Benchmark.SIZE_10_2 + COMPLIANT_POLICY;
            public static final String SIZE_50_10 = Benchmark.SIZE_50_10 + COMPLIANT_POLICY;
            public static final String SIZE_100_20 = Benchmark.SIZE_100_20 + COMPLIANT_POLICY;
        }

        public static class NonCompliant {
            private static final String NON_COMPLIANT = "/non-compliant/";

            private NonCompliant() {
                throw new IllegalStateException("Utility class NonCompliant");
            }
            public static final String SIZE_10_2 = Benchmark.SIZE_10_2 + NON_COMPLIANT;
            public static final String SIZE_50_10 = Benchmark.SIZE_50_10 + NON_COMPLIANT;
            public static final String SIZE_100_20 = Benchmark.SIZE_100_20 + NON_COMPLIANT;
        }
    }

    public static class History {
        private static final String HISTORIES = "/histories/";

        private History() {
            throw new IllegalStateException("Utility class History");
        }

        public static final String SIZE_10_2 = Benchmark.SIZE_10_2 + HISTORIES;
        public static final String SIZE_50_10 = Benchmark.SIZE_50_10 + HISTORIES;
        public static final String SIZE_100_20 = Benchmark.SIZE_100_20 + HISTORIES;

    }

}