if({{ debug }}) { printf("[FrameStub] incarnating capsule part %s\n", part->role()->name ); fflush(stdout); }

UMLRTCapsuleId capsuleId = framePort->incarnate(part);
if(capsuleId.isValid()) {
    char cmd[64], id[32];
    sprintf(id, "{{ ownerFQN }}-%s%d", part->role()->name, capsuleId.getCapsule()->getIndex());
    capsuleId.setId(strdup(id));

    sprintf(cmd, "kubectl scale deployments/%s --replicas=1", id);
    if({{ debug }}) { printf("[FrameStub] running command '%s'\n", cmd); fflush(stdout); }
    system(cmd);
}
return capsuleId;

