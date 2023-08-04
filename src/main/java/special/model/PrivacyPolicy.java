package special.model;

import org.semanticweb.owlapi.model.OWLClassExpression;

public class PrivacyPolicy {
    private final PolicyLogic<OWLClassExpression> policyLogic;
    private final History history;
    private boolean isCompliant;
    private int index = 0;
    private int stsCount = 0;

    private final double[] executionTime = new double[10];

    public PrivacyPolicy(PolicyLogic<OWLClassExpression> policyLogic, History history) {
        this.policyLogic = policyLogic;
        this.history = history;
    }

    public void setStsCount(int stsCount) {
        this.stsCount = stsCount;
    }


    public void setCompliant(boolean compliant) {
        this.isCompliant = compliant;
    }

    public String getPolicyLogic() {
        return policyLogic.id();
    }

    public String getHistory() {
        return history.getId();
    }

    public boolean isCompliant() {
        return isCompliant;
    }

    public void setExecutionTime(double time) {
        executionTime[index] = time;
        index++;
    }

    public double getExecutionTime(int index) {
        return executionTime[index];
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
                this.history.getId() +
                delimiter +
                isCompliant +
                delimiter +
                stsCount+
                delimiter+
                stringBuilder;
    }
}