/*const CapsuleIdStub & stub = static_cast<const CapsuleIdStub&>(id);*/
if({{ debug }}) {
    printf("[FrameStub] destroying capsule in slot %s\n", id.getCapsule()->getSlot()->name );
    fflush(stdout);
}

char cmd[64];
sprintf(cmd, "kubectl scale deployments/{{ ownerFQN }}-%s%d --replicas=0",
    id.getCapsule()->getSlot()->role()->name, id.getCapsule()->getSlot()->capsuleIndex);
if({{ debug }}) { printf("[FrameStub] running command '%s'\n", cmd); fflush(stdout); }
system(cmd);

return framePort->destroy(id);