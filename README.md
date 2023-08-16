# PolicyLogicReasoner-Release

Welcome to the Privacy PolicyLogicReasoner project!
This powerful tool is designed to enable precise compliance verification for privacy policies,
not only between two privacy policies but also between a privacy policy and a history of policies,
all powered by the capabilities of the OWL2 Reasoner.
Contains a Compliance Checker Reasoner for Policy Logic with Nominal and Histories.
<Br />
Check https://trapeze-project.eu/ for more details.

## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Requirements](#Requirements)
- [Getting Started](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Contributing](#contributing)
- [License](#license)

## Introduction

In an evolving digital landscape, ensuring privacy policy compliance has become paramount.
Organizations and developers need effective means
to verify that their privacy policies adhere to the necessary regulations and standards.
This is where the Privacy Policy Compliance Checker steps in with a distinctive edge.

Our project harnesses the capabilities of the OWL2 Reasoner,
a cutting-edge technology in the realm of semantic web tools,
to facilitate in-depth analysis of policy compliance.
It goes beyond simplistic keyword matching,
offering advanced assessment of policy elements, hierarchies, and intricate relationships.

## Features
* **OWL2 Reasoning**: Employs the prowess of the OWL2 Reasoner for advanced policy analysis.
* **Policy-to-Policy Compliance**: Assesses the compliance between two privacy policies, highlighting potential discrepancies and conflicts.
* **Privacy Policy and History Compliance**: Extends compliance checks to include a privacy policy and a history of policies. This history, presented as a JSON file, contains signed policy entries (permit and deny).
* **Semantic Analysis**: Ensures thorough compliance evaluation by taking into account hierarchical relationships within policies.
* **Comprehensive Reporting**: Generates detailed compliance reports,
  pointing out detected issues and suggesting feasible resolutions.
Whether you're an organization
  striving to ensure policy alignment with the latest privacy regulations or a developer
  integrating third-party services,
  our Privacy Policy Compliance Checker is your indispensable companion.
  With support for both policy-to-policy and policy-to-history checks, we're here to streamline your compliance endeavors.

## Requirements

1. Java Development Kit 17 (JDK v17).
2. Java Runtime Environment (JRE).

## Getting Started

To embark on your journey, refer to the [Installation](#installation) and [Usage](#usage) sections of this README.
We're thrilled to have you join our community of privacy-conscious developers and organizations,
collectively working towards a safer, more compliant digital realm.

Remember, the Privacy Policy Compliance Checker isn't just a tool;
it's a catalyst for a more transparent and compliant digital world.

## Installation

Follow these steps to get started with the Privacy Policy Compliance Checker:

1. Extract project directory or download it from GitHub
2. Go inside PolicyLogicReasoner-Release directory.
3. NB. Don't move, modify or drop ▶️`/ontology` and ▶️`/test`folder. They contain ontologies, policies and histories json files.
4. Open terminal into ▶️`/PolicyLogicReasoner-Release` directory and run:
```bash
 java -cp PolicyLogicReasoner.jar special.reasoner.Main
```

# Usage
Once the installation is complete, you can start using the compliance checker.
- **Import the ontology**: load ontology into your project using OntologyLoader class. You can load from file or from web:
```java
class ComplianceTest{

    private static final OWLOntology ontology = OntologyLoader.load(OntologyLoader.PATH_ONTOLOGY);  // HERE from file
    private static final OWLOntology ontology_web = OntologyLoader.load("http://example.com/ontology.owl"); // HERE from web


}
```
- **Create Policy Logic Reasoner**: create a new reasoner with buildReasoner method: 
```java
class ComplianceTest{

    private static final OWLOntology ontology = OntologyLoader.load(OntologyLoader.PATH_ONTOLOGY); 
    private static final PLReasoner plReasoner = ReasonerBuilder.buildReasoner(ontology); // HERE

}
```
-  **Choose which policy you want to load**: you can upload compliance and non-compliance policies of different sizes. Sizes are 10_2, 50_10, 100_20. In this example we load compliant with SIZE_100_20.
```java
class ComplianceTest{

    private static final OWLOntology ontology = OntologyLoader.load(OntologyLoader.PATH_ONTOLOGY);
    private static final PLReasoner plReasoner = ReasonerBuilder.buildReasoner(ontology); 
    private static final String POLICY_FOLDER = Benchmark.Realistic.Policy.Compliant.SIZE_100_20; // HERE
    
}
```
-  **Declare a PolicyIterator**: policy iterator loads all policies from .json files and allows to iterate them. If you want to enable Knowledge Base check of terms then set the third parameter to true.
```java
class ComplianceTest{

    private static final OWLOntology ontology = OntologyLoader.load(OntologyLoader.PATH_ONTOLOGY);
    private static final PLReasoner plReasoner = ReasonerBuilder.buildReasoner(ontology); 
    private static final String POLICY_FOLDER = Benchmark.Realistic.Policy.Compliant.SIZE_100_20; // HERE

    public static void main(String[] args) {
        
        final PolicyIterator policyIterator = new PolicyIterator(ontology, POLICY_FOLDER, false); // HERE
        final PolicyLogic<OWLClassExpression> c = policyIterator.toOwl(); // HERE

    }
}
```
-  **Policy Compliance Comparison**: To compare two privacy policies, get instances of PLReasoner and use the isEntailed method.
```java
class ComplianceTest {
    
    /*
    ... all declarations
    */

    public static void main(String[] args) {

        final PolicyIterator policyIterator = new PolicyIterator(ontology, POLICY_FOLDER, false);

        final PolicyLogic<OWLClassExpression> c = policyIterator.toOwl();

        // ADD FROM HERE
        while (policyIterator.hasNext()) {
            final PolicyLogic<OWLClassExpression> d = policyIterator.toOwl();

            boolean isEntailed = plReasoner.isEntailed(c, d);  // COMPLIANCE CHECK
            if (isEntailed) {
                System.out.println(c.id() + " entails " + d.id());
            } else {
                System.out.println(c.id() + " doesn't entails " + d.id());
            }
        } 
        // TO HERE
    }
}
```
-  **Policy and History Compliance**: To check compliance between a privacy policy and a history, use the isEntailed method. To load histories, you have to use an HistoryIterator too.
```java
class ComplianceTest{
    
    /*
    ... all declarations
    */
    private static final String HISTORY_FOLDER = Benchmark.Realistic.History.SIZE_100_20;  // ADD HERE

    public static void main(String[] args) {

        final PolicyIterator policyIterator = new PolicyIterator(ontology, POLICY_FOLDER, false);

        // ADD FROM HERE
        final HistoryIterator historyIterator = new HistoryIterator(ontology, HISTORY_FOLDER, false);
        final History history = historyIterator.next();


        do {
            final PolicyLogic<OWLClassExpression> c = policyIterator.toOwl();
            
            boolean isEntailed = plReasoner.isEntailed(c, history);  // COMPLIANCE CHECK
            
            if(isEntailed){
                System.out.println(c.id()+" entails "+history.id());
            }else{
                System.out.println(c.id()+" doesn't entails "+history.id());
            }
        } while (policyIterator.hasNext());
        // TO HERE
    }
}
```
-  **Use isEntailed with OWLClassExpression**: To check compliance between two privacy policies, use the isEntailed method differently.
```java
class ComplianceTest{

    private static final OWLOntology ontology = OntologyLoader.load(OntologyLoader.PATH_ONTOLOGY);
    private static final PLReasoner plReasoner = ReasonerBuilder.buildReasoner(ontology);
    
    public static void main(String[] args) {
      
        final OWLClassExpression c = loadSomeExpression(); // HERE
        final OWLClassExpression d = loadSomeExpression(); // HERE
        
      boolean isEntailed = plReasoner.isEntailed(c, d);  // COMPLIANCE CHECK

    }
}
```
-  **Use isEntailed with JSON policy file**: To check compliance between two privacy policies, use the isEntailed method differently.
```java
class ComplianceTest{

    private static final OWLOntology ontology = OntologyLoader.load(OntologyLoader.PATH_ONTOLOGY);
    private static final PLReasoner plReasoner = ReasonerBuilder.buildReasoner(ontology);
    
    public static void main(String[] args) {
      
        final File c = someJSONpolicy(); // HERE
        final File d = someJSONpolicy(); // HERE
        
      boolean isEntailed = plReasoner.isEntailed(c, d);  // COMPLIANCE CHECK

    }
}
```
-  **Use isEntailedHistory with JSON policy file**: To check compliance between privacy policy and history, use the isEntailedHistory method differently.
```java
class ComplianceTest{

    private static final OWLOntology ontology = OntologyLoader.load(OntologyLoader.PATH_ONTOLOGY);
    private static final PLReasoner plReasoner = ReasonerBuilder.buildReasoner(ontology);
    
    public static void main(String[] args) {
      
        final File c = someJSONpolicy(); // HERE
        final File h = someHistorypolicy(); // HERE
        
      boolean isEntailed = plReasoner.isEntailedHistory(c, h);  // COMPLIANCE CHECK

    }
}
```
-  **Use isEntailed with two trees (internal format)**: To check compliance between two privacy policies, use the isEntailed method differently and use TranslatorEngine to parse policies.
```java
import special.model.tree.ANDNODE;

class ComplianceTest {

  private static final OWLOntology ontology = OntologyLoader.load(OntologyLoader.PATH_ONTOLOGY);
  private static final PLReasoner plReasoner = ReasonerBuilder.buildReasoner(ontology);

  public static void main(String[] args) {
    File policy1 = new File("test/testbed-LBS-old-onto-full/realistic/size-10-2/compliant/Contr-pol-compl-no-exc.json");
    File policy2 =new File("test/testbed-LBS-old-onto-full/realistic/size-10-2/compliant/Contr-pol-compl-ovrd-1.json");

    TranslatorEngine translator = new TranslatorEngine(new OntologyAxioms(ontology));
    ANDNODE c = translator.parseJSONPolicy(policy1);
    ANDNODE d = translator.parseJSONPolicy(policy2);
    boolean entailed = plReasoner.isEntailed(c, d);
    System.out.println(entailed);

  }
}
```
-  **Use isEntailed with tree and history (internal format)**: To check compliance between privacy policy and history, use the isEntailed and TranslatorEngine to parse policies.

```java
import special.model.SignedPolicy;
import special.model.tree.ANDNODE;

class ComplianceTest {

  private static final OWLOntology ontology = OntologyLoader.load(OntologyLoader.PATH_ONTOLOGY);
  private static final PLReasoner plReasoner = ReasonerBuilder.buildReasoner(ontology);

  public static void main(String[] args) {
    File policy = new File("test/testbed-LBS-old-onto-full/realistic/size-10-2/compliant/Contr-pol-compl-no-exc.json");
    File history =new File("test/testbed-LBS-old-onto-full/realistic/size-10-2/histories/hist-10-2-1.json");

    TranslatorEngine translator = new TranslatorEngine(new OntologyAxioms(ontology));
    ANDNODE c = translator.parseJSONPolicy(policy);
    SignedPolicy<ANDNODE>[] h = translator.parseJSONHistory(history);
    boolean entailed = plReasoner.isEntailed(c, h);
    System.out.println(entailed);

  }
}
```

Make sure to refer to the Documentation for a comprehensive understanding of the available methods,
their parameters, and how to interpret the compliance results.

Now you're ready to integrate the Privacy Policy Compliance Checker into your project
and enhance your compliance verification process!

For more advanced usage and in-depth examples, refer to the Examples section.

## Contributing

If you want to contribute to the project check https://trapeze-project.eu/.

## License

This project is distributed under the [Trapeze License](https://trapeze-project.eu/).

