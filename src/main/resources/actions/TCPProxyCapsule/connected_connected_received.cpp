if({{ debug }}) log.log("[%s] got message %s", this->getSlot()->name, payload);
UMLRTOutSignal signal;
int destPortIdx;
UMLRTJSONCoder::fromJSON(payload, signal, getSlot(), &destPortIdx);
if({{ debug }}) log.log("[%s] decoded signal %s", this->getSlot()->name, signal.getName());
signal.send();