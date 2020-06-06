char servicePortVar[64];
if({{ debug }}) log.log("[%s] service name is {{ serviceName }}", this->getSlot()->name);
sprintf(servicePortVar, "{{ serviceName }}_SERVICE_PORT_%s_%d", this->getName(), this->getIndex());

int i = 0;
while (servicePortVar[i]) {
    servicePortVar[i] = toupper(servicePortVar[i]);
    i++;
}

if(!getenv(servicePortVar)) {
    log.log("[%s] error resolving env variable %s", this->getSlot()->name, servicePortVar);
    return;
}

if({{ debug }}) log.log("[%s] port is %s", this->getSlot()->name, servicePortVar);
this->port = atoi(getenv(servicePortVar));

if({{ debug }}) log.log("[%s] listening on port %d", this->getSlot()->name, this->port);
server.listen(this->port);
server.accept();