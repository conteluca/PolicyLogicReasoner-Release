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

//------------------------compliant-----------------------------
class OWL2DataControllerBenchmarkCompliant extends ComplianceBenchmark {
    static final String PROXIMUS = rootPath + owl2Path +"DataControllerPolicies"+ slash +"PROXIMUS"+ slash;
    static final String TR = rootPath + owl2Path + "DataControllerPolicies"+ slash +"TR"+ slash;

}
class JsonDataControllerBenchmarkCompliant extends ComplianceBenchmark {
    static final String PROXIMUS = rootPath + jsonPath+ "DataControllerPolicies"+ slash +"PROXIMUS"+ slash;
    static final String TR = rootPath + jsonPath + "DataControllerPolicies"+ slash +"TR"+ slash;

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
class OWL2DataSubjectsBenchmarkCompliant extends ComplianceBenchmark {
    static final String PROXIMUS = rootPath + owl2Path+"DataSubjectsPolicies"+ slash +"PROXIMUS"+ slash;
    static final String TR = rootPath + owl2Path+"DataSubjectsPolicies"+ slash +"TR"+ slash;

}
class JsonDataSubjectsBenchmarkCompliant extends ComplianceBenchmark {
    static final String PROXIMUS = rootPath + jsonPath + "DataSubjectsPolicies"+ slash +"PROXIMUS"+ slash;
    static final String TR = rootPath + jsonPath + "DataSubjectsPolicies"+ slash +"TR"+ slash;

}

//------------------------non-compliant-----------------------------

class OWL2DataControllerBenchmarkNotCompliant extends NotComplianceBenchmark {
    static final String PROXIMUS = rootPath + owl2Path +"DataControllerPolicies"+ slash +"PROXIMUS"+ slash;
    static final String TR = rootPath + owl2Path + "DataControllerPolicies"+ slash +"TR"+ slash;

}
class JsonDataControllerBenchmarkNotCompliant extends NotComplianceBenchmark {
    static final String PROXIMUS = rootPath + jsonPath+ "DataControllerPolicies"+ slash +"PROXIMUS"+ slash;
    static final String TR = rootPath + jsonPath + "DataControllerPolicies"+ slash +"TR"+ slash;

}
class OWL2DataSubjectsBenchmarkNotCompliant extends NotComplianceBenchmark {
    static final String PROXIMUS = rootPath + owl2Path+"DataSubjectsPolicies"+ slash +"PROXIMUS"+ slash;
    static final String TR = rootPath + owl2Path+"DataSubjectsPolicies"+ slash +"TR"+ slash;

}
class JsonDataSubjectsBenchmarkNotCompliant extends NotComplianceBenchmark {
    static final String PROXIMUS = rootPath + jsonPath + "DataSubjectsPolicies"+ slash +"PROXIMUS"+ slash;
    static final String TR = rootPath + jsonPath + "DataSubjectsPolicies"+ slash +"TR"+ slash;

}