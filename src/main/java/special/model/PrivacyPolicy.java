package special.model;
/**
 * @author Luca Conte
 */

public class PrivacyPolicy {
    private final PolicyLogic<?> policyLogic;
    private final History history;
    private boolean isCompliant;
    private int index = 0;
    private int stsCount = 0;

    private final double[] executionTime = new double[10];

    public PrivacyPolicy(PolicyLogic<?> policyLogic, History history) {
        this.policyLogic = policyLogic;
        this.history = history;
    }

    public void setStsCount(int stsCount) {
        this.stsCount = stsCount;
    }


    public void setCompliant(boolean compliant) {
        this.isCompliant = compliant;
    }

    public String getHistory() {
        return history.id();
    }

    public boolean isCompliant() {
        return isCompliant;
    }

    public void setExecutionTime(double time) {
        executionTime[index] = time;
        index++;
    }

    @Override
    public String toString() {
        String delimiter = ";";
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < this.executionTime.length - 1; i++) {
            stringBuilder.append(this.executionTime[i]).append(";");
        }
        stringBuilder.append(this.executionTime[ this.executionTime.length - 1]);
        return this.policyLogic.id() +
                delimiter +
                this.history.id() +
                delimiter +
                isCompliant +
                delimiter +
                stsCount+
                delimiter+
                stringBuilder;
    }
}