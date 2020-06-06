if({{ debug }}) log.log("[%s] connection error", this->getSlot()->name);

for(int i=0; i<{{ internalPortsCount }}; i++) {
    if(internalPorts[i]->spp && !internalPorts[i]->automatic)
        UMLRTProtocol::deregisterSppPort(internalPorts[i]);
}

timer.informIn(UMLRTTimespec(1,0));