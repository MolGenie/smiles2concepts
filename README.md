# Smiles2concepts
This software provides a classification service for small organic molecules.

The open access CDK/Ambit and OpenChemLib library was used for compound structure classification based on a MolGenie Organic Chemistry Ontology (MOCO) in the OBO 1.2 format. Please note that most ontology editors (like OBOEdit or Protégé) are not able to read this format in full extent. MOCO contains 114965 concept nodes, of which 106487 are leaf nodes. 2497 nodes contain SMARTS definitions, 104186 nodes contain ID-Codes that are used to classify the compound ring system structure(s).

The output is given as a JSON formatted response using the input SMILES string and the detected chemical classes for the given input molecule.

Molecule2structure is designed as a client-server solution. It can run either locally on a single machine or on a server that is accessible within a network.

# How to build
Create the Smiles2concepts web service as Maven project. The Maven command 
`mvn package assembly:single

creates the target/smiles2concepts-distribution.tar.gz archive which contains the application, resources, configurations and shell-scripts to run the application.

# How to use
Extract molecule2concepts-distribution.tar.gz.

smiles2concepts/bin contains executables to run: startSmiles2concepts.sh starts application as a server 

stopSmiles2concepts.sh stops running the server 

smiles2conceptsWithCurl.sh executes requests on running server smiles2concepts.sh run the application to execute single action

smiles2concepts/config contains the configuration of the application
After service starts up, the following folders appear: /bin /service /logs

The service PID found in smiles2concepts/run can be used to stop the service.

## HTTP request
The request URL path is /classify thus a complete request URL would be http://SERVER:PORT/smiles2concepts/classify/REQUEST with SERVER set to the server address where the service runs and listens at given PORT (set to 9141 by default), and REQUEST set one of the possible requests below. The JSON formatted request data has to be provided in the content of the POST request: 
`{"smiles":"c1ccccc1"}`

If processing was successful the response given by the service is a JSON-formatted string in the following form: 
`{ "smiles" : "c1ccccc1",  "classifier": "MolGenie Ambit+OpenChemLib Classifier v1.0", "classifications" : OUTCOME }`

## CURL request
E.g. using the ./smiles2conceptsWithCurl.sh in the projects /bin directory with smiles server port:
`./smiles2conceptsWithCurl.sh c1ccccc1 localhost 9141

## Java program
E.g. using the smiles2concepts.jar in the projects upper directory:
`java -jar target/smiles2concepts.jar -i "c1cccc1"`  

please note that this is inefficient as the ontology will be loaded each time the program is called.

As an example, the smiles c1ccccc1 for benzene shall return: 
`{ "smiles": "c1ccccc1", "classifier": "MolGenie Ambit+OpenChemLib Classifier v1.0", "classifications": [ { "classID": "MGN110000003", "name": "n-cyclic compounds" }, { "classID": "MGN100001453", "name": "scaffolds" }, { "classID": "MGN100003363", "name": "ring systems" }, { "classID": "MGN310000001", "name": "benzene" }, { "classID": "MGN100000604", "name": "aromatic compounds" }, { "classID": "MGN100000147", "name": "cyclic compounds" }, { "classID": "MGN100000302", "name": "organic compounds" }, { "classID": "MGN200000011", "name": "monocyclic compounds" } ] }`

It is also possible to process SDF files or gzipped SDF files, the output is to stdout , which can be redirected to a suitable result file:
`java -jar target/smiles2concepts.jar -sdf test.sdf.gz > test.json`

The output is in the form of JSON-LD lines, including also the SDFs compound identifier. 
`{"cid": "131000163", "smiles": "O[C@@H](CC1)C#CCCCCC#C[C@H]1O", "classifications": [ { "classID": "MGN100000585", "className": "oxygen containing functional groups" }, { "classID": "MGN110000001", "className": "macrocyclic ring systems" }, { "classID": "MGN100001926", "className": "alcohols" }, { "classID": "MGN100003154", "className": "diols" }, { "classID": "MGN100000096", "className": "chiral compounds" }, { "classID": "MGN100003043", "className": "secondary alcohols" }, { "classID": "MGN100003160", "className": "polyalcohols" }, { "classID": "MGN100000147", "className": "cyclic compounds" }, { "classID": "MGN100000113", "className": "aliphatic compounds" }, { "classID": "MGN100000302", "className": "organic compounds" }]}`
    
If an error occurred, response takes the following form: 
`{ "processState" : { "errorMsg" : "ERROR_MESSAGE", "errorCode" : ERROR_CODE, "processed" : false } }` 

If no input smiles wsas found, the status code is 100. If the input smiles is invalid, the status code is 200. If the request was processed successfully it returns with status code 300. If the server is not running the code is 000. For other processing errors it returns with code 500.


# Settings
Default settings can be modified using the config/classify.properties file. For example: 

timeout= 600 

baseApiPath= /smiles2concepts 

port= 9141 

maxThreads= 10 

baseApiPath=/smiles2concepts 

module= ambit

ontologyFilename= ./src/main/resources/mol_classes_2025-07-18.obo

writeLeafsOnly= true

logLevel= Level.ERROR

the writeLeafsOnly=true option writes only the direct class parents of a compound smiles, false will output all classes the compound is member of.# smiles2concepts
