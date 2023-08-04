package special.model;

import java.util.Arrays;

public class History {
    private String id;
    private String description;
    private String [] ontologies;
    private String [] context;
    private SignedPolicy<ANDNODE> [] signedPolicy;

    public History(String id,SignedPolicy<ANDNODE>[] signedPolicy) {
        this.id=id;
        this.signedPolicy = signedPolicy;
    }

    @Override
    public String toString() {
        return "History{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", ontologies=" + Arrays.toString(ontologies) +
                ", context=" + Arrays.toString(context) +
                ", signedPolicy=" + Arrays.toString(signedPolicy) +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String[] getOntologies() {
        return ontologies;
    }

    public String[] getContext() {
        return context;
    }

    public SignedPolicy<ANDNODE>[] getSignedPolicy() {
        return signedPolicy;
    }

}
