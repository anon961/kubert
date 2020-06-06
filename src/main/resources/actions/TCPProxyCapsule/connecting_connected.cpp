if({{ debug }}) log.log("[%s] connected", this->getSlot()->name);

for(int i=0; i<{{ borderPortsCount }}; i++) {
    borderPorts[i]->recall();
}

for(int i=0; i<{{ internalPortsCount }}; i++) {
    if(internalPorts[i]->sap && !internalPorts[i]->automatic)
        UMLRTProtocol::registerSapPort(internalPorts[i], internalPorts[i]->registrationOverride);
    internalPorts[i]->recall();
}