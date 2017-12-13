#!/bin/bash

mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=jar/CLS_Vespers_Data.jar -DgroupId=localjars -DartifactId=clsvespersdata -Dversion=1.0.0 -Dpackaging=jar -DlocalRepositoryPath=repo
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=jar/SS_Core_Model.jar    -DgroupId=localjars -DartifactId=sscoremodel    -Dversion=1.0.0 -Dpackaging=jar -DlocalRepositoryPath=repo
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=jar/SS_Data_Core.jar     -DgroupId=localjars -DartifactId=ssdatacore     -Dversion=1.0.0 -Dpackaging=jar -DlocalRepositoryPath=repo
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=jar/SS_Utilities.jar     -DgroupId=localjars -DartifactId=ssutilities    -Dversion=1.0.0 -Dpackaging=jar -DlocalRepositoryPath=repo
