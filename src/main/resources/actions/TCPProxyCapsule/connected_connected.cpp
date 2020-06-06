if({{ debug }}) log.log("[%s] got signal %s on port %s", this->getSlot()->name, msg->signal.getName(), msg->signal.getSrcPort()->getName());
char* json = NULL;
UMLRTJSONCoder::toJSON(msg, &json);
if({{ debug }}) log.log("[%s] sending message @%s", this->getSlot()->name, json);

if(json == NULL)
    log.log("[%s] error encoding signal", this->getSlot()->name);
else if(!tcp.send(json))
    log.log("[%s] error sending message", this->getSlot()->name);