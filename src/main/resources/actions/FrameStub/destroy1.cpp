if({{ debug }}) { printf("[FrameStub] destroying capsule part %s\n", part->role()->name ); fflush(stdout); }

char cmd[64];
for(int i=0; i<part->numSlot; i++) {
    sprintf(cmd, "kubectl scale deployments/{{ ownerFQN }}-%s%d --replicas=0", part->role()->name, i);
    if({{ debug }}) { printf("[FrameStub] running command '%s'\n", cmd); fflush(stdout); }
    system(cmd);
}

return framePort->destroy(part);