# PolicyLogicReasoner-Release

This projects contains a Compliance Checker Reasoner for Policy Logic with Nominal. <br />
Check https://trapeze-project.eu/ for more details. 

## Table of contents
* [Requirements](#Requirements)
* [Quick start](#Quick-start)
* [Test with Hermit](#Test-with-Hermit)
* [Test with Default PLR](#Test-with-Default-PLR)
* [Test with Buffering PLR](#Test-with-Buffering-PLR)
* [Test with Single cache PLR](#Test-with-Single-cache-PLR)
* [Test with Double cache PLR](#Test-with-Double-cache-PLR)
* [Example Single cache PLR Resultus](#Example-Single-cache-PLR-Resultus)

## Requirements

1. Java Development Kit 17 (JDK v17). Check installation here: https://www.oracle.com/java/technologies/downloads/
2. Java Runtime Environment (JRE). Check installation here: https://www.java.com/it/download/manual.jsp

## Quick start

1. Extract project directory
2. Go inside PolicyLogicReasoner-Release directory.
3. Don't move, modify or drop 'benchmarkWithNominal' folder and subfolder. It contains dataset policy files.
4. Open terminal and run:
```bash
 java -cp PolicyLogicReasoner.jar special.reasoner.Main
```
5. On terminal will be show this message:
```bash
Ontology loaded! Total axioms: 1224
Press 0 for Hermit Reasoner
Press 1 for Default PLR
Press 2 for Buffering PLR
Press 3 for Single cache PLR
Press 4 for Double cache PLR
Press other key to Exit
```

## Test with Hermit

1. On terminal insert 0 and press enter
2. Hermit Reasoner will be built and it starts compliance checking
3. Finally will display results

## Test with Default PLR

1. On terminal insert 1 and press enter
2. Default PLR Reasoner will be built and it starts compliance checking
3. Finally will display results

## Test with Buffering PLR

1. On terminal insert 2 and press enter
2. Buffering PLR will be built and it starts compliance checking
3. Finally will display results


## Test with Single cache PLR

1. On terminal insert 3 and press enter
2. Single cache PLR will be built and it starts compliance checking
3. Finally will display results


## Test with Double cache PLR

1. On terminal insert 4 and press enter
2. Double cache PLR will be built and it starts compliance checking
3. Finally will display results

## Example Single cache PLR Resultus
```bash
Building Single Cache PLR with cacheSize 150...
 Testing proximus compliant policies..
        Compliant: 6000, Not compliant: 0, Total time = 972.92771 milliseconds
        Time per compliant check: 0.16215461833333333 milliseconds
        Standard deviation: 0.16728012082976235
        Maximum time compliance check: 3.1825 milliseconds
        Minimum time compliance check: 0.01047 milliseconds
 Testing TR compliant policies..
        Compliant: 4999, Not compliant: 0, Total time = 224.3558 milliseconds
        Time per compliant check: 0.04488013602720544 milliseconds
        Standard deviation: 0.049418416191064525
        Maximum time compliance check: 0.92469 milliseconds
        Minimum time compliance check: 0.00976 milliseconds
 Testing proximus non-compliant policies..
        Compliant: 0, Not compliant: 6000, Total time = 140.69882 milliseconds
        Time per compliant check: 0.02344980333333333 milliseconds
        Standard deviation: 0.025895984121635986
        Maximum time compliance check: 0.86043 milliseconds
        Minimum time compliance check: 0.00373 milliseconds
 Testing TR non-compliant policies..
        Compliant: 0, Not compliant: 5000, Total time = 156.46123 milliseconds
        Time per compliant check: 0.031292246 milliseconds
        Standard deviation: 0.04025383463442266
        Maximum time compliance check: 0.82884 milliseconds
        Minimum time compliance check: 0.00602 milliseconds
```