# ARMS
This is the simulation engine for ARMS: Activity-Resource Modelling Simulator

# How to run
1. Clone this repository
2. Run the webserver with: `./gradlew run`
3. Send an BPMN file to `http://127.0.0.1:8080/simulate`
   - cURL command: `curl -X POST --data-binary "@file.bpmn" http://127.0.0.1:8080/simulate`