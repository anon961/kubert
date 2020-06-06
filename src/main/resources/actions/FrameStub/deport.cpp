if({{ debug }}) { printf("[FrameStub] deport called\n"); fflush(stdout); }

if(id.isValid()) {
    char cmd[1024];
    sprintf(cmd, "kubectl scale deployments/{{ ownerFQN }}-%s%d --replicas=0", part->role()->name, id.getCapsule()->getIndex());

    if({{ debug }}) { printf("[FrameStub] running command '%s'\n", cmd); fflush(stdout); }
    system(cmd);

    return framePort->destroy(id);
}
return false;