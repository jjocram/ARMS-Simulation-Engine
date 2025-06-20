# ARMS
This is the simulation engine for ARMS: Activity-Resource Modelling Simulator

# How to run
1. Clone this repository
2. Run the webserver with: `./gradlew run`
3. Send a BPMN file to `http://127.0.0.1:8080/simulate`
   - You can use the modeler in [this repository](https://github.com/jjocram/ARMS-Editor)

# To deploy
Docker has to be running on your system
1. `az login`
2. `az acr login --name armscontainers`
3. `docker buildx build --platform linux/amd64 --push -t armscontainers.azurecr.io/arms-simulator:<version> .`