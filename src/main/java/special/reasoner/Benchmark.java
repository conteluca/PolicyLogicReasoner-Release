package special.reasoner;

abstract class Benchmark {
    protected static final String slash = "/";
    protected static final String rootPath = slash + "Users"+ slash +"luca"+ slash +"Desktop"+ slash +"Trapeze"+ slash +"PolicyLogicReasoner-Release"+slash;
}
abstract class ComplianceBenchmark extends Benchmark{
    protected static final String owl2Path = "benchmarkWithNominal"+ slash +"dataset"+ slash +"pilot"+ slash +"compliance"+ slash;
    protected static final String jsonPath = "benchmarkJson"+ slash +"dataset"+ slash +"pilot"+ slash +"compliance"+ slash;

}
abstract class NotComplianceBenchmark extends Benchmark{
    protected static final String owl2Path = "benchmarkWithNominal"+ slash +"dataset"+ slash +"pilot"+ slash +"non-compliance"+ slash;
    protected static final String jsonPath = "benchmarkJson"+ slash +"dataset"+ slash +"pilot"+ slash +"non-compliance"+ slash;
}

class TestbedIDOnlyCompliant extends ComplianceBenchmark{
    static final String SIZE_10_2 = rootPath + "test/testbed-LBS-old-onto-full/realistic/size-10-2/compliant"+ slash;
    static final String SIZE_50_10 = rootPath + "test/testbed-LBS-old-onto-full/realistic/size-50-10/compliant"+ slash;
    static final String SIZE_100_20 = rootPath + "test/testbed-LBS-old-onto-full/realistic/size-100-20/compliant"+ slash;
   }
   class TestbedIDOnlyNotCompliant extends ComplianceBenchmark{
    static final String SIZE_10_2 = rootPath + "test/testbed-LBS-old-onto-full/realistic/size-10-2/non-compliant"+ slash;
    static final String SIZE_50_10 = rootPath + "test/testbed-LBS-old-onto-full/realistic/size-50-10/non-compliant"+ slash;
    static final String SIZE_100_20 = rootPath + "test/testbed-LBS-old-onto-full/realistic/size-100-20/non-compliant"+ slash;
   }
   class TestbedIDOnlyHistory extends ComplianceBenchmark{
    static final String SIZE_10_2 = rootPath + "test/testbed-LBS-old-onto-full/realistic/size-10-2/histories"+ slash;
    static final String SIZE_50_10 = rootPath + "test/testbed-LBS-old-onto-full/realistic/size-50-10/histories"+ slash;
    static final String SIZE_100_20 = rootPath + "test/testbed-LBS-old-onto-full/realistic/size-100-20/histories"+ slash;
   }