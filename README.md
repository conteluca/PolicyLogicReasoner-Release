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
- [How To](#HowTo)
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

## Usage
Once the installation is complete, you can start import ontology and use the compliance checker.
### How import the ontology
First step is load ontology into your project using the methods from OntologyLoader class. 
  - Import the ontology from .owl file with **load method**: 
    ```java
    class ComplianceTest{
    
        private static final File src = new File("path/ontology.owl");
        private static final OWLOntology ontology = OntologyLoader.load(src);
    
    }
    ```
  - Import the ontology from policy or history .json file using **getOntologyNames and load methods**:
    ```java
    class ComplianceTest{
    
        public static void main(String[] args) {
            final File src = new File("path/policies/any.json");

            final Set<String> ontologyNames = OntologyLoader.getOntologyNames(src);
            final OWLOntology ontology = OntologyLoader.load(ontologyNames);
        }
    }
    ```
  - Import the ontology from some policies and histories .json file using **getOntologyNames and load methods** (the output ontology is the union of all ontologies):
    ```java
    class ComplianceTest{
    
          public static void main(String[] args) {
            final File a = new File("path/policies/policy.json");
            final File b = new File("path/policies/history.json");
            final HashSet<String> set = new HashSet<>();
            set.add(a);
            set.add(b);
            final Set<String> ontologyNames = OntologyLoader.getOntologyNames(set);
            final OWLOntology ontology = OntologyLoader.load(ontologyNames);
        }      
    }
    ```
  - Import the ontology from any **custom IRI**:
    ```java
     class ComplianceTest{
    
        public static void main(String[] args) {
            final IRI ontologyIRI = IRI.create("http://www.w3id.org/dpv/dpv-gdpr");
            final OWLOntology ontology = OntologyLoader.load(ontologyIRI);
        }  
    }
    ```
## Create Policy Logic Reasoner
After the ontology is imported, we can instantiate a new reasoner with buildReasoner method: 
```java
class ComplianceTest{

  public static void main(String[] args) {
    final IRI ontologyIRI = IRI.create("http://www.w3id.org/dpv/dpv-gdpr");
    final OWLOntology ontology = OntologyLoader.load(ontologyIRI);
    
    final PLReasoner plReasoner = ReasonerBuilder.buildReasoner(ontology); // HERE
  }

}

```
## Compliance Check comparison (policy vs policy)
-  Use isEntailed method with **OWLClassExpression policies**: To check compliance between two privacy policies, use the isEntailed method differently.
The loadSomeExpression method is explained in [How to](#HowTo) section
```java
class ComplianceTest{
    
    public static void main(String[] args) {
        final IRI ontologyIRI = IRI.create("http://www.w3id.org/dpv/dpv-gdpr");
        final OWLOntology ontology = OntologyLoader.load(ontologyIRI);
        final PLReasoner plReasoner = ReasonerBuilder.buildReasoner(ontology);
        
        
        final OWLClassExpression c = loadSomeExpression(); // HERE
        final OWLClassExpression d = loadSomeExpression(); // HERE
        
        boolean isEntailed = plReasoner.isEntailed(c, d);  // COMPLIANCE CHECK

    }
}
```
-  Use isEntailed with **JSON policy file**: given two JSON policy files, check compliance between them using the isEntailed method.
```java
class ComplianceTest{

     
    public static void main(String[] args) {
        final IRI ontologyIRI = IRI.create("http://www.w3id.org/dpv/dpv-gdpr");
        final OWLOntology ontology = OntologyLoader.load(ontologyIRI);
        final PLReasoner plReasoner = ReasonerBuilder.buildReasoner(ontology);
        
        final File c = new File("pathToJSONFile.json"); // HERE
        final File d = new File("pathToJSONFile2.json"); // HERE
        
        boolean isEntailed = plReasoner.isEntailed(c, d);  // COMPLIANCE CHECK

    }
}
```
-  Use isEntailed with **ANDNODE policy format**: given two trees representing policies, check compliance between them using the isEntailed method.
   The buildTreeFromPolicy method is explained in [How to](#HowTo) section
```java
class ComplianceTest{

     
    public static void main(String[] args) {
        final IRI ontologyIRI = IRI.create("http://www.w3id.org/dpv/dpv-gdpr");
        final OWLOntology ontology = OntologyLoader.load(ontologyIRI);
        final PLReasoner plReasoner = ReasonerBuilder.buildReasoner(ontology);

        final ANDNODE c = buildTreeFromPolicy(policy1);
        final ANDNODE d = buildTreeFromPolicy(policy2);
        
        boolean entailed = plReasoner.isEntailed(c, d);

    }
}
```
## Compliance Check comparison (policy vs history)

-  **Use isEntailedHistory with JSON policy file**: To check compliance between privacy policy and history, use the isEntailedHistory method differently.
```java
class ComplianceTest{
    
    public static void main(String[] args) {
        // ontology and reasoner declaration.
        
        final File c = someJSONpolicy(); // HERE
        final File h = someHistorypolicy(); // HERE
        
        boolean isEntailed = plReasoner.isEntailedHistory(c, h);  // COMPLIANCE CHECK

    }
}
```
-  **Use isEntailed with tree format**: To check compliance between privacy policy and history, use the isEntailed method.

```java
import special.model.SignedPolicy;
import special.model.tree.ANDNODE;

class ComplianceTest {

  public static void main(String[] args) {
      // ontology and reasoner declaration.
      
    final ANDNODE c = parseJSONPolicy(policy);
    final SignedPolicy<ANDNODE>[] h = parseJSONHistory(history);
    boolean entailed = plReasoner.isEntailed(c, h);

  }
}
```

Make sure to refer to the Documentation for a comprehensive understanding of the available methods,
their parameters, and how to interpret the compliance results.

Now you're ready to integrate the Privacy Policy Compliance Checker into your project
and enhance your compliance verification process!

For more advanced usage and in-depth examples, refer to the [How To](#HowTo) section.
## How To
In this section, we provide a step-by-step guide on how to effectively use some methods to facilitate the process.
We explain practical examples and walk you through common tasks to help you get started quickly. 
Whether you're a beginner or an experienced user,
this guide will assist you in understanding the core functionalities and best practices of a Trapeze project.

### Translate json policies to tree format
Use TranslatorEngine class and parseJSONPolicy method.
```java
class ComplianceTest {

  public static void main(String[] args) {
      final IRI ontologyIRI = IRI.create("http://www.w3id.org/dpv/dpv-gdpr");
      final OWLOntology ontology = OntologyLoader.load(ontologyIRI);

      final File jsonPolicy = new File("policy.json");
      final TranslatorEngine translator = new TranslatorEngine(new OntologyAxioms(ontology));

      final ANDNODE tree = translator.parseJSONPolicy(jsonPolicy);

  }
}
```
### Translate json history to trees signed policies format
Use TranslatorEngine class and parseJSONHistory method.
```java
class ComplianceTest {

  public static void main(String[] args) {
      final IRI ontologyIRI = IRI.create("http://www.w3id.org/dpv/dpv-gdpr");
      final OWLOntology ontology = OntologyLoader.load(ontologyIRI);

      final File jsonPolicy = new File("history.json");
      final TranslatorEngine translator = new TranslatorEngine(new OntologyAxioms(ontology));

      final SignedPolicy<ANDNODE>[] tree = translator.parseJSONHistory(jsonPolicy);

  }
}
```
### Translate OWL2 Policy to tree format
Use TranslatorEngine class and buildTree method; (`dataset/` folder have to contain some OWL2 policy)
```java
class ComplianceTest {

  public static void main(String[] args) {
      // ontology and reasoner declaration.

      final OWLPolicyIterator iterator = new OWLPolicyIterator("dataset/");
      final PolicyLogic<OWLClassExpression> next = iterator.next();

      final ANDNODE tree = plReasoner.buildTree(next.expression());
      
  }
}
```
### Iterate JSON policy
Use JSONPolicyIterator object
to iterate all policies contained into some directory; (`dataset/` folder have
to contain some JSON policies).
The iterator can return different format types of policy.

```java
class ComplianceTest {

    public static void main(String[] args) {
        // ontology and reasoner declaration.

        final JSONPolicyIterator iterator = new JSONPolicyIterator(ontology,"dataset/",false);
        final PolicyLogic<JSONArray> jsonType = iterator.next();                
        final PolicyLogic<OWLClassExpression> owlType = iterator.toOwl();

    }
}
```
### Iterate JSON history
Use JSONHistoryIterator object
to iterate all histories contained into some directory; (`dataset/` folder have
to contain some JSON histories).
    
```java
class ComplianceTest {

    public static void main(String[] args) {
        // ontology and reasoner declaration.

        final JSONHistoryIterator iterator = new JSONHistoryIterator(ontology,"dataset/",false);
        final History history = iterator.next();
        System.out.println(history.id()+" loaded");

    }
}
```

## Contributing

If you want to contribute to the project check https://trapeze-project.eu/.

## License

This project is distributed under the [Trapeze License](https://trapeze-project.eu/).

