# The Vector OCS Domain Plugins
Welcome to the Vector OCS Domain Plugins repository.  

The source code published in this repository belongs to the One Click Startup Domain Plugins released in the 
MICROSAR Automation SDK. It gives full transparency regarding the implemented functionality of the OCS Domain Plugins.
Furthermore, it allows the extension of the existing functionality.

## Current Status of the OCS Domain Plugins
| Plugin             | Status Information                                                                                                                                                           |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Communication      | Legacy code to address features in the Communication Domain,<br>not optimized or refactored anyway                                                                           |
| Diagnostics        | Hexagonal architecture to achieve unit testable code within the Diagnostic Domain,<br>including an example for testing the business logic                                    |
| EcuStateManagement | Class design to reflect the handling of root features, features and parameters within the BSW Management Domain.                                                             |
| NvMemory           | Class design for MCAL abstraction via object-oriented programming,<br> but limited to MICROSAR and AURIX2G specific BSW modules.                                             |
| RuntimeSystem      | Class design to address dynamic scaling between SingleCore and MultiCore of the RuntimeSystem Domain,<br> depending on the OsPublishedInformation number of OsPhysicalCores. |
| Security           | 100% Kotlin based implementation of the Security Domain, including abstraction of the MDF specific APIs.                                                                     |
| Shared Library     | Reusable helper functionalities.                                                                                                                                             |

## Getting Started
The MICROSAR Automation SDK offers everything that is needed to build an OCS Custom App and the Domain Plugins. It
can be downloaded from the Vector Download Center in the 
[DaVinci Team](https://www.vector.com/de/de/support-downloads/download-center/#product=%5B%2251284%22%5D&tab=1&pageSize=15&sort=date&order=desc) product category.
Most important artifact which is required from the MICROSAR Automation SDK is the so called OCS Core. For more 
information we strongly recommend to read the contained documentation.

### Preparation
For simplification of the build setup we recommend as a first step to arrange the required files and folders as follows:  
```
├── BswPackage
|   └── MICROSAR_Classic_R31
|   |   └── Components
|   |   └── DaVinciConfigurator
|   |   └── ...
|   └── MICROSAR_Classic_R33
|   |   └── Components
|   |   └── DaVinciConfigurator
|   |   └── ...
├── ocs-custom-app
├── ocs-domain-plugins
└── repository
```
Within the BswPackage folder different MICROSAR Classic BSW Package releases can be stored. In this cases one for R31
and one for R33. The ocs-custom-app and the repository folder are both part of the MICROSAR Automation SDK. The
ocs-domain-plugins directory contains the content of this repository. Arranging the folder in this way will simplify the
customization of the gradle properties.

### Customization of the Gradle properties
Within the `ocs-domain-plugins` folder you can find the `gradle.properties` file:
```
#localSipStore=path/to/sip/store/directory
#localSip=path/to/used/sip/external
ocsRepo=../repository

vOcsCore=1.7.0
vPluginsCommon=1.5.0
vOcsCom=1.8.0
vOcsDiag=1.8.0
vOcsNvM=1.8.0
vOcsEcuState=1.8.0
vOcsRuntime=1.8.0
vOcsSecurity=1.6.0
```
Based on the folder structure example above the explanation of the properties becomes easier. 
Adapt the properties as following:
```
localSipStore=D:/dev/ocs/BswPackages
localSip=MICROSAR_Classic_R31
ocsRepo=../repository

vOcsCore=1.7.0
vPluginsCommon=my-company-1.5.0
vOcsCom=my-company-1.8.0
vOcsDiag=my-company-1.8.0
vOcsNvM=my-company-1.8.0
vOcsEcuState=my-company-1.8.0
vOcsRuntime=my-company-1.8.0
vOcsSecurity=my-company-1.6.0
```
The `localSipStore` should point to the root directory of the BSW Packages while the `localSip` itself mentions a 
dedicated BSW Package against which the source code will be compiled. The gradle project of the `ocs-custom-app` and
`ocs-domain-plugins` are prepared to handle the `ocsRepo` property as it is. If you want to adapt the storage location
of the ocsRepo additional adaptions within the gradle projects are perhaps necessary.

### Why should I use some company specific versions?
We kindly ask you to adapt the version information mentioned in the `gradle.properties`. Reason for this is to ensure
that the plugins you build by yourself can be distinguished from those delivered as pre-build artifacts in the MICROSAR
Automation SDK. Here an example for the resulting files:
```groovy
ocs-communication-plugin-1.7.0.jar            // without adaptions
ocs-communication-plugin-my-company-1.7.0.jar // with adaptions
```

### Build the OCS Domain Plugins
Next step after adapting the `gradle.properties` file is to build the gradle project. For simplification the build will
be described for the commandline terminal, but you can also use an IDE of your choice that supports gradle projects.
Switch into the `ocs-domain-plugins` directory and call the `.\gradlew build` command:
```batch
D:\dev\ocs\ocs-domain-plugins>.\gradlew build
```
The first build can take some time as additional gradle plugins are downloaded from the maven repository. You succeed when
the build states: `BUILD SUCCESSFUL`. Otherwise, you need to figure out what went wrong.

### Publish the OCS Domain Plugin jar files
After successfully building the jar files the next step is to publish the files to you local maven repository. 
Publishing the OCS Domain Plugins to your local maven repository simplifies the handling of the ocs-custom-app gradle
project. We will dive deeper into this part soon. For now, execute the following command:
```batch
D:\dev\ocs\ocs-domain-plugins>.\gradlew publishMavenPublicationToMavenLocalRepository
```
Now, the important part. The maven local repository is not equal to the `repository` mentioned in the folder structure 
above. Reason for this is a simple conceptional decision that this `repository` contains the artifacts 
delivered by Vector. In most cases, local maven repository which is used for the publication can be found under you
Windows user account, for example:
```batch
C:\Users\<your user account>\.m2\repository\com\vector\ocs\plugins\ocs-communication-plugin\my-company-1.7.0\ocs-communication-plugin-my-company-1.7.0.jar
```

## Building an OCS Custom App
### Customizing of the Gradle properties
This step will look quite familiar to you. In the root folder of the `ocs-custom-app` you find the `gradle.properties`
file: 
```
#localSipStore=path/to/sip/store/directory
#localSip=path/to/used/sip/
ocsRepo=../repository

vOcsCore=2.2.0
vPluginsCommon=1.5.0
vOcsCom=1.8.0
vOcsDiag=1.8.0
vOcsNvM=1.8.0
vOcsEcuState=1.8.0
vOcsRuntime=1.8.0
vOcsSecurity=1.6.0
```
The `localSipStore`, `localSip` and `ocsRepo` should be configured in the same way as for the `ocs-domain-plugins`.
If you keep the other version numbers as stated in the file you would build the OCS Custom App based on the Vector
pre-build artifacts. In fact, you want to use the plugins you built by yourself, therefore you also need to adapt those
properties, so that the result looks as follows:
```
localSipStore=D:/dev/ocs/BswPackages
localSip=MICROSAR_Classic_R31
ocsRepo=../repository

vOcsCore=1.7.0
vPluginsCommon=my-company-1.5.0
vOcsCom=my-company-1.8.0
vOcsDiag=my-company-1.8.0
vOcsNvM=my-company-1.8.0
vOcsEcuState=my-company-1.8.0
vOcsRuntime=my-company-1.8.0
vOcsSecurity=my-company-1.6.0
```
Please note that the gradle project of the OCS Custom App is prepared in a way that it handles the ocsRepo as well as
the local maven repository.

### Build the OCS Custom App
The steps are equal to the OCS Domain Plugins. This time go to the `ocs-custom-app` directory and call the `.\gradlew build` command:
```batch
D:\dev\ocs\ocs-custom-app>.\gradlew build
```
The first build can take some time as additional gradle plugins are downloaded from the maven repository. You succeed when
the build states: `BUILD SUCCESSFUL`. Otherwise, you need to figure out what went wrong.

### Executing the OCS Custom App
Details regarding the execution can be found in the delivered documentation.

## Dependencies
To compile the source code of the OCS Domain Plugins additional tools are required. 
Beside the mentioned dependencies the shared gradle project may point to additional gradle plugins.

| External Tool | Description |
|---------------|-------------|
| JAVA JDK      | 1.8         |
| Gradle        | 8.13        |

## Release Information
| Release | Belongs to                     | Supports MICROSAR Classic Releases | 
|---------|--------------------------------|------------------------------------|
| v1.6.0  | MICROSAR Automation SDK v1.5.0 | R31 - R34                          |
| v1.7.0  | MICROSAR Automation SDK v1.6.0 | R31 - R34                          |
| v1.8.0  | MICROSAR Automation SDK v1.7.0 | R31 - R34                          |

## Abbreviations
| Abbreviation | Description                       |
|--------------|-----------------------------------|
| BSW          | Basic Software                    |
| MCAL         | Microcontroller Abstraction Layer |
| OCS          | One Click Startup                 |
