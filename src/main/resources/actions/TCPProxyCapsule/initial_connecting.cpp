const char * serviceName;
{% for entry in serviceNames %}
if(strcmp(this->getName(), "{{ entry.partName }}") == 0
    && this->getIndex() == {{ entry.partIndex }})
    serviceName = "{{ entry.serviceName }}";
{% endfor %}

const char * portName = "{{ portName }}";
if({{ debug }}) log.log("[%s] service name is %s", this->getSlot()->name, serviceName);
if({{ debug }}) log.log("[%s] portName name is %s", this->getSlot()->name, portName);

char serviceHostVar[64];
sprintf(serviceHostVar, "%s_SERVICE_HOST", serviceName, this->getIndex());
if({{ debug }}) log.log("[%s] host name is %s", this->getSlot()->name, serviceHostVar);

char servicePortVar[64];
sprintf(servicePortVar, "%s_SERVICE_PORT_%s", serviceName, portName);
if({{ debug }}) log.log("[%s] port is %s", this->getSlot()->name, servicePortVar);

if(!getenv(serviceHostVar)) {
    log.log("[%s] error resolving env variable %s", this->getSlot()->name, serviceHostVar);
    return;
}

if(!getenv(servicePortVar)) {
    log.log("[%s] error resolving env variable %s", this->getSlot()->name, servicePortVar);
    return;
}

this->hostname = getenv(serviceHostVar);
this->port = atoi(getenv(servicePortVar));
if({{ debug }}) log.log("[%s] connecting to service @%s:%d", this->getSlot()->name, this->hostname, this->port);
tcp.connect(this->hostname, this->port);