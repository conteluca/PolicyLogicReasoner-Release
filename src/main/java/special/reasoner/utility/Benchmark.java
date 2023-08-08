package special.reasoner.utility;

public class Benchmark {

    private Benchmark() {
        throw new IllegalStateException("Utility class Benchmark");
    }

    public static class Realistic {
        private Realistic() {
            throw new IllegalStateException("Realistic class Benchmark");
        }

        private static final String REALISTIC = "test/testbed-LBS-old-onto-full/realistic/";
        private static final String SIZE_10_2 = REALISTIC + "size-10-2";
        private static final String SIZE_50_10 = REALISTIC + "size-50-10";
        private static final String SIZE_100_20 = REALISTIC + "size-100-20";

        public static class Policy {
            private Policy() {
                throw new IllegalStateException("Utility class Policy");
            }

            public static class Compliant {
                private static final String COMPLIANT_POLICY = "/compliant/";

                private Compliant() {
                    throw new IllegalStateException("Utility class Compliant");
                }

                public static final String SIZE_10_2 = Benchmark.Realistic.SIZE_10_2 + COMPLIANT_POLICY;
                public static final String SIZE_50_10 = Benchmark.Realistic.SIZE_50_10 + COMPLIANT_POLICY;
                public static final String SIZE_100_20 = Benchmark.Realistic.SIZE_100_20 + COMPLIANT_POLICY;
            }

            public static class NonCompliant {
                private static final String NON_COMPLIANT = "/non-compliant/";

                private NonCompliant() {
                    throw new IllegalStateException("Utility class NonCompliant");
                }

                public static final String SIZE_10_2 = Benchmark.Realistic.SIZE_10_2 + NON_COMPLIANT;
                public static final String SIZE_50_10 = Benchmark.Realistic.SIZE_50_10 + NON_COMPLIANT;
                public static final String SIZE_100_20 = Benchmark.Realistic.SIZE_100_20 + NON_COMPLIANT;
            }
        }

        public static class History {
            private static final String HISTORIES = "/histories/";

            private History() {
                throw new IllegalStateException("Utility class History");
            }

            public static final String SIZE_10_2 = Benchmark.Realistic.SIZE_10_2 + HISTORIES;
            public static final String SIZE_50_10 = Benchmark.Realistic.SIZE_50_10 + HISTORIES;
            public static final String SIZE_100_20 = Benchmark.Realistic.SIZE_100_20 + HISTORIES;

        }
    }

    public static class Stress {
        private Stress() {
            throw new IllegalStateException("Stress class Policy");
        }

        private static final String STRESS_COMPLIANT = "test/testbed-LBS-old-onto-full/stress/compliant/";
        private static final String STRESS_NOT_COMPLIANT = "test/testbed-LBS-old-onto-full/stress/non-compliant/";

        public static class Policy{
            private Policy() {
                throw new IllegalStateException("Policy Stress class Policy");
            }

            public static class Compliant{
                private Compliant() {
                    throw new IllegalStateException("Compliant Policy Stress class Policy");
                }

                public static final String SIZE_10_OVRD_2 = STRESS_COMPLIANT + "size-10-ovrd-2/policies/";
                public static final String SIZE_10_OVRD_4 = STRESS_COMPLIANT + "size-10-ovrd-4/policies/";
                public static final String SIZE_50_OVRD_2 = STRESS_COMPLIANT + "size-50-ovrd-2/policies/";
                public static final String SIZE_50_OVRD_4 = STRESS_COMPLIANT + "size-50-ovrd-4/policies/";
                public static final String SIZE_100_OVRD_2 = STRESS_COMPLIANT + "size-100-ovrd-2/policies/";
                public static final String SIZE_100_OVRD_4 = STRESS_COMPLIANT + "size-100-ovrd-4/policies/";



            }
            public static class NonCompliant{
                private NonCompliant() {
                    throw new IllegalStateException("NonCompliant Policy Stress class Policy");
                }
                public static final String SIZE_10_OVRD_3 = STRESS_NOT_COMPLIANT + "size-10-ovrd-3/policies/";
                public static final String SIZE_10_OVRD_5 = STRESS_NOT_COMPLIANT + "size-10-ovrd-5/policies/";
                public static final String SIZE_50_OVRD_3 = STRESS_NOT_COMPLIANT + "size-50-ovrd-3/policies/";
                public static final String SIZE_50_OVRD_5 = STRESS_NOT_COMPLIANT + "size-50-ovrd-5/policies/";
                public static final String SIZE_100_OVRD_3 = STRESS_NOT_COMPLIANT + "size-100-ovrd-3/policies/";
                public static final String SIZE_100_OVRD_5 = STRESS_NOT_COMPLIANT + "size-100-ovrd-5/policies/";

            }

        }
        public static class History{
            private History() {
                throw new IllegalStateException("History Stress class Policy");
            }

            public static class Compliant{
                private Compliant() {
                    throw new IllegalStateException("Compliant Policy Stress class Policy");
                }

                public static final String SIZE_10_OVRD_2 = STRESS_COMPLIANT + "size-10-ovrd-2/histories/";
                public static final String SIZE_10_OVRD_4 = STRESS_COMPLIANT + "size-10-ovrd-4/histories/";
                public static final String SIZE_50_OVRD_2 = STRESS_COMPLIANT + "size-50-ovrd-2/histories/";
                public static final String SIZE_50_OVRD_4 = STRESS_COMPLIANT + "size-50-ovrd-4/histories/";
                public static final String SIZE_100_OVRD_2 = STRESS_COMPLIANT + "size-100-ovrd-2/histories/";
                public static final String SIZE_100_OVRD_4 = STRESS_COMPLIANT + "size-100-ovrd-4/histories/";

            }
            public static class NonCompliant{
                private NonCompliant() {
                    throw new IllegalStateException("NonCompliant Policy Stress class Policy");
                }
                public static final String SIZE_10_OVRD_3 = STRESS_NOT_COMPLIANT + "size-10-ovrd-3/histories/";
                public static final String SIZE_10_OVRD_5 = STRESS_NOT_COMPLIANT + "size-10-ovrd-5/histories/";
                public static final String SIZE_50_OVRD_3 = STRESS_NOT_COMPLIANT + "size-50-ovrd-3/histories/";
                public static final String SIZE_50_OVRD_5 = STRESS_NOT_COMPLIANT + "size-50-ovrd-5/histories/";
                public static final String SIZE_100_OVRD_3 = STRESS_NOT_COMPLIANT + "size-100-ovrd-3/histories/";
                public static final String SIZE_100_OVRD_5 = STRESS_NOT_COMPLIANT + "size-100-ovrd-5/histories/";


            }
        }
    }

}